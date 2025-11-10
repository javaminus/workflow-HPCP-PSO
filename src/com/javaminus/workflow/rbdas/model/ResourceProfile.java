package com.javaminus.workflow.rbdas.model;

/**
 * Resource profile for a task/cloudlet
 */
public class ResourceProfile {
    private double cpuIntensity;
    private double memIntensity;
    private double ioIntensity;
    private double netIntensity;
    private double dataSize;
    private boolean requiresGpu;
    private WorkloadType classifiedType;

    public ResourceProfile() {
        this.cpuIntensity = 0.0;
        this.memIntensity = 0.0;
        this.ioIntensity = 0.0;
        this.netIntensity = 0.0;
        this.dataSize = 0.0;
        this.requiresGpu = false;
        this.classifiedType = WorkloadType.MIX;
    }

    // Getters and setters
    public double getCpuIntensity() { return cpuIntensity; }
    public void setCpuIntensity(double cpuIntensity) { this.cpuIntensity = cpuIntensity; }

    public double getMemIntensity() { return memIntensity; }
    public void setMemIntensity(double memIntensity) { this.memIntensity = memIntensity; }

    public double getIoIntensity() { return ioIntensity; }
    public void setIoIntensity(double ioIntensity) { this.ioIntensity = ioIntensity; }

    public double getNetIntensity() { return netIntensity; }
    public void setNetIntensity(double netIntensity) { this.netIntensity = netIntensity; }

    public double getDataSize() { return dataSize; }
    public void setDataSize(double dataSize) { this.dataSize = dataSize; }

    public boolean isRequiresGpu() { return requiresGpu; }
    public void setRequiresGpu(boolean requiresGpu) { this.requiresGpu = requiresGpu; }

    public WorkloadType getClassifiedType() { return classifiedType; }
    public void setClassifiedType(WorkloadType classifiedType) { this.classifiedType = classifiedType; }

    @Override
    public String toString() {
        return "ResourceProfile{" +
                "cpuIntensity=" + cpuIntensity +
                ", memIntensity=" + memIntensity +
                ", ioIntensity=" + ioIntensity +
                ", netIntensity=" + netIntensity +
                ", dataSize=" + dataSize +
                ", requiresGpu=" + requiresGpu +
                ", classifiedType=" + classifiedType +
                '}';
    }
}
