package io.th0rgal.oraxen.items;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class ModelData {

    public static final int STARTING_CMD = 1000;
    private final Material type;
    private final int modelData;
    public static final Map<Material, Map<String, Integer>> DATAS = new HashMap<>();

    public ModelData(Material type, String model, int modelData) {
        this.type = type;
        this.modelData = modelData;
        Map<String, Integer> usedModelDatas = DATAS.getOrDefault(type, new HashMap<>());
        usedModelDatas.put(model, modelData);
        DATAS.put(type, usedModelDatas);
    }

    public Material getType() {
        return type;
    }

    public int getModelData() {
        return modelData;
    }

    public static int generateId(String model, Material type) {
        Map<String, Integer> usedModelDatas = DATAS.computeIfAbsent(type, ignored -> new HashMap<>());
        Integer existingModelData = usedModelDatas.get(model);
        if (existingModelData != null)
            return existingModelData;

        int modelData = STARTING_CMD;
        while (usedModelDatas.containsValue(modelData))
            modelData++;
        usedModelDatas.put(model, modelData);
        return modelData;
    }
}
