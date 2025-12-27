package io.th0rgal.oraxen.hopper;

import md.thomas.hopper.Dependency;
import md.thomas.hopper.FailurePolicy;
import md.thomas.hopper.bukkit.BukkitHopper;
import md.thomas.hopper.version.UpdatePolicy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Handles automatic downloading of optional dependencies using Hopper.
 * <p>
 * This class registers and downloads PacketEvents if neither ProtocolLib nor PacketEvents
 * is installed on the server. Downloaded plugins are automatically loaded at runtime
 * without requiring a server restart.
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
        Logger logger = plugin.getLogger();
        BukkitHopper.register(plugin, deps -> {
            // Only download PacketEvents if neither ProtocolLib nor PacketEvents is available
            // We check for files in the plugins folder since plugins aren't loaded yet in constructor
            boolean hasProtocolLib = pluginJarExists("ProtocolLib");
            boolean hasPacketEvents = pluginJarExists("PacketEvents") || pluginJarExists("packetevents");

            if (!hasProtocolLib && !hasPacketEvents) {
                logger.info("Neither ProtocolLib nor PacketEvents detected, registering PacketEvents for download...");
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
     * Downloads all registered dependencies and automatically loads them.
     * Should be called in the plugin's onLoad() method.
     * <p>
     * This method uses Hopper's auto-load feature to load downloaded plugins
     * at runtime without requiring a server restart.
     *
     * @param plugin the Oraxen plugin instance
     * @return true if all dependencies are satisfied and loaded, false if restart is required
     */
    public static boolean download(@NotNull Plugin plugin) {
        Logger logger = plugin.getLogger();
        BukkitHopper.DownloadAndLoadResult result = BukkitHopper.downloadAndLoad(plugin);

        downloadComplete = true;
        requiresRestart = !result.noRestartRequired();

        if (requiresRestart) {
            // Some plugins couldn't be auto-loaded
            logger.warning("=".repeat(60));
            logger.warning("  ORAXEN - Some Dependencies Require Restart");
            logger.warning("=".repeat(60));
            for (var failed : result.loadResult().failed()) {
                logger.warning("  ! " + failed.path().getFileName() + ": " + failed.error());
            }
            logger.warning("");
            logger.warning("  Please RESTART the server to load these dependencies.");
            logger.warning("  Oraxen will run in limited mode until restart.");
            logger.warning("=".repeat(60));
        } else if (result.loadResult().hasLoaded()) {
            // Successfully auto-loaded
            logger.info("=".repeat(60));
            logger.info("  ORAXEN - Dependencies Auto-Loaded Successfully");
            logger.info("=".repeat(60));
            for (var loaded : result.loadResult().loaded()) {
                logger.info("  + " + loaded.name() + " v" + loaded.version());
            }
            logger.info("  No restart required!");
            logger.info("=".repeat(60));
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
