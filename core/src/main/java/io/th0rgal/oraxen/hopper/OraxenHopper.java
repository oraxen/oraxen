package io.th0rgal.oraxen.hopper;

import md.thomas.hopper.Dependency;
import md.thomas.hopper.FailurePolicy;
import md.thomas.hopper.LogLevel;
import md.thomas.hopper.Platform;
import md.thomas.hopper.bukkit.BukkitHopper;
import md.thomas.hopper.version.UpdatePolicy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles automatic downloading of dependencies using Hopper.
 * <p>
 * This class registers and downloads:
 * <ul>
 *   <li>CommandAPI - Required for Oraxen commands</li>
 *   <li>PacketEvents - If neither ProtocolLib nor PacketEvents is installed</li>
 * </ul>
 * Downloaded plugins are automatically loaded at runtime without requiring a server restart.
 */
public final class OraxenHopper {

    private static boolean downloadComplete = false;
    private static boolean requiresRestart = false;
    private static boolean enabled = true;

    // Patterns to match plugin jar files
    // Matches: CommandAPI.jar, CommandAPI-11.0.0.jar, CommandAPI-11.0.0-Paper.jar
    private static final Pattern COMMANDAPI_PATTERN = Pattern.compile(
        "(?i)^commandapi([-_][\\d][\\w.-]*)?\\.jar$"
    );
    // Matches: ProtocolLib.jar, ProtocolLib-5.4.0.jar
    private static final Pattern PROTOCOLLIB_PATTERN = Pattern.compile(
        "(?i)^protocollib([-_][\\d][\\w.-]*)?\\.jar$"
    );
    // Matches: packetevents.jar, packetevents-spigot-2.11.1.jar
    private static final Pattern PACKETEVENTS_PATTERN = Pattern.compile(
        "(?i)^packetevents([-_][\\w.-]*)?\\.jar$"
    );

    private OraxenHopper() {}

    /**
     * Registers dependencies with Hopper.
     * Should be called in the plugin constructor.
     *
     * @param plugin the Oraxen plugin instance
     */
    public static void register(@NotNull Plugin plugin) {
        Logger logger = plugin.getLogger();

        // Check if auto-download is enabled via system property (config not loaded in constructor)
        String prop = System.getProperty("oraxen.autoDownloadDependencies");
        if ("false".equalsIgnoreCase(prop)) {
            enabled = false;
            logger.info("Auto-download of dependencies is disabled");
            return;
        }

        Platform platform = Platform.detect();

        BukkitHopper.register(plugin, deps -> {
            // We check for files in the plugins folder since plugins aren't loaded yet in constructor
            boolean hasCommandAPI = pluginJarExists(COMMANDAPI_PATTERN);
            boolean hasProtocolLib = pluginJarExists(PROTOCOLLIB_PATTERN);
            boolean hasPacketEvents = pluginJarExists(PACKETEVENTS_PATTERN);

            // CommandAPI is required for Oraxen commands
            if (!hasCommandAPI) {
                // Primary source: Modrinth
                deps.require(Dependency.modrinth("commandapi")
                    .name("CommandAPI")
                    .minVersion("11.0.0")
                    .updatePolicy(UpdatePolicy.MINOR)
                    .onFailure(FailurePolicy.WARN_SKIP)
                    .build());

                // Fallback source: GitHub releases
                String commandApiPattern = switch (platform) {
                    case PAPER, FOLIA, PURPUR -> "*-Paper.jar";
                    default -> "*-Spigot.jar";
                };
                deps.require(Dependency.github("CommandAPI/CommandAPI")
                    .name("CommandAPI")
                    .minVersion("11.0.0")
                    .assetPattern(commandApiPattern)
                    .updatePolicy(UpdatePolicy.MINOR)
                    .onFailure(FailurePolicy.WARN_SKIP)
                    .build());
            }

            // PacketEvents is optional but recommended (if neither ProtocolLib nor PacketEvents is available)
            if (!hasProtocolLib && !hasPacketEvents) {
                // Primary source: Modrinth (auto-detects platform for correct spigot/paper variant)
                deps.require(Dependency.modrinth("packetevents")
                    .name("PacketEvents")
                    .minVersion("2.7.0")
                    .updatePolicy(UpdatePolicy.MINOR)
                    .onFailure(FailurePolicy.WARN_SKIP)
                    .build());

                // Fallback source: GitHub releases
                String packetEventsPattern = "*-spigot-*.jar"; // Works for all platforms
                deps.require(Dependency.github("retrooper/packetevents")
                    .name("PacketEvents")
                    .minVersion("2.7.0")
                    .assetPattern(packetEventsPattern)
                    .updatePolicy(UpdatePolicy.MINOR)
                    .onFailure(FailurePolicy.WARN_SKIP)
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
        if (!enabled) {
            downloadComplete = true;
            return true;
        }

        Logger logger = plugin.getLogger();
        BukkitHopper.DownloadAndLoadResult result = BukkitHopper.downloadAndLoad(plugin, LogLevel.QUIET);

        downloadComplete = true;
        requiresRestart = !result.noRestartRequired();

        if (requiresRestart) {
            // Some plugins couldn't be auto-loaded - log details
            logger.warning("Some dependencies require a server restart to load:");
            for (var failed : result.loadResult().failed()) {
                logger.warning("  - " + failed.path().getFileName() + ": " + failed.error());
            }
        }
        // Hopper's QUIET mode already logs: "[Hopper] Loaded: CommandAPI 11.1.0, PacketEvents 2.11.1"

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
     * Checks if a plugin jar file exists in the plugins folder using a regex pattern.
     * This is used during constructor phase when plugins haven't loaded yet.
     *
     * @param pattern regex pattern to match the plugin jar filename
     * @return true if a matching jar exists
     */
    private static boolean pluginJarExists(Pattern pattern) {
        java.io.File pluginsFolder = Bukkit.getPluginsFolder();
        if (pluginsFolder == null || !pluginsFolder.exists()) {
            return false;
        }

        java.io.File[] files = pluginsFolder.listFiles((dir, name) ->
            pattern.matcher(name).matches()
        );

        return files != null && files.length > 0;
    }
}
