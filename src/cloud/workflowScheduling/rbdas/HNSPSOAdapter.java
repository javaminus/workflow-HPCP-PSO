package cloud.workflowScheduling.rbdas;

import cloud.workflowScheduling.setting.*;
import cloud.workflowScheduling.methods.Scheduler;
import cloud.workflowScheduling.rbdas.AffinityTable.TaskType;
import cloud.workflowScheduling.rbdas.CostModel.PricingModel;
import java.util.*;

/**
 * Adapter for Hierarchical Nested Swarm PSO with affinity-aware fitness function.
 * Adapts the mapping problem for affinity-based scheduling.
 */
public class HNSPSOAdapter implements Scheduler {
    
    // PSO parameters
    private static final int POPSIZE = 100;
    private static final int NO_OF_ITE = 400;
    private static final double W = 0.5, C1 = 2.0, C2 = 2.0;
    
    // Fitness weights
    private double alphaCost = 1.0;           // Weight for cost
    private double betaDeadline = 100.0;      // Weight for deadline violation
    private double gammaAffinity = 10.0;      // Weight for affinity penalty
    private double deltaFragmentation = 5.0;  // Weight for fragmentation
    
    private Workflow workflow;
    private int range;
    private Random rnd = new Random(42); // Fixed seed for reproducibility
    
    private int dimension;
    private VM[] vmPool;
    
    private ResourceProfiler profiler;
    private AffinityTable affinityTable;
    private CostModel costModel;
    private Map<Integer, TaskType> taskTypes;
    private boolean useSpot;
    
    public HNSPSOAdapter() {
        this(false);
    }
    
    public HNSPSOAdapter(boolean useSpot) {
        this.useSpot = useSpot;
    }
    
    /**
     * Configure fitness weights.
     */
    public void setFitnessWeights(double alphaCost, double betaDeadline, 
                                   double gammaAffinity, double deltaFragmentation) {
        this.alphaCost = alphaCost;
        this.betaDeadline = betaDeadline;
        this.gammaAffinity = gammaAffinity;
        this.deltaFragmentation = deltaFragmentation;
    }
    
    @Override
    public Solution schedule(Workflow wf) {
        this.workflow = wf;
        this.dimension = wf.size();
        this.range = wf.getMaxParallel() * VM.TYPE_NO;
        
        // Initialize VM pool
        this.vmPool = new VM[range];
        for (int i = 0; i < vmPool.length; i++) {
            vmPool[i] = new VM(i / wf.getMaxParallel());
        }
        
        // Initialize profiler and affinity table
        this.profiler = new ResourceProfiler(workflow);
        this.affinityTable = AffinityTable.getInstance();
        this.costModel = CostModel.getInstance();
        
        // Classify all tasks
        this.taskTypes = new HashMap<>();
        for (Task task : workflow) {
            TaskType type = RuleBasedClassifier.classify(profiler.getProfile(task));
            taskTypes.put(task.getId(), type);
        }
        
        // Run PSO optimization
        double xMin = 0, xMax = range - 1;
        double vMax = xMax;
        double[] globalBestPos = new double[dimension];
        Solution globalBestSol = null;
        
        Particle[] particles = new Particle[POPSIZE];
        for (int i = 0; i < POPSIZE; i++) {
            particles[i] = new Particle(vMax, xMin, xMax);
            particles[i].generateSolution();
            
            if (globalBestSol == null || 
                calculateFitness(particles[i].sol) < calculateFitness(globalBestSol)) {
                for (int j = 0; j < dimension; j++) {
                    globalBestPos[j] = particles[i].position[j];
                }
                globalBestSol = particles[i].sol;
            }
        }
        
        // PSO iterations
        for (int iteIndex = 0; iteIndex < NO_OF_ITE; iteIndex++) {
            for (int i = 0; i < POPSIZE; i++) {
                // Update velocity and position
                for (int j = 0; j < dimension; j++) {
                    particles[i].speed[j] = W * particles[i].speed[j]
                            + C1 * rnd.nextDouble() * (particles[i].bestPos[j] - particles[i].position[j])
                            + C2 * rnd.nextDouble() * (globalBestPos[j] - particles[i].position[j]);
                    particles[i].speed[j] = Math.min(particles[i].speed[j], vMax);
                    
                    particles[i].position[j] = particles[i].position[j] + particles[i].speed[j];
                    particles[i].position[j] = Math.max(particles[i].position[j], xMin);
                    particles[i].position[j] = Math.min(particles[i].position[j], xMax);
                }
                
                particles[i].generateSolution();
                
                // Update best solution
                if (calculateFitness(particles[i].sol) < calculateFitness(globalBestSol)) {
                    for (int j = 0; j < dimension; j++) {
                        globalBestPos[j] = particles[i].position[j];
                    }
                    globalBestSol = particles[i].sol;
                }
            }
        }
        
        return globalBestSol;
    }
    
    /**
     * Calculate fitness with affinity awareness.
     * fitness = cost + alpha*deadline_penalty + beta*affinity_penalty + gamma*fragmentation
     */
    private double calculateFitness(Solution sol) {
        double cost = calculateCostWithModel(sol);
        double deadlinePenalty = calculateDeadlinePenalty(sol);
        double affinityPenalty = calculateAffinityPenalty(sol);
        double fragmentationPenalty = calculateFragmentationPenalty(sol);
        
        return alphaCost * cost + 
               betaDeadline * deadlinePenalty + 
               gammaAffinity * affinityPenalty + 
               deltaFragmentation * fragmentationPenalty;
    }
    
    private double calculateCostWithModel(Solution sol) {
        double totalCost = 0.0;
        
        for (VM vm : sol.keySet()) {
            LinkedList<Allocation> allocList = sol.get(vm);
            if (allocList.isEmpty()) {
                continue;
            }
            
            // Calculate VM active time
            double startTime = Double.MAX_VALUE;
            double endTime = 0.0;
            
            for (Allocation alloc : allocList) {
                startTime = Math.min(startTime, alloc.getStartTime());
                endTime = Math.max(endTime, alloc.getFinishTime());
            }
            
            double duration = endTime - startTime;
            
            // Use spot pricing if enabled, otherwise on-demand
            PricingModel model = useSpot ? PricingModel.SPOT : PricingModel.ON_DEMAND;
            totalCost += costModel.calculateVMCost(vm.getType(), duration, model);
        }
        
        return totalCost;
    }
    
    private double calculateDeadlinePenalty(Solution sol) {
        double makespan = sol.calcMakespan();
        double deadline = workflow.getDeadline();
        
        if (makespan > deadline) {
            return makespan - deadline;
        }
        return 0.0;
    }
    
    private double calculateAffinityPenalty(Solution sol) {
        double totalPenalty = 0.0;
        int taskCount = 0;
        
        for (VM vm : sol.keySet()) {
            LinkedList<Allocation> allocList = sol.get(vm);
            
            for (Allocation alloc : allocList) {
                Task task = alloc.getTask();
                TaskType taskType = taskTypes.get(task.getId());
                
                if (taskType != null) {
                    double affinity = affinityTable.getAffinity(taskType, vm.getType());
                    totalPenalty += (1.0 - affinity); // Higher penalty for poor affinity
                    taskCount++;
                }
            }
        }
        
        return taskCount > 0 ? totalPenalty / taskCount : 0.0;
    }
    
    private double calculateFragmentationPenalty(Solution sol) {
        if (sol.isEmpty()) {
            return 0.0;
        }
        
        // Calculate variance in VM utilization
        List<Integer> taskCounts = new ArrayList<>();
        for (VM vm : sol.keySet()) {
            taskCounts.add(sol.get(vm).size());
        }
        
        double mean = taskCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = taskCounts.stream()
            .mapToDouble(count -> Math.pow(count - mean, 2))
            .average().orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * Inner class for PSO particle.
     */
    private class Particle {
        double[] position;
        double[] speed;
        double[] bestPos;
        Solution sol;
        
        Particle(double vMax, double xMin, double xMax) {
            position = new double[dimension];
            speed = new double[dimension];
            bestPos = new double[dimension];
            
            for (int i = 0; i < dimension; i++) {
                position[i] = xMin + rnd.nextDouble() * (xMax - xMin);
                speed[i] = -vMax + rnd.nextDouble() * 2 * vMax;
                bestPos[i] = position[i];
            }
        }
        
        void generateSolution() {
            sol = new Solution();
            
            for (int i = 0; i < dimension; i++) {
                int vmIndex = (int) Math.round(position[i]);
                vmIndex = Math.max(0, Math.min(vmIndex, range - 1));
                
                Task task = workflow.get(i);
                VM vm = vmPool[vmIndex];
                double est = sol.calcEST(task, vm);
                sol.addTaskToVM(vm, task, est, true);
            }
        }
    }
}
