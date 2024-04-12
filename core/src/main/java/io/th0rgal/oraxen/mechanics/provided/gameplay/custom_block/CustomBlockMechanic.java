package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.breaker.ToolTypeSpeedModifier;
import io.th0rgal.oraxen.utils.drops.Drop;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class CustomBlockMechanic extends Mechanic {

    private final CustomBlockType type;
    private final int customVariation;
    private final Key model;
    private final int hardness;
    private final BlockData blockData;

    private final Drop drop;
    private final BlockSounds blockSounds;
    private final LightMechanic light;
    private final LimitedPlacing limitedPlacing;
    private final List<ClickAction> clickActions;
    private final BlockLockerMechanic blockLocker;

    public CustomBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        type = CustomBlockType.fromString(section.getString("type", ""));
        model = Key.key(section.getString("model", section.getParent().getString("Pack.model", getItemID())));
        customVariation = section.getInt("custom_variation");
        hardness = section.getInt("hardness", 1);
        blockData = createBlockData();

        clickActions = ClickAction.parseList(section);
        light = new LightMechanic(section);

        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        drop = dropSection != null ? Drop.createDrop(CustomBlockFactory.getInstance().toolTypes(type), dropSection, getItemID()) : new Drop(new ArrayList<>(), false, false, getItemID());

        ConfigurationSection limitedPlacingSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedPlacingSection != null ? new LimitedPlacing(limitedPlacingSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection blockLockerSection = section.getConfigurationSection("blocklocker");
        blockLocker = blockLockerSection != null ? new BlockLockerMechanic(blockLockerSection) : null;
    }

    public CustomBlockType type() {
        return type;
    }

    public BlockData createBlockData() {
        return Material.AIR.createBlockData();
    }

    public BlockData blockData() {
        return this.blockData;
    }

    public boolean hasLimitedPlacing() { return limitedPlacing != null; }
    public LimitedPlacing limitedPlacing() { return limitedPlacing; }

    public boolean hasBlockSounds() { return blockSounds != null; }
    public BlockSounds blockSounds() { return blockSounds; }

    public Key model() {
        return model;
    }

    public int customVariation() {
        return customVariation;
    }

    public Drop drop() {
        return drop;
    }

    public long breakTime(Player player) {
        double breakTime = speedMultiplier(player) / hardness / 30;
        return breakTime > 1 ? 0L : (long) (1 / breakTime) / 20;
    }

    public boolean hasHardness() {
        return hardness != -1;
    }

    public int hardness() {
        return hardness;
    }

    private double speedMultiplier(Player player) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        AtomicReference<Float> speedMultiplier = new AtomicReference<>((float) 1);

        List<ToolTypeSpeedModifier> validToolTypes = ToolTypeSpeedModifier.VANILLA.stream().filter(t -> t.getToolType().contains(itemInMainHand.getType()))
                .sorted(Comparator.comparingDouble(ToolTypeSpeedModifier::getSpeedModifier))
                .toList();



        /*// Find first validToolTypes that contains the block material
        // If none found, use the first validToolTypes
        validToolTypes.stream().filter(t -> t.getMaterials().contains(block.getType()))
                .findFirst().ifPresentOrElse(toolTypeSpeedModifier -> speedMultiplier.set(toolTypeSpeedModifier.getSpeedModifier()), () ->
                        speedMultiplier.set(validToolTypes.stream().findFirst().get().getSpeedModifier()));*/

        float multiplier = speedMultiplier.get();
        if (itemInMainHand.containsEnchantment(Enchantment.DIG_SPEED))
            multiplier *= 1f + (itemInMainHand.getEnchantmentLevel(Enchantment.DIG_SPEED) ^ 2 + 1);

        PotionEffect haste = player.getPotionEffect(PotionEffectType.FAST_DIGGING);
        if (haste != null) multiplier *= 1f + (0.2F * haste.getAmplifier() + 1);

        // Whilst the player has this when they start digging, period is calculated before it is applied
        PotionEffect miningFatigue = player.getPotionEffect(PotionEffectType.SLOW_DIGGING);
        if (miningFatigue != null) multiplier *= 1f - (0.3F * miningFatigue.getAmplifier() + 1);

        ItemStack helmet = player.getEquipment().getHelmet();
        if (player.isInWater() && (helmet == null || !helmet.containsEnchantment(Enchantment.WATER_WORKER)))
            multiplier /= 5;

        if (!player.isOnGround()) multiplier /= 5;

        return multiplier;
    }

    public boolean hasLight() {
        return light.hasLightLevel();
    }

    public LightMechanic light() {
        return light;
    }

    public boolean hasClickActions() { return !clickActions.isEmpty(); }

    public void runClickActions(final Player player) {
        for (final ClickAction action : clickActions) {
            if (action.canRun(player)) {
                action.performActions(player);
            }
        }
    }

    public BlockLockerMechanic blockLocker() {
        return blockLocker;
    }
}
