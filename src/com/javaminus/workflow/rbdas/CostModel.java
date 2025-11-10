package com.javaminus.workflow.rbdas;

import com.javaminus.workflow.rbdas.model.VmType;

/**
 * Cost model for computing VM and data transfer costs
 */
public class CostModel {
    private static final double BILLING_INTERVAL_HOURS = 1.0;
    
    /**
     * Compute cost for VM usage
     */
    public static double computeVmCost(VmType vmType, double usageTimeSeconds, String pricingModel) {
        double hourlyRate = getHourlyRate(vmType, pricingModel);
        double usageHours = usageTimeSeconds / 3600.0;
        
        // Round up to billing intervals
        double billingUnits = Math.ceil(usageHours / BILLING_INTERVAL_HOURS);
        return billingUnits * BILLING_INTERVAL_HOURS * hourlyRate;
    }

    /**
     * Get hourly rate based on pricing model
     */
    private static double getHourlyRate(VmType vmType, String pricingModel) {
        if (vmType.getPricing() == null) {
            return 0.1; // Default fallback
        }
        
        switch (pricingModel.toLowerCase()) {
            case "reserved":
                return vmType.getPricing().getReserved();
            case "spot":
                return vmType.getPricing().getSpot();
            case "on_demand":
            default:
                return vmType.getPricing().getOnDemand();
        }
    }

    /**
     * Compute egress cost
     */
    public static double computeEgressCost(double dataSizeGB, double pricePerGB) {
        return dataSizeGB * pricePerGB;
    }

    /**
     * Compute total cost for a solution
     */
    public static double computeTotalCost(double vmCost, double egressCost) {
        return vmCost + egressCost;
    }

    /**
     * Compute deadline penalty
     */
    public static double computeDeadlinePenalty(double makespan, double deadline) {
        if (makespan <= deadline) {
            return 0.0;
        }
        return (makespan - deadline) / deadline;
    }

    /**
     * Compute fragmentation metric (resource utilization inefficiency)
     */
    public static double computeFragmentation(int totalVms, int usedVms, 
                                             double avgUtilization) {
        if (totalVms == 0) return 0.0;
        
        // Fragmentation is high when we have many VMs with low utilization
        double vmUtilRatio = (double) usedVms / totalVms;
        double fragmentation = 1.0 - (vmUtilRatio * avgUtilization);
        return Math.max(0.0, Math.min(1.0, fragmentation));
    }

    /**
     * Compute fitness value for PSO optimization
     */
    public static double computeFitness(double cost, double deadlinePenalty, 
                                       double affinityScore, double fragmentation,
                                       double alpha, double beta, double gamma) {
        // Lower is better
        // fitness = cost + alpha * deadline_penalty + beta * (1 - affinity) + gamma * fragmentation
        return cost + alpha * deadlinePenalty + beta * (1.0 - affinityScore) + gamma * fragmentation;
    }
}
