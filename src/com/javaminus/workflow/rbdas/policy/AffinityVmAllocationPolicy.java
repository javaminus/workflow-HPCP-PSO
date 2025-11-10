package com.javaminus.workflow.rbdas.policy;

import com.javaminus.workflow.rbdas.model.VmType;

import java.util.*;

/**
 * VM Allocation Policy managing pre-reserved, spot, and on-demand pools
 */
public class AffinityVmAllocationPolicy {
    private List<VmInstance> preReservedPool;
    private List<VmInstance> spotPool;
    private List<VmInstance> onDemandPool;
    private Map<String, VmInstance> allocatedVms;
    private int nextVmId;

    /**
     * VM Instance representation
     */
    public static class VmInstance {
        private String id;
        private VmType vmType;
        private String pricingModel;
        private boolean allocated;
        private double startTime;
        private double endTime;

        public VmInstance(String id, VmType vmType, String pricingModel) {
            this.id = id;
            this.vmType = vmType;
            this.pricingModel = pricingModel;
            this.allocated = false;
            this.startTime = 0.0;
            this.endTime = 0.0;
        }

        // Getters and setters
        public String getId() { return id; }
        public VmType getVmType() { return vmType; }
        public String getPricingModel() { return pricingModel; }
        public boolean isAllocated() { return allocated; }
        public void setAllocated(boolean allocated) { this.allocated = allocated; }
        public double getStartTime() { return startTime; }
        public void setStartTime(double startTime) { this.startTime = startTime; }
        public double getEndTime() { return endTime; }
        public void setEndTime(double endTime) { this.endTime = endTime; }
    }

    public AffinityVmAllocationPolicy() {
        this.preReservedPool = new ArrayList<>();
        this.spotPool = new ArrayList<>();
        this.onDemandPool = new ArrayList<>();
        this.allocatedVms = new HashMap<>();
        this.nextVmId = 0;
    }

    /**
     * Initialize pre-reserved pool
     */
    public void initializePreReservedPool(List<VmType> vmTypes, int countPerType) {
        for (VmType vmType : vmTypes) {
            for (int i = 0; i < countPerType; i++) {
                VmInstance vm = new VmInstance("reserved-" + nextVmId++, vmType, "reserved");
                preReservedPool.add(vm);
            }
        }
    }

    /**
     * Initialize spot pool
     */
    public void initializeSpotPool(List<VmType> vmTypes, int countPerType) {
        for (VmType vmType : vmTypes) {
            for (int i = 0; i < countPerType; i++) {
                VmInstance vm = new VmInstance("spot-" + nextVmId++, vmType, "spot");
                spotPool.add(vm);
            }
        }
    }

    /**
     * Initialize on-demand pool (unlimited capacity)
     */
    public void initializeOnDemandPool(List<VmType> vmTypes) {
        for (VmType vmType : vmTypes) {
            // Pre-create some instances, but can create more on demand
            VmInstance vm = new VmInstance("ondemand-" + nextVmId++, vmType, "on_demand");
            onDemandPool.add(vm);
        }
    }

    /**
     * Allocate VM from pools (try reserved -> spot -> on-demand)
     */
    public VmInstance allocateVm(VmType desiredVmType, String preferredPricing) {
        VmInstance vm = null;

        // Try preferred pricing first
        if ("reserved".equals(preferredPricing)) {
            vm = allocateFromPool(preReservedPool, desiredVmType);
        } else if ("spot".equals(preferredPricing)) {
            vm = allocateFromPool(spotPool, desiredVmType);
        }

        // Fallback to other pools
        if (vm == null) {
            vm = allocateFromPool(preReservedPool, desiredVmType);
        }
        if (vm == null) {
            vm = allocateFromPool(spotPool, desiredVmType);
        }
        if (vm == null) {
            vm = allocateFromPool(onDemandPool, desiredVmType);
            
            // Create new on-demand instance if none available
            if (vm == null) {
                vm = new VmInstance("ondemand-" + nextVmId++, desiredVmType, "on_demand");
                onDemandPool.add(vm);
                vm.setAllocated(true);
            }
        }

        if (vm != null) {
            allocatedVms.put(vm.getId(), vm);
        }

        return vm;
    }

    /**
     * Allocate from specific pool
     */
    private VmInstance allocateFromPool(List<VmInstance> pool, VmType desiredVmType) {
        for (VmInstance vm : pool) {
            if (!vm.isAllocated() && vm.getVmType().getId().equals(desiredVmType.getId())) {
                vm.setAllocated(true);
                return vm;
            }
        }
        return null;
    }

    /**
     * Release VM back to pool
     */
    public void releaseVm(String vmId) {
        VmInstance vm = allocatedVms.get(vmId);
        if (vm != null) {
            vm.setAllocated(false);
            allocatedVms.remove(vmId);
        }
    }

    /**
     * Get all allocated VMs
     */
    public Collection<VmInstance> getAllocatedVms() {
        return allocatedVms.values();
    }

    /**
     * Get pool statistics
     */
    public Map<String, Integer> getPoolStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("reserved_total", preReservedPool.size());
        stats.put("reserved_allocated", (int) preReservedPool.stream().filter(VmInstance::isAllocated).count());
        stats.put("spot_total", spotPool.size());
        stats.put("spot_allocated", (int) spotPool.stream().filter(VmInstance::isAllocated).count());
        stats.put("ondemand_total", onDemandPool.size());
        stats.put("ondemand_allocated", (int) onDemandPool.stream().filter(VmInstance::isAllocated).count());
        return stats;
    }
}
