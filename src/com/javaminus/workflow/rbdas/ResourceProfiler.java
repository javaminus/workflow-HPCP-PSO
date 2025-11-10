package com.javaminus.workflow.rbdas;

import cloud.workflowScheduling.setting.Task;
import cloud.workflowScheduling.setting.Workflow;
import cloud.workflowScheduling.setting.Edge;
import com.javaminus.workflow.rbdas.model.ResourceProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Resource profiler that collects static metadata and runtime stats
 * for workflow tasks to enable classification
 */
public class ResourceProfiler {
    private Map<Task, ResourceProfile> profiles;
    private static final double CPU_THRESHOLD = 1000.0; // Arbitrary unit
    private static final double MEM_THRESHOLD = 512.0;  // MB
    private static final double IO_THRESHOLD = 100.0;   // MB
    
    public ResourceProfiler() {
        profiles = new HashMap<>();
    }

    /**
     * Profile all tasks in a workflow
     */
    public void profileWorkflow(Workflow workflow) {
        for (Task task : workflow) {
            ResourceProfile profile = profileTask(task);
            profiles.put(task, profile);
        }
    }

    /**
     * Profile a single task based on its characteristics
     */
    public ResourceProfile profileTask(Task task) {
        ResourceProfile profile = new ResourceProfile();
        
        // CPU intensity based on task size (execution time)
        double taskSize = task.getTaskSize();
        profile.setCpuIntensity(taskSize);
        
        // Calculate memory intensity (heuristic based on task size)
        profile.setMemIntensity(taskSize * 0.5);
        
        // I/O intensity based on input/output data sizes
        double totalDataSize = 0.0;
        for (Edge edge : task.getInEdges()) {
            totalDataSize += edge.getDataSize();
        }
        for (Edge edge : task.getOutEdges()) {
            totalDataSize += edge.getDataSize();
        }
        profile.setIoIntensity(totalDataSize / 1024.0); // Convert to KB
        profile.setDataSize(totalDataSize);
        
        // Network intensity (correlated with I/O for now)
        profile.setNetIntensity(profile.getIoIntensity());
        
        // GPU requirement (heuristic: large computational tasks might benefit)
        profile.setRequiresGpu(taskSize > 10000.0);
        
        return profile;
    }

    /**
     * Get profile for a task
     */
    public ResourceProfile getProfile(Task task) {
        return profiles.get(task);
    }

    /**
     * Get all profiles
     */
    public Map<Task, ResourceProfile> getAllProfiles() {
        return profiles;
    }

    /**
     * Update profile with runtime statistics (for sliding window)
     */
    public void updateProfile(Task task, double actualCpu, double actualMem, 
                            double actualIo, double actualNet) {
        ResourceProfile profile = profiles.get(task);
        if (profile != null) {
            // Simple exponential moving average with alpha=0.3
            profile.setCpuIntensity(0.7 * profile.getCpuIntensity() + 0.3 * actualCpu);
            profile.setMemIntensity(0.7 * profile.getMemIntensity() + 0.3 * actualMem);
            profile.setIoIntensity(0.7 * profile.getIoIntensity() + 0.3 * actualIo);
            profile.setNetIntensity(0.7 * profile.getNetIntensity() + 0.3 * actualNet);
        }
    }
}
