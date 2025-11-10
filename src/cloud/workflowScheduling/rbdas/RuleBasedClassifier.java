package cloud.workflowScheduling.rbdas;

import cloud.workflowScheduling.rbdas.AffinityTable.TaskType;
import cloud.workflowScheduling.rbdas.ResourceProfiler.TaskProfile;

/**
 * Classifies tasks into types (CPU/IO/MEM/NET/GPU/MIX) using rule-based logic.
 */
public class RuleBasedClassifier {
    
    // Thresholds for classification
    private static final double CPU_THRESHOLD = 0.7;
    private static final double MEM_THRESHOLD = 0.7;
    private static final double IO_THRESHOLD = 0.6;
    private static final double NET_THRESHOLD = 0.6;
    private static final double MIXED_THRESHOLD = 0.5;
    
    /**
     * Classify a task based on its resource profile.
     * @param profile The task's resource profile
     * @return The classified task type
     */
    public static TaskType classify(TaskProfile profile) {
        if (profile == null) {
            return TaskType.MIX; // Default for unknown profiles
        }
        
        // GPU tasks take priority
        if (profile.gpuRequired) {
            return TaskType.GPU;
        }
        
        // Count how many resources are highly utilized
        int highIntensityCount = 0;
        double maxIntensity = 0.0;
        TaskType dominantType = TaskType.MIX;
        
        // Check CPU intensity
        if (profile.cpuIntensity >= CPU_THRESHOLD) {
            highIntensityCount++;
            if (profile.cpuIntensity > maxIntensity) {
                maxIntensity = profile.cpuIntensity;
                dominantType = TaskType.CPU;
            }
        }
        
        // Check memory intensity
        if (profile.memoryIntensity >= MEM_THRESHOLD) {
            highIntensityCount++;
            if (profile.memoryIntensity > maxIntensity) {
                maxIntensity = profile.memoryIntensity;
                dominantType = TaskType.MEM;
            }
        }
        
        // Check I/O intensity
        if (profile.ioIntensity >= IO_THRESHOLD) {
            highIntensityCount++;
            if (profile.ioIntensity > maxIntensity) {
                maxIntensity = profile.ioIntensity;
                dominantType = TaskType.IO;
            }
        }
        
        // Check network intensity
        if (profile.networkIntensity >= NET_THRESHOLD) {
            highIntensityCount++;
            if (profile.networkIntensity > maxIntensity) {
                maxIntensity = profile.networkIntensity;
                dominantType = TaskType.NET;
            }
        }
        
        // Classification logic:
        // - If multiple resources are high intensity, classify as MIX
        // - If only one resource is dominant, classify by that resource
        // - If no clear dominant resource, classify as MIX
        
        if (highIntensityCount >= 2) {
            return TaskType.MIX;
        } else if (highIntensityCount == 1) {
            return dominantType;
        }
        
        // If no high intensity resources, check for moderate intensity
        // and pick the highest one
        maxIntensity = Math.max(Math.max(profile.cpuIntensity, profile.memoryIntensity),
                               Math.max(profile.ioIntensity, profile.networkIntensity));
        
        if (maxIntensity < MIXED_THRESHOLD) {
            return TaskType.MIX; // All resources are low, treat as mixed
        }
        
        // Return the dominant resource type
        if (maxIntensity == profile.cpuIntensity) {
            return TaskType.CPU;
        } else if (maxIntensity == profile.memoryIntensity) {
            return TaskType.MEM;
        } else if (maxIntensity == profile.ioIntensity) {
            return TaskType.IO;
        } else if (maxIntensity == profile.networkIntensity) {
            return TaskType.NET;
        }
        
        return TaskType.MIX;
    }
}
