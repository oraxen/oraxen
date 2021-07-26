package io.th0rgal.oraxen.utils.logs;

import io.th0rgal.oraxen.config.Message;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.LogRecord;

public class CustomLogger extends PluginLogger {

    CustomLogger(Plugin context) {
        super(context);
    }

    @Override
    public void log(@NotNull LogRecord logRecord) {
        if (logRecord != null && logRecord.getLevel() != Level.INFO) {
            try {
                logRecord.setMessage(Message.PREFIX.toString() + ' ' + logRecord.getMessage());
            } catch (NullPointerException exception) {
                logRecord.setMessage("Oraxen | " + logRecord.getMessage());
            }
            super.log(logRecord);
        }
    }

    void newLog(Level level, String message) {
        super.log(new LogRecord(level, message));
    }

}
