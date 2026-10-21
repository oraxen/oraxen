package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent registry and deterministic allocator for custom block {@code custom_variation} IDs.
 * <p>
 * When an item's block mechanic omits {@code custom_variation} (or sets it to {@code -1} / {@code "auto"}),
 * {@link VariationRegistry} assigns a non-colliding variation ID within the substrate's valid range
 * and persists the mapping in {@code plugins/Oraxen/data/allocated_variations.yml} so existing worlds
 * never shift block states across reloads or item additions.
 */
public final class VariationRegistry {

    private static final String DATA_FILE_PATH = "data/allocated_variations.yml";

    private static final Map<String, Map<String, Integer>> PERSISTED_BY_POOL = new ConcurrentHashMap<>();
    private static final Map<String, Set<Integer>> RESERVED_IN_SESSION = new ConcurrentHashMap<>();
    private static volatile boolean loadedFromDisk = false;

    private VariationRegistry() {
    }

    public static synchronized void resetSessionReservations() {
        RESERVED_IN_SESSION.clear();
    }

    public static synchronized void resetAllForTests() {
        PERSISTED_BY_POOL.clear();
        RESERVED_IN_SESSION.clear();
        loadedFromDisk = false;
    }

    /**
     * Resolves the {@code custom_variation} for an item's block mechanic section.
     * If an explicit valid integer is configured in YAML, it is reserved and returned;
     * if omitted or set to {@code -1} / {@code "auto"}, a persistent variation ID is allocated.
     */
    public static synchronized int resolveOrAllocate(
            @NotNull String poolName,
            @NotNull String itemId,
            @NotNull ConfigurationSection section,
            int minVariation,
            int maxVariation
    ) {
        ensureLoaded();

        Object rawValue = section.get("custom_variation");
        if (rawValue == null) {
            rawValue = section.get("custom-variation");
        }

        boolean autoRequested = rawValue == null
                || ("auto".equalsIgnoreCase(String.valueOf(rawValue).trim()))
                || (rawValue instanceof Number num && num.intValue() == -1);

        Set<Integer> sessionReserved = RESERVED_IN_SESSION.computeIfAbsent(poolName, k -> new HashSet<>());
        Map<String, Integer> poolMap = PERSISTED_BY_POOL.computeIfAbsent(poolName, k -> new LinkedHashMap<>());

        if (!autoRequested && rawValue instanceof Number explicitNumber) {
            int explicit = explicitNumber.intValue();
            sessionReserved.add(explicit);
            return explicit;
        }

        // Check if this item already has a persisted variation that is still within bounds and free
        Integer persisted = poolMap.get(itemId);
        if (persisted != null && persisted >= minVariation && persisted <= maxVariation
                && !sessionReserved.contains(persisted)) {
            sessionReserved.add(persisted);
            section.set("custom_variation", persisted);
            return persisted;
        }

        // Allocate the lowest available variation not reserved in this session or by another persisted item
        Set<Integer> allTaken = new HashSet<>(sessionReserved);
        for (Map.Entry<String, Integer> entry : poolMap.entrySet()) {
            if (!entry.getKey().equals(itemId)) {
                allTaken.add(entry.getValue());
            }
        }

        int allocated = -1;
        for (int candidate = minVariation; candidate <= maxVariation; candidate++) {
            if (!allTaken.contains(candidate)) {
                allocated = candidate;
                break;
            }
        }

        // Fallback if persisted entries exhausted the pool: pick any ID not reserved in the active session
        if (allocated == -1) {
            for (int candidate = minVariation; candidate <= maxVariation; candidate++) {
                if (!sessionReserved.contains(candidate)) {
                    allocated = candidate;
                    break;
                }
            }
        }

        if (allocated != -1) {
            sessionReserved.add(allocated);
            poolMap.put(itemId, allocated);
            section.set("custom_variation", allocated);
            saveToDisk();
            return allocated;
        }

        return section.getInt("custom_variation", minVariation);
    }

    private static void ensureLoaded() {
        if (loadedFromDisk) return;
        loadedFromDisk = true;

        File file = getStorageFile();
        if (file == null || !file.exists()) return;

        try {
            YamlConfiguration yaml = OraxenYaml.loadConfiguration(file);
            for (String pool : yaml.getKeys(false)) {
                ConfigurationSection poolSec = yaml.getConfigurationSection(pool);
                if (poolSec == null) continue;
                Map<String, Integer> map = PERSISTED_BY_POOL.computeIfAbsent(pool, k -> new LinkedHashMap<>());
                for (String itemId : poolSec.getKeys(false)) {
                    map.put(itemId, poolSec.getInt(itemId));
                }
            }
        } catch (Exception e) {
            Logs.logWarning("Failed to load allocated_variations.yml: " + e.getMessage());
        }
    }

    private static void saveToDisk() {
        File file = getStorageFile();
        if (file == null) return;

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<String, Map<String, Integer>> poolEntry : PERSISTED_BY_POOL.entrySet()) {
                ConfigurationSection poolSec = yaml.createSection(poolEntry.getKey());
                for (Map.Entry<String, Integer> itemEntry : poolEntry.getValue().entrySet()) {
                    poolSec.set(itemEntry.getKey(), itemEntry.getValue());
                }
            }
            yaml.save(file);
        } catch (IOException e) {
            Logs.logWarning("Failed to save allocated_variations.yml: " + e.getMessage());
        }
    }

    private static @Nullable File getStorageFile() {
        try {
            OraxenPlugin plugin = OraxenPlugin.get();
            if (plugin == null || plugin.getDataFolder() == null) return null;
            return new File(plugin.getDataFolder(), DATA_FILE_PATH);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
