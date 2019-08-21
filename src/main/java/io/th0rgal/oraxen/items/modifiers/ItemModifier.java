package io.th0rgal.oraxen.items.modifiers;

import io.th0rgal.oraxen.items.Item;

public abstract class ItemModifier {

    protected Item item;

    public abstract Item getItem(Item item);

}
