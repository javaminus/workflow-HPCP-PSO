package com.javaminus.workflow.rbdas;

import cloud.workflowScheduling.setting.Task;
import cloud.workflowScheduling.setting.Workflow;
import com.javaminus.workflow.rbdas.model.ResourceProfile;
import com.javaminus.workflow.rbdas.model.VmType;
import com.javaminus.workflow.rbdas.model.WorkloadType;
import com.javaminus.workflow.rbdas.policy.AffinityVmAllocationPolicy;
import com.javaminus.workflow.rbdas.scheduler.HNSPSOAdapter;
import com.javaminus.workflow.rbdas.simulator.SpotInterruptSimulator;
import com.javaminus.workflow.rbdas.util.AffinityTable;
import com.javaminus.workflow.rbdas.util.VmCatalog;

import java.io.IOException;
import java.util.*;

/**
 * Affinity Datacenter Broker orchestrating the RBDAS pipeline:
 * profile -> classify -> pack -> request -> HNSPSO mapping -> submit
 */
public class AffinityDatacenterBroker {
    private Workflow workflow;
    private VmCatalog vmCatalog;
    private AffinityTable affinityTable;
    private AffinityVmAllocationPolicy allocationPolicy;
    private ResourceProfiler profiler;
    private RuleBasedClassifier classifier;
    private SpotInterruptSimulator spotSimulator;
    private long randomSeed;
    private boolean spotEnabled;

    /**
     * Execution result
     */
    public static class ExecutionResult {
        public double totalCost;
        public double makespan;
        public int vmCount;
        public double avgResourceUtilization;
        public int taskSuccessCount;
        public int taskFailureCount;
        public int interruptionCount;
        public Map<String, Object> metrics;

        public ExecutionResult() {
            metrics = new HashMap<>();
        }

        @Override
        public String toString() {
            return String.format("ExecutionResult{totalCost=%.2f, makespan=%.2f, vmCount=%d, " +
                               "avgUtil=%.2f, success=%d, failure=%d, interruptions=%d}",
                               totalCost, makespan, vmCount, avgResourceUtilization,
                               taskSuccessCount, taskFailureCount, interruptionCount);
        }
    }

    public AffinityDatacenterBroker(String vmCatalogPath, String affinityTablePath, 
                                   long randomSeed, boolean spotEnabled) throws IOException {
        this.vmCatalog = VmCatalog.loadFromFile(vmCatalogPath);
        this.affinityTable = AffinityTable.loadFromFile(affinityTablePath);
        this.allocationPolicy = new AffinityVmAllocationPolicy();
        this.profiler = new ResourceProfiler();
        this.classifier = new RuleBasedClassifier();
        this.spotSimulator = new SpotInterruptSimulator(randomSeed, 0.1); // 10% interruption rate
        this.randomSeed = randomSeed;
        this.spotEnabled = spotEnabled;
        
        initializePools();
    }

    /**
     * Initialize VM pools
     */
    private void initializePools() {
        List<VmType> allVmTypes = vmCatalog.getVmTypes();
        
        // Pre-reserved: 2 of each type
        allocationPolicy.initializePreReservedPool(allVmTypes, 2);
        
        // Spot: 5 of each type (if enabled)
        if (spotEnabled) {
            allocationPolicy.initializeSpotPool(allVmTypes, 5);
        }
        
        // On-demand: unlimited (initialized with catalog)
        allocationPolicy.initializeOnDemandPool(allVmTypes);
    }

    /**
     * Execute workflow with RBDAS
     */
    public ExecutionResult executeWorkflow(Workflow workflow, double deadline) {
        this.workflow = workflow;
        
        // Step 1: Profile tasks
        System.out.println("Step 1: Profiling workflow tasks...");
        profiler.profileWorkflow(workflow);
        Map<Task, ResourceProfile> profiles = profiler.getAllProfiles();

        // Step 2: Classify workloads
        System.out.println("Step 2: Classifying workloads...");
        for (Map.Entry<Task, ResourceProfile> entry : profiles.entrySet()) {
            ResourceProfile profile = entry.getValue();
            WorkloadType type = classifier.classify(profile);
            profile.setClassifiedType(type);
        }

        // Step 3: Pack tasks using A2MDBFD
        System.out.println("Step 3: Packing tasks with A2MDBFD...");
        List<VmType> preReserved = getVmTypesFromPool("reserved");
        List<VmType> spot = spotEnabled ? getVmTypesFromPool("spot") : new ArrayList<>();
        List<VmType> onDemand = getVmTypesFromPool("on_demand");
        
        A2MDBFD packer = new A2MDBFD(affinityTable, preReserved, spot, onDemand);
        List<A2MDBFD.VmAllocation> initialPacking = packer.pack(new ArrayList<>(workflow), profiles);

        // Step 4: HNSPSO mapping for optimization
        System.out.println("Step 4: Running HNSPSO optimization...");
        List<VmType> availableVms = vmCatalog.getVmTypes();
        HNSPSOAdapter psoAdapter = new HNSPSOAdapter(workflow, availableVms, profiles, 
                                                     affinityTable, deadline, randomSeed);
        Map<Task, VmType> optimalMapping = psoAdapter.findOptimalMapping();

        // Step 5: Allocate VMs and execute
        System.out.println("Step 5: Allocating VMs and executing workflow...");
        ExecutionResult result = executeWithMapping(optimalMapping, profiles);

        return result;
    }

    /**
     * Get VM types from allocation policy pools
     */
    private List<VmType> getVmTypesFromPool(String poolType) {
        // Return unique VM types available in the catalog
        return vmCatalog.getVmTypes();
    }

    /**
     * Execute workflow with given task-VM mapping
     */
    private ExecutionResult executeWithMapping(Map<Task, VmType> mapping, 
                                              Map<Task, ResourceProfile> profiles) {
        ExecutionResult result = new ExecutionResult();
        double currentTime = 0.0;
        Set<String> allocatedVmIds = new HashSet<>();
        
        for (Map.Entry<Task, VmType> entry : mapping.entrySet()) {
            Task task = entry.getKey();
            VmType vmType = entry.getValue();
            
            // Allocate VM
            AffinityVmAllocationPolicy.VmInstance vm = 
                allocationPolicy.allocateVm(vmType, spotEnabled ? "spot" : "reserved");
            
            if (vm != null) {
                allocatedVmIds.add(vm.getId());
                
                // Simulate task execution
                double executionTime = task.getTaskSize() / (vmType.getVcpus() * 1000.0);
                double taskCost = CostModel.computeVmCost(vmType, executionTime, vm.getPricingModel());
                
                result.totalCost += taskCost;
                currentTime += executionTime;
                
                // Simulate spot interruption
                if (spotEnabled && "spot".equals(vm.getPricingModel()) && 
                    spotSimulator.shouldInterrupt()) {
                    result.interruptionCount++;
                    // Create checkpoint
                    spotSimulator.createCheckpoint(String.valueOf(task.getId()), 
                                                  executionTime * 0.7, executionTime, null);
                }
                
                result.taskSuccessCount++;
                
                // Release VM after task completion
                allocationPolicy.releaseVm(vm.getId());
            } else {
                result.taskFailureCount++;
            }
        }

        result.makespan = currentTime;
        result.vmCount = allocatedVmIds.size();
        result.avgResourceUtilization = 0.7; // Simplified
        
        // Add detailed metrics
        result.metrics.put("checkpoints_created", spotSimulator.getCheckpointCount());
        result.metrics.putAll(allocationPolicy.getPoolStatistics());

        return result;
    }

    /**
     * Set custom classifier thresholds
     */
    public void setClassifierThresholds(double cpuThreshold, double memThreshold,
                                       double ioThreshold, double netThreshold) {
        this.classifier = new RuleBasedClassifier(cpuThreshold, memThreshold, 
                                                  ioThreshold, netThreshold);
    }

    /**
     * Get allocation policy
     */
    public AffinityVmAllocationPolicy getAllocationPolicy() {
        return allocationPolicy;
    }

    /**
     * Get profiler
     */
    public ResourceProfiler getProfiler() {
        return profiler;
    }
}
