package cloud.workflowScheduling.rbdas;

import cloud.workflowScheduling.setting.VM;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Cost model for computing VM costs (reserved/on-demand/spot) and egress fees.
 */
public class CostModel {
    
    public enum PricingModel {
        PRE_RESERVED,
        ON_DEMAND,
        SPOT
    }
    
    private static class VMPricing {
        double preReserved;
        double onDemand;
        double spot;
        
        VMPricing(double preReserved, double onDemand, double spot) {
            this.preReserved = preReserved;
            this.onDemand = onDemand;
            this.spot = spot;
        }
    }
    
    private Map<Integer, VMPricing> vmPricing;
    private double billingInterval;
    private double egressPricePerGB;
    private static CostModel instance;
    
    private CostModel(String configPath) throws IOException, ParseException {
        loadFromJSON(configPath);
    }
    
    public static CostModel getInstance(String configPath) {
        if (instance == null) {
            try {
                instance = new CostModel(configPath);
            } catch (IOException | ParseException e) {
                throw new RuntimeException("Failed to load cost model: " + e.getMessage(), e);
            }
        }
        return instance;
    }
    
    public static CostModel getInstance() {
        return getInstance("config/vm_catalog.json");
    }
    
    private void loadFromJSON(String configPath) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader reader = new FileReader(configPath);
        JSONObject root = (JSONObject) parser.parse(reader);
        reader.close();
        
        vmPricing = new HashMap<>();
        
        // Load billing interval
        Object intervalObj = root.get("billingInterval");
        if (intervalObj instanceof Long) {
            billingInterval = ((Long) intervalObj).doubleValue();
        } else {
            billingInterval = 3600.0; // Default 1 hour
        }
        
        // Load egress price
        Object egressObj = root.get("egressPricePerGB");
        if (egressObj instanceof Double) {
            egressPricePerGB = (Double) egressObj;
        } else if (egressObj instanceof Long) {
            egressPricePerGB = ((Long) egressObj).doubleValue();
        } else {
            egressPricePerGB = 0.09; // Default
        }
        
        // Load VM types
        JSONArray vmTypes = (JSONArray) root.get("vmTypes");
        for (Object obj : vmTypes) {
            JSONObject vmType = (JSONObject) obj;
            
            int id = ((Long) vmType.get("id")).intValue();
            JSONObject pricing = (JSONObject) vmType.get("pricing");
            
            double preReserved = getDouble(pricing.get("preReserved"));
            double onDemand = getDouble(pricing.get("onDemand"));
            double spot = getDouble(pricing.get("spot"));
            
            vmPricing.put(id, new VMPricing(preReserved, onDemand, spot));
        }
    }
    
    private double getDouble(Object obj) {
        if (obj instanceof Double) {
            return (Double) obj;
        } else if (obj instanceof Long) {
            return ((Long) obj).doubleValue();
        }
        return 0.0;
    }
    
    /**
     * Calculate cost for a VM running for a specific duration.
     * @param vmType The VM type ID
     * @param durationSeconds Duration in seconds
     * @param pricingModel The pricing model to use
     * @return Total cost in dollars
     */
    public double calculateVMCost(int vmType, double durationSeconds, PricingModel pricingModel) {
        VMPricing pricing = vmPricing.get(vmType);
        if (pricing == null) {
            // Fallback to existing VM cost model if not found
            return calculateVMCostFallback(vmType, durationSeconds);
        }
        
        // Calculate number of billing intervals (round up)
        double intervals = Math.ceil(durationSeconds / billingInterval);
        
        double hourlyRate = 0.0;
        switch (pricingModel) {
            case PRE_RESERVED:
                hourlyRate = pricing.preReserved;
                break;
            case ON_DEMAND:
                hourlyRate = pricing.onDemand;
                break;
            case SPOT:
                hourlyRate = pricing.spot;
                break;
        }
        
        return hourlyRate * intervals;
    }
    
    /**
     * Fallback to existing VM cost calculation for compatibility.
     */
    private double calculateVMCostFallback(int vmType, double durationSeconds) {
        if (vmType < 0 || vmType >= VM.TYPE_NO) {
            return 0.0;
        }
        
        double intervals = Math.ceil(durationSeconds / VM.INTERVAL);
        return VM.UNIT_COSTS[vmType] * intervals;
    }
    
    /**
     * Calculate egress cost for data transfer.
     * @param dataGB Data transferred in GB
     * @return Egress cost in dollars
     */
    public double calculateEgressCost(double dataGB) {
        return dataGB * egressPricePerGB;
    }
    
    /**
     * Get the unit cost per hour for a VM type and pricing model.
     */
    public double getUnitCost(int vmType, PricingModel pricingModel) {
        VMPricing pricing = vmPricing.get(vmType);
        if (pricing == null) {
            // Fallback to existing VM cost model
            if (vmType >= 0 && vmType < VM.TYPE_NO) {
                return VM.UNIT_COSTS[vmType];
            }
            return 0.0;
        }
        
        switch (pricingModel) {
            case PRE_RESERVED:
                return pricing.preReserved;
            case ON_DEMAND:
                return pricing.onDemand;
            case SPOT:
                return pricing.spot;
            default:
                return pricing.onDemand;
        }
    }
    
    /**
     * Get spot price ratio compared to on-demand.
     * @return Value between 0 and 1
     */
    public double getSpotPriceRatio(int vmType) {
        VMPricing pricing = vmPricing.get(vmType);
        if (pricing == null || pricing.onDemand == 0) {
            return 0.3; // Default 30% of on-demand
        }
        return pricing.spot / pricing.onDemand;
    }
}
