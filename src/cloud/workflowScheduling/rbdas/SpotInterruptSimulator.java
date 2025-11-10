package cloud.workflowScheduling.rbdas;

import java.util.*;

/**
 * Simulates spot instance interruptions with configurable probability.
 * Simplified event-based simulation without CloudSim integration.
 */
public class SpotInterruptSimulator {
    
    /**
     * Represents a spot interruption event.
     */
    public static class InterruptionEvent {
        public int vmId;
        public double time;
        public String reason;
        
        public InterruptionEvent(int vmId, double time, String reason) {
            this.vmId = vmId;
            this.time = time;
            this.reason = reason;
        }
    }
    
    private double interruptionProbability;
    private Random random;
    private List<InterruptionEvent> interruptionLog;
    
    public SpotInterruptSimulator(double interruptionProbability) {
        this(interruptionProbability, 42);
    }
    
    public SpotInterruptSimulator(double interruptionProbability, long seed) {
        this.interruptionProbability = Math.max(0.0, Math.min(1.0, interruptionProbability));
        this.random = new Random(seed);
        this.interruptionLog = new ArrayList<>();
    }
    
    /**
     * Check if a spot VM should be interrupted at a given time.
     * @param vmId VM identifier
     * @param currentTime Current simulation time
     * @param vmStartTime When the VM was started
     * @return true if interruption occurs
     */
    public boolean shouldInterrupt(int vmId, double currentTime, double vmStartTime) {
        double runningTime = currentTime - vmStartTime;
        
        // VMs running less than 1 minute are safe from interruption (grace period)
        if (runningTime < 60) {
            return false;
        }
        
        // Check interruption probability
        if (random.nextDouble() < interruptionProbability) {
            InterruptionEvent event = new InterruptionEvent(
                vmId, currentTime, "Spot capacity reclaimed");
            interruptionLog.add(event);
            return true;
        }
        
        return false;
    }
    
    /**
     * Simulate spot interruptions for a set of VMs over a time period.
     * @param vmIds List of spot VM IDs
     * @param startTime Simulation start time
     * @param endTime Simulation end time
     * @param checkInterval Time interval between checks (e.g., 300 seconds)
     * @return List of interruption events
     */
    public List<InterruptionEvent> simulateInterruptions(
            List<Integer> vmIds, double startTime, double endTime, double checkInterval) {
        
        interruptionLog.clear();
        Map<Integer, Double> vmStartTimes = new HashMap<>();
        
        // Initialize VM start times
        for (int vmId : vmIds) {
            vmStartTimes.put(vmId, startTime);
        }
        
        // Simulate over time intervals
        for (double time = startTime; time <= endTime; time += checkInterval) {
            List<Integer> activeVMs = new ArrayList<>(vmIds);
            
            for (int vmId : activeVMs) {
                double vmStartTime = vmStartTimes.get(vmId);
                
                if (shouldInterrupt(vmId, time, vmStartTime)) {
                    vmIds.remove((Integer) vmId);
                    // VM would be restarted with new ID in real scenario
                }
            }
        }
        
        return new ArrayList<>(interruptionLog);
    }
    
    /**
     * Get total number of interruptions that occurred.
     */
    public int getInterruptionCount() {
        return interruptionLog.size();
    }
    
    /**
     * Get all logged interruption events.
     */
    public List<InterruptionEvent> getInterruptionLog() {
        return new ArrayList<>(interruptionLog);
    }
    
    /**
     * Calculate average time between interruptions for a VM.
     */
    public double calculateMeanTimeToInterruption(double observationPeriod) {
        if (interruptionProbability == 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        // Expected value for geometric distribution
        return 1.0 / interruptionProbability;
    }
}
