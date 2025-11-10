package com.javaminus.workflow.rbdas.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.javaminus.workflow.rbdas.model.WorkloadType;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Affinity table loader and lookup utility
 */
public class AffinityTable {
    private Map<String, Map<String, Double>> affinityMappings;

    public AffinityTable() {
        affinityMappings = new HashMap<>();
    }

    /**
     * Load affinity table from JSON file
     */
    public static AffinityTable loadFromFile(String filepath) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filepath)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            AffinityTable table = new AffinityTable();
            
            JsonObject mappings = root.getAsJsonObject("mappings");
            for (String workloadType : mappings.keySet()) {
                JsonObject vmFamilies = mappings.getAsJsonObject(workloadType);
                Map<String, Double> familyScores = new HashMap<>();
                for (String vmFamily : vmFamilies.keySet()) {
                    familyScores.put(vmFamily, vmFamilies.get(vmFamily).getAsDouble());
                }
                table.affinityMappings.put(workloadType, familyScores);
            }
            
            return table;
        }
    }

    /**
     * Get affinity score for a workload type and VM family
     */
    public double getAffinityScore(WorkloadType workloadType, String vmFamily) {
        Map<String, Double> familyScores = affinityMappings.get(workloadType.name());
        if (familyScores == null) {
            return 0.5; // Default medium affinity
        }
        return familyScores.getOrDefault(vmFamily, 0.5);
    }

    /**
     * Get all affinity scores for a workload type
     */
    public Map<String, Double> getAffinityScores(WorkloadType workloadType) {
        return affinityMappings.getOrDefault(workloadType.name(), new HashMap<>());
    }
}
