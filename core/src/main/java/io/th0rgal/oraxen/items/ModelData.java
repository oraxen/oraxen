package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.config.Settings;
import org.bukkit.Material;

import java.util.*;

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
        Map<String, Integer> usedModelDatas = new HashMap<>();
        if (!DATAS.containsKey(type) && !getSkippedCustomModelData().contains(STARTING_CMD)) {
            usedModelDatas.put(model, STARTING_CMD);
            DATAS.put(type, usedModelDatas);
            return STARTING_CMD;
        } else usedModelDatas = DATAS.getOrDefault(type, new HashMap<>());

        if (usedModelDatas.containsKey(model)) {
            return usedModelDatas.get(model);
        }

        int currentHighestModelData = Collections.max(usedModelDatas.values());
        for (int i = STARTING_CMD; i < currentHighestModelData; i++) {
            if (!usedModelDatas.containsValue(i)) { // if the id is available
                if (getSkippedCustomModelData().contains(i)) continue; // if the id should be skipped
                usedModelDatas.put(model, i);
                DATAS.put(type, usedModelDatas);
                return i;
            }
        }
        // if no durability was available between the chosen, let's create a new one
        // bigger
        int newHighestModelData = currentHighestModelData + 1;
        if (getSkippedCustomModelData().contains(newHighestModelData)) { // if the id should be skipped
            newHighestModelData = getNextNotSkippedCustomModelData(newHighestModelData);
        }

        usedModelDatas.put(model, newHighestModelData);
        DATAS.put(type, usedModelDatas);
        return newHighestModelData;
    }

    private static int getNextNotSkippedCustomModelData(int i) {
        List<Integer> sorted = new ArrayList<>(getSkippedCustomModelData());
        sorted.sort(Comparator.naturalOrder());
        return sorted.stream().filter(index -> index > i).toList().get(0);
    }

    private static Set<Integer> getSkippedCustomModelData() {
        Set<Integer> skippedCustomModelData = new HashSet<>();
        for (String s : Settings.SKIPPED_MODEL_DATA_NUMBERS.toStringList()) {
            if (s.contains("-")) {
                String[] s2 = s.split("-");
                int min = Integer.parseInt(s2[0]);
                int max = Integer.parseInt(s2[1]);
                for (int i = min; i <= max; i++)
                    skippedCustomModelData.add(i);
            } else skippedCustomModelData.add(Integer.parseInt(s));
        }
        return skippedCustomModelData;
    }
}
