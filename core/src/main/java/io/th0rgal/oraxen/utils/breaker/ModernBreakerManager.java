package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModernBreakerManager implements BreakerManager {

    private final ConcurrentHashMap<UUID, AttributeModifier> modifierMap;

    public ModernBreakerManager(ConcurrentHashMap<UUID, AttributeModifier> modifierMap) {
        this.modifierMap = modifierMap;
    }

    @Override
    public void startFurnitureBreak(Player player, ItemDisplay baseEntity, FurnitureMechanic mechanic, Block block) {
        //TODO See if this can be handled even with packet-barriers
    }

    @Override
    public void startBlockBreak(Player player, Block block, CustomBlockMechanic mechanic) {
        removeTransientModifier(player);
        if (player.getGameMode() == GameMode.CREATIVE) return;

        addTransientModifier(player, createBreakingModifier(block, mechanic.breakable().hardness()));
    }

    @Override
    public void stopBlockBreak(Player player) {
        removeTransientModifier(player);
    }

    private AttributeModifier createBreakingModifier(Block block, double hardness) {
        return AttributeModifier.deserialize(
                Map.of(
                        "slot", EquipmentSlot.HAND,
                        "uuid", UUID.nameUUIDFromBytes(block.toString().getBytes()).toString(),
                        "name", "oraxen:custom_break_speed",
                        "operation", AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                        "amount", defaultBlockHardness(block) / hardness - 1
                )
        );
    }

    private double defaultBlockHardness(Block block) {
        if (block.getType() == Material.NOTE_BLOCK) return 0.8;
        else if (block.getType() == Material.TRIPWIRE) return 1;
        else return 1;
    }

    private void addTransientModifier(Player player, AttributeModifier modifier) {
        removeTransientModifier(player);
        modifierMap.put(player.getUniqueId(), modifier);
        Optional.ofNullable(player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED)).ifPresent(a -> a.addTransientModifier(modifier));
    }

    private void removeTransientModifier(Player player) {
        Optional.ofNullable(modifierMap.remove(player.getUniqueId())).ifPresent(modifier ->
                Optional.ofNullable(player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED)).ifPresent(a -> a.removeModifier(modifier))
        );
    }
}
