package io.th0rgal.oraxen.utils;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ModelEngineUtils {
    private static final ModelEngineVersion version = getVersion();
    private static Method setRotationLockMethod;
    private static Method addModel;

    static {
        try {
            if (version != ModelEngineVersion.NONE) {
                Class<?> modeledEntityClass = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
                if (version == ModelEngineVersion.MEG_4) {
                    setRotationLockMethod = modeledEntityClass.getDeclaredMethod("setModelRotationLocked", boolean.class);
                } else if (version == ModelEngineVersion.MEG_3) {
                    setRotationLockMethod = modeledEntityClass.getDeclaredMethod("setModelRotationLock", boolean.class);
                }
                addModel = modeledEntityClass.getDeclaredMethod("addModel", ActiveModel.class, boolean.class);
            }
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }
    }

    public static boolean isModelEngineEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("ModelEngine");
    }

    enum ModelEngineVersion {
        MEG_4, MEG_3, NONE
    }

    static ModelEngineVersion getVersion() {
        try {
            Class.forName("com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint");
            return ModelEngineVersion.MEG_4;
        } catch (Exception e) {
            try {
                Class.forName("com.ticxo.modelengine.api.generator.model.ModelBluePrint");
                return ModelEngineVersion.MEG_3;
            } catch (Exception ignored) {
                return ModelEngineVersion.NONE;
            }
        }
    }

    public static void addModel(ModeledEntity entity, ActiveModel model, boolean overrideHitboxes) {
        try {
            addModel.invoke(entity, model, overrideHitboxes);
        } catch (IllegalAccessException | InvocationTargetException e) {
            if (Settings.DEBUG.toBool()) Logs.logError(e.getMessage());
        }
    }

    public static void setRotationLock(ModeledEntity model, boolean lock) {
        try {
            setRotationLockMethod.invoke(model, lock);
        } catch (IllegalAccessException | InvocationTargetException e) {
            if (Settings.DEBUG.toBool()) Logs.logError(e.getMessage());
        }
    }
}
