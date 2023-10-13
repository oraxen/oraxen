package io.th0rgal.oraxen.utils;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ModelEngineUtils {
    private static final ModelEngineVersion version = getVersion();
    private static Method setRotationLockMethod;
    private static Method addModel;

    static {
        try {
            Class<?> modeledEntityClass = Class.forName(ModeledEntity.class.getName());
            if (version == ModelEngineVersion.MEG_4) {
                setRotationLockMethod = modeledEntityClass.getDeclaredMethod("setModelRotationLocked", boolean.class);
            } else {
                setRotationLockMethod = modeledEntityClass.getDeclaredMethod("setModelRotationLock", boolean.class);
            }
            addModel = modeledEntityClass.getDeclaredMethod("addModel", ActiveModel.class, boolean.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    enum ModelEngineVersion {
        MEG_4, MEG_3
    }

    static ModelEngineVersion getVersion() {
        try {
            Class.forName("com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint");
            return ModelEngineVersion.MEG_4;
        } catch (Exception e) {
            return ModelEngineVersion.MEG_3;
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
