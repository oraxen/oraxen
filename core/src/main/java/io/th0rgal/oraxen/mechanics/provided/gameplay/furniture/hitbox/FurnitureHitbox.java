package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FurnitureHitbox {
    public static final FurnitureHitbox EMPTY = new FurnitureHitbox(List.of(), List.of());
    private final List<BarrierHitbox> barrierHitboxes;
    private final List<InteractionHitbox> interactionHitboxes;

    public FurnitureHitbox(@NotNull ConfigurationSection hitboxSection) {
        List<BarrierHitbox> barrierHitboxes = new ArrayList<>();
        for (String barrierString : hitboxSection.getStringList("barrierHitboxes"))
            barrierHitboxes.add(new BarrierHitbox(barrierString));

        List<InteractionHitbox> interactionHitboxes = new ArrayList<>();
        for (String interactionString : hitboxSection.getStringList("interactionHitboxes"))
            interactionHitboxes.add(new InteractionHitbox(interactionString));

        this.barrierHitboxes = barrierHitboxes;
        this.interactionHitboxes = interactionHitboxes;
    }

    public FurnitureHitbox(Collection<BarrierHitbox> barrierHitboxes, Collection<InteractionHitbox> interactionHitboxes) {
        this.barrierHitboxes = new ArrayList<>(barrierHitboxes);
        this.interactionHitboxes = new ArrayList<>(interactionHitboxes);
    }

    public List<BarrierHitbox> barrierHitboxes() {
        return barrierHitboxes;
    }

    public List<InteractionHitbox> interactionHitboxes() {
        return interactionHitboxes;
    }

    public double hitboxHeight() {
        int highestBarrier = Utils.getLastOrDefault(barrierHitboxes.stream().map(b -> b.getY() + 1).sorted().toList(), 0);
        double highestInteraction = Utils.getLastOrDefault(interactionHitboxes.stream().map(i -> i.offset().clone().add(new Vector(0, i.height(), 0)).getY()).sorted().toList(), 0.0);
        return Math.max(highestInteraction, highestBarrier);
    }

    public void handleHitboxes(ItemDisplay baseEntity, FurnitureMechanic mechanic) {
        IFurniturePacketManager packetManager = FurnitureFactory.instance.packetManager();

        for (Player player : baseEntity.getWorld().getNearbyPlayers(baseEntity.getLocation(), FurnitureFactory.get().simulationRadius)) {
            packetManager.sendFurnitureEntityPacket(baseEntity, mechanic, player);
            packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
            packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player);
        }
    }

    public List<Location> hitboxLocations(Location center, float yaw) {
        List<Location> hitboxLocations = new ArrayList<>();
        hitboxLocations.addAll(barrierHitboxLocations(center, yaw));
        hitboxLocations.addAll(interactionHitboxLocations(center, yaw));

        return hitboxLocations;
    }

    public List<Location> barrierHitboxLocations(Location center, float rotation) {
        return barrierHitboxes.stream().map(b -> b.groundRotate(rotation).add(center)).toList();
    }

    public List<Location> interactionHitboxLocations(Location center, float rotation) {
        return interactionHitboxes.stream().map(i -> center.clone().add(i.offset(rotation))).toList();
    }
}
