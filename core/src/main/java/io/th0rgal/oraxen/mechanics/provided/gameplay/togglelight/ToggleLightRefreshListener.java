package io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.OraxenNativeMechanicsRegisteredEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ToggleLightRefreshListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMechanicsRegistered(OraxenNativeMechanicsRegisteredEvent event) {
        // Refresh light for all existing furniture, noteblocks, and stringblocks when mechanics are registered (on load/reload)
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            ToggleLightMechanicFactory factory = ToggleLightMechanicFactory.getInstance();
            if (factory == null) return;

            for (World world : Bukkit.getServer().getWorlds()) {
                // Refresh furniture entities
                world.getEntities().stream()
                        .filter(OraxenFurniture::isBaseEntity)
                        .forEach(entity -> {
                            FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(entity);
                            if (furnitureMechanic == null) return;
                            ToggleLightMechanic toggleLight = factory.getMechanic(furnitureMechanic.getItemID());
                            if (toggleLight != null && (toggleLight.hasToggleLight() || toggleLight.getBaseLightLevel() > 0)) {
                                furnitureMechanic.refreshLight(entity);
                            }
                        });

                // Note: NoteBlocks and StringBlocks are not refreshed here to avoid expensive chunk iteration.
                // Their light state persists in PDC and will be refreshed on interaction or when placed.
            }
        }, 20L); // Delay to ensure all mechanics are fully loaded
    }
}

