package io.th0rgal.oraxen.hopper;

import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import md.thomas.hopper.Dependency;
import md.thomas.hopper.DownloadResult;
import md.thomas.hopper.FailurePolicy;
import md.thomas.hopper.bukkit.BukkitHopper;
import md.thomas.hopper.version.UpdatePolicy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Handles automatic downloading of optional dependencies using Hopper.
 * <p>
 * This class registers and downloads PacketEvents if neither ProtocolLib nor PacketEvents
 * is installed on the server.
 */
public final class OraxenHopper {

    private static boolean downloadComplete = false;
    private static boolean requiresRestart = false;

    private OraxenHopper() {}

    /**
     * Registers dependencies with Hopper.
     * Should be called in the plugin constructor.
     *
     * @param plugin the Oraxen plugin instance
     */
    public static void register(@NotNull Plugin plugin) {
        BukkitHopper.register(plugin, deps -> {
            // Only download PacketEvents if neither ProtocolLib nor PacketEvents is available
            // We check for files in the plugins folder since plugins aren't loaded yet in constructor
            boolean hasProtocolLib = pluginJarExists("ProtocolLib");
            boolean hasPacketEvents = pluginJarExists("PacketEvents") || pluginJarExists("packetevents");

            if (!hasProtocolLib && !hasPacketEvents) {
                Logs.logInfo("Neither ProtocolLib nor PacketEvents detected, registering PacketEvents for download...");
                deps.require(Dependency.modrinth("packetevents")
                    .name("PacketEvents")
                    .minVersion("2.7.0")
                    .updatePolicy(UpdatePolicy.MINOR)
                    .onFailure(FailurePolicy.WARN_SKIP) // Don't fail if download fails
                    .build());
            }
        });
    }

    /**
     * Downloads all registered dependencies.
     * Should be called in the plugin's onLoad() method.
     *
     * @param plugin the Oraxen plugin instance
     * @return true if all dependencies are satisfied (no restart needed), false if restart is required
     */
    public static boolean download(@NotNull Plugin plugin) {
        DownloadResult result = BukkitHopper.download(plugin);
        BukkitHopper.logResult(plugin, result);

        downloadComplete = true;
        requiresRestart = result.requiresRestart();

        if (requiresRestart) {
            Logs.logWarning("=".repeat(60));
            Logs.logWarning("  ORAXEN - Dependencies Downloaded");
            Logs.logWarning("=".repeat(60));
            for (DownloadResult.DownloadedDependency dep : result.downloaded()) {
                Logs.logWarning("  + " + dep.name() + " v" + dep.version());
            }
            Logs.logWarning("");
            Logs.logWarning("  Please RESTART the server to load the new dependencies.");
            Logs.logWarning("  Oraxen will run in limited mode until restart.");
            Logs.logWarning("=".repeat(60));
        }

        return !requiresRestart;
    }

    /**
     * Checks if all dependencies are ready (downloaded and loaded).
     *
     * @param plugin the Oraxen plugin instance
     * @return true if ready
     */
    public static boolean isReady(@NotNull Plugin plugin) {
        return BukkitHopper.isReady(plugin);
    }

    /**
     * @return true if a restart is required to load newly downloaded dependencies
     */
    public static boolean requiresRestart() {
        return requiresRestart;
    }

    /**
     * @return true if the download phase has completed
     */
    public static boolean isDownloadComplete() {
        return downloadComplete;
    }

    /**
     * Checks if a plugin jar file exists in the plugins folder.
     * This is used during constructor phase when plugins haven't loaded yet.
     */
    private static boolean pluginJarExists(String pluginName) {
        java.io.File pluginsFolder = Bukkit.getPluginsFolder();
        if (pluginsFolder == null || !pluginsFolder.exists()) {
            return false;
        }

        java.io.File[] files = pluginsFolder.listFiles((dir, name) ->
            name.toLowerCase().contains(pluginName.toLowerCase()) && name.endsWith(".jar")
        );

        return files != null && files.length > 0;
    }
}
