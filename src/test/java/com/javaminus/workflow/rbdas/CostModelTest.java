package com.javaminus.workflow.rbdas;

import com.javaminus.workflow.rbdas.model.VmType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for CostModel
 */
public class CostModelTest {

    private VmType testVmType;

    @Before
    public void setUp() {
        testVmType = new VmType();
        testVmType.setId("test.medium");
        testVmType.setVcpus(4);
        testVmType.setMemoryGb(16);
        
        VmType.Pricing pricing = new VmType.Pricing();
        pricing.setOnDemand(0.20);
        pricing.setReserved(0.12);
        pricing.setSpot(0.08);
        testVmType.setPricing(pricing);
    }

    @Test
    public void testComputeVmCostOnDemand() {
        double usageTime = 3600.0; // 1 hour
        double cost = CostModel.computeVmCost(testVmType, usageTime, "on_demand");
        assertEquals(0.20, cost, 0.01);
    }

    @Test
    public void testComputeVmCostReserved() {
        double usageTime = 3600.0; // 1 hour
        double cost = CostModel.computeVmCost(testVmType, usageTime, "reserved");
        assertEquals(0.12, cost, 0.01);
    }

    @Test
    public void testComputeVmCostSpot() {
        double usageTime = 3600.0; // 1 hour
        double cost = CostModel.computeVmCost(testVmType, usageTime, "spot");
        assertEquals(0.08, cost, 0.01);
    }

    @Test
    public void testComputeVmCostPartialHour() {
        double usageTime = 1800.0; // 0.5 hours
        double cost = CostModel.computeVmCost(testVmType, usageTime, "on_demand");
        // Should round up to 1 billing unit (1 hour)
        assertEquals(0.20, cost, 0.01);
    }

    @Test
    public void testComputeEgressCost() {
        double dataSizeGB = 100.0;
        double pricePerGB = 0.09;
        double cost = CostModel.computeEgressCost(dataSizeGB, pricePerGB);
        assertEquals(9.0, cost, 0.01);
    }

    @Test
    public void testComputeTotalCost() {
        double vmCost = 10.0;
        double egressCost = 5.0;
        double totalCost = CostModel.computeTotalCost(vmCost, egressCost);
        assertEquals(15.0, totalCost, 0.01);
    }

    @Test
    public void testComputeDeadlinePenalty() {
        // Within deadline
        double penalty = CostModel.computeDeadlinePenalty(90.0, 100.0);
        assertEquals(0.0, penalty, 0.01);
        
        // Exceeds deadline
        penalty = CostModel.computeDeadlinePenalty(150.0, 100.0);
        assertEquals(0.5, penalty, 0.01);
    }

    @Test
    public void testComputeFragmentation() {
        // High utilization, low fragmentation
        double frag = CostModel.computeFragmentation(10, 8, 0.9);
        assertTrue(frag < 0.3);
        
        // Low utilization, high fragmentation
        frag = CostModel.computeFragmentation(10, 3, 0.3);
        assertTrue(frag > 0.7);
    }

    @Test
    public void testComputeFitness() {
        double cost = 100.0;
        double deadlinePenalty = 0.2;
        double affinityScore = 0.8;
        double fragmentation = 0.3;
        double alpha = 100.0;
        double beta = 10.0;
        double gamma = 5.0;
        
        double fitness = CostModel.computeFitness(cost, deadlinePenalty, affinityScore, 
                                                 fragmentation, alpha, beta, gamma);
        
        // fitness = 100 + 100*0.2 + 10*(1-0.8) + 5*0.3 = 100 + 20 + 2 + 1.5 = 123.5
        assertEquals(123.5, fitness, 0.1);
    }
}
