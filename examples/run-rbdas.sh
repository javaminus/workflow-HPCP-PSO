#!/bin/bash
# Example script to run RBDAS on DAX workflows

set -e

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Build the project first
echo "Building project..."
mvn clean compile -DskipTests

# Define DAX files to test
DAX_FILES=(
    "files/dax/Montage_30.xml"
    "files/dax/LIGO_30.xml"
    "files/dax/Epigenomics_30.xml"
    "files/dax/CyberShake_30.xml"
)

# Run with spot enabled
echo ""
echo "==================================="
echo "Running workflows WITH spot instances"
echo "==================================="
for dax in "${DAX_FILES[@]}"; do
    if [ -f "$dax" ]; then
        echo ""
        echo "Processing: $dax"
        mvn exec:java -Dexec.mainClass="com.javaminus.workflow.rbdas.RunRbdas" \
            -Dexec.args="--dax $dax --seed 42" -q
    else
        echo "Warning: File not found: $dax"
    fi
done

# Run with spot disabled
echo ""
echo "==================================="
echo "Running workflows WITHOUT spot instances"
echo "==================================="
for dax in "${DAX_FILES[@]}"; do
    if [ -f "$dax" ]; then
        echo ""
        echo "Processing: $dax"
        mvn exec:java -Dexec.mainClass="com.javaminus.workflow.rbdas.RunRbdas" \
            -Dexec.args="--dax $dax --no-spot --seed 42" -q
    else
        echo "Warning: File not found: $dax"
    fi
done

echo ""
echo "==================================="
echo "All workflows executed successfully!"
echo "Results saved in results/ directory"
echo "==================================="
