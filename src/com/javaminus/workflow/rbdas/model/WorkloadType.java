package com.javaminus.workflow.rbdas.model;

/**
 * Workload classification types for RBDAS
 */
public enum WorkloadType {
    CPU,    // CPU-intensive workloads
    IO,     // I/O-intensive workloads
    MEM,    // Memory-intensive workloads
    NET,    // Network-intensive workloads
    GPU,    // GPU-accelerated workloads
    MIX     // Mixed workloads
}
