package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemDisplay;

import java.util.Collection;
import java.util.UUID;

public class FurnitureSubEntity {
    private final UUID baseUuid;
    private final IntList entityIds;

    public FurnitureSubEntity(UUID baseUuid, IntList entityIds) {
        this.baseUuid = baseUuid;
        this.entityIds = entityIds;
    }

    public FurnitureSubEntity(UUID baseUuid, Collection<Integer> entityIds) {
        this.baseUuid = baseUuid;
        this.entityIds = new IntArrayList(entityIds);
    }

    public UUID baseUUID() {
        return baseUuid;
    }

    public ItemDisplay baseEntity() {
        return (ItemDisplay) Bukkit.getEntity(baseUuid);
    }

    public IntList entityIds() {
        return entityIds;
    }

}
