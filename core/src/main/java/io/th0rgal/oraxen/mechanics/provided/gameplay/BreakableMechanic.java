package io.th0rgal.oraxen.mechanics.provided.gameplay;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.utils.breaker.ToolTypeSpeedModifier;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import io.th0rgal.oraxen.utils.wrappers.PotionEffectTypeWrapper;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class BreakableMechanic {

    private final double hardness;
    private final Drop drop;
    private final String itemId;

    public BreakableMechanic(ConfigurationSection section) {
        itemId = section.getParent().getParent().getName();
        hardness = section.getInt("hardness", 1);
        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        drop = dropSection != null ? Drop.createDrop(FurnitureFactory.get().toolTypes, dropSection, itemId) : new Drop(new ArrayList<>(), false, false, itemId);
    }

    public Drop drop() {
        return drop;
    }

    public double hardness() {
        return hardness;
    }

    /**
     * Calculates the time it should take for a Player to break this CustomBlock / Furniture
     *
     * @param player The Player breaking the block
     * @return Time in ticks it takes for player to break this block / furniture
     */
    public int breakTime(Player player) {
        double damage = speedMultiplier(player) / hardness / 30;
        return damage > 1 ? 0 : (int) Math.ceil(1 / damage);
    }

    public double speedMultiplier(Player player) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        AtomicReference<Float> speedMultiplier = new AtomicReference<>((float) 1);

        ToolTypeSpeedModifier.VANILLA.stream()
                .filter(t -> t.toolTypes().contains(itemInMainHand.getType()))
                .min(Comparator.comparingDouble(ToolTypeSpeedModifier::speedModifier))
                .ifPresentOrElse(
                        t -> speedMultiplier.set(this.drop.isToolEnough(itemInMainHand)
                                ? t.speedModifier() : ToolTypeSpeedModifier.EMPTY.speedModifier()),
                        () -> speedMultiplier.set(ToolTypeSpeedModifier.EMPTY.speedModifier())
                );

        float multiplier = speedMultiplier.get();

        final int efficiencyLevel = itemInMainHand.getEnchantmentLevel(EnchantmentWrapper.EFFICIENCY);
        if (multiplier > 1.0F && efficiencyLevel != 0) multiplier += (float) (Math.pow(efficiencyLevel, 2) + 1);

        PotionEffect haste = player.getPotionEffect(PotionEffectTypeWrapper.HASTE);
        if (haste != null) multiplier *= 1.0F + (float) (haste.getAmplifier() + 1) * 0.2F;

        // Whilst the player has this when they start digging, period is calculated before it is applied
        PotionEffect miningFatigue = player.getPotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE);
        if (miningFatigue != null) multiplier *= (float) Math.pow(0.3, Math.min(miningFatigue.getAmplifier(), 4));

        // 1.20.5+ speed-modifier attribute
        float miningSpeedModifier = Arrays.stream(Attribute.values()).filter(a -> a.name().equalsIgnoreCase("PLAYER_BLOCK_BREAK_SPEED"))
                .map(player::getAttribute).filter(Objects::nonNull).map(AttributeInstance::getBaseValue).findFirst().orElse(1.0).floatValue();
        multiplier *= miningSpeedModifier;

        if (player.isUnderWater() && !Optional.ofNullable(player.getEquipment().getHelmet()).orElse(new ItemStack(Material.PAPER)).containsEnchantment(EnchantmentWrapper.AQUA_AFFINITY)) {
            multiplier /= 5;
        }

        if (!player.isOnGround()) multiplier /= 5;

        return multiplier;
    }
}
