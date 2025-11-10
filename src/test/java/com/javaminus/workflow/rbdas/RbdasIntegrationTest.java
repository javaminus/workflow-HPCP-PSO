package com.javaminus.workflow.rbdas;

import cloud.workflowScheduling.setting.Workflow;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration test for RBDAS
 */
public class RbdasIntegrationTest {

    @Test
    public void testWorkflowExecutionWithSpot() throws Exception {
        // Use a small workflow for testing
        String daxFile = "files/dax/Montage_30.xml";
        Workflow workflow = new Workflow(daxFile);
        
        double criticalPath = workflow.get(0).getbLevel();
        double deadline = criticalPath * 1.5;
        workflow.setDeadline(deadline);
        
        AffinityDatacenterBroker broker = new AffinityDatacenterBroker(
            "config/vm_catalog.json",
            "config/affinity_table.json",
            42L,
            true // spot enabled
        );
        
        AffinityDatacenterBroker.ExecutionResult result = broker.executeWorkflow(workflow, deadline);
        
        assertNotNull(result);
        assertTrue(result.totalCost > 0);
        assertTrue(result.makespan > 0);
        assertTrue(result.vmCount > 0);
        assertTrue(result.taskSuccessCount > 0);
        assertEquals(workflow.size(), result.taskSuccessCount);
    }

    @Test
    public void testWorkflowExecutionWithoutSpot() throws Exception {
        // Use a small workflow for testing
        String daxFile = "files/dax/LIGO_30.xml";
        Workflow workflow = new Workflow(daxFile);
        
        double criticalPath = workflow.get(0).getbLevel();
        double deadline = criticalPath * 1.5;
        workflow.setDeadline(deadline);
        
        AffinityDatacenterBroker broker = new AffinityDatacenterBroker(
            "config/vm_catalog.json",
            "config/affinity_table.json",
            42L,
            false // spot disabled
        );
        
        AffinityDatacenterBroker.ExecutionResult result = broker.executeWorkflow(workflow, deadline);
        
        assertNotNull(result);
        assertTrue(result.totalCost > 0);
        assertTrue(result.makespan > 0);
        assertTrue(result.vmCount > 0);
        assertTrue(result.taskSuccessCount > 0);
        assertEquals(workflow.size(), result.taskSuccessCount);
        assertEquals(0, result.interruptionCount);
    }

    @Test
    public void testCompareSpotVsNoSpot() throws Exception {
        String daxFile = "files/dax/Epigenomics_30.xml";
        Workflow workflow1 = new Workflow(daxFile);
        Workflow workflow2 = new Workflow(daxFile);
        
        double criticalPath = workflow1.get(0).getbLevel();
        double deadline = criticalPath * 1.5;
        workflow1.setDeadline(deadline);
        workflow2.setDeadline(deadline);
        
        // Run with spot
        AffinityDatacenterBroker brokerSpot = new AffinityDatacenterBroker(
            "config/vm_catalog.json",
            "config/affinity_table.json",
            42L,
            true
        );
        AffinityDatacenterBroker.ExecutionResult resultSpot = 
            brokerSpot.executeWorkflow(workflow1, deadline);
        
        // Run without spot
        AffinityDatacenterBroker brokerNoSpot = new AffinityDatacenterBroker(
            "config/vm_catalog.json",
            "config/affinity_table.json",
            42L,
            false
        );
        AffinityDatacenterBroker.ExecutionResult resultNoSpot = 
            brokerNoSpot.executeWorkflow(workflow2, deadline);
        
        // Spot should generally be cheaper (though not always due to interruptions)
        assertNotNull(resultSpot);
        assertNotNull(resultNoSpot);
        
        // Both should complete successfully
        assertTrue(resultSpot.taskSuccessCount > 0);
        assertTrue(resultNoSpot.taskSuccessCount > 0);
        
        System.out.println("Spot cost: " + resultSpot.totalCost + ", No-spot cost: " + resultNoSpot.totalCost);
        System.out.println("Spot interruptions: " + resultSpot.interruptionCount);
    }
}
