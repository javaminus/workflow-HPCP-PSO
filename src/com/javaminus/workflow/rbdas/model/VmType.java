package com.javaminus.workflow.rbdas.model;

/**
 * VM Type model representing a VM configuration from catalog
 */
public class VmType {
    private String id;
    private String family;
    private int vcpus;
    private double memoryGb;
    private double storageGb;
    private double networkGbps;
    private boolean gpu;
    private Pricing pricing;

    public static class Pricing {
        private double onDemand;
        private double reserved;
        private double spot;

        public double getOnDemand() { return onDemand; }
        public void setOnDemand(double onDemand) { this.onDemand = onDemand; }
        
        public double getReserved() { return reserved; }
        public void setReserved(double reserved) { this.reserved = reserved; }
        
        public double getSpot() { return spot; }
        public void setSpot(double spot) { this.spot = spot; }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }

    public int getVcpus() { return vcpus; }
    public void setVcpus(int vcpus) { this.vcpus = vcpus; }

    public double getMemoryGb() { return memoryGb; }
    public void setMemoryGb(double memoryGb) { this.memoryGb = memoryGb; }

    public double getStorageGb() { return storageGb; }
    public void setStorageGb(double storageGb) { this.storageGb = storageGb; }

    public double getNetworkGbps() { return networkGbps; }
    public void setNetworkGbps(double networkGbps) { this.networkGbps = networkGbps; }

    public boolean isGpu() { return gpu; }
    public void setGpu(boolean gpu) { this.gpu = gpu; }

    public Pricing getPricing() { return pricing; }
    public void setPricing(Pricing pricing) { this.pricing = pricing; }

    @Override
    public String toString() {
        return "VmType{" +
                "id='" + id + '\'' +
                ", family='" + family + '\'' +
                ", vcpus=" + vcpus +
                ", memoryGb=" + memoryGb +
                ", gpu=" + gpu +
                '}';
    }
}
