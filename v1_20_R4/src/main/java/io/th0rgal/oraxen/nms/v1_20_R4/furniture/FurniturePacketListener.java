package io.th0rgal.oraxen.nms.v1_20_R4.furniture;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.OraxenNativeMechanicsRegisteredEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.seats.FurnitureSeat;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Optional;

public class FurniturePacketListener implements Listener {

    @EventHandler
    public void onFurnitureFactory(OraxenNativeMechanicsRegisteredEvent event) {
        double r = FurnitureFactory.get().simulationRadius;
        for (Player player : Bukkit.getOnlinePlayers())
            for (ItemDisplay baseEntity : player.getLocation().getNearbyEntitiesByType(ItemDisplay.class, r, r, r)) {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
                IFurniturePacketManager packetManager = FurnitureFactory.instance.packetManager();
                if (mechanic == null) continue;

                packetManager.sendFurnitureEntityPacket(baseEntity, mechanic, player);
                packetManager.sendLightMechanicPacket(baseEntity, mechanic, player);
                packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
                packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player);
            }
    }

    @EventHandler
    public void onFurnitureAdded(EntityAddToWorldEvent event) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(event.getEntity());
        if (mechanic == null || FurnitureSeat.isSeat(event.getEntity())) return;
        ItemDisplay baseEntity = (ItemDisplay) event.getEntity();
        IFurniturePacketManager packetManager = FurnitureFactory.get().packetManager();

        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            for (Player player : baseEntity.getWorld().getNearbyPlayers(baseEntity.getLocation(), FurnitureFactory.get().simulationRadius)) {
                packetManager.sendFurnitureEntityPacket(baseEntity, mechanic, player);
                packetManager.sendLightMechanicPacket(baseEntity, mechanic, player);
                packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
                packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player);
            }
        }, 2L);

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFurnitureRemoval(EntityRemoveFromWorldEvent event) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(event.getEntity());
        if (mechanic == null) return;
        ItemDisplay baseEntity = (ItemDisplay) event.getEntity();
        IFurniturePacketManager packetManager = FurnitureFactory.get().packetManager();

        packetManager.removeFurnitureEntityPacket(baseEntity, mechanic);
        packetManager.removeLightMechanicPacket(baseEntity, mechanic);
        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic);
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic);
    }

    @EventHandler
    public void onFurnitureTeleport(EntityTeleportEvent event) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(event.getEntity());
        if (mechanic == null) return;
        ItemDisplay baseEntity = (ItemDisplay) event.getEntity();
        IFurniturePacketManager packetManager = FurnitureFactory.get().packetManager();

        packetManager.removeFurnitureEntityPacket(baseEntity, mechanic);
        packetManager.removeLightMechanicPacket(baseEntity, mechanic);
        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic);
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic);
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        Player player = event.getPlayer();
        IFurniturePacketManager packetManager = FurnitureFactory.instance.packetManager();
        for (Entity entity : event.getChunk().getEntities()) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
            if (mechanic == null || FurnitureSeat.isSeat(entity)) continue;
            ItemDisplay baseEntity = (ItemDisplay) entity;

            Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                packetManager.sendFurnitureEntityPacket(baseEntity, mechanic, player);
                packetManager.sendLightMechanicPacket(baseEntity, mechanic, player);
                packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
                packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player);
            });
        }
    }

    @EventHandler
    public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
        Player player = event.getPlayer();
        IFurniturePacketManager packetManager = FurnitureFactory.instance.packetManager();
        if (packetManager != null) for (Entity entity : event.getChunk().getEntities()) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
            if (mechanic == null || FurnitureSeat.isSeat(entity)) continue;
            ItemDisplay baseEntity = (ItemDisplay) entity;

            packetManager.removeFurnitureEntityPacket(baseEntity, mechanic, player);
            packetManager.removeLightMechanicPacket(baseEntity, mechanic, player);
            packetManager.removeInteractionHitboxPacket(baseEntity, mechanic, player);
            packetManager.removeBarrierHitboxPacket(baseEntity, mechanic, player);
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        IFurniturePacketManager packetManager = FurnitureFactory.instance.packetManager();

        for (ItemDisplay baseEntity : event.getFrom().getEntitiesByClass(ItemDisplay.class)) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
            if (mechanic == null || FurnitureSeat.isSeat(baseEntity)) continue;

            packetManager.removeFurnitureEntityPacket(baseEntity, mechanic, player);
            packetManager.removeLightMechanicPacket(baseEntity, mechanic, player);
            packetManager.removeInteractionHitboxPacket(baseEntity, mechanic, player);
            packetManager.removeBarrierHitboxPacket(baseEntity, mechanic, player);
        }
    }

    @EventHandler
    public void onUseUnknownEntity(PlayerUseUnknownEntityEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack itemInHand = hand == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        Vector relativePosition = event.getClickedRelativePosition();
        Location interactionPoint = relativePosition != null ? relativePosition.toLocation(player.getWorld()) : null;
        IFurniturePacketManager packetManager = FurnitureFactory.instance.packetManager();

        ItemDisplay baseEntity = packetManager.baseEntityFromHitbox(event.getEntityId());
        if (baseEntity == null) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null || FurnitureSeat.isSeat(baseEntity)) return;

        if (ProtectionLib.canBreak(player, baseEntity.getLocation()) && event.isAttack())
            OraxenFurniture.remove(baseEntity, player);
        else if (ProtectionLib.canInteract(player, baseEntity.getLocation()))
            EventUtils.callEvent(new OraxenFurnitureInteractEvent(mechanic, baseEntity, player, itemInHand, hand, interactionPoint));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDamageBarrierHitbox(BlockDamageEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Location location = block.getLocation();
        Entity baseEntity = FurnitureFactory.instance.packetManager().baseEntityFromHitbox(new BlockLocation(location));

        if (baseEntity == null || !ProtectionLib.canBreak(player, location)) return;
        OraxenFurniture.remove(baseEntity, player);
        //if (!ProtectionLib.canBreak(player, location)) return;
        //FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(location);
        //if (mechanic == null) return;
        //OraxenPlugin.get().breakerManager().startFurnitureBreak(event.getPlayer(), mechanic.baseEntity(event.getBlock()), mechanic, event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBreakBarrierHitbox(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (!ProtectionLib.canBreak(player, location)) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(location);
        if (mechanic != null) OraxenFurniture.remove(location, player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractBarrierHitbox(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        Location interactionPoint = event.getInteractionPoint();
        FurnitureMechanic mechanic = Optional.ofNullable(OraxenFurniture.getFurnitureMechanic(clickedBlock))
                .orElse(OraxenFurniture.getFurnitureMechanic(interactionPoint));
        if (mechanic == null) return;

        ItemDisplay baseEntity = Optional.ofNullable(mechanic.baseEntity(clickedBlock)).orElse(mechanic.baseEntity(interactionPoint));
        if (baseEntity == null) return;

        if (action == Action.RIGHT_CLICK_BLOCK && ProtectionLib.canInteract(player, baseEntity.getLocation()))
            EventUtils.callEvent(new OraxenFurnitureInteractEvent(mechanic, baseEntity, player, event.getItem(), event.getHand(), interactionPoint));
        //else if (action == Action.LEFT_CLICK_BLOCK && ProtectionLib.canBreak(player, baseEntity.getLocation()))
            //OraxenFurniture.remove(baseEntity, player);

        // Resend the hitbox as client removes the "ghost block"
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                FurnitureFactory.instance.packetManager().sendBarrierHitboxPacket(baseEntity, mechanic, player), 1L);
    }
}
