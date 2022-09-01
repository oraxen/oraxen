package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.config.Settings;
import org.bukkit.Material;

import java.util.*;

public class ModelData {

    private final Material type;
    private final int durability;
    private static final Map<Material, Map<String, Integer>> DATAS = new HashMap<>();

    public ModelData(Material type, String model, int durability) {
        this.type = type;
        this.durability = durability;
        Map<String, Integer> usedDurabilities = DATAS.getOrDefault(type, new HashMap<>());
        usedDurabilities.put(model, durability);
        DATAS.put(type, usedDurabilities);
    }

    public Material getType() {
        return type;
    }

    public int getDurability() {
        return durability;
    }

    public static int generateId(String model, Material type) {
        Map<String, Integer> usedDurabilities;
        if (!DATAS.containsKey(type) && !getSkippedCustomModelData().contains(1)) {
            usedDurabilities = new HashMap<>();
            usedDurabilities.put(model, 1);
            DATAS.put(type, usedDurabilities);
            return 1;
        } else usedDurabilities = DATAS.get(type);

        if (usedDurabilities.containsKey(model)) {
            return usedDurabilities.get(model);
        }

        int currentMaxDurability = Collections.max(usedDurabilities.values());
        for (int i = 1; i < currentMaxDurability; i++) {
            if (!usedDurabilities.containsValue(i)) { // if the id is available
                if (getSkippedCustomModelData().contains(i)) // if the id should be skipped
                    continue;
                usedDurabilities.put(model, i);
                DATAS.put(type, usedDurabilities);
                return i;
            }
        }
        // if no durability was available between the chosen, let's create a new one
        // bigger
        int newMaxDurability = currentMaxDurability + 1;
        if (getSkippedCustomModelData().contains(newMaxDurability)) { // if the id should be skipped
            newMaxDurability = getNextNotSkippedCustomModelData(newMaxDurability);
        }

        usedDurabilities.put(model, newMaxDurability);
        DATAS.put(type, usedDurabilities);
        return newMaxDurability;
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
