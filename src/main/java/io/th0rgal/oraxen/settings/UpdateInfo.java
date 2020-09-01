package io.th0rgal.oraxen.settings;

import java.io.File;
import java.lang.reflect.Method;

import org.bukkit.configuration.file.YamlConfiguration;

import com.syntaxphoenix.syntaxapi.reflection.ReflectionTools;
import com.syntaxphoenix.syntaxapi.utils.java.Exceptions;

import io.th0rgal.oraxen.utils.logs.Logs;

public class UpdateInfo {

    private final Update update;
    private final Method method;

    public UpdateInfo(Update update, Method method) {
        this.update = update;
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public Update getUpdate() {
        return update;
    }

    public long getVersion() {
        return update.version();
    }

    public long getRequiredVersion() {
        return update.required();
    }

    public int getType() {
        return update.type();
    }

    public String[] getPath() {
        return update.path();
    }

    public String getPathAsString() {
        return String.join("/", update.path());
    }

    public boolean isApplyable(long version) {
        return update.required() <= version;
    }

    public boolean apply(File file, YamlConfiguration configuration) {
        try {
            switch (update.type()) {
            case 0:
                ReflectionTools.execute(null, method, configuration);
                break;
            case 1:
                ReflectionTools.execute(null, method, configuration, file);
                break;
            case 2:
                ReflectionTools.execute(null, method, file);
                break;
            default:
                break;
            }
            return true;
        } catch (Exception exception) {
            Logs.logError("Couldn't update " + file.getPath() + " to v" + update.version() + "!");
            Logs.logError(Exceptions.stackTraceToString(exception));
            return false;
        }
    }

}
