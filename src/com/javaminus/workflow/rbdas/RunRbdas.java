package com.javaminus.workflow.rbdas;

import cloud.workflowScheduling.setting.Workflow;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main entry point for running RBDAS on workflow DAX files
 */
public class RunRbdas {
    private static final long DEFAULT_SEED = 42L;
    private static final String DEFAULT_VM_CATALOG = "config/vm_catalog.json";
    private static final String DEFAULT_AFFINITY_TABLE = "config/affinity_table.json";
    private static final String RESULTS_DIR = "results";

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            String daxFile = null;
            String vmCatalog = DEFAULT_VM_CATALOG;
            String affinityTable = DEFAULT_AFFINITY_TABLE;
            long seed = DEFAULT_SEED;
            boolean spotEnabled = true;
            double deadlineFactor = 1.5; // 1.5x the critical path length

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--dax":
                        if (i + 1 < args.length) daxFile = args[++i];
                        break;
                    case "--config":
                        if (i + 1 < args.length) vmCatalog = args[++i];
                        break;
                    case "--affinity":
                        if (i + 1 < args.length) affinityTable = args[++i];
                        break;
                    case "--seed":
                        if (i + 1 < args.length) seed = Long.parseLong(args[++i]);
                        break;
                    case "--no-spot":
                        spotEnabled = false;
                        break;
                    case "--deadline-factor":
                        if (i + 1 < args.length) deadlineFactor = Double.parseDouble(args[++i]);
                        break;
                    case "--help":
                        printUsage();
                        return;
                }
            }

            if (daxFile == null) {
                System.err.println("Error: --dax parameter is required");
                printUsage();
                System.exit(1);
            }

            System.out.println("=== RBDAS Workflow Execution ===");
            System.out.println("DAX File: " + daxFile);
            System.out.println("VM Catalog: " + vmCatalog);
            System.out.println("Affinity Table: " + affinityTable);
            System.out.println("Random Seed: " + seed);
            System.out.println("Spot Enabled: " + spotEnabled);
            System.out.println("Deadline Factor: " + deadlineFactor);
            System.out.println();

            // Load workflow
            System.out.println("Loading workflow from " + daxFile + "...");
            Workflow workflow = new Workflow(daxFile);
            
            // Calculate deadline based on critical path
            double criticalPath = workflow.get(0).getbLevel();
            double deadline = criticalPath * deadlineFactor;
            workflow.setDeadline(deadline);
            
            System.out.println("Workflow loaded: " + workflow.size() + " tasks");
            System.out.println("Critical path length: " + criticalPath);
            System.out.println("Deadline: " + deadline);
            System.out.println();

            // Initialize broker
            AffinityDatacenterBroker broker = new AffinityDatacenterBroker(
                vmCatalog, affinityTable, seed, spotEnabled);

            // Execute workflow
            AffinityDatacenterBroker.ExecutionResult result = 
                broker.executeWorkflow(workflow, deadline);

            // Print results
            System.out.println("\n=== Execution Results ===");
            System.out.println(result);
            System.out.println("\nDetailed Metrics:");
            for (String key : result.metrics.keySet()) {
                System.out.println("  " + key + ": " + result.metrics.get(key));
            }

            // Save results to CSV
            saveResultsToCsv(daxFile, result, spotEnabled, seed);

            System.out.println("\nExecution completed successfully!");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java com.javaminus.workflow.rbdas.RunRbdas [options]");
        System.out.println();
        System.out.println("Required options:");
        System.out.println("  --dax <file>              Path to DAX workflow file");
        System.out.println();
        System.out.println("Optional options:");
        System.out.println("  --config <file>           Path to VM catalog JSON (default: config/vm_catalog.json)");
        System.out.println("  --affinity <file>         Path to affinity table JSON (default: config/affinity_table.json)");
        System.out.println("  --seed <number>           Random seed for reproducibility (default: 42)");
        System.out.println("  --no-spot                 Disable spot instances");
        System.out.println("  --deadline-factor <num>   Deadline factor multiplier (default: 1.5)");
        System.out.println("  --help                    Show this help message");
    }

    private static void saveResultsToCsv(String daxFile, 
                                        AffinityDatacenterBroker.ExecutionResult result,
                                        boolean spotEnabled, long seed) {
        try {
            // Create results directory if it doesn't exist
            File resultsDir = new File(RESULTS_DIR);
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }

            // Generate filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String workflowName = new File(daxFile).getName().replace(".xml", "");
            String filename = String.format("%s/rbdas_%s_%s.csv", RESULTS_DIR, workflowName, timestamp);

            // Check if file exists to determine if we need header
            File file = new File(filename);
            boolean writeHeader = !file.exists();

            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                if (writeHeader) {
                    writer.println("timestamp,workflow,spot_enabled,seed,total_cost,makespan," +
                                 "vm_count,avg_utilization,success_count,failure_count," +
                                 "interruption_count");
                }

                writer.println(String.format("%s,%s,%b,%d,%.2f,%.2f,%d,%.2f,%d,%d,%d",
                    timestamp, workflowName, spotEnabled, seed,
                    result.totalCost, result.makespan, result.vmCount,
                    result.avgResourceUtilization, result.taskSuccessCount,
                    result.taskFailureCount, result.interruptionCount));
            }

            System.out.println("\nResults saved to: " + filename);

        } catch (IOException e) {
            System.err.println("Warning: Could not save results to CSV: " + e.getMessage());
        }
    }
}
