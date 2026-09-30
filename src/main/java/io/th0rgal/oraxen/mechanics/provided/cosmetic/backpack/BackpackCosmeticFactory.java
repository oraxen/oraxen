package io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
    category = "cosmetic",
    description = "Displays a cosmetic backpack on the player's back using packet-based armor stands"
)
public class BackpackCosmeticFactory extends MechanicFactory {

    private static BackpackCosmeticFactory instance;
    private static final String armorStandEnabledKey = "armor_stand_enabled";
    private static final String armorStandRangeKey = "armor_stand_range";
    private final BackpackCosmeticListener listener;
    private final boolean armorStandEnabled;
    private final int armorStandRange;

    @ConfigProperty(type = PropertyType.STRING, description = "Equipment slot that triggers backpack display", defaultValue = "CHEST")
    public static final String PROP_SLOT = "slot";

    @ConfigProperty(type = PropertyType.STRING, description = "Model to display as the backpack")
    public static final String PROP_MODEL = "model";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Scale of the backpack", defaultValue = "1.0", min = 0.1, max = 3.0)
    public static final String PROP_SCALE = "scale";

    @ConfigProperty(type = PropertyType.INTEGER, description = "View distance in blocks", defaultValue = "48", min = 8, max = 128)
    public static final String PROP_VIEW_DISTANCE = "view_distance";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Hide backpack in spectator mode", defaultValue = "true")
    public static final String PROP_HIDE_IN_SPECTATOR = "hide_in_spectator";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Hide backpack while the player is swimming", defaultValue = "true")
    public static final String PROP_HIDE_WHILE_SWIMMING = "hide_while_swimming";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Hide backpack while the player is gliding", defaultValue = "true")
    public static final String PROP_HIDE_WHILE_GLIDING = "hide_while_gliding";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Use small armor stand", defaultValue = "false")
    public static final String PROP_SMALL = "small";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether the backpack is visible to the player themselves", defaultValue = "true")
    public static final String PROP_VISIBLE_TO_SELF = "visible_to_self";

    public BackpackCosmeticFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        armorStandEnabled = section.getBoolean(armorStandEnabledKey, true);
        armorStandRange = Math.max(1, section.getInt(armorStandRangeKey, 128));

        listener = new BackpackCosmeticListener(this);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), listener,
                listener.createMountListener(OraxenPlugin.get()));

        BackpackCosmeticManager manager = BackpackCosmeticManager.getInstance();

        SchedulerUtil.ScheduledTask positionTask = SchedulerUtil.runTaskTimer(1L, 1L, manager::updateAllBackpackPositions);
        MechanicsManager.registerTask(getMechanicID(), positionTask);

        SchedulerUtil.ScheduledTask refreshTask = SchedulerUtil.runTaskTimer(20L, 20L, manager::refreshAllViewers);
        MechanicsManager.registerTask(getMechanicID(), refreshTask);

        SchedulerUtil.ScheduledTask armorStandTask = SchedulerUtil.runTaskLater(1L, listener::startExistingArmorStandViewerTasks);
        MechanicsManager.registerTask(getMechanicID(), armorStandTask);

        if (Settings.DEBUG.toBool()) {
            io.th0rgal.oraxen.utils.logs.Logs.logSuccess("BackpackCosmeticFactory initialized");
        }
    }

    public static BackpackCosmeticFactory getInstance() {
        return instance;
    }

    public boolean isArmorStandEnabled() {
        return armorStandEnabled;
    }

    public int getArmorStandRange() {
        return armorStandRange;
    }

    @Override
    public void onUnregister() {
        listener.cleanupArmorStandDisplays();
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        BackpackCosmeticMechanic mechanic = new BackpackCosmeticMechanic(this, section);
        addToImplemented(mechanic);
        if (Settings.DEBUG.toBool()) {
            io.th0rgal.oraxen.utils.logs.Logs.logSuccess("Registered backpack cosmetic: " + mechanic.getItemID() + " (slot: " + mechanic.getTriggerSlot() + ")");
        }
        return mechanic;
    }
}
