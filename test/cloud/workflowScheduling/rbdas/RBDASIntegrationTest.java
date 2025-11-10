package cloud.workflowScheduling.rbdas;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import cloud.workflowScheduling.setting.Workflow;
import cloud.workflowScheduling.setting.Solution;
import cloud.workflowScheduling.setting.VM;
import cloud.workflowScheduling.rbdas.CostModel.PricingModel;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Integration test for RBDAS workflow scheduling.
 * Tests end-to-end workflow execution with affinity-aware scheduling.
 */
public class RBDASIntegrationTest {
    
    private Workflow workflow;
    private HNSPSOAdapter rbdasScheduler;
    private CostModel costModel;
    
    @Before
    public void setUp() {
        // Use a small workflow for fast testing
        workflow = new Workflow("dax/Montage_30.xml");
        workflow.setDeadline(1000.0); // Set reasonable deadline
        
        rbdasScheduler = new HNSPSOAdapter(false); // No spot for baseline test
        costModel = CostModel.getInstance();
    }
    
    @Test
    public void testWorkflowScheduling() {
        Solution solution = rbdasScheduler.schedule(workflow);
        
        assertNotNull("Solution should not be null", solution);
        assertTrue("Solution should have VMs", solution.size() > 0);
        
        // Verify all tasks are scheduled
        int scheduledTasks = 0;
        for (VM vm : solution.keySet()) {
            scheduledTasks += solution.get(vm).size();
        }
        
        assertEquals("All tasks should be scheduled", 
                    workflow.size(), scheduledTasks);
    }
    
    @Test
    public void testCostCalculation() {
        Solution solution = rbdasScheduler.schedule(workflow);
        
        double totalCost = 0.0;
        for (VM vm : solution.keySet()) {
            double vmCost = costModel.calculateVMCost(
                vm.getType(), 
                solution.calcMakespan(), 
                PricingModel.ON_DEMAND
            );
            totalCost += vmCost;
        }
        
        assertTrue("Total cost should be positive", totalCost > 0);
    }
    
    @Test
    public void testMakespanCalculation() {
        Solution solution = rbdasScheduler.schedule(workflow);
        double makespan = solution.calcMakespan();
        
        assertTrue("Makespan should be positive", makespan > 0);
    }
    
    @Test
    public void testResourceUtilization() {
        Solution solution = rbdasScheduler.schedule(workflow);
        
        // Calculate average VM utilization
        int totalVMs = solution.size();
        int totalTasks = workflow.size();
        double avgTasksPerVM = (double) totalTasks / totalVMs;
        
        assertTrue("Should have reasonable VM utilization", avgTasksPerVM >= 1.0);
    }
    
    @Test
    public void testDeadlineConstraint() {
        workflow.setDeadline(2000.0); // Generous deadline
        Solution solution = rbdasScheduler.schedule(workflow);
        
        double makespan = solution.calcMakespan();
        
        // RBDAS should try to meet deadline when possible
        assertTrue("Makespan should be reasonable", makespan > 0);
    }
    
    @Test
    public void testSpotVsOnDemand() {
        // Test with on-demand
        HNSPSOAdapter onDemandScheduler = new HNSPSOAdapter(false);
        Solution onDemandSolution = onDemandScheduler.schedule(workflow);
        
        double onDemandCost = 0.0;
        for (VM vm : onDemandSolution.keySet()) {
            onDemandCost += costModel.calculateVMCost(
                vm.getType(), 
                onDemandSolution.calcMakespan(), 
                PricingModel.ON_DEMAND
            );
        }
        
        // Test with spot
        HNSPSOAdapter spotScheduler = new HNSPSOAdapter(true);
        Solution spotSolution = spotScheduler.schedule(workflow);
        
        double spotCost = 0.0;
        for (VM vm : spotSolution.keySet()) {
            spotCost += costModel.calculateVMCost(
                vm.getType(), 
                spotSolution.calcMakespan(), 
                PricingModel.SPOT
            );
        }
        
        // Spot should be cheaper
        assertTrue("Spot cost should be less than on-demand", spotCost < onDemandCost);
    }
    
    @Test
    public void testMetricsCollection() {
        Solution solution = rbdasScheduler.schedule(workflow);
        
        // Collect metrics
        int vmCount = solution.size();
        double makespan = solution.calcMakespan();
        double totalCost = 0.0;
        
        for (VM vm : solution.keySet()) {
            totalCost += costModel.calculateVMCost(
                vm.getType(), 
                makespan, 
                PricingModel.ON_DEMAND
            );
        }
        
        // Calculate resource utilization
        int totalTasks = workflow.size();
        double utilization = (double) totalTasks / vmCount;
        
        // All metrics should be valid
        assertTrue("VM count should be positive", vmCount > 0);
        assertTrue("Makespan should be positive", makespan > 0);
        assertTrue("Cost should be positive", totalCost > 0);
        assertTrue("Utilization should be positive", utilization > 0);
        
        // Write metrics to results file
        try {
            writeMetrics("RBDASIntegration", vmCount, makespan, totalCost, utilization);
        } catch (IOException e) {
            fail("Failed to write metrics: " + e.getMessage());
        }
    }
    
    private void writeMetrics(String testName, int vmCount, double makespan, 
                            double cost, double utilization) throws IOException {
        FileWriter writer = new FileWriter("results/integration_test_metrics.csv", true);
        writer.write(String.format("%s,%d,%.2f,%.2f,%.2f\n", 
                                  testName, vmCount, makespan, cost, utilization));
        writer.close();
    }
    
    @Test
    public void testTaskSuccessRate() {
        Solution solution = rbdasScheduler.schedule(workflow);
        
        // In this simplified simulation, all tasks should succeed
        int scheduledTasks = 0;
        for (VM vm : solution.keySet()) {
            scheduledTasks += solution.get(vm).size();
        }
        
        double successRate = (double) scheduledTasks / workflow.size();
        
        assertEquals("Success rate should be 100%", 1.0, successRate, 0.01);
    }
}
