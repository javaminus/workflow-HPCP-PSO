package cloud.workflowScheduling.rbdas;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Loads and manages affinity scores between task types and VM types from JSON configuration.
 * Affinity scores range from 0.0 (poor fit) to 1.0 (perfect fit).
 */
public class AffinityTable {
    
    public enum TaskType {
        CPU, MEM, IO, NET, GPU, MIX
    }
    
    private Map<String, Map<String, Double>> affinityScores;
    private static AffinityTable instance;
    
    private AffinityTable(String configPath) throws IOException, ParseException {
        loadFromJSON(configPath);
    }
    
    public static AffinityTable getInstance(String configPath) {
        if (instance == null) {
            try {
                instance = new AffinityTable(configPath);
            } catch (IOException | ParseException e) {
                throw new RuntimeException("Failed to load affinity table: " + e.getMessage(), e);
            }
        }
        return instance;
    }
    
    public static AffinityTable getInstance() {
        return getInstance("config/affinity_table.json");
    }
    
    private void loadFromJSON(String configPath) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader reader = new FileReader(configPath);
        JSONObject root = (JSONObject) parser.parse(reader);
        reader.close();
        
        affinityScores = new HashMap<>();
        JSONObject scoresObj = (JSONObject) root.get("affinityScores");
        
        for (Object key : scoresObj.keySet()) {
            String taskType = (String) key;
            JSONObject vmScores = (JSONObject) scoresObj.get(taskType);
            Map<String, Double> scores = new HashMap<>();
            
            for (Object vmKey : vmScores.keySet()) {
                String vmType = (String) vmKey;
                Object scoreObj = vmScores.get(vmType);
                double score = 0.0;
                
                if (scoreObj instanceof Long) {
                    score = ((Long) scoreObj).doubleValue();
                } else if (scoreObj instanceof Double) {
                    score = (Double) scoreObj;
                }
                
                scores.put(vmType, score);
            }
            
            affinityScores.put(taskType, scores);
        }
    }
    
    /**
     * Get affinity score between a task type and VM type name.
     * @param taskType The type of task (CPU, MEM, IO, NET, GPU, MIX)
     * @param vmTypeName The VM type name (e.g., "c5.large")
     * @return Affinity score between 0.0 and 1.0, or 0.5 as default if not found
     */
    public double getAffinity(TaskType taskType, String vmTypeName) {
        Map<String, Double> scores = affinityScores.get(taskType.name());
        if (scores == null) {
            return 0.5; // Default neutral affinity
        }
        return scores.getOrDefault(vmTypeName, 0.5);
    }
    
    /**
     * Get affinity score using task type ID (0-8 mapping to VM types).
     * This adapts to the existing VM.TYPE_NO system.
     */
    public double getAffinity(TaskType taskType, int vmTypeId) {
        String[] vmTypeNames = {
            "t2.small", "t2.medium", "c5.large", "c5.xlarge", 
            "c5.2xlarge", "r5.large", "r5.xlarge", "i3.large", "i3.xlarge"
        };
        
        if (vmTypeId < 0 || vmTypeId >= vmTypeNames.length) {
            return 0.5; // Default neutral affinity
        }
        
        return getAffinity(taskType, vmTypeNames[vmTypeId]);
    }
    
    /**
     * Get best VM type for a given task type based on affinity scores.
     * @return VM type ID with highest affinity
     */
    public int getBestVMType(TaskType taskType) {
        int bestType = 0;
        double bestScore = 0.0;
        
        for (int i = 0; i < 9; i++) {
            double score = getAffinity(taskType, i);
            if (score > bestScore) {
                bestScore = score;
                bestType = i;
            }
        }
        
        return bestType;
    }
}
