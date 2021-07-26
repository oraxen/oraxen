package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.ClickListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.CustomListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public enum CustomEvent {

    CLICK(ClickListener::new);

    private final CustomListenerConstructor constructor;

    CustomEvent(CustomListenerConstructor constructor) {
        this.constructor = constructor;
    }

    @FunctionalInterface
    interface CustomListenerConstructor {
        CustomListener create(String itemID, CustomEvent event,
                              List<CustomCondition> conditions, List<CustomAction> actions);
    }

    private final List<String> params = new ArrayList<>();

    public static CustomEvent get(String event) {
        String[] eventParams = event.split(":");
        CustomEvent customEvent = CustomEvent.valueOf(eventParams[0]);
        customEvent.params.addAll(Arrays.asList(eventParams).subList(1, eventParams.length));
        return customEvent;
    }

    public List<String> getParams() {
        return params;
    }

    public CustomListener getListener(String itemID,
                                      List<CustomCondition> conditions,
                                      List<CustomAction> actions) {
        return constructor.create(itemID, this, conditions, actions);
    }
}
