package com.javaminus.workflow.rbdas;

import com.javaminus.workflow.rbdas.model.WorkloadType;
import com.javaminus.workflow.rbdas.util.AffinityTable;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Unit tests for AffinityTable
 */
public class AffinityTableTest {

    @Test
    public void testLoadAffinityTable() throws IOException {
        AffinityTable table = AffinityTable.loadFromFile("config/affinity_table.json");
        assertNotNull(table);
    }

    @Test
    public void testGetAffinityScore() throws IOException {
        AffinityTable table = AffinityTable.loadFromFile("config/affinity_table.json");
        
        // Test CPU workload on compute_optimized
        double score = table.getAffinityScore(WorkloadType.CPU, "compute_optimized");
        assertEquals(0.9, score, 0.01);
        
        // Test GPU workload on gpu_accelerated
        score = table.getAffinityScore(WorkloadType.GPU, "gpu_accelerated");
        assertEquals(1.0, score, 0.01);
        
        // Test IO workload on io_optimized
        score = table.getAffinityScore(WorkloadType.IO, "io_optimized");
        assertEquals(0.9, score, 0.01);
    }

    @Test
    public void testDefaultAffinityScore() throws IOException {
        AffinityTable table = AffinityTable.loadFromFile("config/affinity_table.json");
        
        // Test unknown VM family - should return default
        double score = table.getAffinityScore(WorkloadType.CPU, "unknown_family");
        assertEquals(0.5, score, 0.01);
    }
}
