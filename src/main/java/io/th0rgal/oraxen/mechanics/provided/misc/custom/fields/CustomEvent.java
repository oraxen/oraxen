package io.th0rgal.oraxen.mechanics.provided.misc.custom.fields;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.CustomListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomEvent {

    public final CustomEventType type;
    private final List<String> params = new ArrayList<>();

    public CustomEvent(String action) {
        String[] actionParams = action.split(":");
        type = CustomEventType.valueOf(actionParams[0]);
        params.addAll(Arrays.asList(actionParams).subList(1, actionParams.length));
    }

    public List<String> getParams() {
        return params;
    }

    public CustomListener getListener(String itemID,
                                      List<CustomCondition> conditions,
                                      List<CustomAction> actions) {
        return type.constructor.create(itemID, this, conditions, actions);
    }

}
