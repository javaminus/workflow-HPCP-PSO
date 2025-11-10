# workflow-HPCP-PSO

This directory contains the main code of the following paper: 
Liwen Yang, Yuanqing Xia, Lingjuan Ye, Runze Gao, and Yufeng Zhan, "A Fully Hybrid Algorithm for Deadline Constrained Workflow Scheduling in Clouds," IEEE Transactions on Cloud Computing, accepted, 2023, https://doi.org/10.1109/TCC.2023.3269144.

It is redeveloped based on the framework of the following paper: 
Q. Wu, F. Ishikawa, Q. Zhu, Y. Xia, and J. Wen, "Deadline-constrained cost optimization approaches for workflow scheduling in clouds," IEEE Trans. Parallel Distrib. Syst., vol. 28, no. 12, pp. 3401–3412, Dec. 2017.

---

## RBDAS: Resource Bottleneck-Aware Dynamic Affinity Scheduling

This repository now includes an implementation of **Resource Bottleneck-Aware Dynamic Affinity Scheduling (RBDAS)**, a cloud workflow scheduling system that optimizes cost and performance through:

- **Resource Profiling**: Characterizes tasks by CPU, memory, I/O, and network intensity
- **Workload Classification**: Rule-based classification into CPU, MEM, IO, NET, GPU, or MIX types
- **Affinity-Aware Scheduling**: Maps workloads to VM types with high affinity scores
- **Multi-Pool VM Management**: Supports reserved, spot, and on-demand instance pools
- **PSO Optimization**: Uses Particle Swarm Optimization for task-to-VM mapping
- **Spot Instance Support**: Handles spot interruptions with checkpoint/resume semantics

### Build Requirements

- **JDK 11** or higher
- **Maven 3.6+**

### Quick Start

1. **Build the project:**
   ```bash
   mvn clean compile
   ```

2. **Run tests:**
   ```bash
   mvn test
   ```

3. **Run RBDAS on a workflow:**
   ```bash
   mvn exec:java -Dexec.mainClass="com.javaminus.workflow.rbdas.RunRbdas" \
     -Dexec.args="--dax files/dax/Montage_30.xml --seed 42"
   ```

4. **Run example batch script:**
   ```bash
   ./examples/run-rbdas.sh
   ```

### Configuration

RBDAS uses JSON configuration files located in the `config/` directory:

- **`config/vm_catalog.json`**: Defines VM types, capabilities (vCPUs, memory, storage, GPU), and pricing models (reserved, spot, on-demand)
- **`config/affinity_table.json`**: Maps workload types to VM families with affinity scores (0.0 to 1.0)

### Command Line Options

```bash
java com.javaminus.workflow.rbdas.RunRbdas [options]

Required:
  --dax <file>              Path to DAX workflow file

Optional:
  --config <file>           VM catalog JSON (default: config/vm_catalog.json)
  --affinity <file>         Affinity table JSON (default: config/affinity_table.json)
  --seed <number>           Random seed for reproducibility (default: 42)
  --no-spot                 Disable spot instances
  --deadline-factor <num>   Deadline factor multiplier (default: 1.5)
  --help                    Show help message
```

### Results

Execution results are saved as CSV files in the `results/` directory with timestamps. Metrics include:

- **Total Cost**: VM and egress costs
- **Makespan**: Workflow completion time
- **VM Count**: Number of VMs allocated
- **Resource Utilization**: Average utilization across VMs
- **Task Success/Failure**: Task completion statistics
- **Interruption Count**: Number of spot interruptions (if enabled)

### Examples

The `files/dax/` directory contains sample DAX workflow files:
- Montage (astronomy)
- LIGO (gravitational wave detection)
- Epigenomics (bioinformatics)
- CyberShake (earthquake hazard)

Run with different configurations:

```bash
# With spot instances (default)
mvn exec:java -Dexec.mainClass="com.javaminus.workflow.rbdas.RunRbdas" \
  -Dexec.args="--dax files/dax/LIGO_30.xml --seed 42"

# Without spot instances
mvn exec:java -Dexec.mainClass="com.javaminus.workflow.rbdas.RunRbdas" \
  -Dexec.args="--dax files/dax/Epigenomics_30.xml --no-spot --seed 42"

# Custom deadline factor
mvn exec:java -Dexec.mainClass="com.javaminus.workflow.rbdas.RunRbdas" \
  -Dexec.args="--dax files/dax/CyberShake_30.xml --deadline-factor 2.0"
```

### Architecture

RBDAS follows a pipeline architecture:

1. **ResourceProfiler**: Profiles task resource requirements
2. **RuleBasedClassifier**: Classifies tasks by workload type
3. **A2MDBFD**: Affinity-aware multi-dimensional Best-Fit Decreasing packing
4. **HNSPSOAdapter**: PSO-based optimization for task-VM mapping
5. **AffinityVmAllocationPolicy**: Manages VM pools and allocation
6. **SpotInterruptSimulator**: Simulates spot interruptions with checkpointing
7. **CostModel**: Computes costs and fitness functions

### Testing

Run the test suite:

```bash
mvn test
```

Tests include:
- **Unit Tests**: AffinityTable, CostModel, A2MDBFD packing logic
- **Integration Tests**: Full workflow execution with/without spot instances

### Continuous Integration

GitHub Actions automatically runs tests on pull requests and pushes to `main`. See `.github/workflows/maven-test.yml`.

### Known Limitations

- **Checkpoint Overhead**: Checkpointing overhead is not explicitly modeled
- **Classification Thresholds**: Default thresholds may need tuning for specific workflows
- **Affinity Weights**: Initial affinity scores are heuristic-based defaults
- **Simplified Execution Model**: Does not model full CloudSim event queue

### Configuration Options

Tune RBDAS behavior by editing configuration files:

**Classifier Thresholds** (default values):
- CPU: 1000.0
- Memory: 512.0 MB
- I/O: 100.0 MB
- Network: 50.0 Mbps

**PSO Parameters** (in `HNSPSOAdapter`):
- Population size: 50
- Max iterations: 100
- Inertia weight (W): 0.7
- Cognitive coefficient (C1): 1.5
- Social coefficient (C2): 1.5

**Fitness Weights**:
- Alpha (deadline penalty): 100.0
- Beta (affinity): 10.0
- Gamma (fragmentation): 5.0

### License

See LICENSE file for details.
