package cloud.workflowScheduling.rbdas;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import cloud.workflowScheduling.rbdas.CostModel.PricingModel;

/**
 * Unit tests for CostModel.
 */
public class CostModelTest {
    
    private CostModel costModel;
    
    @Before
    public void setUp() {
        costModel = CostModel.getInstance("config/vm_catalog.json");
    }
    
    @Test
    public void testLoadCostModel() {
        assertNotNull("CostModel should be loaded", costModel);
    }
    
    @Test
    public void testSpotPricingLowerThanOnDemand() {
        // Spot pricing should always be lower than on-demand
        for (int vmType = 0; vmType < 9; vmType++) {
            double spotCost = costModel.getUnitCost(vmType, PricingModel.SPOT);
            double onDemandCost = costModel.getUnitCost(vmType, PricingModel.ON_DEMAND);
            
            assertTrue("Spot cost should be less than on-demand for VM type " + vmType,
                      spotCost < onDemandCost);
        }
    }
    
    @Test
    public void testPreReservedPricingLowerThanOnDemand() {
        // Pre-reserved pricing should be lower than on-demand
        for (int vmType = 0; vmType < 9; vmType++) {
            double preReservedCost = costModel.getUnitCost(vmType, PricingModel.PRE_RESERVED);
            double onDemandCost = costModel.getUnitCost(vmType, PricingModel.ON_DEMAND);
            
            assertTrue("Pre-reserved cost should be less than on-demand for VM type " + vmType,
                      preReservedCost < onDemandCost);
        }
    }
    
    @Test
    public void testCalculateVMCost() {
        // Test VM cost calculation for 1 hour (3600 seconds)
        int vmType = 2; // c5.large
        double duration = 3600; // 1 hour
        
        double onDemandCost = costModel.calculateVMCost(vmType, duration, PricingModel.ON_DEMAND);
        double spotCost = costModel.calculateVMCost(vmType, duration, PricingModel.SPOT);
        
        assertTrue("On-demand cost should be positive", onDemandCost > 0);
        assertTrue("Spot cost should be positive", spotCost > 0);
        assertTrue("Spot cost should be less than on-demand", spotCost < onDemandCost);
    }
    
    @Test
    public void testBillingIntervalRounding() {
        // Test that costs are rounded up to billing intervals
        int vmType = 2;
        double halfHour = 1800; // 30 minutes
        
        double halfHourCost = costModel.calculateVMCost(vmType, halfHour, PricingModel.ON_DEMAND);
        double oneHourCost = costModel.calculateVMCost(vmType, 3600, PricingModel.ON_DEMAND);
        
        // Should be charged for full hour even if only used 30 minutes
        assertEquals("Half hour should cost same as full hour", 
                    oneHourCost, halfHourCost, 0.01);
    }
    
    @Test
    public void testCalculateEgressCost() {
        double dataGB = 100; // 100 GB
        double egressCost = costModel.calculateEgressCost(dataGB);
        
        assertTrue("Egress cost should be positive for 100GB", egressCost > 0);
        
        // Zero data should have zero cost
        double zeroCost = costModel.calculateEgressCost(0);
        assertEquals("Zero data should have zero cost", 0.0, zeroCost, 0.001);
    }
    
    @Test
    public void testGetSpotPriceRatio() {
        // Spot price ratio should be between 0 and 1
        for (int vmType = 0; vmType < 9; vmType++) {
            double ratio = costModel.getSpotPriceRatio(vmType);
            assertTrue("Spot ratio should be > 0 for VM type " + vmType, ratio > 0);
            assertTrue("Spot ratio should be < 1 for VM type " + vmType, ratio < 1);
        }
    }
    
    @Test
    public void testCostIncreaseWithVMSize() {
        // Larger/faster VMs should cost more
        double smallVMCost = costModel.getUnitCost(0, PricingModel.ON_DEMAND); // t2.small
        double largeVMCost = costModel.getUnitCost(8, PricingModel.ON_DEMAND); // i3.xlarge
        
        assertTrue("Larger VM should cost more than smaller VM", 
                  largeVMCost > smallVMCost);
    }
}
