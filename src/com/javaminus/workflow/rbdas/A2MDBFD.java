package com.javaminus.workflow.rbdas;

import cloud.workflowScheduling.setting.Task;
import com.javaminus.workflow.rbdas.model.ResourceProfile;
import com.javaminus.workflow.rbdas.model.VmType;
import com.javaminus.workflow.rbdas.model.WorkloadType;
import com.javaminus.workflow.rbdas.util.AffinityTable;

import java.util.*;

/**
 * Affinity-Aware Multi-Dimensional Best-Fit Decreasing packer
 * Packs tasks into VMs considering affinity and resource dimensions
 */
public class A2MDBFD {
    private AffinityTable affinityTable;
    private List<VmType> preReservedPool;
    private List<VmType> spotPool;
    private List<VmType> onDemandPool;

    public static class VmAllocation {
        public VmType vmType;
        public String pricingModel; // "reserved", "spot", "on_demand"
        public List<Task> tasks;
        public double cpuUsed;
        public double memUsed;
        public double storageUsed;

        public VmAllocation(VmType vmType, String pricingModel) {
            this.vmType = vmType;
            this.pricingModel = pricingModel;
            this.tasks = new ArrayList<>();
            this.cpuUsed = 0.0;
            this.memUsed = 0.0;
            this.storageUsed = 0.0;
        }

        public double getCpuUtilization() {
            return vmType.getVcpus() > 0 ? cpuUsed / vmType.getVcpus() : 0.0;
        }

        public double getMemUtilization() {
            return vmType.getMemoryGb() > 0 ? memUsed / vmType.getMemoryGb() : 0.0;
        }

        public double getStorageUtilization() {
            return vmType.getStorageGb() > 0 ? storageUsed / vmType.getStorageGb() : 0.0;
        }
    }

    public A2MDBFD(AffinityTable affinityTable, List<VmType> preReservedPool,
                   List<VmType> spotPool, List<VmType> onDemandPool) {
        this.affinityTable = affinityTable;
        this.preReservedPool = preReservedPool != null ? preReservedPool : new ArrayList<>();
        this.spotPool = spotPool != null ? spotPool : new ArrayList<>();
        this.onDemandPool = onDemandPool != null ? onDemandPool : new ArrayList<>();
    }

    /**
     * Pack tasks into VMs using affinity-aware multi-dimensional BFD
     */
    public List<VmAllocation> pack(List<Task> tasks, Map<Task, ResourceProfile> profiles) {
        List<VmAllocation> allocations = new ArrayList<>();
        
        // Sort tasks by resource requirements (descending)
        List<Task> sortedTasks = new ArrayList<>(tasks);
        sortedTasks.sort((t1, t2) -> {
            ResourceProfile p1 = profiles.get(t1);
            ResourceProfile p2 = profiles.get(t2);
            double score1 = p1.getCpuIntensity() + p1.getMemIntensity() + p1.getIoIntensity();
            double score2 = p2.getCpuIntensity() + p2.getMemIntensity() + p2.getIoIntensity();
            return Double.compare(score2, score1);
        });

        // Try to pack each task
        for (Task task : sortedTasks) {
            ResourceProfile profile = profiles.get(task);
            WorkloadType workloadType = profile.getClassifiedType();
            
            // Try to find best-fit VM in existing allocations
            VmAllocation bestFit = findBestFit(allocations, task, profile, workloadType);
            
            if (bestFit != null) {
                // Add to existing VM
                addTaskToAllocation(bestFit, task, profile);
            } else {
                // Need new VM - try pools in order: reserved -> spot -> on-demand
                VmAllocation newAlloc = allocateNewVm(task, profile, workloadType);
                if (newAlloc != null) {
                    allocations.add(newAlloc);
                    addTaskToAllocation(newAlloc, task, profile);
                }
            }
        }

        return allocations;
    }

    /**
     * Find best-fit existing VM allocation for a task
     */
    private VmAllocation findBestFit(List<VmAllocation> allocations, Task task,
                                    ResourceProfile profile, WorkloadType workloadType) {
        VmAllocation bestFit = null;
        double bestScore = Double.MAX_VALUE;

        for (VmAllocation alloc : allocations) {
            if (!canFit(alloc, profile)) {
                continue;
            }

            // Calculate fit score (lower is better)
            double affinityScore = affinityTable.getAffinityScore(workloadType, alloc.vmType.getFamily());
            double utilScore = (alloc.getCpuUtilization() + alloc.getMemUtilization() + 
                              alloc.getStorageUtilization()) / 3.0;
            
            // Prefer high affinity and high utilization (packing efficiency)
            double fitScore = (1.0 - affinityScore) + (1.0 - utilScore);

            if (fitScore < bestScore) {
                bestScore = fitScore;
                bestFit = alloc;
            }
        }

        return bestFit;
    }

    /**
     * Check if task can fit in VM allocation
     */
    private boolean canFit(VmAllocation alloc, ResourceProfile profile) {
        // Simplified fit check based on estimated resource usage
        double cpuNeeded = profile.getCpuIntensity() / 1000.0; // Normalize
        double memNeeded = profile.getMemIntensity() / 1024.0; // Convert to GB
        double storageNeeded = profile.getDataSize() / (1024.0 * 1024.0); // Convert to GB

        return (alloc.cpuUsed + cpuNeeded <= alloc.vmType.getVcpus()) &&
               (alloc.memUsed + memNeeded <= alloc.vmType.getMemoryGb()) &&
               (alloc.storageUsed + storageNeeded <= alloc.vmType.getStorageGb());
    }

    /**
     * Add task to VM allocation
     */
    private void addTaskToAllocation(VmAllocation alloc, Task task, ResourceProfile profile) {
        alloc.tasks.add(task);
        alloc.cpuUsed += profile.getCpuIntensity() / 1000.0;
        alloc.memUsed += profile.getMemIntensity() / 1024.0;
        alloc.storageUsed += profile.getDataSize() / (1024.0 * 1024.0);
    }

    /**
     * Allocate a new VM from pools
     */
    private VmAllocation allocateNewVm(Task task, ResourceProfile profile, WorkloadType workloadType) {
        // Try reserved pool first
        VmType vmType = selectBestVmType(preReservedPool, workloadType);
        if (vmType != null) {
            return new VmAllocation(vmType, "reserved");
        }

        // Try spot pool
        vmType = selectBestVmType(spotPool, workloadType);
        if (vmType != null) {
            return new VmAllocation(vmType, "spot");
        }

        // Fall back to on-demand pool
        vmType = selectBestVmType(onDemandPool, workloadType);
        if (vmType != null) {
            return new VmAllocation(vmType, "on_demand");
        }

        return null;
    }

    /**
     * Select best VM type from pool based on affinity
     */
    private VmType selectBestVmType(List<VmType> pool, WorkloadType workloadType) {
        if (pool.isEmpty()) {
            return null;
        }

        VmType bestVm = null;
        double bestAffinity = -1.0;

        for (VmType vmType : pool) {
            double affinity = affinityTable.getAffinityScore(workloadType, vmType.getFamily());
            if (affinity > bestAffinity) {
                bestAffinity = affinity;
                bestVm = vmType;
            }
        }

        return bestVm;
    }
}
