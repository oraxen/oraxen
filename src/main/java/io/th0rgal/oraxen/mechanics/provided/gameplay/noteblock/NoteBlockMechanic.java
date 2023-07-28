package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockDryout;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip.LogStripping;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class NoteBlockMechanic extends Mechanic {

    public static final NamespacedKey FARMBLOCK_KEY = new NamespacedKey(OraxenPlugin.get(), "farmblock");
    private final int customVariation;
    private final Drop drop;
    private final LimitedPlacing limitedPlacing;
    private final StorageMechanic storage;
    private final BlockSounds blockSounds;
    private String model;
    private final int hardness;
    private final int light;
    private final boolean canIgnite;
    private final boolean isFalling;
    private final FarmBlockDryout farmBlockDryout;
    private final LogStripping logStripping;
    private final DirectionalBlock directionalBlock;
    private final List<ClickAction> clickActions;

    @SuppressWarnings("unchecked")
    public NoteBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic
         */
        super(mechanicFactory, section);
        if (section.isString("model"))
            model = section.getString("model");

        customVariation = section.getInt("custom_variation");

        List<Loot> loots = new ArrayList<>();
        if (section.isConfigurationSection("drop")) {
            ConfigurationSection drop = section.getConfigurationSection("drop");
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>)
                    drop.getList("loots"))
                loots.add(new Loot(lootConfig));

            if (drop.isString("minimal_type")) {
                NoteBlockMechanicFactory mechanic = (NoteBlockMechanicFactory) mechanicFactory;
                List<String> bestTools = drop.isList("best_tools")
                        ? drop.getStringList("best_tools")
                        : new ArrayList<>();
                this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"),
                        drop.getBoolean("fortune"), getItemID(),
                        drop.getString("minimal_type"),
                        bestTools);
            } else
                this.drop = new Drop(loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"),
                        getItemID());
        } else
            drop = new Drop(loots, false, false, getItemID());

        // hardness requires protocollib
        hardness = section.getInt("hardness", 1);

        light = Math.min(section.getInt("light", -1), 15);
        clickActions = ClickAction.parseList(section);
        canIgnite = section.getBoolean("can_ignite", false);
        isFalling = section.getBoolean("is_falling", false);

        if (section.isConfigurationSection("farmblock")) {
            farmBlockDryout = new FarmBlockDryout(getItemID(), Objects.requireNonNull(section.getConfigurationSection("farmblock")));
            ((NoteBlockMechanicFactory) getFactory()).registerFarmBlock();
        } else farmBlockDryout = null;

        if (section.isConfigurationSection("logStrip")) {
            logStripping = new LogStripping(Objects.requireNonNull(section.getConfigurationSection("logStrip")));
        } else logStripping = null;

        if (section.isConfigurationSection("directional")) {
            directionalBlock = new DirectionalBlock(Objects.requireNonNull(section.getConfigurationSection("directional")));
        } else directionalBlock = null;

        if (section.isConfigurationSection("limited_placing")) {
            limitedPlacing = new LimitedPlacing(Objects.requireNonNull(section.getConfigurationSection("limited_placing")));
        } else limitedPlacing = null;

        if (section.isConfigurationSection("storage")) {
            storage = new StorageMechanic(Objects.requireNonNull(section.getConfigurationSection("storage")));
        } else storage = null;

        if (section.isConfigurationSection("block_sounds")) {
            blockSounds = new BlockSounds(Objects.requireNonNull(section.getConfigurationSection("block_sounds")));
        } else blockSounds = null;
    }

    public boolean hasLimitedPlacing() { return limitedPlacing != null; }
    public LimitedPlacing getLimitedPlacing() { return limitedPlacing; }

    public boolean isStorage() { return storage != null; }
    public StorageMechanic getStorage() { return storage; }

    public boolean hasBlockSounds() { return blockSounds != null; }
    public BlockSounds getBlockSounds() { return blockSounds; }

    public boolean hasDryout() { return farmBlockDryout != null; }
    public FarmBlockDryout getDryout() { return farmBlockDryout; }

    public boolean isLog() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return logStripping != null || directionalBlock.getParentMechanic().isLog();
        } else return logStripping != null;
    }
    public LogStripping getLog() { return logStripping; }

    public boolean isFalling() {
        if (isDirectional() && !directionalBlock.isParentBlock()) {
            return isFalling || directionalBlock.getParentMechanic().isFalling();
        } else return isFalling;
    }

    public boolean isDirectional() { return directionalBlock != null; }
    public DirectionalBlock getDirectional() { return directionalBlock; }

    public String getModel(ConfigurationSection section) {
        if (model != null)
            return model;
        // use the itemstack model if block model isn't set
        return section.getString("Pack.model");
    }

    public int getCustomVariation() {
        return customVariation;
    }

    public Drop getDrop() {
        return drop;
    }

    public boolean hasHardness() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return hardness != -1 || directionalBlock.getParentMechanic().hasHardness();
        } else return hardness != -1;
    }

    public int getHardness() {
        return hardness;
    }

    public boolean hasLight() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return light != -1 || directionalBlock.getParentMechanic().hasLight();
        } else return light != -1;
    }

    public int getLight() {
        return light;
    }

    public boolean canIgnite() {
        if (isDirectional() && !getDirectional().isParentBlock()) {
            return canIgnite || directionalBlock.getParentMechanic().canIgnite();
        } else return canIgnite;
    }

    public boolean hasClickActions() { return !clickActions.isEmpty(); }

    public void runClickActions(final Player player) {
        for (final ClickAction action : clickActions) {
            if (action.canRun(player)) {
                action.performActions(player);
            }
        }
    }

    public boolean isInteractable() {
        return hasClickActions() || isStorage();
    }

}
