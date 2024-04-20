package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.*;

public class FurnitureBaseEntity {

    private final UUID baseUuid;
    private final Map<FurnitureType, Integer> entityIds;

    public FurnitureBaseEntity(UUID baseUuid, FurnitureType type, int entityId) {
        this.baseUuid = baseUuid;
        this.entityIds = Map.of(type, entityId);
    }

    public FurnitureBaseEntity(UUID baseUuid, Map<FurnitureType, Integer> entityIds) {
        this.baseUuid = baseUuid;
        this.entityIds = entityIds;
    }

    public UUID baseUUID() {
        return baseUuid;
    }

    public Entity baseEntity() {
        return Bukkit.getEntity(baseUuid);
    }

    public FurnitureMechanic mechanic() {
        return OraxenFurniture.getFurnitureMechanic(baseEntity());
    }

    public int entityId(FurnitureType type) {
        return entityIds.get(type);
    }

    public int entityId() {
        return entityIds.get(mechanic().furnitureType());
    }

    public IntList entityIds() {
        return new IntArrayList(entityIds.values());
    }
}
