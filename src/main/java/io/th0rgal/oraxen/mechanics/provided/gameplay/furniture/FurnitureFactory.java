package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolutionTask;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.jukebox.JukeboxListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text.FurnitureTextEntry;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text.FurnitureTextPacketBridge;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text.FurnitureTextRegistry;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    private boolean warnedAboutGhastDebugVisibility;
    private boolean warnedAboutShulkerDebugVisibility;
    private static EvolutionTask evolutionTask;
    public final boolean customSounds;
    public final boolean detectViabackwards;

    public FurnitureFactory(ConfigurationSection section) {
        super(section);
        defaultFurnitureType = FurnitureMechanic.FurnitureType.getType(section.getString("default_furniture_type", "DISPLAY_ENTITY"));
        toolTypes = section.getStringList("tool_types");
        evolutionCheckDelay = section.getInt("evolution_check_delay");
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(),
                new FurnitureListener(),
                new FurnitureUpdater(),
                new EvolutionListener(),
                new JukeboxListener(),
                new FurnitureTextLoadListener()
        );
        if (VersionUtil.atOrAbove("1.21.5"))
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurniturePickItemListener());
        if (VersionUtil.atOrAbove("1.21.6"))
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurnitureGhastEntities());
        if (VersionUtil.atOrAbove("1.21.2")) {
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurnitureShulkerEntities());
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurnitureBeds());
            MechanicsManager.registerTask(getMechanicID(), FurnitureBeds.startNightSkipTask());
        }
        // EntityRemoveEvent is missing from early 1.20.4 builds
        if (VersionUtil.atOrAbove("1.20.5"))
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurniturePaperListener());
        evolvingFurnitures = false;
        instance = this;
        FurniturePacketDispatcher.init();
        FurnitureTextPacketBridge.unregister();
        FurnitureTextPacketBridge.register();
        customSounds = areCustomSoundsEnabled();

        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FurnitureSoundListener());
        detectViabackwards = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("furniture").getBoolean("detect_viabackwards", true);
    }

    @Override
    public void onUnregister() {
        BreakerSystem.unregisterModifier(FurnitureListener.class);
        FurnitureBeds.leaveAll();
    }

    public static boolean setDefaultType(ConfigurationSection mechanicSection) {
        if (mechanicSection.isSet("type")) return true;
        mechanicSection.set("type", defaultFurnitureType.toString());
        return false;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        FurnitureMechanic mechanic = new FurnitureMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        if (!warnedAboutGhastDebugVisibility && mechanic.hasGhastHitboxes() && Settings.DEBUG.toBool()) {
            warnedAboutGhastDebugVisibility = true;
            Logs.logWarning("Ghast type furniture hitboxes are visible while in debug mode.");
        }
        if (!warnedAboutShulkerDebugVisibility && mechanic.hasShulkerHitboxes() && Settings.DEBUG.toBool()) {
            warnedAboutShulkerDebugVisibility = true;
            Logs.logWarning("Shulker type furniture hitboxes are visible while in debug mode.");
        }
        return mechanic;
    }

    public void resetHitboxDebugWarnings() {
        warnedAboutGhastDebugVisibility = false;
        warnedAboutShulkerDebugVisibility = false;
    }

    public static boolean isEnabled() {
        return instance != null && MechanicsManager.isMechanicEnabled("furniture");
    }

    public static boolean areCustomSoundsEnabled() {
        ConfigurationSection customSoundsSection = OraxenPlugin.get().getConfigsManager().getMechanics()
                .getConfigurationSection("custom_block_sounds");
        return BlockSounds.isFurnitureSoundEnabled(customSoundsSection);
    }

    public static FurnitureFactory getInstance() {
        return instance;
    }

    private static void registerLoadedFurnitureText() {
        // OraxenItemsLoadedEvent fires on the global thread (startup) or a
        // command thread (reload). Enumerating the world entity list is a safe
        // snapshot read, but registering reads entity PDC and, for missing
        // viewers, Entity#getTrackedBy, which Folia thread-checks against the
        // owning region; hop to each entity's scheduler before touching it.
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                SchedulerUtil.runForEntity(entity, () -> {
                    if (!OraxenFurniture.isBaseEntity(entity)) return;
                    registerTextEntity(entity, true);
                });
            }
        }
    }

    static void registerTextEntity(Entity entity) {
        registerTextEntity(entity, false);
    }

    static void registerTextEntity(Entity entity, boolean spawnForMissingViewers) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic != null && mechanic.hasTextDefinitions()) {
            FurnitureTextEntry previous = FurnitureTextRegistry.byUuid(entity.getUniqueId());
            boolean reused = FurnitureTextRegistry.canReuse(entity.getUniqueId(), mechanic.getTextDefinitions().size());
            if (previous != null && !reused) {
                FurnitureTextPacketBridge.destroyAndUnregister(entity.getUniqueId());
            }
            FurnitureTextEntry entry = FurnitureTextRegistry.register(entity, mechanic.getTextDefinitions());
            if (previous != null && reused) {
                if (FurnitureTextPacketBridge.placementChanged(previous.getBaseLocation(), entry.getBaseLocation()))
                    FurnitureTextPacketBridge.respawnTrackedViewers(entry);
                else
                    FurnitureTextPacketBridge.updateTrackedViewers(entry);
            } else if (spawnForMissingViewers) {
                FurnitureTextPacketBridge.spawnForTrackedViewers(entry);
            }
        } else {
            FurnitureTextPacketBridge.destroyAndUnregister(entity.getUniqueId());
        }
    }

    private static final class FurnitureTextLoadListener implements Listener {
        @EventHandler
        public void onItemsLoaded(OraxenItemsLoadedEvent event) {
            registerLoadedFurnitureText();
        }
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
        FurnitureBeds.leaveAll();
        FurniturePacketDispatcher.shutdown();
        FurnitureTextPacketBridge.unregister();
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
                        List.of("DISPLAY_ENTITY", "ITEM_FRAME", "GLOW_ITEM_FRAME", "ARMOR_STAND")),
                MechanicConfigProperty.integer("hardness", "Break hardness (higher = slower to break)", 1, 0),
                MechanicConfigProperty.object("appearance", "Placed furniture appearance; uses the same options as pack", Map.of(
                        "model", MechanicConfigProperty.string("model", "Placed furniture model path"),
                        "generate_model", MechanicConfigProperty.bool("generate_model", "Generate the furniture model from textures", false),
                        "textures", MechanicConfigProperty.list("textures", "Furniture model textures"),
                        "parent_model", MechanicConfigProperty.string("parent_model", "Parent for generated furniture models"),
                        "models", MechanicConfigProperty.object("models", "Named furniture stage model paths", Map.of())
                )),
                MechanicConfigProperty.string("item", "Alternative Oraxen item ID to display"),
                MechanicConfigProperty.string("modelengine_id", "ModelEngine model ID to use"),
                MechanicConfigProperty.bool("farmland_required", "Whether farmland is required for placement", false),
                MechanicConfigProperty.bool("farmblock_required", "Whether farmblock is required for placement", false),
                MechanicConfigProperty.list("lights", "List of light entries formatted '<x>,<y>,<z> <level>'"),
                MechanicConfigProperty.enumType("restricted_rotation", "Rotation restriction mode",
                        List.of("NONE", "STRICT", "VERY_STRICT")),
                MechanicConfigProperty.bool("rotatable", "Whether furniture can be rotated after placement", true),
                MechanicConfigProperty.bool("small", "Whether an armor stand furniture uses the small variant", true),
                MechanicConfigProperty.list("hitboxes", "List of hitbox offsets and dimensions formatted '<x>,<y>,<z> <width>,<height>'"),
                MechanicConfigProperty.list("ghasts", "List of solid hitboxes formatted '<x>,<y>,<z> <scale>'. Requires Minecraft 1.21.6 or newer"),
                MechanicConfigProperty.list("shulkers", "List of solid hitboxes formatted '<x>,<y>,<z> <scale> [peek] [direction]'. Requires Minecraft 1.21.2 or newer"),
                MechanicConfigProperty.list("beds", "List of spots where players lie down, formatted '<x>,<y>,<z> [skip_night] [reset_phantoms] [night_only] [set_respawn]'. The offset is where the head rests. The options default to true, true, true and false. Requires Minecraft 1.21.2 or newer"),
                MechanicConfigProperty.list("seats", "List of seat offsets relative to the furniture center formatted '<x>,<y>,<z>' or '<x>,<y>,<z> <yaw>'"),
                MechanicConfigProperty.list("barriers", "List of barrier block offsets relative to furniture formatted '<x>,<y>,<z>' with integer coordinates"),
                MechanicConfigProperty.list("events", "Actions for furniture clicks or successful player breaks or placements. Use trigger: click with type: LEFT, RIGHT, or BOTH, trigger: break, or trigger: place. The trigger defaults to click; the legacy click filter remains supported."),
                MechanicConfigProperty.object("properties", "Furniture entity properties; unsupported properties are ignored", Map.of(
                        "display_transform", MechanicConfigProperty.enumType("display_transform", "Display transform mode",
                                List.of("NONE", "THIRDPERSON_LEFTHAND", "THIRDPERSON_RIGHTHAND", "FIRSTPERSON_LEFTHAND",
                                        "FIRSTPERSON_RIGHTHAND", "HEAD", "GUI", "GROUND", "FIXED")),
                        "scale", MechanicConfigProperty.builder("scale", MechanicConfigProperty.Type.OBJECT_OR_NUMBER)
                                .description("Entity scale; armor stands also accept a number for uniform scaling").nested(Map.of(
                                "x", MechanicConfigProperty.decimal("x", "X-axis scale", 1.0),
                                "y", MechanicConfigProperty.decimal("y", "Y-axis scale", 1.0),
                                "z", MechanicConfigProperty.decimal("z", "Z-axis scale", 1.0)
                        )).build(),
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
