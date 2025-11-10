package cloud.workflowScheduling.rbdas;

import org.junit.Test;
import static org.junit.Assert.*;
import cloud.workflowScheduling.rbdas.AffinityTable.TaskType;
import cloud.workflowScheduling.rbdas.ResourceProfiler.TaskProfile;

/**
 * Unit tests for RuleBasedClassifier.
 */
public class ClassifierTest {
    
    @Test
    public void testClassifyCPUIntensiveTask() {
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.9;
        profile.memoryIntensity = 0.3;
        profile.ioIntensity = 0.2;
        profile.networkIntensity = 0.2;
        profile.gpuRequired = false;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("High CPU intensity should classify as CPU", TaskType.CPU, type);
    }
    
    @Test
    public void testClassifyMemoryIntensiveTask() {
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.3;
        profile.memoryIntensity = 0.9;
        profile.ioIntensity = 0.2;
        profile.networkIntensity = 0.2;
        profile.gpuRequired = false;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("High memory intensity should classify as MEM", TaskType.MEM, type);
    }
    
    @Test
    public void testClassifyIOIntensiveTask() {
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.3;
        profile.memoryIntensity = 0.3;
        profile.ioIntensity = 0.8;
        profile.networkIntensity = 0.2;
        profile.gpuRequired = false;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("High I/O intensity should classify as IO", TaskType.IO, type);
    }
    
    @Test
    public void testClassifyNetworkIntensiveTask() {
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.3;
        profile.memoryIntensity = 0.3;
        profile.ioIntensity = 0.2;
        profile.networkIntensity = 0.8;
        profile.gpuRequired = false;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("High network intensity should classify as NET", TaskType.NET, type);
    }
    
    @Test
    public void testClassifyGPUTask() {
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.5;
        profile.memoryIntensity = 0.5;
        profile.ioIntensity = 0.5;
        profile.networkIntensity = 0.5;
        profile.gpuRequired = true;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("GPU required should classify as GPU", TaskType.GPU, type);
    }
    
    @Test
    public void testClassifyMixedTask() {
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.7;
        profile.memoryIntensity = 0.7;
        profile.ioIntensity = 0.5;
        profile.networkIntensity = 0.5;
        profile.gpuRequired = false;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("Multiple high intensities should classify as MIX", TaskType.MIX, type);
    }
    
    @Test
    public void testClassifyBalancedTask() {
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.45;
        profile.memoryIntensity = 0.45;
        profile.ioIntensity = 0.45;
        profile.networkIntensity = 0.45;
        profile.gpuRequired = false;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("Balanced low intensities should classify as MIX", TaskType.MIX, type);
    }
    
    @Test
    public void testClassifyNullProfile() {
        TaskType type = RuleBasedClassifier.classify(null);
        assertEquals("Null profile should default to MIX", TaskType.MIX, type);
    }
    
    @Test
    public void testClassifyLowIntensityTask() {
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.2;
        profile.memoryIntensity = 0.2;
        profile.ioIntensity = 0.2;
        profile.networkIntensity = 0.2;
        profile.gpuRequired = false;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("Low intensities should classify as MIX", TaskType.MIX, type);
    }
    
    @Test
    public void testGPUTaskPriority() {
        // GPU requirement should take priority over other intensities
        TaskProfile profile = new TaskProfile();
        profile.cpuIntensity = 0.9;
        profile.memoryIntensity = 0.9;
        profile.ioIntensity = 0.9;
        profile.networkIntensity = 0.9;
        profile.gpuRequired = true;
        
        TaskType type = RuleBasedClassifier.classify(profile);
        assertEquals("GPU required should override other classifications", TaskType.GPU, type);
    }
}
