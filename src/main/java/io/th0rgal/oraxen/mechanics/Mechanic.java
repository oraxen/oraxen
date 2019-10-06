package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.items.modifiers.ItemModifier;
import org.bukkit.configuration.ConfigurationSection;

public abstract class Mechanic {

    private MechanicFactory mechanicFactory;
    private ConfigurationSection section;
    private ItemModifier[] itemModifiers;
    private String itemID;

    public Mechanic(MechanicFactory mechanicFactory, ConfigurationSection section, ItemModifier... modifiers) {
        this.mechanicFactory = mechanicFactory;
        this.section = section;
        this.itemModifiers = modifiers;
        this.itemID = section.getParent().getParent().getName();
    }

    public String getItemID() {
        return itemID;
    }

    public ItemModifier[] getItemModifiers() {
        return itemModifiers;
    }

    public MechanicFactory getFactory() {
        return mechanicFactory;
    }

    protected ConfigurationSection getSection() {
        return section;
    }

}
