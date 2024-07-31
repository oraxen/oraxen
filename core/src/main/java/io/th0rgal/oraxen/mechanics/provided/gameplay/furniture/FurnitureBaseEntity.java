package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class FurnitureBaseEntity {

    private ItemStack itemStack;
    private final UUID baseUuid;
    private final int baseId;
    private final FurnitureMechanic mechanic;

    public FurnitureBaseEntity(Entity baseEntity, FurnitureMechanic mechanic, IFurniturePacketManager packetManager) {
        this.mechanic = mechanic;
        ItemStack furnitureItem = OraxenItems.getItemById(mechanic.getItemID()).build().clone();
        ItemUtils.displayName(furnitureItem, null);
        this.itemStack = furnitureItem;
        this.baseUuid = baseEntity.getUniqueId();
        this.baseId = baseEntity.getEntityId();
    }

    public ItemStack itemStack() {
        return this.itemStack;
    }
    public void itemStack(ItemStack itemStack) {
        ItemUtils.displayName(itemStack.clone(), null);
        this.itemStack = itemStack;
    }

    public UUID baseUUID() {
        return baseUuid;
    }

    public ItemDisplay baseEntity() {
        return (ItemDisplay) Bukkit.getEntity(baseUuid);
    }

    public Integer baseId() {
        return baseId;
    }

    public FurnitureMechanic mechanic() {
        return Optional.ofNullable(mechanic).orElse(OraxenFurniture.getFurnitureMechanic(baseEntity()));
    }
}
