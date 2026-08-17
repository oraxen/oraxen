package io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.OraxenNativeMechanicsRegisteredEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ToggleLightRefreshListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMechanicsRegistered(OraxenNativeMechanicsRegisteredEvent event) {
        // Refresh light for all existing furniture, noteblocks, and stringblocks when mechanics are registered (on load/reload)
        // The delay ensures all mechanics are fully loaded. The scan itself stays on the global scheduler
        // because it enumerates entities across every region of a world, which no single region thread owns;
        // the actual light refresh then hops to the region thread owning each entity, as ItemUpdater does.
        SchedulerUtil.runTaskLater(20L, () -> {
            for (World world : Bukkit.getServer().getWorlds()) refreshWorldLight(world);
        });
    }

    private void refreshWorldLight(World world) {
        ToggleLightMechanicFactory factory = ToggleLightMechanicFactory.getInstance();
        if (factory == null) return;

        // Refresh furniture entities - schedule on each entity's region thread for Folia compatibility
        world.getEntities().stream()
                .filter(OraxenFurniture::isBaseEntity)
                .forEach(entity -> SchedulerUtil.runForEntity(entity, () -> refreshEntityLight(factory, entity)));

        // Note: NoteBlocks and StringBlocks are not refreshed here to avoid expensive chunk iteration.
        // Their light state persists in PDC and will be refreshed on interaction or when placed.
    }

    private void refreshEntityLight(ToggleLightMechanicFactory factory, Entity entity) {
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (furnitureMechanic == null) return;
        ToggleLightMechanic toggleLight = factory.getMechanic(furnitureMechanic.getItemID());
        if (toggleLight != null && (toggleLight.hasToggleLight() || toggleLight.getBaseLightLevel() > 0)) {
            furnitureMechanic.refreshLight(entity);
        }
    }
}

