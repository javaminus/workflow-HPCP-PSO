package com.javaminus.workflow.rbdas.scheduler;

import cloud.workflowScheduling.setting.Task;
import cloud.workflowScheduling.setting.Workflow;
import com.javaminus.workflow.rbdas.A2MDBFD;
import com.javaminus.workflow.rbdas.CostModel;
import com.javaminus.workflow.rbdas.model.ResourceProfile;
import com.javaminus.workflow.rbdas.model.VmType;
import com.javaminus.workflow.rbdas.util.AffinityTable;

import java.util.*;

/**
 * Adapter for HNSPSO (Hybrid Neighborhood Structure PSO) for task-VM mapping
 * Uses PSO to optimize the mapping considering cost, deadline, affinity, and fragmentation
 */
public class HNSPSOAdapter {
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_ITERATIONS = 100;
    private static final double W = 0.7; // Inertia weight
    private static final double C1 = 1.5; // Cognitive coefficient
    private static final double C2 = 1.5; // Social coefficient
    
    private Workflow workflow;
    private List<VmType> availableVms;
    private Map<Task, ResourceProfile> profiles;
    private AffinityTable affinityTable;
    private double deadline;
    private Random random;
    
    // Fitness function weights
    private double alpha; // Deadline penalty weight
    private double beta;  // Affinity weight
    private double gamma; // Fragmentation weight

    public HNSPSOAdapter(Workflow workflow, List<VmType> availableVms,
                        Map<Task, ResourceProfile> profiles, AffinityTable affinityTable,
                        double deadline, long seed) {
        this.workflow = workflow;
        this.availableVms = availableVms;
        this.profiles = profiles;
        this.affinityTable = affinityTable;
        this.deadline = deadline;
        this.random = new Random(seed);
        this.alpha = 100.0;  // High penalty for deadline violations
        this.beta = 10.0;    // Moderate weight for affinity
        this.gamma = 5.0;    // Lower weight for fragmentation
    }

    /**
     * Solution representation: task-to-VM mapping
     */
    private static class Solution {
        int[] taskToVmMapping; // Index into availableVms list
        double fitness;

        Solution(int numTasks) {
            taskToVmMapping = new int[numTasks];
            fitness = Double.MAX_VALUE;
        }

        Solution copy() {
            Solution copy = new Solution(taskToVmMapping.length);
            System.arraycopy(taskToVmMapping, 0, copy.taskToVmMapping, 0, taskToVmMapping.length);
            copy.fitness = fitness;
            return copy;
        }
    }

    /**
     * Particle for PSO
     */
    private class Particle {
        double[] position;
        double[] velocity;
        Solution currentSolution;
        Solution personalBest;

        Particle(int dimension) {
            position = new double[dimension];
            velocity = new double[dimension];
            currentSolution = new Solution(dimension);
            personalBest = null;
        }
    }

    /**
     * Run PSO to find optimal mapping
     */
    public Map<Task, VmType> findOptimalMapping() {
        int dimension = workflow.size();
        
        // Initialize particles
        Particle[] particles = new Particle[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            particles[i] = initializeParticle(dimension);
        }

        Solution globalBest = null;

        // PSO iterations
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            for (Particle particle : particles) {
                // Update velocity and position
                updateParticle(particle, globalBest);
                
                // Decode position to solution
                decodeSolution(particle);
                
                // Evaluate fitness
                double fitness = evaluateFitness(particle.currentSolution);
                particle.currentSolution.fitness = fitness;

                // Update personal best
                if (particle.personalBest == null || fitness < particle.personalBest.fitness) {
                    particle.personalBest = particle.currentSolution.copy();
                }

                // Update global best
                if (globalBest == null || fitness < globalBest.fitness) {
                    globalBest = particle.currentSolution.copy();
                }
            }
        }

        // Convert best solution to mapping
        return solutionToMapping(globalBest);
    }

    /**
     * Initialize a particle
     */
    private Particle initializeParticle(int dimension) {
        Particle particle = new Particle(dimension);
        for (int i = 0; i < dimension; i++) {
            particle.position[i] = random.nextDouble() * availableVms.size();
            particle.velocity[i] = (random.nextDouble() - 0.5) * availableVms.size();
        }
        return particle;
    }

    /**
     * Update particle velocity and position
     */
    private void updateParticle(Particle particle, Solution globalBest) {
        for (int i = 0; i < particle.position.length; i++) {
            double r1 = random.nextDouble();
            double r2 = random.nextDouble();

            // Update velocity
            double cognitiveComponent = 0;
            if (particle.personalBest != null) {
                cognitiveComponent = C1 * r1 * (particle.personalBest.taskToVmMapping[i] - particle.position[i]);
            }

            double socialComponent = 0;
            if (globalBest != null) {
                socialComponent = C2 * r2 * (globalBest.taskToVmMapping[i] - particle.position[i]);
            }

            particle.velocity[i] = W * particle.velocity[i] + cognitiveComponent + socialComponent;

            // Limit velocity
            double maxVelocity = availableVms.size() / 2.0;
            particle.velocity[i] = Math.max(-maxVelocity, Math.min(maxVelocity, particle.velocity[i]));

            // Update position
            particle.position[i] += particle.velocity[i];

            // Boundary handling
            if (particle.position[i] < 0) {
                particle.position[i] = 0;
            } else if (particle.position[i] >= availableVms.size()) {
                particle.position[i] = availableVms.size() - 0.01;
            }
        }
    }

    /**
     * Decode particle position to solution
     */
    private void decodeSolution(Particle particle) {
        for (int i = 0; i < particle.position.length; i++) {
            particle.currentSolution.taskToVmMapping[i] = (int) particle.position[i];
        }
    }

    /**
     * Evaluate fitness of a solution
     */
    private double evaluateFitness(Solution solution) {
        // Calculate costs, makespan, affinity, fragmentation
        double totalCost = 0.0;
        double makespan = 0.0;
        double totalAffinity = 0.0;
        Set<Integer> usedVms = new HashSet<>();
        
        for (int i = 0; i < solution.taskToVmMapping.length; i++) {
            Task task = workflow.get(i);
            int vmIndex = solution.taskToVmMapping[i];
            VmType vmType = availableVms.get(vmIndex);
            
            ResourceProfile profile = profiles.get(task);
            
            // Calculate cost (simplified)
            double executionTime = task.getTaskSize() / (vmType.getVcpus() * 1000.0);
            totalCost += CostModel.computeVmCost(vmType, executionTime, "on_demand");
            
            // Track makespan (simplified - actual would need dependency analysis)
            makespan = Math.max(makespan, executionTime);
            
            // Calculate affinity
            double affinity = affinityTable.getAffinityScore(profile.getClassifiedType(), vmType.getFamily());
            totalAffinity += affinity;
            
            usedVms.add(vmIndex);
        }

        double avgAffinity = totalAffinity / solution.taskToVmMapping.length;
        double deadlinePenalty = CostModel.computeDeadlinePenalty(makespan, deadline);
        
        // Fragmentation (simplified)
        double fragmentation = CostModel.computeFragmentation(
            availableVms.size(), usedVms.size(), 0.7);

        return CostModel.computeFitness(totalCost, deadlinePenalty, avgAffinity, 
                                       fragmentation, alpha, beta, gamma);
    }

    /**
     * Convert solution to task-VM mapping
     */
    private Map<Task, VmType> solutionToMapping(Solution solution) {
        Map<Task, VmType> mapping = new HashMap<>();
        for (int i = 0; i < solution.taskToVmMapping.length; i++) {
            Task task = workflow.get(i);
            VmType vmType = availableVms.get(solution.taskToVmMapping[i]);
            mapping.put(task, vmType);
        }
        return mapping;
    }

    // Getters and setters for weights
    public void setAlpha(double alpha) { this.alpha = alpha; }
    public void setBeta(double beta) { this.beta = beta; }
    public void setGamma(double gamma) { this.gamma = gamma; }
}
