package com.javaminus.workflow.rbdas;

import com.javaminus.workflow.rbdas.model.ResourceProfile;
import com.javaminus.workflow.rbdas.model.WorkloadType;

/**
 * Rule-based classifier for workload types
 */
public class RuleBasedClassifier {
    private double cpuThreshold;
    private double memThreshold;
    private double ioThreshold;
    private double netThreshold;

    /**
     * Constructor with default thresholds
     */
    public RuleBasedClassifier() {
        this.cpuThreshold = 1000.0;
        this.memThreshold = 512.0;
        this.ioThreshold = 100.0;
        this.netThreshold = 50.0;
    }

    /**
     * Constructor with custom thresholds
     */
    public RuleBasedClassifier(double cpuThreshold, double memThreshold, 
                               double ioThreshold, double netThreshold) {
        this.cpuThreshold = cpuThreshold;
        this.memThreshold = memThreshold;
        this.ioThreshold = ioThreshold;
        this.netThreshold = netThreshold;
    }

    /**
     * Classify a resource profile into a workload type
     */
    public WorkloadType classify(ResourceProfile profile) {
        // GPU requirement takes precedence
        if (profile.isRequiresGpu()) {
            return WorkloadType.GPU;
        }

        // Count which thresholds are exceeded
        int cpuHigh = profile.getCpuIntensity() > cpuThreshold ? 1 : 0;
        int memHigh = profile.getMemIntensity() > memThreshold ? 1 : 0;
        int ioHigh = profile.getIoIntensity() > ioThreshold ? 1 : 0;
        int netHigh = profile.getNetIntensity() > netThreshold ? 1 : 0;
        
        int totalHigh = cpuHigh + memHigh + ioHigh + netHigh;

        // If multiple resources are high, classify as MIX
        if (totalHigh > 1) {
            return WorkloadType.MIX;
        }

        // Single dominant resource
        if (cpuHigh == 1) {
            return WorkloadType.CPU;
        } else if (memHigh == 1) {
            return WorkloadType.MEM;
        } else if (ioHigh == 1) {
            return WorkloadType.IO;
        } else if (netHigh == 1) {
            return WorkloadType.NET;
        }

        // If nothing is particularly high, default to CPU
        // (most tasks are compute-bound)
        double max = Math.max(Math.max(profile.getCpuIntensity(), profile.getMemIntensity()),
                             Math.max(profile.getIoIntensity(), profile.getNetIntensity()));
        
        if (max == profile.getCpuIntensity()) return WorkloadType.CPU;
        if (max == profile.getMemIntensity()) return WorkloadType.MEM;
        if (max == profile.getIoIntensity()) return WorkloadType.IO;
        if (max == profile.getNetIntensity()) return WorkloadType.NET;
        
        return WorkloadType.MIX;
    }

    // Getters and setters for thresholds
    public double getCpuThreshold() { return cpuThreshold; }
    public void setCpuThreshold(double cpuThreshold) { this.cpuThreshold = cpuThreshold; }

    public double getMemThreshold() { return memThreshold; }
    public void setMemThreshold(double memThreshold) { this.memThreshold = memThreshold; }

    public double getIoThreshold() { return ioThreshold; }
    public void setIoThreshold(double ioThreshold) { this.ioThreshold = ioThreshold; }

    public double getNetThreshold() { return netThreshold; }
    public void setNetThreshold(double netThreshold) { this.netThreshold = netThreshold; }
}
