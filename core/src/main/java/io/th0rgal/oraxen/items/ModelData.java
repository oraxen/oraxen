package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ModelData {

    public static final int STARTING_CMD = 1000;
    private final Material type;
    private final int modelData;
    public static final Map<Material, Map<Key, Integer>> DATAS = new HashMap<>();

    public ModelData(Material type, OraxenMeta oraxenMeta, ConfigurationSection packSection) {
        this.type = type;
        this.modelData = packSection.getInt("custom_model_data");
        DATAS.compute(type, (mat, datas) -> {
           if (datas == null) datas = new HashMap<>();
           datas.put(oraxenMeta.modelKey(), modelData);
            return datas;
        });
    }

    public ModelData(Material type, Key model, int modelData) {
        this.type = type;
        this.modelData = modelData;
        DATAS.compute(type, (mat, datas) -> {
            if (datas == null) datas = new HashMap<>();
            datas.put(model, modelData);
            return datas;
        });
    }

    public Material getType() {
        return type;
    }

    public int getModelData() {
        return modelData;
    }

    public static int generateId(Key model, Material type) {
        Map<Key, Integer> usedModelDatas = new HashMap<>();
        if (!DATAS.containsKey(type) && !getSkippedCustomModelData(type).contains(STARTING_CMD)) {
            usedModelDatas.put(model, STARTING_CMD);
            DATAS.put(type, usedModelDatas);
            return STARTING_CMD;
        } else usedModelDatas = DATAS.getOrDefault(type, new HashMap<>());

        if (usedModelDatas.containsKey(model)) return usedModelDatas.get(model);

        int currentHighestModelData = Collections.max(usedModelDatas.values());
        for (int i = STARTING_CMD; i < currentHighestModelData; i++) {
            if (!usedModelDatas.containsValue(i)) { // if the id is available
                if (getSkippedCustomModelData(type).contains(i)) continue; // if the id should be skipped
                usedModelDatas.put(model, i);
                DATAS.put(type, usedModelDatas);
                return i;
            }
        }
        // if no durability was available between the chosen, let's create a new one
        // bigger
        int newHighestModelData = currentHighestModelData + 1;
        if (getSkippedCustomModelData(type).contains(newHighestModelData)) { // if the id should be skipped
            newHighestModelData = getNextNotSkippedCustomModelData(type, newHighestModelData);
        }

        usedModelDatas.put(model, newHighestModelData);
        DATAS.put(type, usedModelDatas);
        return newHighestModelData;
    }

    private static int getNextNotSkippedCustomModelData(Material type, int i) {
        List<Integer> sorted = new ArrayList<>(getSkippedCustomModelData(type));
        sorted.sort(Comparator.naturalOrder());
        return sorted.stream().filter(index -> index > i).toList().get(0);
    }

    private static Set<Integer> getSkippedCustomModelData(Material type) {
        Set<Integer> skippedCustomModelData = new HashSet<>();
        ConfigurationSection section = Settings.SKIPPED_MODEL_DATA_NUMBERS.toConfigSection();
        if (section == null || section.get(type.name()) == null) return skippedCustomModelData;

        String skippedString = section.getString(type.name().toLowerCase());
        skippedString = skippedString != null ? skippedString : section.getString(type.name());
        if (skippedString != null) {
            if (skippedString.contains("..")) {
                try {
                    String[] s = skippedString.split("..");
                    int min = Integer.parseInt(s[0]);
                    int max = Integer.parseInt(s[1]);
                    for (int i = min; i <= max; i++)
                        skippedCustomModelData.add(i);
                } catch (NumberFormatException e) {
                    Logs.logError("Invalid skipped model-data range for " + type.name() + " in settings.yml");
                    return skippedCustomModelData;
                }
            } else try {
                skippedCustomModelData.add(Integer.parseInt(skippedString));
            } catch (NumberFormatException e) {
                Logs.logError("Invalid skipped model-data number for " + type.name() + " in settings.yml");
                return skippedCustomModelData;
            }
        } else for (String s : section.getStringList(type.name().toLowerCase())) {
            if (s.contains("..")) {
                try {
                    String[] s2 = s.split("..");
                    int min = Integer.parseInt(s2[0]);
                    int max = Integer.parseInt(s2[1]);
                    for (int i = min; i <= max; i++)
                        skippedCustomModelData.add(i);
                } catch (NumberFormatException e) {
                    Logs.logError("Invalid skipped model-data range for " + type.name() + " in settings.yml");
                }
            } else try {
                skippedCustomModelData.add(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                Logs.logError("Invalid skipped model-data number for " + type.name() + " in settings.yml");
            }
        }

        return skippedCustomModelData;
    }
}
