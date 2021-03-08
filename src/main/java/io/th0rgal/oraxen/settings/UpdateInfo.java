package io.th0rgal.oraxen.settings;

import java.io.File;
import java.lang.reflect.Method;

import org.bukkit.configuration.file.YamlConfiguration;

import com.syntaxphoenix.syntaxapi.reflection.ReflectionTools;
import com.syntaxphoenix.syntaxapi.utils.java.Exceptions;

import io.th0rgal.oraxen.utils.logs.Logs;

public class UpdateInfo implements Comparable<UpdateInfo> {

    private final Update update;
    private final Method method;

    private final Object instance;

    public UpdateInfo(Object instance, Update update, Method method) {
        this.update = update;
        this.method = method;

        this.instance = instance;
    }

    public Object getInstance() {
        return instance;
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

    public int getPriority() {
        return update.priority();
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
                ReflectionTools.execute(instance, method, configuration);
                break;
            case 1:
                ReflectionTools.execute(instance, method, configuration, file);
                break;
            case 2:
                ReflectionTools.execute(instance, method, file);
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

    @Override
    public int compareTo(UpdateInfo info) {
        return Integer.compare(update.priority(), info.update.priority());
    }

    @Override
    public String toString() {
        return method.getDeclaringClass().getSimpleName() + "(" + instance + ")" + " in " + method.getName()
            + " only files at '" + getPathAsString() + "' with required " + update.required() + " updates to "
            + update.version() + " and priority of " + update.priority();
    }

}
