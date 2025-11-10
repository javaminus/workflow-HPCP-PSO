package cloud.workflowScheduling.rbdas;

import cloud.workflowScheduling.setting.Task;
import cloud.workflowScheduling.setting.Edge;
import cloud.workflowScheduling.setting.Workflow;
import java.util.HashMap;
import java.util.Map;

/**
 * Collects static and runtime statistics per workflow and task.
 * Used for task classification and resource estimation.
 */
public class ResourceProfiler {
    
    /**
     * Profile data for a single task.
     */
    public static class TaskProfile {
        public double cpuIntensity;      // Estimated CPU intensity (0-1)
        public double memoryIntensity;   // Estimated memory intensity (0-1)
        public double ioIntensity;       // Estimated I/O intensity (0-1)
        public double networkIntensity;  // Estimated network intensity (0-1)
        public boolean gpuRequired;      // Whether GPU is required
        
        public TaskProfile() {
            this.cpuIntensity = 0.5;
            this.memoryIntensity = 0.5;
            this.ioIntensity = 0.5;
            this.networkIntensity = 0.5;
            this.gpuRequired = false;
        }
    }
    
    private Map<Integer, TaskProfile> taskProfiles;
    private Workflow workflow;
    
    public ResourceProfiler(Workflow workflow) {
        this.workflow = workflow;
        this.taskProfiles = new HashMap<>();
        profileAllTasks();
    }
    
    /**
     * Profile all tasks in the workflow to estimate resource intensities.
     */
    private void profileAllTasks() {
        // Calculate statistics for the workflow
        double avgTaskSize = calculateAverageTaskSize();
        double maxTaskSize = calculateMaxTaskSize();
        double avgDataSize = calculateAverageDataSize();
        double maxDataSize = calculateMaxDataSize();
        
        for (Task task : workflow) {
            TaskProfile profile = new TaskProfile();
            
            // CPU intensity: based on task size relative to maximum
            if (maxTaskSize > 0) {
                profile.cpuIntensity = Math.min(1.0, task.getTaskSize() / maxTaskSize);
            }
            
            // Network intensity: based on total input/output data
            double totalData = 0;
            for (Edge e : task.getInEdges()) {
                totalData += e.getDataSize();
            }
            for (Edge e : task.getOutEdges()) {
                totalData += e.getDataSize();
            }
            
            if (maxDataSize > 0) {
                profile.networkIntensity = Math.min(1.0, totalData / maxDataSize);
            }
            
            // I/O intensity: estimate based on data transfer characteristics
            // Tasks with high data relative to computation are I/O intensive
            if (task.getTaskSize() > 0) {
                double dataToComputeRatio = totalData / task.getTaskSize();
                profile.ioIntensity = Math.min(1.0, dataToComputeRatio / 10.0); // Normalized
            }
            
            // Memory intensity: heuristic based on task size and data
            // Larger tasks with more data typically need more memory
            profile.memoryIntensity = Math.min(1.0, 
                (profile.cpuIntensity * 0.4 + profile.ioIntensity * 0.6));
            
            // GPU: determined by task name patterns (example heuristic)
            profile.gpuRequired = isGPUTask(task);
            
            taskProfiles.put(task.getId(), profile);
        }
    }
    
    /**
     * Check if a task requires GPU based on naming patterns.
     */
    private boolean isGPUTask(Task task) {
        String name = task.getName().toLowerCase();
        // Common GPU task patterns
        return name.contains("gpu") || name.contains("cuda") || 
               name.contains("render") || name.contains("ml") ||
               name.contains("ai") || name.contains("neural");
    }
    
    private double calculateAverageTaskSize() {
        double sum = 0;
        int count = 0;
        for (Task task : workflow) {
            if (!task.getName().equals("entry") && !task.getName().equals("exit")) {
                sum += task.getTaskSize();
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }
    
    private double calculateMaxTaskSize() {
        double max = 0;
        for (Task task : workflow) {
            if (task.getTaskSize() > max) {
                max = task.getTaskSize();
            }
        }
        return max;
    }
    
    private double calculateAverageDataSize() {
        double sum = 0;
        int count = 0;
        for (Task task : workflow) {
            for (Edge e : task.getOutEdges()) {
                sum += e.getDataSize();
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }
    
    private double calculateMaxDataSize() {
        double max = 0;
        for (Task task : workflow) {
            for (Edge e : task.getOutEdges()) {
                if (e.getDataSize() > max) {
                    max = e.getDataSize();
                }
            }
        }
        return max;
    }
    
    /**
     * Get the profile for a specific task.
     */
    public TaskProfile getProfile(Task task) {
        return taskProfiles.get(task.getId());
    }
    
    /**
     * Get the profile by task ID.
     */
    public TaskProfile getProfile(int taskId) {
        return taskProfiles.get(taskId);
    }
}
