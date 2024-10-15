package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.moulberry.axiom.event.AxiomManipulateEntityEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AxiomCompatibility implements Listener {

    public AxiomCompatibility() {
        Logs.logInfo("Registering Axiom-Compatibility for furniture...");
    }

    @EventHandler
    public void onAxiomManipFurniture(AxiomManipulateEntityEvent event) {
        if (!(event.getEntity() instanceof ItemDisplay baseEntity)) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;
        IFurniturePacketManager packetManager = FurnitureFactory.get().packetManager();

        packetManager.removeFurnitureEntityPacket(baseEntity, mechanic);
        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic);
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic);

        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            for (Player player : baseEntity.getWorld().getNearbyPlayers(baseEntity.getLocation(), FurnitureFactory.get().simulationRadius)) {
                packetManager.sendFurnitureEntityPacket(baseEntity, mechanic, player);
                packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
                packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player);
            }
        }, 2L);
    }
}
