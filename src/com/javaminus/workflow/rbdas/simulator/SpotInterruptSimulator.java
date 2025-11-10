package com.javaminus.workflow.rbdas.simulator;

import java.util.*;

/**
 * Spot interruption simulator
 * Simulates spot instance interruptions and manages checkpoint/resume
 */
public class SpotInterruptSimulator {
    private Random random;
    private double interruptionProbability;
    private Map<String, CloudletCheckpoint> checkpoints;
    private List<InterruptionEvent> interruptionEvents;

    /**
     * Interruption event
     */
    public static class InterruptionEvent {
        public String vmId;
        public double time;
        public String cloudletId;

        public InterruptionEvent(String vmId, double time, String cloudletId) {
            this.vmId = vmId;
            this.time = time;
            this.cloudletId = cloudletId;
        }
    }

    public SpotInterruptSimulator(long seed, double interruptionProbability) {
        this.random = new Random(seed);
        this.interruptionProbability = interruptionProbability;
        this.checkpoints = new HashMap<>();
        this.interruptionEvents = new ArrayList<>();
    }

    /**
     * Check if interruption should occur
     */
    public boolean shouldInterrupt() {
        return random.nextDouble() < interruptionProbability;
    }

    /**
     * Generate random interruption time within execution window
     */
    public double generateInterruptionTime(double startTime, double duration) {
        return startTime + random.nextDouble() * duration;
    }

    /**
     * Create checkpoint for a cloudlet
     */
    public void createCheckpoint(String cloudletId, double executedLength, 
                                 double totalLength, Map<String, Object> state) {
        CloudletCheckpoint checkpoint = new CloudletCheckpoint(
            cloudletId, executedLength, totalLength, System.currentTimeMillis(), state);
        checkpoints.put(cloudletId, checkpoint);
    }

    /**
     * Get checkpoint for a cloudlet
     */
    public CloudletCheckpoint getCheckpoint(String cloudletId) {
        return checkpoints.get(cloudletId);
    }

    /**
     * Remove checkpoint after successful completion
     */
    public void removeCheckpoint(String cloudletId) {
        checkpoints.remove(cloudletId);
    }

    /**
     * Schedule interruption event
     */
    public void scheduleInterruption(String vmId, double time, String cloudletId) {
        interruptionEvents.add(new InterruptionEvent(vmId, time, cloudletId));
    }

    /**
     * Get all interruption events
     */
    public List<InterruptionEvent> getInterruptionEvents() {
        return interruptionEvents;
    }

    /**
     * Get checkpoint count
     */
    public int getCheckpointCount() {
        return checkpoints.size();
    }

    /**
     * Cloudlet checkpoint data structure
     */
    public static class CloudletCheckpoint {
        private String cloudletId;
        private double executedLength;
        private double totalLength;
        private long timestamp;
        private Map<String, Object> state;

        public CloudletCheckpoint(String cloudletId, double executedLength, 
                                 double totalLength, long timestamp,
                                 Map<String, Object> state) {
            this.cloudletId = cloudletId;
            this.executedLength = executedLength;
            this.totalLength = totalLength;
            this.timestamp = timestamp;
            this.state = state != null ? state : new HashMap<>();
        }

        public String getCloudletId() { return cloudletId; }
        public double getExecutedLength() { return executedLength; }
        public double getTotalLength() { return totalLength; }
        public long getTimestamp() { return timestamp; }
        public Map<String, Object> getState() { return state; }

        public double getProgress() {
            return totalLength > 0 ? executedLength / totalLength : 0.0;
        }

        public double getRemainingLength() {
            return totalLength - executedLength;
        }
    }
}
