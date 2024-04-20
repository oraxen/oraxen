package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class FurnitureBaseEntity {

    private ItemStack itemStack;
    private final UUID baseUuid;
    private final Map<FurnitureType, Integer> entityIds;

    public FurnitureBaseEntity(Entity baseEntity, FurnitureMechanic mechanic) {
        this.itemStack = OraxenItems.getItemById(mechanic.getItemID()).build();
        this.baseUuid = baseEntity.getUniqueId();
        this.entityIds = Arrays.stream(FurnitureType.values()).map(type -> Map.entry(type, net.minecraft.world.entity.Entity.nextEntityId()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public FurnitureBaseEntity(UUID baseUuid, ItemStack itemStack, Map<FurnitureType, Integer> entityIds) {
        this.itemStack = itemStack;
        this.baseUuid = baseUuid;
        this.entityIds = entityIds;
    }

    public ItemStack itemStack() {
        return this.itemStack;
    }
    public void itemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
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
