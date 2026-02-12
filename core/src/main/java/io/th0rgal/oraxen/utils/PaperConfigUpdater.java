package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to update Paper's paper-global.yml configuration file.
 * Uses line-by-line processing to preserve comments and formatting.
 * <p>
 * Disable auto-update via settings.yml: Plugin.auto_update_paper_config: false
 * Or via system property: -Doraxen.autoUpdatePaperConfig=false
 */
public final class PaperConfigUpdater {

    private static final String CONFIG_FILE = "config/paper-global.yml";

    private PaperConfigUpdater() {}

    /**
     * Ensures all block update settings are disabled in paper-global.yml.
     * Updates: disable-noteblock-updates, disable-tripwire-updates, disable-chorus-plant-updates
     * <p>
     * Only runs on Paper 1.20.1+ servers.
     *
     * @return list of setting names that were updated (empty if none changed)
     */
    public static List<String> ensureAllBlockUpdatesDisabled() {
        List<String> updatedSettings = new ArrayList<>();

        // Check if disabled via config or system property
        if (!Settings.AUTO_UPDATE_PAPER_CONFIG.toBool()) {
            return updatedSettings;
        }
        String prop = System.getProperty("oraxen.autoUpdatePaperConfig");
        if ("false".equalsIgnoreCase(prop)) {
            return updatedSettings;
        }

        if (!VersionUtil.isPaperServer() || !VersionUtil.atOrAbove("1.20.1")) {
            return updatedSettings;
        }

        Path configPath = Path.of(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            return updatedSettings;
        }

        try {
            List<String> lines = Files.readAllLines(configPath);
            List<String> updatedLines = new ArrayList<>(lines);

            boolean inBlockUpdates = false;
            int blockUpdatesIndent = -1;

            boolean updateNoteblockUpdates = !Settings.DISABLE_NOTE_BLOCK_FUNCTIONALITY.toBool();
            for (int i = 0; i < updatedLines.size(); i++) {
                String line = updatedLines.get(i);
                String trimmed = line.trim();

                // Track when we enter the block-updates section
                if (trimmed.startsWith("block-updates:")) {
                    inBlockUpdates = true;
                    blockUpdatesIndent = getIndent(line);
                    continue;
                }

                // Check if we've exited the block-updates section
                if (inBlockUpdates && !trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    int currentIndent = getIndent(line);
                    if (currentIndent <= blockUpdatesIndent) {
                        inBlockUpdates = false;
                    }
                }

                // Process settings within block-updates
                if (inBlockUpdates) {
                    String updated;
                    if (updateNoteblockUpdates) {
                        updated = tryUpdateSetting(line, "disable-noteblock-updates", updatedSettings);
                        if (updated != null) { updatedLines.set(i, updated); continue; }
                    }

                    updated = tryUpdateSetting(line, "disable-tripwire-updates", updatedSettings);
                    if (updated != null) { updatedLines.set(i, updated); continue; }

                    updated = tryUpdateSetting(line, "disable-chorus-plant-updates", updatedSettings);
                    if (updated != null) { updatedLines.set(i, updated); }
                }
            }

            // Only write if we made changes
            if (!updatedSettings.isEmpty()) {
                Files.write(configPath, updatedLines);
            }

        } catch (IOException e) {
            Logs.logWarning("Failed to auto-update paper-global.yml: " + e.getMessage());
        }

        return updatedSettings;
    }

    private static String tryUpdateSetting(String line, String settingName, List<String> updatedSettings) {
        // Pattern: "  disable-noteblock-updates: false" -> true
        Pattern pattern = Pattern.compile("^(\\s*)" + Pattern.quote(settingName) + ":\\s*false\\s*$");
        Matcher matcher = pattern.matcher(line);

        if (matcher.matches()) {
            updatedSettings.add(settingName);
            return matcher.group(1) + settingName + ": true";
        }
        return null;
    }

    private static int getIndent(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') indent++;
            else if (c == '\t') indent += 2;
            else break;
        }
        return indent;
    }
}
