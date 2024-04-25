package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.ItemUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class FurnitureBaseEntity {

    private ItemStack itemStack;
    private final UUID baseUuid;
    private final Map<FurnitureType, Integer> entityIds;
    private final FurnitureMechanic mechanic;

    public FurnitureBaseEntity(Entity baseEntity, FurnitureMechanic mechanic) {
        this.mechanic = mechanic;
        ItemStack furnitureItem = OraxenItems.getItemById(mechanic.getItemID()).build().clone();
        ItemUtils.displayName(furnitureItem, Component.empty());
        this.itemStack = furnitureItem;
        this.baseUuid = baseEntity.getUniqueId();
        this.entityIds = Arrays.stream(FurnitureType.values()).map(type -> Map.entry(type, net.minecraft.world.entity.Entity.nextEntityId()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    public int entityId(FurnitureType type) {
        return entityIds.get(type);
    }

    public int entityId() {
        return entityIds.get(mechanic().furnitureType());
    }

    public IntList entityIds() {
        return new IntArrayList(entityIds.values());
    }

    public FurnitureMechanic mechanic() {
        return Optional.ofNullable(mechanic).orElse(OraxenFurniture.getFurnitureMechanic(baseEntity()));
    }
}
