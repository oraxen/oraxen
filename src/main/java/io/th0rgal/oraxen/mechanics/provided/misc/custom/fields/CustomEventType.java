package io.th0rgal.oraxen.mechanics.provided.misc.custom.fields;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.BreakListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.ClickListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.CustomListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.DeathListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.DropListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.EquipListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.InvClickListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.PickupListener;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.UnequipListener;
import io.th0rgal.oraxen.utils.actions.ClickAction;

public enum CustomEventType {

    BREAK(BreakListener::new),
    CLICK(ClickListener::new),
    INV_CLICK(InvClickListener::new),
    DROP(DropListener::new),
    PICKUP(PickupListener::new),
    EQUIP(EquipListener::new),
    UNEQUIP(UnequipListener::new),
    DEATH(DeathListener::new)
    ;

    public final CustomListenerConstructor constructor;

    CustomEventType(CustomListenerConstructor constructor) {
        this.constructor = constructor;
    }

    @FunctionalInterface
    interface CustomListenerConstructor {
        CustomListener create(String itemID, long cooldown, CustomEvent event, ClickAction clickAction);
    }

}
