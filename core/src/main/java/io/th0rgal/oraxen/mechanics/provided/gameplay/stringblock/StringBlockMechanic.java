package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerMechanic;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StringBlockMechanic extends Mechanic {

    private final int customVariation;
    private final Drop drop;
    private final BlockSounds blockSounds;
    private final LimitedPlacing limitedPlacing;
    private final StorageMechanic storage;
    private String model;
    private final double hardness;
    private final LightMechanic light;
    private final boolean blastResistant;
    private final boolean immovable;
    private final boolean isFalling;

    private final List<String> randomPlaceBlock;
    private final SaplingMechanic saplingMechanic;
    private final boolean isTall;

    private final BlockLockerMechanic blockLocker;
    private final List<ClickAction> clickActions;

    // Stackable block support: each variation represents one stack level
    private final List<StackVariation> stackVariations;

    /**
     * Represents a single stack level for stackable blocks (e.g., Pink Petals).
     * Each level has its own custom_variation and model, and the drop amount
     * scales with the stack index.
     */
    public record StackVariation(int customVariation, String model) {}

    @SuppressWarnings("unchecked")
    public StringBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic
         */
        super(mechanicFactory, section);

        model = section.getString("model");
        customVariation = section.getInt("custom_variation");
        isTall = section.getBoolean("is_tall", false);
        hardness = section.getDouble("hardness", 1.0D);
        light = new LightMechanic(section);
        blastResistant = section.getBoolean("blast_resistant", false);
        immovable = section.getBoolean("immovable", false);
        isFalling = section.getBoolean("is_falling", false);

        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        drop = dropSection != null ? Drop.createDrop(StringBlockMechanicFactory.getInstance().toolTypes, dropSection, getItemID()) : new Drop(new ArrayList<>(), false, false, getItemID());

        ConfigurationSection randomPlaceSection = section.getConfigurationSection("random_place");
        randomPlaceBlock = randomPlaceSection != null ? randomPlaceSection.getStringList("blocks") : new ArrayList<>();

        ConfigurationSection saplingSection = section.getConfigurationSection("sapling");
        saplingMechanic = saplingSection != null ? new SaplingMechanic(getItemID(), saplingSection) : null;

        ConfigurationSection limitedSection = section.getConfigurationSection("limited_placing");
        limitedPlacing = limitedSection != null ? new LimitedPlacing(limitedSection) : null;

        ConfigurationSection storageSection = section.getConfigurationSection("storage");
        storage = storageSection != null ? new StorageMechanic(storageSection) : null;

        ConfigurationSection blockSoundsSection = section.getConfigurationSection("block_sounds");
        blockSounds = blockSoundsSection != null ? new BlockSounds(blockSoundsSection) : null;

        ConfigurationSection blockLockerSection = section.getConfigurationSection("blocklocker");
        blockLocker = blockLockerSection != null ? new BlockLockerMechanic(blockLockerSection) : null;

        clickActions = ClickAction.parseList(section);

        // Parse stackable block variations
        List<?> stackList = section.getList("stackable");
        if (stackList != null && !stackList.isEmpty()) {
            List<StackVariation> parsed = new ArrayList<>();
            for (Object entry : stackList) {
                if (entry instanceof Map<?, ?> map) {
                    int variation = map.containsKey("custom_variation")
                            ? ((Number) map.get("custom_variation")).intValue() : 0;
                    String stackModel = map.containsKey("model")
                            ? map.get("model").toString() : null;
                    if (variation > 0) {
                        parsed.add(new StackVariation(variation, stackModel));
                    }
                }
            }
            stackVariations = Collections.unmodifiableList(parsed);
        } else {
            stackVariations = Collections.emptyList();
        }
    }

    public String getModel(ConfigurationSection section) {
        return model != null ? model : section.getString("Pack.model");
    }

    public boolean hasBlockSounds() {
        return blockSounds != null;
    }
    public BlockSounds getBlockSounds() {
        return blockSounds;
    }

    public boolean hasLimitedPlacing() { return limitedPlacing != null; }
    public LimitedPlacing getLimitedPlacing() { return limitedPlacing; }

    public boolean isSapling() { return saplingMechanic != null; }
    public SaplingMechanic getSaplingMechanic() { return saplingMechanic; }

    public boolean isTall() { return isTall; }

    public int getCustomVariation() {
        return customVariation;
    }

    public Drop getDrop() {
        return drop;
    }

    public boolean hasHardness() {
        return hardness != -1.0D;
    }

    public double getHardness() {
        return hardness;
    }

    public boolean hasLight() {
        return light.hasLightLevel();
    }

    public LightMechanic getLight() {
        return light;
    }

    public boolean hasRandomPlace() {
        return !randomPlaceBlock.isEmpty();
    }

    public List<String> getRandomPlaceBlock() {
        return randomPlaceBlock;
    }

    public boolean isBlastResistant() {
        return blastResistant;
    }

    public boolean isImmovable() {
        return immovable;
    }

    public BlockLockerMechanic getBlockLocker() {
        return blockLocker;
    }

    public boolean isFalling() {
        return isFalling;
    }

    public boolean isStorage() {
        return storage != null;
    }

    public StorageMechanic getStorage() {
        return storage;
    }

    public boolean hasClickActions() {
        return !clickActions.isEmpty();
    }

    public List<ClickAction> getClickActions() {
        return clickActions;
    }

    public void runClickActions(final Player player) {
        for (final ClickAction action : clickActions) {
            if (action.canRun(player)) {
                action.performActions(player);
            }
        }
    }

    public boolean isInteractable() {
        return hasClickActions() || isStorage() || isStackable();
    }

    // --- Stackable block API ---

    /**
     * Whether this block supports stacking (has stack variations defined).
     */
    public boolean isStackable() {
        return !stackVariations.isEmpty();
    }

    /**
     * Returns the list of stack variations. The parent block is stage 0 (implicit),
     * and each entry here represents stage 1, 2, etc.
     */
    public List<StackVariation> getStackVariations() {
        return stackVariations;
    }

    /**
     * Given a custom_variation currently in the world, returns the next stack variation,
     * or null if already at the maximum stack level.
     */
    public StackVariation getNextStackVariation(int currentVariation) {
        if (stackVariations.isEmpty()) return null;

        // If the current block is the parent (base), return the first stack variation
        if (currentVariation == this.customVariation) {
            return stackVariations.get(0);
        }

        // Find the current variation in the stack list and return the next
        for (int i = 0; i < stackVariations.size(); i++) {
            if (stackVariations.get(i).customVariation() == currentVariation) {
                return (i + 1 < stackVariations.size()) ? stackVariations.get(i + 1) : null;
            }
        }
        return null;
    }

    /**
     * Returns the stack index (1-based drop multiplier) for a given custom_variation.
     * The parent block returns 1, the first stack variation returns 2, etc.
     */
    public int getStackDropMultiplier(int currentVariation) {
        if (currentVariation == this.customVariation) return 1;
        for (int i = 0; i < stackVariations.size(); i++) {
            if (stackVariations.get(i).customVariation() == currentVariation) {
                return i + 2; // stage 0 = 1x, stage 1 = 2x, stage 2 = 3x, ...
            }
        }
        return 1;
    }

    /**
     * Returns whether the given custom_variation belongs to this stackable block
     * (either the parent or one of its stack variations).
     */
    public boolean isStackVariation(int variation) {
        if (variation == this.customVariation) return true;
        for (StackVariation sv : stackVariations) {
            if (sv.customVariation() == variation) return true;
        }
        return false;
    }

}
