package cloud.workflowScheduling.rbdas;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import cloud.workflowScheduling.setting.Workflow;
import cloud.workflowScheduling.setting.Task;
import cloud.workflowScheduling.rbdas.A2MDBFD.VMAllocation;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests for A2MDBFD packer.
 */
public class A2MDBFDTest {
    
    private Workflow workflow;
    private A2MDBFD packer;
    
    @Before
    public void setUp() {
        // Use a small test workflow
        workflow = new Workflow("dax/Montage_30.xml");
        packer = new A2MDBFD(workflow, 3, 0.5); // 3 pre-reserved, 50% spot
    }
    
    @Test
    public void testPackerInitialization() {
        assertNotNull("Packer should be initialized", packer);
    }
    
    @Test
    public void testTaskClassification() {
        // All tasks should be classified
        for (Task task : workflow) {
            assertNotNull("Task should have classification", packer.getTaskType(task));
        }
    }
    
    @Test
    public void testPackTasks() {
        List<Task> tasks = new ArrayList<>();
        // Skip entry and exit nodes
        for (int i = 1; i < workflow.size() - 1; i++) {
            tasks.add(workflow.get(i));
        }
        
        List<VMAllocation> allocations = packer.packTasks(tasks);
        
        assertNotNull("Allocations should not be null", allocations);
        assertTrue("Should have at least some allocations", allocations.size() > 0);
        
        // Count total allocated tasks
        int totalAllocated = 0;
        for (VMAllocation alloc : allocations) {
            totalAllocated += alloc.allocatedTasks.size();
        }
        
        assertEquals("All tasks should be allocated", tasks.size(), totalAllocated);
    }
    
    @Test
    public void testPreReservedAllocation() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i < Math.min(5, workflow.size() - 1); i++) {
            tasks.add(workflow.get(i));
        }
        
        List<VMAllocation> allocations = packer.packTasks(tasks);
        
        // Count pre-reserved VMs
        long preReservedCount = allocations.stream()
            .filter(a -> a.isPreReserved)
            .count();
        
        assertTrue("Should have pre-reserved VMs", preReservedCount > 0);
    }
    
    @Test
    public void testSpotAllocation() {
        List<Task> tasks = new ArrayList<>();
        // Use more tasks to ensure spot allocation
        for (int i = 1; i < workflow.size() - 1; i++) {
            tasks.add(workflow.get(i));
        }
        
        List<VMAllocation> allocations = packer.packTasks(tasks);
        
        // Count spot VMs (excluding pre-reserved)
        long spotCount = allocations.stream()
            .filter(a -> a.isSpot && !a.isPreReserved)
            .count();
        
        // With 50% spot ratio and enough tasks, we should have some spot instances
        if (tasks.size() > 5) {
            assertTrue("Should have spot VMs for large task sets", spotCount > 0);
        }
    }
    
    @Test
    public void testFragmentationCalculation() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i < workflow.size() - 1; i++) {
            tasks.add(workflow.get(i));
        }
        
        List<VMAllocation> allocations = packer.packTasks(tasks);
        double fragmentation = packer.calculateFragmentationPenalty(allocations);
        
        assertTrue("Fragmentation should be non-negative", fragmentation >= 0);
    }
    
    @Test
    public void testEmptyTaskList() {
        List<Task> emptyTasks = new ArrayList<>();
        List<VMAllocation> allocations = packer.packTasks(emptyTasks);
        
        assertNotNull("Allocations should not be null for empty list", allocations);
        
        // Should still have pre-reserved pool
        long preReservedCount = allocations.stream()
            .filter(a -> a.isPreReserved)
            .count();
        assertTrue("Should have pre-reserved pool even with no tasks", preReservedCount > 0);
    }
    
    @Test
    public void testAffinityScores() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i < Math.min(10, workflow.size() - 1); i++) {
            tasks.add(workflow.get(i));
        }
        
        List<VMAllocation> allocations = packer.packTasks(tasks);
        
        // All allocations should have valid affinity scores
        for (VMAllocation alloc : allocations) {
            assertTrue("Affinity score should be between 0 and 1", 
                      alloc.affinityScore >= 0.0 && alloc.affinityScore <= 1.0);
        }
    }
}
