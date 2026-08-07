package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockMechanicFactory extends MechanicFactory {

    private static BlockMechanicFactory instance;

    private final NoteBlockMechanicFactory noteBlockFactory;
    private final StringBlockMechanicFactory stringBlockFactory;
    private final ChorusBlockMechanicFactory chorusBlockFactory;
    private final ShapedBlockMechanicFactory shapedBlockFactory;
    public final List<String> toolTypes;

    public BlockMechanicFactory(ConfigurationSection section) {
        super(section);
        normalizeFactoryAliases(section);
        toolTypes = section.getStringList("tool_types");

        noteBlockFactory = new NoteBlockMechanicFactory(section, true);
        stringBlockFactory = new StringBlockMechanicFactory(section, true);
        chorusBlockFactory = new ChorusBlockMechanicFactory(section, true);
        shapedBlockFactory = new ShapedBlockMechanicFactory(section, true);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BlockMechanicListener());
        instance = this;
    }

    public static BlockMechanicFactory getInstance() {
        return instance;
    }

    public static boolean isEnabled() {
        return instance != null && MechanicsManager.isMechanicEnabled("block");
    }

    @Override
    public void onUnregister() {
        ItemUpdater.resetQueuedTasks();
        if (instance == this) instance = null;
        NoteBlockMechanicFactory.clearInstance(noteBlockFactory);
        StringBlockMechanicFactory.clearInstance(stringBlockFactory);
        ChorusBlockMechanicFactory.clearInstance(chorusBlockFactory);
        ShapedBlockMechanicFactory.clearInstance(shapedBlockFactory);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        normalizeAliases(itemMechanicConfiguration);

        BlockType type = BlockType.fromConfig(itemMechanicConfiguration.getString("type"));
        if (type == null) {
            Logs.logError("The block mechanic of " + itemId(itemMechanicConfiguration)
                    + " requires a valid type.");
            Logs.logWarning("Valid block types are: FULL, STAIR, SLAB, DOOR, TRAPDOOR, GRATE, BULB, STRING, CHORUS");
            return null;
        }

        Mechanic mechanic = switch (type) {
            case FULL -> noteBlockFactory.parse(itemMechanicConfiguration);
            case STRING -> stringBlockFactory.parse(itemMechanicConfiguration);
            case CHORUS -> chorusBlockFactory.parse(itemMechanicConfiguration);
            case STAIR, SLAB, DOOR, TRAPDOOR, GRATE, BULB -> shapedBlockFactory.parse(itemMechanicConfiguration);
        };

        // Delegate factories keep their own registries for legacy subsystem lookups;
        // the unified block factory also registers the same mechanic for block API lookups.
        if (mechanic != null) addToImplemented(mechanic);
        else Logs.logWarning("Failed to parse block mechanic for " + itemId(itemMechanicConfiguration));
        return mechanic;
    }

    private void normalizeAliases(ConfigurationSection section) {
        copyIfPresent(section, "custom-variation", "custom_variation");
        copyIfPresent(section, "block-sounds", "block_sounds");
        copyIfPresent(section, "limited-placing", "limited_placing");
        copyIfPresent(section, "can-ignite", "can_ignite");
        copyIfPresent(section, "is-falling", "is_falling");
        copyIfPresent(section, "blast-resistant", "blast_resistant");
        copyIfPresent(section, "random-place", "random_place");
        copyIfPresent(section, "is-tall", "is_tall");

        ConfigurationSection appearance = section.getConfigurationSection("appearance");
        if (appearance == null) return;

        if (!section.contains("model") && appearance.contains("model"))
            section.set("model", appearance.get("model"));
        if (!section.contains("textures") && appearance.contains("textures"))
            section.set("textures", appearance.get("textures"));
    }

    private void normalizeFactoryAliases(ConfigurationSection section) {
        copyIfPresent(section, "tool-types", "tool_types");
        copyIfPresent(section, "farmblock-check-delay", "farmblock_check_delay");
        copyIfPresent(section, "sapling-growth-check-delay", "sapling_growth_check_delay");
        copyIfPresent(section, "disable-vanilla-strings", "disable_vanilla_strings");
        copyIfPresent(section, "remove-mineable-tag", "remove_mineable_tag");
        copyIfPresent(section, "convert-vanilla-waxed", "convert_vanilla_waxed");
        copyIfPresent(section, "handle-world-generation", "handle_world_generation");
    }

    private void copyIfPresent(ConfigurationSection section, String from, String to) {
        if (!section.contains(to) && section.contains(from)) section.set(to, section.get(from));
    }

    private String itemId(ConfigurationSection section) {
        ConfigurationSection itemSection = section.getParent() != null ? section.getParent().getParent() : null;
        return itemSection != null ? itemSection.getName() : section.getCurrentPath();
    }

    public ShapedBlockMechanicFactory getShapedBlockFactory() {
        return shapedBlockFactory;
    }

    @Override
    public @Nullable String getMechanicCategory() {
        return "gameplay";
    }

    @Override
    public @Nullable String getMechanicDescription() {
        return "Creates custom blocks using the configured backing block type";
    }

    @Override
    public @NotNull List<MechanicConfigProperty> getConfigSchema() {
        return List.of(
                MechanicConfigProperty.enumType("type", "Backing block type",
                        List.of("FULL", "STAIR", "SLAB", "DOOR", "TRAPDOOR", "GRATE", "BULB", "STRING", "CHORUS")),
                MechanicConfigProperty.object("appearance", "Placed block appearance", Map.of(
                        "model", MechanicConfigProperty.string("model", "Placed block model path"),
                        "textures", MechanicConfigProperty.list("textures", "Placed block textures")
                )),
                MechanicConfigProperty.integer("custom-variation", "Backing-state variation ID", 1),
                MechanicConfigProperty.list("breaking", "Ordered block breaking rules"),
                MechanicConfigProperty.list("events", "Click events with actions to run when the placed block is clicked"),
                MechanicConfigProperty.integer("light", "Light level emitted by the placed block (0-15, 0 disables)", 0, 0, 15),
                MechanicConfigProperty.object("placeable", "Placement face and block restrictions", Map.of(
                        "roof", MechanicConfigProperty.bool("roof", "Allow placement on ceilings", true),
                        "wall", MechanicConfigProperty.bool("wall", "Allow placement on walls", true),
                        "floor", MechanicConfigProperty.bool("floor", "Allow placement on floors", true),
                        "allow", MechanicConfigProperty.list("allow", "Vanilla block IDs this block may be placed against"),
                        "disallow", MechanicConfigProperty.list("disallow", "Vanilla block IDs this block may not be placed against")
                )),
                MechanicConfigProperty.object("block-sounds", "Custom block sounds", Map.of(
                        "place-sound", MechanicConfigProperty.string("place-sound", "Sound when placed"),
                        "break-sound", MechanicConfigProperty.string("break-sound", "Sound when broken"),
                        "step-sound", MechanicConfigProperty.string("step-sound", "Sound when stepped on"),
                        "hit-sound", MechanicConfigProperty.string("hit-sound", "Sound when hit"),
                        "fall-sound", MechanicConfigProperty.string("fall-sound", "Sound when fallen on")
                ))
        );
    }

    private enum BlockType {
        FULL,
        STAIR,
        SLAB,
        DOOR,
        TRAPDOOR,
        GRATE,
        BULB,
        STRING,
        CHORUS;

        @Nullable
        private static BlockType fromConfig(String value) {
            if (value == null || value.isBlank()) return null;
            try {
                return valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
