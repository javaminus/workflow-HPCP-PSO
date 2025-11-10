package cloud.workflowScheduling.rbdas;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import cloud.workflowScheduling.rbdas.AffinityTable.TaskType;

/**
 * Unit tests for AffinityTable.
 */
public class AffinityTableTest {
    
    private AffinityTable affinityTable;
    
    @Before
    public void setUp() {
        affinityTable = AffinityTable.getInstance("config/affinity_table.json");
    }
    
    @Test
    public void testLoadAffinityTable() {
        assertNotNull("AffinityTable should be loaded", affinityTable);
    }
    
    @Test
    public void testGetAffinityByName() {
        // CPU tasks should have high affinity with compute-optimized instances
        double cpuAffinityC5 = affinityTable.getAffinity(TaskType.CPU, "c5.xlarge");
        assertTrue("CPU affinity for c5.xlarge should be high", cpuAffinityC5 >= 0.9);
        
        // MEM tasks should have high affinity with memory-optimized instances
        double memAffinityR5 = affinityTable.getAffinity(TaskType.MEM, "r5.xlarge");
        assertTrue("MEM affinity for r5.xlarge should be high", memAffinityR5 >= 0.9);
        
        // IO tasks should have high affinity with storage-optimized instances
        double ioAffinityI3 = affinityTable.getAffinity(TaskType.IO, "i3.xlarge");
        assertTrue("IO affinity for i3.xlarge should be high", ioAffinityI3 >= 0.9);
    }
    
    @Test
    public void testGetAffinityById() {
        // VM type 4 is c5.2xlarge (compute-optimized)
        double cpuAffinity = affinityTable.getAffinity(TaskType.CPU, 4);
        assertTrue("CPU affinity for VM type 4 should be high", cpuAffinity >= 0.8);
        
        // VM type 6 is r5.xlarge (memory-optimized)
        double memAffinity = affinityTable.getAffinity(TaskType.MEM, 6);
        assertTrue("MEM affinity for VM type 6 should be high", memAffinity >= 0.9);
    }
    
    @Test
    public void testGetBestVMType() {
        // Best VM for CPU tasks should be compute-optimized (c5 series)
        int bestCpuVM = affinityTable.getBestVMType(TaskType.CPU);
        assertTrue("Best CPU VM should be in range 2-4 (c5 series)", 
                   bestCpuVM >= 2 && bestCpuVM <= 4);
        
        // Best VM for MEM tasks should be memory-optimized (r5 series)
        int bestMemVM = affinityTable.getBestVMType(TaskType.MEM);
        assertTrue("Best MEM VM should be in range 5-6 (r5 series)", 
                   bestMemVM >= 5 && bestMemVM <= 6);
        
        // Best VM for IO tasks should be storage-optimized (i3 series)
        int bestIoVM = affinityTable.getBestVMType(TaskType.IO);
        assertTrue("Best IO VM should be in range 7-8 (i3 series)", 
                   bestIoVM >= 7 && bestIoVM <= 8);
    }
    
    @Test
    public void testAffinityScoreRange() {
        // All affinity scores should be between 0 and 1
        for (TaskType taskType : TaskType.values()) {
            for (int vmType = 0; vmType < 9; vmType++) {
                double affinity = affinityTable.getAffinity(taskType, vmType);
                assertTrue("Affinity score should be >= 0", affinity >= 0.0);
                assertTrue("Affinity score should be <= 1", affinity <= 1.0);
            }
        }
    }
    
    @Test
    public void testDefaultAffinityForUnknownVM() {
        // Test with out-of-range VM type
        double affinity = affinityTable.getAffinity(TaskType.CPU, 99);
        assertEquals("Default affinity should be 0.5", 0.5, affinity, 0.01);
    }
}
