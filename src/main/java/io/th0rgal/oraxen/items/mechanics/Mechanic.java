package io.th0rgal.oraxen.items.mechanics;

import io.th0rgal.oraxen.items.modifiers.ItemModifier;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;

public abstract class Mechanic {

    private ConfigurationSection section;
    private ItemModifier[] itemModifiers;

    public Mechanic(ConfigurationSection section, ItemModifier... modifiers) {
        this.section = section;
        this.itemModifiers = modifiers;
        MechanicsManager.addItemMechanic(getItemID(), this);
    }

    public abstract Set<String> getItems();

    public abstract String getMechanicID();

    protected String getItemID() {
        return section.getParent().getParent().getName();
    }

    public ItemModifier[] getItemModifiers() {
        return itemModifiers;
    }
}
