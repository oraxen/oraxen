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
    private final boolean blastResistant;

    public CustomBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        type = CustomBlockType.fromMechanicSection(section);
        model = Key.key(section.getString("model", section.getParent().getString("Pack.model", getItemID())));
        customVariation = section.getInt("custom_variation");
        hardness = section.getInt("hardness", 1);
        blockData = createBlockData();

        clickActions = ClickAction.parseList(section);
        light = new LightMechanic(section);
        blastResistant = section.getBoolean("blast_resistant");

        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        drop = dropSection != null ? Drop.createDrop(CustomBlockFactory.get().toolTypes(type), dropSection, getItemID()) : new Drop(new ArrayList<>(), false, false, getItemID());

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

    public Key model() {
        return model;
    }

    public int customVariation() {
        return customVariation;
    }

    /**
     * Calculates the time it should take for a Player to break this CustomBlock
     *
     * @param player The Player breaking the block
     * @return Time in ticks it takes for player to break this block
     */
    public int breakTime(Player player) {
        double damage = speedMultiplier(player) / hardness() / 30;
        return damage > 1 ? 0 : (int) Math.ceil(1 / damage);
    }

    public int hardness() {
        return hardness;
    }

    private double speedMultiplier(Player player) {
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

        final int efficiencyLevel = itemInMainHand.getEnchantmentLevel(Enchantment.DIG_SPEED);
        if (multiplier > 1.0F && efficiencyLevel != 0) multiplier += (float) (Math.pow(efficiencyLevel, 2) + 1);

        PotionEffect haste = player.getPotionEffect(PotionEffectType.FAST_DIGGING);
        if (haste != null) multiplier *= 1.0F + (float) (haste.getAmplifier() + 1) * 0.2F;

        // Whilst the player has this when they start digging, period is calculated before it is applied
        PotionEffect miningFatigue = player.getPotionEffect(PotionEffectType.SLOW_DIGGING);
        if (miningFatigue != null) multiplier *= (float) Math.pow(0.3, Math.min(miningFatigue.getAmplifier(), 4));

        ItemStack helmet = player.getEquipment().getHelmet();
        if (player.isUnderWater() && (helmet == null || !helmet.containsEnchantment(Enchantment.WATER_WORKER)))
            multiplier /= 5;

        if (!player.isOnGround()) multiplier /= 5;

        return multiplier;
    }

    public Drop drop() {
        return drop;
    }

    public boolean hasLight() {
        return light.hasLightLevel();
    }

    public LightMechanic light() {
        return light;
    }

    public boolean hasLimitedPlacing() {
        return limitedPlacing != null;
    }

    public LimitedPlacing limitedPlacing() {
        return limitedPlacing;
    }

    public boolean hasBlockSounds() {
        return blockSounds != null;
    }

    public BlockSounds blockSounds() {
        return blockSounds;
    }

    public boolean hasClickActions() {
        return !clickActions.isEmpty();
    }

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

    public boolean isBlastResistant() {
        return blastResistant;
    }
}
