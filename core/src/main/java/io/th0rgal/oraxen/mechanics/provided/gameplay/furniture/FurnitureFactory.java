package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionTask;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox.JukeboxListener;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class FurnitureFactory extends MechanicFactory {

    public static FurnitureMechanic.FurnitureType defaultFurnitureType;
    public static FurnitureFactory instance;
    public final List<String> toolTypes;
    public final int evolutionCheckDelay;
    private boolean evolvingFurnitures;
    private static EvolutionTask evolutionTask;
    public final boolean customSounds;
    public final boolean detectViabackwards;

    public FurnitureFactory(ConfigurationSection section) {
        super(section);
        if (OraxenPlugin.supportsDisplayEntities)
            defaultFurnitureType = FurnitureMechanic.FurnitureType.getType(section.getString("default_furniture_type", "DISPLAY_ENTITY"));
        else defaultFurnitureType = FurnitureMechanic.FurnitureType.ITEM_FRAME;
        toolTypes = section.getStringList("tool_types");
        evolutionCheckDelay = section.getInt("evolution_check_delay");
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                new FurnitureListener(),
                new FurnitureUpdater(),
                new EvolutionListener(),
                new JukeboxListener()
        );
        evolvingFurnitures = false;
        instance = this;
        customSounds = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds").getBoolean("stringblock_and_furniture", true);

        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurnitureSoundListener());
        detectViabackwards = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("furniture").getBoolean("detect_viabackwards", true);
        //TODO Fix this to not permanently and randomly break furniture
        //if (VersionUtil.isPaperServer()) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurniturePaperListener());
    }

    public static boolean setDefaultType(ConfigurationSection mechanicSection) {
        if (mechanicSection.isSet("type")) return true;
        mechanicSection.set("type", defaultFurnitureType.toString());
        return false;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new FurnitureMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    public static FurnitureFactory getInstance() {
        return instance;
    }

    public static EvolutionTask getEvolutionTask() {
        return evolutionTask;
    }

    public void registerEvolution() {
        if (evolvingFurnitures)
            return;
        if (evolutionTask != null)
            evolutionTask.cancel();
        evolutionTask = new EvolutionTask(this, evolutionCheckDelay);
        MechanicsManager.registerTask(getMechanicID(), evolutionTask.start(0, evolutionCheckDelay));
        evolvingFurnitures = true;
    }

    public static void unregisterEvolution() {
        if (evolutionTask != null)
            evolutionTask.cancel();
    }

    @Override
    public FurnitureMechanic getMechanic(String itemID) {
        return (FurnitureMechanic) super.getMechanic(itemID);
    }

    @Override
    public FurnitureMechanic getMechanic(org.bukkit.inventory.ItemStack itemStack) {
        return (FurnitureMechanic) super.getMechanic(itemStack);
    }

    @Override
    public @Nullable String getMechanicCategory() {
        return "gameplay";
    }

    @Override
    public @Nullable String getMechanicDescription() {
        return "Places items as interactive furniture entities in the world";
    }

    @Override
    public @NotNull List<MechanicConfigProperty> getConfigSchema() {
        return List.of(
                MechanicConfigProperty.enumType("type", "Entity type for the furniture",
                        List.of("DISPLAY_ENTITY", "ITEM_FRAME", "GLOW_ITEM_FRAME")),
                MechanicConfigProperty.integer("hardness", "Break hardness (higher = slower to break)", 1, 0),
                MechanicConfigProperty.string("item", "Alternative Oraxen item ID to display"),
                MechanicConfigProperty.string("modelengine_id", "ModelEngine model ID to use"),
                MechanicConfigProperty.bool("farmland_required", "Whether farmland is required for placement", false),
                MechanicConfigProperty.bool("farmblock_required", "Whether farmblock is required for placement", false),
                MechanicConfigProperty.integer("light", "Light level emitted (0-15)", 0, 0, 15),
                MechanicConfigProperty.enumType("restricted_rotation", "Rotation restriction mode",
                        List.of("NONE", "STRICT", "VERY_STRICT")),
                MechanicConfigProperty.bool("rotatable", "Whether furniture can be rotated after placement", true),
                MechanicConfigProperty.object("hitbox", "Custom hitbox dimensions", Map.of(
                        "width", MechanicConfigProperty.decimal("width", "Hitbox width", 1.0, 0.0, 10.0),
                        "height", MechanicConfigProperty.decimal("height", "Hitbox height", 1.0, 0.0, 10.0)
                )),
                MechanicConfigProperty.object("seat", "Seat configuration for sittable furniture", Map.of(
                        "height", MechanicConfigProperty.decimal("height", "Seat height offset", 0.0),
                        "yaw", MechanicConfigProperty.decimal("yaw", "Seat rotation", 0.0)
                )),
                MechanicConfigProperty.list("barriers", "List of barrier block positions relative to furniture"),
                MechanicConfigProperty.object("display_entity_properties", "Display entity configuration", Map.of(
                        "display_transform", MechanicConfigProperty.enumType("display_transform", "Display transform mode",
                                List.of("NONE", "THIRDPERSON_LEFTHAND", "THIRDPERSON_RIGHTHAND", "FIRSTPERSON_LEFTHAND",
                                        "FIRSTPERSON_RIGHTHAND", "HEAD", "GUI", "GROUND", "FIXED")),
                        "scale", MechanicConfigProperty.object("scale", "Scale of the display entity", Map.of(
                                "x", MechanicConfigProperty.decimal("x", "X-axis scale", 1.0),
                                "y", MechanicConfigProperty.decimal("y", "Y-axis scale", 1.0),
                                "z", MechanicConfigProperty.decimal("z", "Z-axis scale", 1.0)
                        )),
                        "translation", MechanicConfigProperty.object("translation", "Position offset in blocks", Map.of(
                                "x", MechanicConfigProperty.decimal("x", "X-axis offset", 0.0),
                                "y", MechanicConfigProperty.decimal("y", "Y-axis offset", 0.0),
                                "z", MechanicConfigProperty.decimal("z", "Z-axis offset", 0.0)
                        )),
                        "brightness", MechanicConfigProperty.object("brightness", "Light levels", Map.of(
                                "block", MechanicConfigProperty.integer("block", "Block light level", 0, 0, 15),
                                "sky", MechanicConfigProperty.integer("sky", "Sky light level", 0, 0, 15)
                        ))
                )),
                MechanicConfigProperty.object("drop", "Drop configuration when broken", Map.of(
                        "silktouch", MechanicConfigProperty.bool("silktouch", "Require silk touch to drop", false),
                        "loots", MechanicConfigProperty.list("loots", "List of loot entries")
                )),
                MechanicConfigProperty.object("storage", "Storage container configuration", Map.of(
                        "type", MechanicConfigProperty.enumType("type", "Storage type", List.of("STORAGE", "PERSONAL", "ENDERCHEST", "DISPOSAL")),
                        "rows", MechanicConfigProperty.integer("rows", "Number of inventory rows", 1, 1, 6),
                        "title", MechanicConfigProperty.string("title", "Inventory title")
                ))
        );
    }
}
