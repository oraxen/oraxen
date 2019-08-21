package io.th0rgal.oraxen.items.mechanics.provided.durability;

import io.th0rgal.oraxen.items.Item;
import io.th0rgal.oraxen.items.modifiers.ItemModifier;

public class DurabilityModifier extends ItemModifier {

    private int durability;

    DurabilityModifier(int durability) {
        this.durability = durability;

    }

    @Override
    public Item getItem(Item item) {
        return item.setIntNBTTag("durability", durability);
    }
}
