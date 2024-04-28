package io.th0rgal.oraxen.nms.v1_20_R3.furniture;

import com.moulberry.axiom.AxiomPaper;
import com.moulberry.axiom.event.AxiomManipulateEntityEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

public class AxiomCompatibility implements Listener {

    private final AxiomPaper axiomPaper;

    public AxiomCompatibility() {
        Logs.logInfo("Registering Axiom-Compatibility for furniture...");
        this.axiomPaper = (AxiomPaper) Bukkit.getPluginManager().getPlugin("AxiomPaper");
    }

    @EventHandler
    public void onAxiomManipFurniture(AxiomManipulateEntityEvent event) {
        Player player = event.getPlayer();
        // Get the baseEntity if manipulated is server-side
        // If client-side entity, find the baseEntity it belongs to if any
        Entity baseEntity = Optional.ofNullable(event.getEntity()).orElseGet(() ->
                FurniturePacketManager.furnitureBaseMap.stream().
                filter(base -> event.getUUID().equals(base.uuid(player)))
                .map(FurnitureBaseEntity::baseEntity).findFirst().orElse(null)
        );

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (baseEntity == null || mechanic == null) return;
        if (!(baseEntity instanceof ItemDisplay itemDisplay)) return;

        IFurniturePacketManager packetManager = FurnitureFactory.get().packetManager();
        packetManager.removeFurnitureEntityPacket(baseEntity, mechanic);
        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic);
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic);

        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            mechanic.hitbox().handleHitboxes(baseEntity, mechanic);
        }, 1L);
    }

}
