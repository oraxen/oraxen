package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class FurnitureBaseEntity {

    private ItemStack itemStack;
    private final UUID baseUuid;
    private final int baseEntityId;

    public FurnitureBaseEntity(Entity baseEntity, FurnitureMechanic mechanic) {
        this.itemStack = OraxenItems.getItemById(mechanic.getItemID()).build();
        this.baseUuid = baseEntity.getUniqueId();
        this.baseEntityId = baseEntity.getEntityId();
    }

    public FurnitureBaseEntity(Entity baseEntity, ItemStack itemStack) {
        this.itemStack = itemStack;
        this.baseUuid = baseEntity.getUniqueId();
        this.baseEntityId = baseEntity.getEntityId();
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

    public int entityId() {
        return baseEntityId;
    }

    public FurnitureMechanic mechanic() {
        return OraxenFurniture.getFurnitureMechanic(baseEntity());
    }
}
