package io.th0rgal.oraxen.nms.v1_20_R1;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.OraxenNativeMechanicsRegisteredEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.utils.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class FurniturePacketListener implements Listener {

    double simulationDistance = Math.pow((Bukkit.getServer().getSimulationDistance() * 16.0), 2.0);

    @EventHandler
    public void onFurnitureFactory(OraxenNativeMechanicsRegisteredEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) for (Entity baseEntity : player.getNearbyEntities(simulationDistance, simulationDistance, simulationDistance)) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
            IFurniturePacketManager packetManager = FurnitureFactory.instance.furniturePacketManager();
            if (mechanic == null) continue;
            packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
            packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player);
        }
    }

    @EventHandler
    public void onFurnitureAdded(EntityAddToWorldEvent event) {
        Entity baseEntity = event.getEntity();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;
        IFurniturePacketManager packetManager = FurnitureFactory.get().furniturePacketManager();

        double simulationDistance = Math.pow((Bukkit.getServer().getSimulationDistance() * 16.0), 2.0);

        // Delay 1 tick, otherwise barrier at 0,0,0 will be updated client-side due to entity spawning after
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            for (Player player : baseEntity.getWorld().getNearbyPlayers(baseEntity.getLocation(), simulationDistance)) {
                packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
                packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player);
            }
        }, 1L);

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFurnitureRemoval(EntityRemoveFromWorldEvent event) {
        Entity baseEntity = event.getEntity();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;
        IFurniturePacketManager packetManager = FurnitureFactory.get().furniturePacketManager();

        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic);
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic);
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        Player player = event.getPlayer();
        IFurniturePacketManager packetManager = FurnitureFactory.instance.furniturePacketManager();
        for (Entity baseEntity : event.getChunk().getEntities()) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
            if (mechanic == null) return;
            packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
            packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
        }
    }

    @EventHandler
    public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
        Player player = event.getPlayer();
        IFurniturePacketManager packetManager = FurnitureFactory.instance.furniturePacketManager();
        if (packetManager != null) for (Entity baseEntity : event.getChunk().getEntities()) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
            if (mechanic == null) return;
            packetManager.removeInteractionHitboxPacket(baseEntity, mechanic, player);
            packetManager.removeBarrierHitboxPacket(baseEntity, mechanic, player);
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        IFurniturePacketManager packetManager = FurnitureFactory.instance.furniturePacketManager();

        for (Entity baseEntity : event.getFrom().getEntities()) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
            if (mechanic == null) return;
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
        IFurniturePacketManager packetManager = FurnitureFactory.instance.furniturePacketManager();

        Entity baseEntity = packetManager.baseEntityFromHitbox(event.getEntityId());
        if (baseEntity == null) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;

        if (event.isAttack()) OraxenFurniture.remove(baseEntity, player);
        else EventUtils.callEvent(new OraxenFurnitureInteractEvent(mechanic, baseEntity, player, itemInHand, hand, interactionPoint));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDamageBarrierHitbox(BlockDamageEvent event) {
        Location location = event.getBlock().getLocation();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(location);
        if (mechanic == null) return;
        OraxenFurniture.remove(location, null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreativePlayerBreakBarrierHitbox(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(location);
        if (mechanic == null) return;
        OraxenFurniture.remove(location, null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractBarrierHitbox(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Location interactionPoint = event.getInteractionPoint();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(interactionPoint);
        if (mechanic == null) return;
        Entity baseEntity = mechanic.baseEntity(interactionPoint);
        if (baseEntity == null) return;

        EventUtils.callEvent(new OraxenFurnitureInteractEvent(mechanic, baseEntity, player, event.getItem(), event.getHand(), interactionPoint));
        // Resend the hitbox as client removes the "ghost block"
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                FurnitureFactory.instance.furniturePacketManager().sendBarrierHitboxPacket(baseEntity, mechanic, player)
        , 1L);
    }
}
