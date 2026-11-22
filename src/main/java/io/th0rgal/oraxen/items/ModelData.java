package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;

import java.util.*;

public class ModelData {

    public static final int STARTING_CMD = 1000;
    private final Material type;
    private final int modelData;
    public static final Map<Material, Map<String, Integer>> DATAS = new HashMap<>();
    private static volatile SkippedModelData skippedModelData;

    private record SkippedModelData(List<String> entries, Set<Integer> numbers) {
    }

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
        Map<String, Integer> usedModelDatas = new HashMap<>();
        if (!DATAS.containsKey(type) && !getSkippedCustomModelData().contains(STARTING_CMD)) {
            usedModelDatas.put(model, STARTING_CMD);
            DATAS.put(type, usedModelDatas);
            return STARTING_CMD;
        } else
            usedModelDatas = DATAS.getOrDefault(type, new HashMap<>());

        if (usedModelDatas.containsKey(model)) {
            return usedModelDatas.get(model);
        }

        Set<Integer> skippedCustomModelData = getSkippedCustomModelData();
        if (usedModelDatas.isEmpty()) {
            int newModelData = STARTING_CMD;
            while (skippedCustomModelData.contains(newModelData)) {
                newModelData++;
            }
            usedModelDatas.put(model, newModelData);
            DATAS.put(type, usedModelDatas);
            return newModelData;
        }

        int currentHighestModelData = Collections.max(usedModelDatas.values());
        for (int i = STARTING_CMD; i < currentHighestModelData; i++) {
            if (!usedModelDatas.containsValue(i)) { // if the id is available
                if (skippedCustomModelData.contains(i))
                    continue; // if the id should be skipped
                usedModelDatas.put(model, i);
                DATAS.put(type, usedModelDatas);
                return i;
            }
        }
        // if no durability was available between the chosen, let's create a new one
        // bigger
        int newHighestModelData = currentHighestModelData + 1;
        if (skippedCustomModelData.contains(newHighestModelData)) { // if the id should be skipped
            newHighestModelData = getNextNotSkippedCustomModelData(newHighestModelData, skippedCustomModelData);
        }

        usedModelDatas.put(model, newHighestModelData);
        DATAS.put(type, usedModelDatas);
        return newHighestModelData;
    }

    static int getNextNotSkippedCustomModelData(int i, Set<Integer> skippedCustomModelData) {
        int nextModelData = i;
        while (skippedCustomModelData.contains(nextModelData)) {
            nextModelData++;
        }
        return nextModelData;
    }

    // Parses the setting only when it changed, so a bad entry warns once instead of on every item.
    private static Set<Integer> getSkippedCustomModelData() {
        List<String> entries = Settings.SKIPPED_MODEL_DATA_NUMBERS.toStringList();
        SkippedModelData cached = skippedModelData;
        if (cached == null || !cached.entries().equals(entries)) {
            cached = new SkippedModelData(entries, parseSkippedCustomModelData(entries));
            skippedModelData = cached;
        }
        return cached.numbers();
    }

    private static Set<Integer> parseSkippedCustomModelData(List<String> entries) {
        Set<Integer> skippedCustomModelData = new HashSet<>();
        for (String entry : entries) {
            int separator = entry.indexOf('-');
            try {
                int min = Integer.parseInt((separator < 0 ? entry : entry.substring(0, separator)).trim());
                int max = separator < 0 ? min : Integer.parseInt(entry.substring(separator + 1).trim());
                for (long i = min; i <= max; i++)
                    skippedCustomModelData.add((int) i);
            } catch (NumberFormatException e) {
                Logs.logWarning("Invalid skipped model data number \"" + entry + "\" in settings.yml, ignoring it.");
            }
        }
        return Set.copyOf(skippedCustomModelData);
    }
}
