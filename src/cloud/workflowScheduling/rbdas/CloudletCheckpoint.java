package cloud.workflowScheduling.rbdas;

/**
 * Checkpoint/restore support for workflow tasks (cloudlets).
 * Simplified implementation without full CloudSim integration.
 */
public class CloudletCheckpoint {
    
    /**
     * Represents a checkpoint state for a task.
     */
    public static class CheckpointState {
        public int taskId;
        public double completedWork;     // Amount of work completed (0.0 to 1.0)
        public double checkpointTime;    // When checkpoint was taken
        public double totalWork;         // Total work units
        
        public CheckpointState(int taskId, double completedWork, 
                              double checkpointTime, double totalWork) {
            this.taskId = taskId;
            this.completedWork = completedWork;
            this.checkpointTime = checkpointTime;
            this.totalWork = totalWork;
        }
        
        /**
         * Calculate remaining work.
         */
        public double getRemainingWork() {
            return totalWork - completedWork;
        }
        
        /**
         * Get completion percentage.
         */
        public double getCompletionPercentage() {
            return totalWork > 0 ? (completedWork / totalWork) * 100.0 : 0.0;
        }
    }
    
    private static final double CHECKPOINT_OVERHEAD = 0.02; // 2% overhead
    
    /**
     * Create a checkpoint of task state.
     * @param taskId Task identifier
     * @param executedTime Time the task has been running
     * @param totalExecutionTime Total time needed to complete task
     * @param currentTime Current simulation time
     * @return Checkpoint state
     */
    public static CheckpointState createCheckpoint(
            int taskId, double executedTime, double totalExecutionTime, double currentTime) {
        
        double completionRatio = totalExecutionTime > 0 ? 
            Math.min(1.0, executedTime / totalExecutionTime) : 0.0;
        
        return new CheckpointState(taskId, completionRatio, currentTime, 1.0);
    }
    
    /**
     * Restore a task from checkpoint.
     * @param checkpoint The checkpoint to restore from
     * @param newStartTime When the task will restart
     * @return Adjusted remaining execution time accounting for checkpoint overhead
     */
    public static double restoreFromCheckpoint(CheckpointState checkpoint, double newStartTime) {
        double remainingWork = checkpoint.getRemainingWork();
        
        // Add checkpoint overhead to remaining work
        double adjustedWork = remainingWork * (1.0 + CHECKPOINT_OVERHEAD);
        
        return adjustedWork;
    }
    
    /**
     * Calculate total overhead from checkpointing.
     */
    public static double calculateCheckpointOverhead(double totalExecutionTime) {
        return totalExecutionTime * CHECKPOINT_OVERHEAD;
    }
    
    /**
     * Simulate task pause due to spot interruption.
     * @param taskId Task being paused
     * @param executedTime How long task has executed
     * @param totalTime Total execution time needed
     * @param interruptTime When interruption occurred
     * @return Checkpoint state
     */
    public static CheckpointState pauseTask(
            int taskId, double executedTime, double totalTime, double interruptTime) {
        
        return createCheckpoint(taskId, executedTime, totalTime, interruptTime);
    }
    
    /**
     * Calculate work lost due to no checkpointing.
     * If checkpointing is not used, all progress is lost on interruption.
     */
    public static double calculateWorkLost(double executedTime, boolean hasCheckpoint) {
        return hasCheckpoint ? 0.0 : executedTime;
    }
}
