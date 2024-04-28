package io.th0rgal.oraxen.nms.v1_20_R3.furniture;

import com.moulberry.axiom.AxiomPaper;
import com.moulberry.axiom.event.AxiomManipulateClientEntityEvent;
import com.moulberry.axiom.event.AxiomManipulateEntityEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureBaseEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AxiomCompatibility implements Listener {

    private final AxiomPaper axiomPaper;

    public AxiomCompatibility() {
        Logs.logInfo("Registering Axiom-Compatibility for furniture...");
        this.axiomPaper = (AxiomPaper) Bukkit.getPluginManager().getPlugin("AxiomPaper");
    }

    @EventHandler
    public void onAxiomManipFurniture(AxiomManipulateEntityEvent event) {
        Entity baseEntity = event.getEntity();

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (baseEntity == null || mechanic == null) return;

        IFurniturePacketManager packetManager = FurnitureFactory.get().packetManager();
        packetManager.removeFurnitureEntityPacket(baseEntity, mechanic);
        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic);
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic);

        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                mechanic.hitbox().handleHitboxes(baseEntity, mechanic), 1L);
    }

    @EventHandler
    public void onAxiomManipFurnitureBase(AxiomManipulateClientEntityEvent event) {
        Player player = event.getPlayer();
        Vec3 relativePos = event.getRelativePos();
        CompoundTag mergeTag = event.getMergeTag();
        ServerLevel serverLevel = ((CraftPlayer) player).getHandle().serverLevel();
        net.minecraft.world.entity.Entity entity = serverLevel.getEntity(event.getUUID());

        Entity baseEntity = FurniturePacketManager.furnitureBaseMap.stream()
                .filter(base -> event.getUUID().equals(base.uuid(player)))
                .map(FurnitureBaseEntity::baseEntity).findFirst().orElse(null);
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (baseEntity == null || mechanic == null) return;

        if (relativePos != null) baseEntity.teleport(baseEntity.getLocation().add(relativePos.x, relativePos.y, relativePos.z));
        if (mergeTag != null && entity != null) entity.load(mergeTag);
    }

}
