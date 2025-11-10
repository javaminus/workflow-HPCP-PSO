package examples;

import cloud.workflowScheduling.setting.Workflow;
import cloud.workflowScheduling.setting.Solution;
import cloud.workflowScheduling.setting.VM;
import cloud.workflowScheduling.rbdas.HNSPSOAdapter;
import cloud.workflowScheduling.rbdas.CostModel;
import cloud.workflowScheduling.rbdas.CostModel.PricingModel;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Example demonstrating RBDAS (Resource Bottleneck-Aware Dynamic Affinity Scheduling).
 * Compares RBDAS with and without spot instances.
 */
public class RBDASExample {
    
    public static void main(String[] args) {
        System.out.println("=== RBDAS Workflow Scheduling Example ===\n");
        
        // Test workflows
        String[] workflows = {
            "dax/Montage_30.xml",
            "dax/Montage_50.xml",
            "dax/Epigenomics_30.xml"
        };
        
        // Deadline factors (multipliers of fastest possible time)
        double[] deadlineFactors = {1.5, 2.0, 3.0};
        
        try {
            // Create results CSV
            FileWriter csvWriter = new FileWriter("results/rbdas_example_results.csv");
            csvWriter.write("Workflow,DeadlineFactor,UseSpot,VMCount,Makespan,Cost,Utilization\n");
            
            for (String workflowFile : workflows) {
                System.out.println("Processing: " + workflowFile);
                
                for (double df : deadlineFactors) {
                    // Test without spot instances
                    runExperiment(workflowFile, df, false, csvWriter);
                    
                    // Test with spot instances
                    runExperiment(workflowFile, df, true, csvWriter);
                }
                
                System.out.println();
            }
            
            csvWriter.close();
            System.out.println("Results saved to: results/rbdas_example_results.csv");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void runExperiment(String workflowFile, double deadlineFactor, 
                                     boolean useSpot, FileWriter csvWriter) 
            throws IOException {
        
        // Load workflow
        Workflow workflow = new Workflow(workflowFile);
        
        // Calculate and set deadline
        // Use fast schedule to estimate minimum time
        double minMakespan = estimateMinMakespan(workflow);
        workflow.setDeadline(minMakespan * deadlineFactor);
        
        // Create scheduler
        HNSPSOAdapter scheduler = new HNSPSOAdapter(useSpot);
        
        // Run scheduling
        long startTime = System.currentTimeMillis();
        Solution solution = scheduler.schedule(workflow);
        long endTime = System.currentTimeMillis();
        
        // Calculate metrics
        int vmCount = solution.size();
        double makespan = solution.calcMakespan();
        double cost = calculateTotalCost(solution, useSpot);
        double utilization = (double) workflow.size() / vmCount;
        
        // Print results
        String spotLabel = useSpot ? "SPOT" : "ON-DEMAND";
        System.out.printf("  DF=%.1f %s: VMs=%d, Makespan=%.2f, Cost=$%.2f, Util=%.2f, Time=%dms\n",
                         deadlineFactor, spotLabel, vmCount, makespan, cost, utilization,
                         (endTime - startTime));
        
        // Write to CSV
        String workflowName = workflowFile.substring(workflowFile.lastIndexOf('/') + 1, 
                                                     workflowFile.lastIndexOf('.'));
        csvWriter.write(String.format("%s,%.1f,%s,%d,%.2f,%.2f,%.2f\n",
                                     workflowName, deadlineFactor, useSpot, 
                                     vmCount, makespan, cost, utilization));
    }
    
    private static double estimateMinMakespan(Workflow workflow) {
        // Simple heuristic: sum all task sizes and divide by fastest VM speed
        double totalWork = 0.0;
        for (int i = 1; i < workflow.size() - 1; i++) {
            totalWork += workflow.get(i).getTaskSize();
        }
        
        // Use fastest VM speed
        double fastestSpeed = VM.SPEEDS[VM.FASTEST];
        return totalWork / fastestSpeed * 0.5; // Factor for parallelism
    }
    
    private static double calculateTotalCost(Solution solution, boolean useSpot) {
        CostModel costModel = CostModel.getInstance();
        double totalCost = 0.0;
        
        PricingModel model = useSpot ? PricingModel.SPOT : PricingModel.ON_DEMAND;
        
        for (VM vm : solution.keySet()) {
            if (solution.get(vm).isEmpty()) {
                continue;
            }
            
            // Calculate VM active time
            double startTime = Double.MAX_VALUE;
            double endTime = 0.0;
            
            var allocList = solution.get(vm);
            for (var alloc : allocList) {
                startTime = Math.min(startTime, alloc.getStartTime());
                endTime = Math.max(endTime, alloc.getFinishTime());
            }
            
            double duration = endTime - startTime;
            totalCost += costModel.calculateVMCost(vm.getType(), duration, model);
        }
        
        return totalCost;
    }
}
