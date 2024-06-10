package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.ItemUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
    private final Map<FurnitureType, UUID> uuids;
    private final FurnitureMechanic mechanic;

    public FurnitureBaseEntity(Entity baseEntity, FurnitureMechanic mechanic, IFurniturePacketManager packetManager) {
        this.mechanic = mechanic;
        ItemStack furnitureItem = OraxenItems.getItemById(mechanic.getItemID()).build().clone();
        ItemUtils.displayName(furnitureItem, null);
        this.itemStack = furnitureItem;
        this.baseUuid = baseEntity.getUniqueId();
        this.entityIds = Arrays.stream(FurnitureType.values())
                .map(type -> Map.entry(type, type.entityType() == baseEntity.getType() ? baseEntity.getEntityId() : packetManager.nextEntityId()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.uuids = Arrays.stream(FurnitureType.values())
                .map(type -> Map.entry(type, type.entityType() == baseEntity.getType() ? baseEntity.getUniqueId() : UUID.randomUUID()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    public Entity baseEntity() {
        return Bukkit.getEntity(baseUuid);
    }

    public int entityId(FurnitureType type) {
        return entityIds.get(type);
    }

    public int entityId(Player player) {
        return entityIds.get(mechanic.furnitureType(player));
    }

    public Integer entityId() {
        return entityIds.get(mechanic().furnitureType());
    }

    public IntList entityIds() {
        return new IntArrayList(entityIds.values());
    }

    public UUID uuid(FurnitureType type) {
        return uuids.get(type);
    }

    public UUID uuid(Player player) {
        return uuids.get(mechanic.furnitureType(player));
    }

    public FurnitureMechanic mechanic() {
        return Optional.ofNullable(mechanic).orElse(OraxenFurniture.getFurnitureMechanic(baseEntity()));
    }
}
