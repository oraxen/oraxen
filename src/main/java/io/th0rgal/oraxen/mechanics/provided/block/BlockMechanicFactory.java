package io.th0rgal.oraxen.mechanics.provided.block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.listeners.EventsManager;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;

import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockMechanicFactory extends MechanicFactory {

    public BlockMechanicFactory(ConfigurationSection section) {
        super(section);
        new EventsManager(OraxenPlugin.get()).addEvents(new BlockMechanicsManager(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BlockMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

}

class BlockMechanicsManager implements Listener {

    private MechanicFactory factory;

    public BlockMechanicsManager(BlockMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlayerPlacesCustomBlock(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        Logs.log("success");
    }

}

class BlockMechanic extends Mechanic {

    List<LinkedHashMap<String, Object>> loots;
    boolean defaultBreakAnimation;

    @SuppressWarnings("unchecked")
    public BlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /* We give:
        - an instance of the Factory which created the mechanic
        - the section used to configure the mechanic
         */
        super(mechanicFactory, section);
        loots = (List<LinkedHashMap<String, Object>>) section.getList("loots");

        if (!section.isConfigurationSection("break_animation")) {
            defaultBreakAnimation = true;
        } else {
            ConfigurationSection breakAnimation = section.getConfigurationSection("break_animation");
            defaultBreakAnimation = !breakAnimation.isBoolean("default") || breakAnimation.getBoolean("default");
        }
    }

    public List<LinkedHashMap<String, Object>> getLoots() {
        return loots;
    }

    public boolean isDefaultBreakAnimation() {
        return defaultBreakAnimation;
    }
}