package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemDisplay;

import java.util.Collection;
import java.util.UUID;

public class FurnitureSubEntity {
    private final UUID baseUuid;
    private final int baseId;
    private final IntList entityIds;

    public FurnitureSubEntity(ItemDisplay baseEntity, Collection<Integer> entityIds) {
        this.baseUuid = baseEntity.getUniqueId();
        this.baseId = baseEntity.getEntityId();
        this.entityIds = new IntArrayList(entityIds);
    }

    public FurnitureSubEntity(UUID baseUuid, int baseId, Collection<Integer> entityIds) {
        this.baseUuid = baseUuid;
        this.baseId = baseId;
        this.entityIds = new IntArrayList(entityIds);
    }

    public UUID baseUUID() {
        return baseUuid;
    }

    public int baseId() {
        return baseId;
    }

    public ItemDisplay baseEntity() {
        return (ItemDisplay) Bukkit.getEntity(baseUuid);
    }

    public IntList entityIds() {
        return entityIds;
    }

}
