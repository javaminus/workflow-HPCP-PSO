package cloud.workflowScheduling.rbdas;

import cloud.workflowScheduling.setting.Task;
import cloud.workflowScheduling.setting.VM;
import cloud.workflowScheduling.setting.Workflow;
import cloud.workflowScheduling.rbdas.AffinityTable.TaskType;
import cloud.workflowScheduling.rbdas.ResourceProfiler.TaskProfile;
import java.util.*;

/**
 * Affinity-Aware Multi-Dimensional Best-Fit Decreasing (A2MDBFD) packer.
 * Implements pre-reserved pool fill, spot-then-on-demand logic.
 */
public class A2MDBFD {
    
    /**
     * VM allocation entry with affinity information.
     */
    public static class VMAllocation {
        public VM vm;
        public TaskType taskType;
        public double affinityScore;
        public List<Task> allocatedTasks;
        public boolean isSpot;
        public boolean isPreReserved;
        
        public VMAllocation(VM vm, TaskType taskType, double affinityScore) {
            this.vm = vm;
            this.taskType = taskType;
            this.affinityScore = affinityScore;
            this.allocatedTasks = new ArrayList<>();
            this.isSpot = false;
            this.isPreReserved = false;
        }
    }
    
    private Workflow workflow;
    private ResourceProfiler profiler;
    private RuleBasedClassifier classifier;
    private AffinityTable affinityTable;
    private Map<Integer, TaskType> taskClassifications;
    
    // Configuration
    private int preReservedPoolSize;
    private double spotUsageRatio;  // Ratio of VMs to allocate as spot
    
    public A2MDBFD(Workflow workflow, int preReservedPoolSize, double spotUsageRatio) {
        this.workflow = workflow;
        this.preReservedPoolSize = preReservedPoolSize;
        this.spotUsageRatio = Math.max(0.0, Math.min(1.0, spotUsageRatio));
        
        this.profiler = new ResourceProfiler(workflow);
        this.classifier = new RuleBasedClassifier();
        this.affinityTable = AffinityTable.getInstance();
        this.taskClassifications = new HashMap<>();
        
        classifyAllTasks();
    }
    
    /**
     * Classify all tasks in the workflow.
     */
    private void classifyAllTasks() {
        for (Task task : workflow) {
            TaskProfile profile = profiler.getProfile(task);
            TaskType type = RuleBasedClassifier.classify(profile);
            taskClassifications.put(task.getId(), type);
        }
    }
    
    /**
     * Get task classification.
     */
    public TaskType getTaskType(Task task) {
        return taskClassifications.getOrDefault(task.getId(), TaskType.MIX);
    }
    
    /**
     * Pack tasks into VMs using affinity-aware best-fit strategy.
     * Returns a list of VM allocations sorted by priority (pre-reserved, spot, on-demand).
     * 
     * @param taskList List of tasks to pack (should be sorted by priority)
     * @return List of VM allocations
     */
    public List<VMAllocation> packTasks(List<Task> taskList) {
        List<VMAllocation> allocations = new ArrayList<>();
        
        // Create pre-reserved pool with best VM types for common task types
        List<VMAllocation> preReservedPool = createPreReservedPool();
        allocations.addAll(preReservedPool);
        
        // Try to fit tasks into pre-reserved VMs first
        List<Task> remainingTasks = new ArrayList<>(taskList);
        for (Task task : taskList) {
            TaskType taskType = getTaskType(task);
            VMAllocation bestFit = findBestFitVM(task, taskType, preReservedPool);
            
            if (bestFit != null) {
                bestFit.allocatedTasks.add(task);
                remainingTasks.remove(task);
            }
        }
        
        // Allocate remaining tasks to spot or on-demand VMs
        int spotCount = (int) Math.ceil(remainingTasks.size() * spotUsageRatio);
        
        for (int i = 0; i < remainingTasks.size(); i++) {
            Task task = remainingTasks.get(i);
            TaskType taskType = getTaskType(task);
            
            // Determine VM type with best affinity
            int vmType = affinityTable.getBestVMType(taskType);
            VM vm = new VM(vmType);
            
            double affinity = affinityTable.getAffinity(taskType, vmType);
            VMAllocation allocation = new VMAllocation(vm, taskType, affinity);
            allocation.allocatedTasks.add(task);
            
            // First N tasks go to spot instances
            if (i < spotCount) {
                allocation.isSpot = true;
            }
            
            allocations.add(allocation);
        }
        
        return allocations;
    }
    
    /**
     * Create pre-reserved VM pool with diverse VM types.
     */
    private List<VMAllocation> createPreReservedPool() {
        List<VMAllocation> pool = new ArrayList<>();
        
        // Distribute pre-reserved VMs across task types
        TaskType[] taskTypes = TaskType.values();
        int vmsPerType = Math.max(1, preReservedPoolSize / taskTypes.length);
        
        for (TaskType taskType : taskTypes) {
            int bestVMType = affinityTable.getBestVMType(taskType);
            
            for (int i = 0; i < vmsPerType && pool.size() < preReservedPoolSize; i++) {
                VM vm = new VM(bestVMType);
                double affinity = affinityTable.getAffinity(taskType, bestVMType);
                VMAllocation allocation = new VMAllocation(vm, taskType, affinity);
                allocation.isPreReserved = true;
                pool.add(allocation);
            }
        }
        
        return pool;
    }
    
    /**
     * Find best-fit VM from available pool for a task.
     * Uses multi-dimensional fit considering affinity and capacity.
     */
    private VMAllocation findBestFitVM(Task task, TaskType taskType, List<VMAllocation> pool) {
        VMAllocation bestFit = null;
        double bestScore = -1.0;
        
        for (VMAllocation allocation : pool) {
            // Calculate affinity between task type and VM
            double affinity = affinityTable.getAffinity(taskType, allocation.vm.getType());
            
            // Calculate capacity utilization (simple heuristic: avoid overloading)
            double utilizationPenalty = allocation.allocatedTasks.size() * 0.1;
            
            // Combined score: higher affinity, lower utilization is better
            double score = affinity - utilizationPenalty;
            
            if (score > bestScore) {
                bestScore = score;
                bestFit = allocation;
            }
        }
        
        // Only return if score is acceptable (affinity > 0.5)
        return (bestScore > 0.5) ? bestFit : null;
    }
    
    /**
     * Calculate fragmentation penalty for the allocation.
     * Lower is better. Based on variance in VM utilization.
     */
    public double calculateFragmentationPenalty(List<VMAllocation> allocations) {
        if (allocations.isEmpty()) {
            return 0.0;
        }
        
        // Calculate mean utilization
        double sum = 0.0;
        for (VMAllocation alloc : allocations) {
            sum += alloc.allocatedTasks.size();
        }
        double mean = sum / allocations.size();
        
        // Calculate variance
        double variance = 0.0;
        for (VMAllocation alloc : allocations) {
            double diff = alloc.allocatedTasks.size() - mean;
            variance += diff * diff;
        }
        variance /= allocations.size();
        
        // Return standard deviation as fragmentation penalty
        return Math.sqrt(variance);
    }
}
