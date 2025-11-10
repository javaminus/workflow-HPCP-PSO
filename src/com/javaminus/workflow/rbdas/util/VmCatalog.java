package com.javaminus.workflow.rbdas.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.javaminus.workflow.rbdas.model.VmType;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * VM catalog loader and manager
 */
public class VmCatalog {
    private List<VmType> vmTypes;
    private JsonObject egressPricing;

    public VmCatalog() {
        vmTypes = new ArrayList<>();
    }

    /**
     * Load VM catalog from JSON file
     */
    public static VmCatalog loadFromFile(String filepath) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filepath)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            VmCatalog catalog = new VmCatalog();
            
            JsonArray vmTypesArray = root.getAsJsonArray("vm_types");
            for (int i = 0; i < vmTypesArray.size(); i++) {
                JsonObject vmJson = vmTypesArray.get(i).getAsJsonObject();
                VmType vmType = gson.fromJson(vmJson, VmType.class);
                catalog.vmTypes.add(vmType);
            }
            
            if (root.has("egress_pricing")) {
                catalog.egressPricing = root.getAsJsonObject("egress_pricing");
            }
            
            return catalog;
        }
    }

    /**
     * Get all VM types
     */
    public List<VmType> getVmTypes() {
        return vmTypes;
    }

    /**
     * Get VM types by family
     */
    public List<VmType> getVmTypesByFamily(String family) {
        List<VmType> result = new ArrayList<>();
        for (VmType vmType : vmTypes) {
            if (vmType.getFamily().equals(family)) {
                result.add(vmType);
            }
        }
        return result;
    }

    /**
     * Get VM type by ID
     */
    public VmType getVmTypeById(String id) {
        for (VmType vmType : vmTypes) {
            if (vmType.getId().equals(id)) {
                return vmType;
            }
        }
        return null;
    }

    /**
     * Get egress pricing
     */
    public double getEgressPrice(String type) {
        if (egressPricing != null && egressPricing.has(type)) {
            return egressPricing.get(type).getAsDouble();
        }
        return 0.0;
    }
}
