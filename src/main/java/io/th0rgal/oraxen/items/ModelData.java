package io.th0rgal.oraxen.items;

import org.bukkit.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModelData {

    private final Material type;
    private final int durability;
    private static final Map<Material, Map<String, Integer>> datas = new HashMap<>();

    public ModelData(Material type, String model, int durability) {
        this.type = type;
        this.durability = durability;
        Map<String, Integer> usedDurabilities = datas.getOrDefault(type, new HashMap<>());
        usedDurabilities.put(model, durability);
        datas.put(type, usedDurabilities);
    }

    public Material getType() {
        return type;
    }

    public int getDurability() {
        return durability;
    }

    public static int generateId(String model, Material type) {
        Map<String, Integer> usedDurabilities;
        if (!datas.containsKey(type)) {
            usedDurabilities = new HashMap<>();
            usedDurabilities.put(model, 1);
            datas.put(type, usedDurabilities);
            return 1;
        } else
            usedDurabilities = datas.get(type);

        if (usedDurabilities.containsKey(model))
            return usedDurabilities.get(model);
        int currentMaxDurability = Collections.max(usedDurabilities.values());
        for (int i = 1; i < currentMaxDurability; i++) {
            if (!usedDurabilities.containsValue(i)) { // if the id is available
                usedDurabilities.put(model, i);
                datas.put(type, usedDurabilities);
                return i;
            }
        }
        //if no durability was available between the choosed, let's create a new one bigger
        int newMaxDurability = currentMaxDurability + 1;
        usedDurabilities.put(model, newMaxDurability);
        datas.put(type, usedDurabilities);
        return newMaxDurability;
    }
}
