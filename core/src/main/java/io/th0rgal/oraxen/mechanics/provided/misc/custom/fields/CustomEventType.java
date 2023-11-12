package io.th0rgal.oraxen.mechanics.provided.misc.custom.fields;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.*;
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
