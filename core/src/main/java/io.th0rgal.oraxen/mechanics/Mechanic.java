package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Function;

public abstract class Mechanic {

    private final MechanicFactory mechanicFactory;
    private final ConfigurationSection section;
    private final Function<ItemBuilder, ItemBuilder>[] itemModifiers;
    private final String itemID;

    @SafeVarargs
    protected Mechanic(MechanicFactory mechanicFactory, ConfigurationSection section,
        Function<ItemBuilder, ItemBuilder>... modifiers) {
        this.mechanicFactory = mechanicFactory;
        this.section = section;
        this.itemModifiers = modifiers;
        this.itemID = section.getParent().getParent().getName();
    }

    public String getItemID() {
        return itemID;
    }

    public Function<ItemBuilder, ItemBuilder>[] getItemModifiers() {
        return itemModifiers;
    }

    public MechanicFactory getFactory() {
        return mechanicFactory;
    }

    public ConfigurationSection getSection() {
        return section;
    }

}
