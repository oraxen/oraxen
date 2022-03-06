package io.th0rgal.oraxen.mechanics.provided.misc.custom.fields;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.ClickListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.CustomListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.DropListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.PickupListener;
import io.th0rgal.oraxen.utils.actions.ClickAction;

import java.util.List;

public enum CustomEventType {

    CLICK(ClickListener::new),
    DROP(DropListener::new),
    PICKUP(PickupListener::new);

    public final CustomListenerConstructor constructor;

    CustomEventType(CustomListenerConstructor constructor) {
        this.constructor = constructor;
    }

    @FunctionalInterface
    interface CustomListenerConstructor {
        CustomListener create(String itemID, long cooldown, CustomEvent event, ClickAction clickAction);
    }

}