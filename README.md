# workflow-HPCP-PSO

This directory contains the main code of the following paper: 
Liwen Yang, Yuanqing Xia, Lingjuan Ye, Runze Gao, and Yufeng Zhan, "A Fully Hybrid Algorithm for Deadline Constrained Workflow Scheduling in Clouds," IEEE Transactions on Cloud Computing, accepted, 2023, https://doi.org/10.1109/TCC.2023.3269144.

It is redeveloped based on the framework of the following paper: 
Q. Wu, F. Ishikawa, Q. Zhu, Y. Xia, and J. Wen, "Deadline-constrained cost optimization approaches for workflow scheduling in clouds," IEEE Trans. Parallel Distrib. Syst., vol. 28, no. 12, pp. 3401–3412, Dec. 2017.

---

## RBDAS Feature: Resource Bottleneck-Aware Dynamic Affinity Scheduling

This branch adds the **RBDAS (Resource Bottleneck-Aware Dynamic Affinity Scheduling)** feature, which provides:

- **Affinity-aware task-to-VM mapping** based on resource characteristics (CPU, Memory, I/O, Network, GPU)
- **Multi-pricing model support** (Pre-reserved, On-demand, Spot instances)
- **Spot instance interruption simulation** with checkpoint/restore capabilities
- **Hierarchical Nested Swarm PSO adapter** with affinity-aware fitness function
- **Cost optimization** while maintaining deadline constraints

### Key Components

#### Core Classes (in `src/cloud/workflowScheduling/rbdas/`)

1. **AffinityTable** - Manages affinity scores between task types and VM types
2. **ResourceProfiler** - Profiles tasks to estimate resource requirements
3. **RuleBasedClassifier** - Classifies tasks into CPU/MEM/IO/NET/GPU/MIX categories
4. **CostModel** - Calculates VM costs for different pricing models
5. **A2MDBFD** - Affinity-Aware Multi-Dimensional Best-Fit Decreasing packer
6. **HNSPSOAdapter** - PSO-based scheduler with affinity-aware fitness
7. **SpotInterruptSimulator** - Simulates spot instance interruptions
8. **CloudletCheckpoint** - Provides checkpoint/restore support for tasks

### Build Instructions

The project uses Java with external JAR dependencies. No Maven or Gradle required.

#### Prerequisites

- Java JDK 7 or higher
- The following JAR files (already included):
  - `commons-math3-3.3.jar`
  - `poi-4.1.0.jar`
  - `json-simple-1.1.1.jar`
  - `junit-4.13.2.jar`
  - `hamcrest-core-1.3.jar`

#### Compile the Project

```bash
# Compile RBDAS components
javac -encoding UTF-8 -cp ".:commons-math3-3.3.jar:poi-4.1.0.jar:json-simple-1.1.1.jar:bin" \
  -d bin src/cloud/workflowScheduling/rbdas/*.java

# Compile existing workflow scheduling code (if needed)
javac -encoding UTF-8 -cp ".:commons-math3-3.3.jar:poi-4.1.0.jar:json-simple-1.1.1.jar:bin" \
  -d bin src/cloud/workflowScheduling/setting/*.java

# Compile examples
javac -encoding UTF-8 -cp ".:commons-math3-3.3.jar:poi-4.1.0.jar:json-simple-1.1.1.jar:bin" \
  -d bin examples/*.java
```

### Running Tests

```bash
# Compile tests
javac -encoding UTF-8 -cp ".:commons-math3-3.3.jar:poi-4.1.0.jar:json-simple-1.1.1.jar:junit-4.13.2.jar:hamcrest-core-1.3.jar:bin" \
  -d bin test/cloud/workflowScheduling/rbdas/*.java

# Run test suite
java -cp ".:commons-math3-3.3.jar:poi-4.1.0.jar:json-simple-1.1.1.jar:junit-4.13.2.jar:hamcrest-core-1.3.jar:bin" \
  cloud.workflowScheduling.rbdas.TestRunner
```

Expected output:
```
=== Running RBDAS Test Suite ===
...
✓ All tests passed!
```

### Running Examples

```bash
# Run RBDAS example comparing spot vs on-demand
java -cp ".:commons-math3-3.3.jar:poi-4.1.0.jar:json-simple-1.1.1.jar:bin" \
  examples.RBDASExample
```

Results will be saved to `results/rbdas_example_results.csv`.

### Configuration Files

The RBDAS feature uses JSON configuration files in the `config/` directory:

#### `config/vm_catalog.json`
Defines VM types with their capabilities and pricing:
- VM specifications (CPU, memory, storage, I/O type)
- Pricing models (pre-reserved, on-demand, spot)
- Billing interval (default: 3600 seconds/1 hour)

#### `config/affinity_table.json`
Defines affinity scores (0.0-1.0) between task types and VM types:
- Task types: CPU, MEM, IO, NET, GPU, MIX
- VM types: t2.small, t2.medium, c5.large, c5.xlarge, c5.2xlarge, r5.large, r5.xlarge, i3.large, i3.xlarge
- Higher scores = better fit

### Interpreting Results

#### Metrics Collected

1. **VM Count** - Number of VMs leased
2. **Makespan** - Total workflow execution time
3. **Cost** - Total cost in dollars (including VM rental and data egress)
4. **Utilization** - Average tasks per VM (higher = better resource utilization)
5. **Success Rate** - Percentage of tasks completed successfully

#### Example Output

```
Workflow,DeadlineFactor,UseSpot,VMCount,Makespan,Cost,Utilization
Montage_30,1.5,false,12,450.23,5.67,2.5
Montage_30,1.5,true,12,450.23,2.13,2.5
```

- **Spot instances reduce cost** by ~60-70% compared to on-demand
- **Higher deadline factors** allow better consolidation (fewer VMs, lower cost)
- **Affinity-aware scheduling** improves performance by matching tasks to optimal VMs

### DAX Workflows

Example workflow files are located in the `dax/` directory:
- Montage (30, 50, 100, 1000 tasks)
- Epigenomics (30, 50, 100, 1000 tasks)
- LIGO/Inspiral (30, 50, 100, 1000 tasks)
- CyberShake (30, 50, 100, 1000 tasks)

### Known Limitations

1. **Simplified spot interruption model** - Uses probability-based simulation rather than historical data
2. **Checkpoint overhead** - Fixed at 2% rather than dynamic based on state size
3. **Deterministic RNG** - Uses fixed seeds for reproducibility in experiments
4. **No network topology** - Assumes flat network with uniform bandwidth
5. **Simplified cost model** - Data transfer costs are simplified

### Future Enhancements

- Integration with CloudSim for full event-based simulation
- Dynamic checkpoint interval optimization
- Multi-region VM allocation with geographic constraints
- Real-time spot price prediction
- Container-based scheduling support

### Citation

If you use the RBDAS feature in your research, please cite the original HPCP-PSO paper along with acknowledging the RBDAS implementation.
