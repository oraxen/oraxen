package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles bStats metrics collection for Oraxen.
 * Provides insights into plugin usage, download sources, and feature adoption.
 */
public class OraxenMetrics {

    private static final int BSTATS_ID = 5371;

    /**
     * Registers all custom bStats charts for Oraxen.
     *
     * @param plugin The Oraxen plugin instance
     */
    public static void register(OraxenPlugin plugin) {
        Metrics metrics = new Metrics(plugin, BSTATS_ID);

        // Download source detection
        metrics.addCustomChart(new SimplePie("download_source", OraxenMetrics::detectDownloadSource));
        metrics.addCustomChart(new SimplePie("license_status", OraxenMetrics::detectLicenseStatus));

        // Server information (minecraft_version is provided natively by bStats)
        metrics.addCustomChart(new SimplePie("server_type", OraxenMetrics::detectServerType));

        // Plugin features
        metrics.addCustomChart(new SingleLineChart("custom_items_count", () -> OraxenItems.getItems().size()));
        metrics.addCustomChart(new SimplePie("pack_upload_type", () -> Settings.UPLOAD_TYPE.toString()));
        metrics.addCustomChart(new SimplePie("glyph_handler", () -> Settings.GLYPH_HANDLER.toString()));

        // Mechanics usage
        metrics.addCustomChart(new AdvancedPie("enabled_mechanics", OraxenMetrics::getEnabledMechanics));

        // Compatibility plugins
        metrics.addCustomChart(new DrilldownPie("compatibility_plugins", OraxenMetrics::getCompatibilityPlugins));
    }

    /**
     * Detects the download source based on placeholder replacement.
     * Placeholders are replaced by marketplaces (Spigot/Polymart) when downloaded.
     */
    private static String detectDownloadSource() {
        // Check for leaked copies first
        if (VersionUtil.isLeaked()) {
            return "Leaked";
        }

        // Check if compiled from source
        if (VersionUtil.isCompiled()) {
            return VersionUtil.isValidCompiler() ? "Official Build" : "Self Compiled";
        }

        DownloadSourceInfo info = new DownloadSourceInfo();

        // Polymart replaces %%__POLYMART__%% with a value
        if (!info.isPolymartPlaceholder()) {
            return "Polymart";
        }

        // Spigot replaces %%__USER__%% but not %%__POLYMART__%%
        if (!info.isUserPlaceholder()) {
            return "Spigot";
        }

        // All placeholders intact - likely compiled or unknown source
        return "Unknown";
    }

    /**
     * Detects license status for Polymart users.
     */
    private static String detectLicenseStatus() {
        if (VersionUtil.isLeaked()) {
            return "Leaked";
        }

        if (VersionUtil.isCompiled()) {
            return "Compiled";
        }

        DownloadSourceInfo info = new DownloadSourceInfo();

        // Check if from Polymart
        if (!info.isPolymartPlaceholder()) {
            // Check if license placeholder was replaced
            return info.isLicensePlaceholder() ? "Unlicensed" : "Licensed";
        }

        // Not from Polymart
        return "N/A";
    }

    /**
     * Detects the server type (Paper, Folia, Spigot).
     */
    private static String detectServerType() {
        if (VersionUtil.isFoliaServer()) {
            return "Folia";
        }
        if (VersionUtil.isPaperServer()) {
            return "Paper";
        }
        return "Spigot";
    }

    /**
     * Gets a map of enabled mechanics for the AdvancedPie chart.
     */
    private static Map<String, Integer> getEnabledMechanics() {
        Map<String, Integer> mechanics = new HashMap<>();

        // Key mechanics to track
        Set<String> trackedMechanics = Set.of(
                "furniture", "noteblock", "stringblock", "block",
                "durability", "repair", "efficiency",
                "armor_effects", "backpack", "custom",
                "soulbound", "consumable", "commands",
                "hat", "aura", "skin",
                "thor", "lifeleech", "bleeding",
                "bigmining", "smelting", "harvesting");

        for (String mechanicId : trackedMechanics) {
            if (MechanicsManager.getMechanicFactory(mechanicId) != null) {
                mechanics.put(mechanicId, 1);
            }
        }

        return mechanics;
    }

    /**
     * Gets compatibility plugins for the DrilldownPie chart.
     * Groups by category -> plugin name.
     */
    private static Map<String, Map<String, Integer>> getCompatibilityPlugins() {
        Map<String, Map<String, Integer>> categories = new HashMap<>();

        // Packet handling
        Map<String, Integer> packets = new HashMap<>();
        if (CompatibilitiesManager.hasPlugin("ProtocolLib"))
            packets.put("ProtocolLib", 1);
        if (CompatibilitiesManager.hasPlugin("packetevents"))
            packets.put("PacketEvents", 1);
        if (!packets.isEmpty())
            categories.put("Packet Handling", packets);

        // World management
        Map<String, Integer> world = new HashMap<>();
        if (CompatibilitiesManager.hasPlugin("WorldEdit"))
            world.put("WorldEdit", 1);
        if (CompatibilitiesManager.hasPlugin("FastAsyncWorldEdit"))
            world.put("FAWE", 1);
        if (!world.isEmpty())
            categories.put("World Management", world);

        // Economy/Items
        Map<String, Integer> items = new HashMap<>();
        if (CompatibilitiesManager.hasPlugin("MMOItems"))
            items.put("MMOItems", 1);
        if (CompatibilitiesManager.hasPlugin("MythicMobs"))
            items.put("MythicMobs", 1);
        if (CompatibilitiesManager.hasPlugin("MythicCrucible"))
            items.put("MythicCrucible", 1);
        if (CompatibilitiesManager.hasPlugin("EcoItems"))
            items.put("EcoItems", 1);
        if (CompatibilitiesManager.hasPlugin("ItemsAdder"))
            items.put("ItemsAdder", 1);
        if (!items.isEmpty())
            categories.put("Custom Items", items);

        // Placeholders
        Map<String, Integer> placeholders = new HashMap<>();
        if (CompatibilitiesManager.hasPlugin("PlaceholderAPI"))
            placeholders.put("PlaceholderAPI", 1);
        if (!placeholders.isEmpty())
            categories.put("Placeholders", placeholders);

        // Model/Entity
        Map<String, Integer> models = new HashMap<>();
        if (CompatibilitiesManager.hasPlugin("ModelEngine"))
            models.put("ModelEngine", 1);
        if (!models.isEmpty())
            categories.put("Models", models);

        // Protection
        Map<String, Integer> protection = new HashMap<>();
        if (CompatibilitiesManager.hasPlugin("BlockLocker"))
            protection.put("BlockLocker", 1);
        if (!protection.isEmpty())
            categories.put("Protection", protection);

        return categories;
    }

    /**
     * Helper class to check download source placeholders.
     * The placeholders are replaced by marketplaces when the plugin is downloaded.
     */
    private static class DownloadSourceInfo {
        // These fields mirror LU.java placeholders
        private final String userId = "%%__USER__%%";
        private final String nonce = "%%__NONCE__%%";
        private final String polymart = "%%__POLYMART__%%";
        private final String license = "%%__LICENSE__%%";
        private final String resourceId = "%%__RESOURCE__%%";

        /**
         * Checks if the user placeholder is still a placeholder (not replaced).
         */
        boolean isUserPlaceholder() {
            return userId.contains("%%");
        }

        /**
         * Checks if the Polymart placeholder is still a placeholder (not replaced).
         */
        boolean isPolymartPlaceholder() {
            return polymart.contains("%%");
        }

        /**
         * Checks if the license placeholder is still a placeholder (not replaced).
         */
        boolean isLicensePlaceholder() {
            return license.contains("%%");
        }

        /**
         * Checks if the nonce placeholder is still a placeholder (not replaced).
         */
        boolean isNoncePlaceholder() {
            return nonce.contains("%%");
        }

        /**
         * Checks if the resource ID placeholder is still a placeholder (not replaced).
         */
        boolean isResourcePlaceholder() {
            return resourceId.contains("%%");
        }
    }
}
