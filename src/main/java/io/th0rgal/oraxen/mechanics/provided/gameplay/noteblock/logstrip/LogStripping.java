package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip;

import org.bukkit.configuration.ConfigurationSection;

public class LogStripping {

    private final String strippedLog;
    private final String dropFromStrippedLog;

    public LogStripping(ConfigurationSection logStripSection) {
        strippedLog = logStripSection.getString("strippedLogBlock");
        dropFromStrippedLog = logStripSection.getString("drop");
    }

    public boolean canBeStripped() { return strippedLog != null; }

    public String getStrippedLogBlock() { return strippedLog; }

    public boolean hasStrippedDrop() { return dropFromStrippedLog != null; }

    public String getStrippedLogDrop() { return dropFromStrippedLog; }
}
