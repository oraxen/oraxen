package io.th0rgal.oraxen.utils;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

public class ModelEngineUtils {

    public static void addModel(ModeledEntity entity, ActiveModel model, boolean overrideHitboxes) {
        entity.addModel(model, overrideHitboxes);
    }

    public static void setRotationLock(ModeledEntity model, boolean lock) {
        model.setModelRotationLocked(lock);
    }
}
