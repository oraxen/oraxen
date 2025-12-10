package io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockInteractEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ToggleLightMechanicListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnitureInteract(OraxenFurnitureInteractEvent event) {
        FurnitureMechanic furnitureMechanic = event.getMechanic();
        ToggleLightMechanic toggleLight = ToggleLightMechanicFactory.getInstance().getMechanic(furnitureMechanic.getItemID());
        if (toggleLight == null || !toggleLight.hasToggleLight()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Entity baseEntity = event.getBaseEntity();

        if (baseEntity == null) return;
        if (!ProtectionLib.canInteract(player, block != null ? block.getLocation() : baseEntity.getLocation())) return;

        // For furniture with barriers, toggle based on base entity and update all barrier blocks
        if (furnitureMechanic.hasBarriers(baseEntity)) {
            toggleLight.toggleFurnitureBarriers(furnitureMechanic, baseEntity);
        } else {
            // For furniture without barriers, toggle the entity location
            toggleLight.toggle(baseEntity);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNoteBlockInteract(OraxenNoteBlockInteractEvent event) {
        NoteBlockMechanic noteBlockMechanic = event.getMechanic();
        ToggleLightMechanic toggleLight = ToggleLightMechanicFactory.getInstance().getMechanic(noteBlockMechanic.getItemID());
        if (toggleLight == null || !toggleLight.hasToggleLight()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!ProtectionLib.canInteract(player, block.getLocation())) return;

        toggleLight.toggle(block);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onStringBlockInteract(OraxenStringBlockInteractEvent event) {
        StringBlockMechanic stringBlockMechanic = event.getMechanic();
        ToggleLightMechanic toggleLight = ToggleLightMechanicFactory.getInstance().getMechanic(stringBlockMechanic.getItemID());
        if (toggleLight == null || !toggleLight.hasToggleLight()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!ProtectionLib.canInteract(player, block.getLocation())) return;

        toggleLight.toggle(block);
    }
}

