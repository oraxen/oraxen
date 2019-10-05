package io.th0rgal.oraxen.items.modifiers;

import io.th0rgal.oraxen.items.ItemBuilder;

public abstract class ItemModifier {

    protected ItemBuilder item;

    public abstract ItemBuilder getItem(ItemBuilder item);

}
