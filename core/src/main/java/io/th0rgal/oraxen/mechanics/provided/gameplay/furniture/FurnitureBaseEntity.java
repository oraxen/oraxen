package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class FurnitureBaseEntity {

    private ItemStack itemStack;
    private final UUID baseUuid;
    private final int baseId;
    private final FurnitureMechanic mechanic;

    public FurnitureBaseEntity(ItemDisplay baseEntity, FurnitureMechanic mechanic) {
        this.mechanic = mechanic;
        ItemStack furnitureItem = OraxenItems.getOptionalItemById(mechanic.getItemID()).orElse(new ItemBuilder(Material.BARRIER)).build().clone();
        ItemUtils.dyeItem(furnitureItem, FurnitureHelpers.furnitureDye(baseEntity));
        ItemUtils.displayName(furnitureItem, null);
        this.itemStack = furnitureItem;
        this.baseUuid = baseEntity.getUniqueId();
        this.baseId = baseEntity.getEntityId();
    }

    public ItemStack itemStack() {
        return this.itemStack;
    }
    public void itemStack(@NotNull ItemStack itemStack) {
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

    public boolean equals(FurnitureBaseEntity baseEntity) {
        return this.baseUuid.equals(baseEntity.baseUuid) && this.baseId == baseEntity.baseId && mechanic.getItemID().equals(baseEntity.mechanic().getItemID());
    }
}
