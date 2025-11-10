package com.javaminus.workflow.rbdas;

import cloud.workflowScheduling.setting.Task;
import com.javaminus.workflow.rbdas.model.ResourceProfile;
import com.javaminus.workflow.rbdas.model.VmType;
import com.javaminus.workflow.rbdas.model.WorkloadType;
import com.javaminus.workflow.rbdas.util.AffinityTable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for A2MDBFD packer
 */
public class A2MDBFDTest {

    private AffinityTable affinityTable;
    private List<VmType> vmPool;

    @Before
    public void setUp() throws IOException {
        affinityTable = AffinityTable.loadFromFile("config/affinity_table.json");
        
        // Create a simple VM pool
        vmPool = new ArrayList<>();
        
        VmType computeVm = new VmType();
        computeVm.setId("c5.large");
        computeVm.setFamily("compute_optimized");
        computeVm.setVcpus(2);
        computeVm.setMemoryGb(4);
        computeVm.setStorageGb(50);
        vmPool.add(computeVm);
        
        VmType memoryVm = new VmType();
        memoryVm.setId("r5.large");
        memoryVm.setFamily("memory_optimized");
        memoryVm.setVcpus(2);
        memoryVm.setMemoryGb(16);
        memoryVm.setStorageGb(50);
        vmPool.add(memoryVm);
    }

    @Test
    public void testPackEmptyTaskList() {
        A2MDBFD packer = new A2MDBFD(affinityTable, vmPool, vmPool, vmPool);
        List<Task> tasks = new ArrayList<>();
        Map<Task, ResourceProfile> profiles = new HashMap<>();
        
        List<A2MDBFD.VmAllocation> allocations = packer.pack(tasks, profiles);
        
        assertNotNull(allocations);
        assertTrue(allocations.isEmpty());
    }

    @Test
    public void testPackSingleTask() {
        A2MDBFD packer = new A2MDBFD(affinityTable, vmPool, vmPool, vmPool);
        
        Task task = new Task("task1", 1000.0);
        List<Task> tasks = new ArrayList<>();
        tasks.add(task);
        
        ResourceProfile profile = new ResourceProfile();
        profile.setCpuIntensity(500.0);
        profile.setMemIntensity(200.0);
        profile.setClassifiedType(WorkloadType.CPU);
        
        Map<Task, ResourceProfile> profiles = new HashMap<>();
        profiles.put(task, profile);
        
        List<A2MDBFD.VmAllocation> allocations = packer.pack(tasks, profiles);
        
        assertNotNull(allocations);
        assertFalse(allocations.isEmpty());
        assertEquals(1, allocations.get(0).tasks.size());
    }

    @Test
    public void testPackMultipleTasks() {
        A2MDBFD packer = new A2MDBFD(affinityTable, vmPool, vmPool, vmPool);
        
        List<Task> tasks = new ArrayList<>();
        Map<Task, ResourceProfile> profiles = new HashMap<>();
        
        // Create CPU-intensive task
        Task cpuTask = new Task("cpu_task", 1000.0);
        tasks.add(cpuTask);
        ResourceProfile cpuProfile = new ResourceProfile();
        cpuProfile.setCpuIntensity(800.0);
        cpuProfile.setMemIntensity(100.0);
        cpuProfile.setClassifiedType(WorkloadType.CPU);
        profiles.put(cpuTask, cpuProfile);
        
        // Create MEM-intensive task
        Task memTask = new Task("mem_task", 500.0);
        tasks.add(memTask);
        ResourceProfile memProfile = new ResourceProfile();
        memProfile.setCpuIntensity(100.0);
        memProfile.setMemIntensity(800.0);
        memProfile.setClassifiedType(WorkloadType.MEM);
        profiles.put(memTask, memProfile);
        
        List<A2MDBFD.VmAllocation> allocations = packer.pack(tasks, profiles);
        
        assertNotNull(allocations);
        assertFalse(allocations.isEmpty());
        
        // Verify all tasks are allocated
        int totalAllocated = 0;
        for (A2MDBFD.VmAllocation alloc : allocations) {
            totalAllocated += alloc.tasks.size();
        }
        assertEquals(2, totalAllocated);
    }

    @Test
    public void testAffinityBasedPacking() {
        A2MDBFD packer = new A2MDBFD(affinityTable, vmPool, vmPool, vmPool);
        
        // Create CPU task - should prefer compute_optimized VM
        Task cpuTask = new Task("cpu_task", 1000.0);
        List<Task> tasks = new ArrayList<>();
        tasks.add(cpuTask);
        
        ResourceProfile cpuProfile = new ResourceProfile();
        cpuProfile.setCpuIntensity(1000.0);
        cpuProfile.setMemIntensity(100.0);
        cpuProfile.setClassifiedType(WorkloadType.CPU);
        
        Map<Task, ResourceProfile> profiles = new HashMap<>();
        profiles.put(cpuTask, cpuProfile);
        
        List<A2MDBFD.VmAllocation> allocations = packer.pack(tasks, profiles);
        
        assertFalse(allocations.isEmpty());
        A2MDBFD.VmAllocation alloc = allocations.get(0);
        
        // CPU tasks should be packed into compute_optimized VMs
        assertEquals("compute_optimized", alloc.vmType.getFamily());
    }
}
