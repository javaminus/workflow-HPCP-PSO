package cloud.workflowScheduling.rbdas;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Test runner for RBDAS unit and integration tests.
 */
public class TestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== Running RBDAS Test Suite ===\n");
        
        // Run all test classes
        Class<?>[] testClasses = {
            AffinityTableTest.class,
            CostModelTest.class,
            ClassifierTest.class,
            A2MDBFDTest.class,
            RBDASIntegrationTest.class
        };
        
        int totalTests = 0;
        int totalFailures = 0;
        
        for (Class<?> testClass : testClasses) {
            System.out.println("Running: " + testClass.getSimpleName());
            Result result = JUnitCore.runClasses(testClass);
            
            totalTests += result.getRunCount();
            totalFailures += result.getFailureCount();
            
            if (result.wasSuccessful()) {
                System.out.println("  ✓ All tests passed (" + result.getRunCount() + " tests)");
            } else {
                System.out.println("  ✗ " + result.getFailureCount() + " test(s) failed:");
                for (Failure failure : result.getFailures()) {
                    System.out.println("    - " + failure.getTestHeader());
                    System.out.println("      " + failure.getMessage());
                }
            }
            System.out.println();
        }
        
        System.out.println("=== Test Suite Summary ===");
        System.out.println("Total tests: " + totalTests);
        System.out.println("Passed: " + (totalTests - totalFailures));
        System.out.println("Failed: " + totalFailures);
        
        if (totalFailures == 0) {
            System.out.println("\n✓ All tests passed!");
            System.exit(0);
        } else {
            System.out.println("\n✗ Some tests failed");
            System.exit(1);
        }
    }
}
