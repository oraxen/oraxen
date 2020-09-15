package io.th0rgal.oraxen.utils.logs;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;

import io.th0rgal.oraxen.language.LanguageProvider;
import io.th0rgal.oraxen.language.Variable;

import java.util.logging.Level;
import java.util.logging.LogRecord;

public class CustomLogger extends PluginLogger {

    CustomLogger(Plugin context) {
        super(context);
    }

    @Override
    public void log(LogRecord logRecord) {
        if (logRecord != null && logRecord.getLevel() != Level.INFO) {
            logRecord
                .setMessage(Variable.PREFIX.legacyMessage(LanguageProvider.DEFAULT_LANGUAGE) + ' ' + logRecord.getMessage());
            super.log(logRecord);
        }
    }

    void newLog(Level level, String message) {
        super.log(new LogRecord(level, message));
    }

}
