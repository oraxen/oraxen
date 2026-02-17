package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.util.Locale;

/**
 * Validation utility for multi-version resource pack configuration.
 * Checks for incompatible settings and missing dependencies at startup.
 */
public final class MultiVersionPackValidator {

    private MultiVersionPackValidator() {
    }

    /**
     * Validates multi-version pack configuration and logs warnings for issues.
     * Should be called during plugin initialization before pack generation.
     *
     * @return true if configuration is valid, false if there are blocking issues
     */
    public static boolean validateAndLogWarnings() {
        if (!Settings.MULTI_VERSION_PACKS.toBool()) {
            return true;
        }

        boolean hasErrors = false;

        String uploadType = Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT);
        if ("self-host".equals(uploadType)) {
            Logs.logError("Multi-version packs are incompatible with 'self-host' upload type!");
            Logs.logError("Self-host can only serve a single pack file at /pack.zip");
            Logs.logError("Change 'Pack.upload.type' to 'polymath' or 'external' to use multi-version packs");
            hasErrors = true;
        }

        if (!Settings.UPLOAD.toBool()) {
            Logs.logWarning("Multi-version packs are enabled but pack upload is disabled!");
            Logs.logWarning("Set 'Pack.upload.enabled: true' to upload packs to players");
        }

        boolean hasViaVersion = PluginUtils.isEnabled("ViaVersion") || PluginUtils.isEnabled("ViaBackwards");
        boolean hasProtocolSupport = PluginUtils.isEnabled("ProtocolSupport");

        if (!hasViaVersion && !hasProtocolSupport) {
            Logs.logWarning("Multi-version packs enabled but no version detection plugin found!");
            Logs.logWarning("Install ViaVersion or ProtocolSupport to detect player client versions");
            Logs.logWarning("Without version detection, all players will receive the server's pack version");
        }

        return !hasErrors;
    }

    /**
     * Checks if multi-version packs are enabled and properly configured.
     *
     * @return true if multi-version mode is active and valid
     */
    public static boolean isMultiVersionEnabledAndValid() {
        if (!Settings.MULTI_VERSION_PACKS.toBool()) {
            return false;
        }

        String uploadType = Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT);
        return !"self-host".equals(uploadType) && Settings.UPLOAD.toBool();
    }
}
