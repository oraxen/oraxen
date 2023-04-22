package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class MechanicFactory {

    private final Map<String, Mechanic> mechanicByItem = new HashMap<>();
    private final ConfigurationSection section;

    protected MechanicFactory(ConfigurationSection section) {
        this.section = section;
    }

    protected ConfigurationSection getSection() {
        return this.section;
    }

    public abstract Mechanic parse(ConfigurationSection itemMechanicConfiguration);

    protected void addToImplemented(Mechanic mechanic) {
        mechanicByItem.put(mechanic.getItemID(), mechanic);
    }

    public Set<String> getItems() {
        return mechanicByItem.keySet();
    }

    public boolean isNotImplementedIn(String itemID) {
        return !mechanicByItem.containsKey(itemID);
    }

    public Mechanic getMechanic(String itemID) {
        return mechanicByItem.get(itemID);
    }

    public Mechanic getMechanic(ItemStack itemStack) {
        return mechanicByItem.get(OraxenItems.getIdByItem(itemStack));
    }

    public String getMechanicID() {
        return section.getName();
    }

}
