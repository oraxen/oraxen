package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class FurnitureBaseEntity {

    private ItemStack itemStack;
    private final UUID baseUuid;
    private final int baseEntityId;

    public FurnitureBaseEntity(Entity baseEntity, FurnitureMechanic mechanic) {
        ItemStack furnitureItem = OraxenItems.getItemById(mechanic.getItemID()).build().clone();
        ItemUtils.displayName(furnitureItem, Component.empty());
        this.itemStack = furnitureItem;
        this.baseUuid = baseEntity.getUniqueId();
        this.baseEntityId = baseEntity.getEntityId();
    }

    public FurnitureBaseEntity(Entity baseEntity, ItemStack itemStack) {
        ItemUtils.displayName(itemStack.clone(), Component.empty());
        this.itemStack = itemStack;
        this.baseUuid = baseEntity.getUniqueId();
        this.baseEntityId = baseEntity.getEntityId();
    }

    public ItemStack itemStack() {
        return this.itemStack;
    }
    public void itemStack(ItemStack itemStack) {
        ItemUtils.displayName(itemStack.clone(), Component.empty());
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
