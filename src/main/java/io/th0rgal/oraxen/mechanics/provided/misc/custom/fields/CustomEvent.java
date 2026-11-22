package io.th0rgal.oraxen.mechanics.provided.misc.custom.fields;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.CustomListener;
import io.th0rgal.oraxen.utils.actions.ClickAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CustomEvent {

    public final CustomEventType type;
    private final List<String> params = new ArrayList<>();
    private final boolean oneUsage;

    public CustomEvent(String action, boolean oneUsage) {
        String[] actionParams = action.split(":");
        String typeName = actionParams.length > 0 ? actionParams[0] : "";
        try {
            type = CustomEventType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown event \"" + typeName + "\". Valid events: "
                    + Arrays.stream(CustomEventType.values()).map(Enum::name).collect(Collectors.joining(", ")) + ".", e);
        }
        params.addAll(Arrays.asList(actionParams).subList(1, actionParams.length));
        this.oneUsage = oneUsage;
    }

    public List<String> getParams() {
        return params;
    }

    public CustomListener getListener(String itemID, long cooldown, ClickAction clickAction) {
        return type.constructor.create(itemID, cooldown, this, clickAction);
    }

    public boolean isOneUsage() {
        return oneUsage;
    }
}
