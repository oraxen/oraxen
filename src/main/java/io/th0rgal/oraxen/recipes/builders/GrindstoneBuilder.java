package io.th0rgal.oraxen.recipes.builders;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class GrindstoneBuilder extends WorkstationBuilder {

    public GrindstoneBuilder(Player player) {
        super(player, "grindstone", "experience");
    }

    @Override
    Inventory createInventory(Player player, Component inventoryTitle) {
        // Paper opens custom grindstone inventories through a separate native GrindstoneMenu.
        // Its visible slots are not backed by the Inventory returned here and vanilla grindstone
        // slot predicates would also reject otherwise valid custom recipe ingredients. A hopper
        // gives us unrestricted, directly-backed authoring slots; slots 0-2 map to base,
        // addition and result, while the builder listener keeps the remaining slots unused.
        return Bukkit.createInventory(player, InventoryType.HOPPER, inventoryTitle);
    }

    public void setExperience(int experience) {
        setValue(experience);
    }
}
