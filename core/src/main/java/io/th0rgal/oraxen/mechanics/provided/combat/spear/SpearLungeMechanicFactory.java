package io.th0rgal.oraxen.mechanics.provided.combat.spear;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.AppearanceMode;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.PropertyType;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@MechanicInfo(
        category = "combat",
        description = "Enables a charge-and-lunge attack with model swapping for spears"
)
public class SpearLungeMechanicFactory extends MechanicFactory implements Listener {

    @ConfigProperty(type = PropertyType.STRING, description = "Model path for active/charging state")
    public static final String PROP_ACTIVE_MODEL = "active_model";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Ticks to fully charge the lunge", defaultValue = "12", min = 1)
    public static final String PROP_CHARGE_TICKS = "charge_ticks";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Velocity multiplier for the lunge", defaultValue = "0.6", min = 0.0, max = 5.0)
    public static final String PROP_LUNGE_VELOCITY = "lunge_velocity";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Maximum range to hit targets during lunge", defaultValue = "3.5", min = 0.0, max = 10.0)
    public static final String PROP_MAX_RANGE = "max_range";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Base damage dealt during lunge", defaultValue = "6.0", min = 0.0)
    public static final String PROP_DAMAGE = "damage";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Minimum damage dealt during lunge (at 0% charge)", defaultValue = "0.0", min = 0.0)
    public static final String PROP_MIN_DAMAGE = "min_damage";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Knockback applied to targets hit by the lunge", defaultValue = "0.5", min = 0.0)
    public static final String PROP_KNOCKBACK = "knockback";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Hitbox radius for lunge hit detection (higher = easier to hit off-center)", defaultValue = "0.5", min = 0.0, max = 5.0)
    public static final String PROP_HITBOX_RADIUS = "hitbox_radius";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Maximum number of entities hit per lunge", defaultValue = "1", min = 1)
    public static final String PROP_MAX_TARGETS = "max_targets";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Minimum charge percentage required to attack (0-1)", defaultValue = "0.3", min = 0.0, max = 1.0)
    public static final String PROP_MIN_CHARGE_PERCENT = "min_charge_percent";

    @ConfigProperty(type = PropertyType.DOUBLE, description = "Movement slowdown while charging (0-1)", defaultValue = "0.4", min = 0.0, max = 1.0)
    public static final String PROP_CHARGE_SLOWDOWN = "charge_slowdown";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Maximum time to hold charge (ticks) before cancelling", defaultValue = "60", min = 1)
    public static final String PROP_MAX_HOLD_TICKS = "max_hold_ticks";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Number of intermediate animation frames used during charge", defaultValue = "0", min = 0)
    public static final String PROP_SMOOTH_FRAMES = "smooth_frames";

    @ConfigProperty(type = PropertyType.LIST, description = "List of intermediate model paths for smooth charge animation")
    public static final String PROP_INTERMEDIATE_MODELS = "intermediate_models";

    private static SpearLungeMechanicFactory instance;
    private final List<SpearLungeMechanic> registeredMechanics = new ArrayList<>();

    public SpearLungeMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(
                OraxenPlugin.get(),
                getMechanicID(),
                new SpearLungeMechanicListener(this),
                this
        );
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        SpearLungeMechanic mechanic = new SpearLungeMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        registeredMechanics.add(mechanic);
        return mechanic;
    }

    @EventHandler
    public void onPackGeneration(OraxenPackGeneratedEvent event) {
        // Spear lunge only uses item model definitions when ITEM_PROPERTIES is enabled on 1.21.4+
        if (!VersionUtil.atOrAbove("1.21.4") || !AppearanceMode.isItemPropertiesEnabled()) {
            return;
        }

        List<VirtualFile> output = event.getOutput();

        for (SpearLungeMechanic mechanic : registeredMechanics) {
            String definitionPath = "assets/oraxen/items";

            if (mechanic.hasActiveModel()) {
                String activeModelId = mechanic.getItemID() + "_active";
                String definitionName = activeModelId + ".json";
                JsonObject definition = createModelDefinition(mechanic.getActiveModelPath());
                VirtualFile virtualFile = new VirtualFile(
                        definitionPath,
                        definitionName,
                        new ByteArrayInputStream(definition.toString().getBytes(StandardCharsets.UTF_8))
                );
                output.add(virtualFile);
            }

            for (int i = 0; i < mechanic.getIntermediateModelCount(); i++) {
                String framePath = mechanic.getIntermediateModelPath(i);
                if (framePath == null) continue;

                String frameModelId = mechanic.getItemID() + "_frame" + i;
                String frameName = frameModelId + ".json";
                JsonObject frameDefinition = createModelDefinition(framePath);
                VirtualFile frameFile = new VirtualFile(
                        definitionPath,
                        frameName,
                        new ByteArrayInputStream(frameDefinition.toString().getBytes(StandardCharsets.UTF_8))
                );
                output.add(frameFile);
            }
        }
    }

    private JsonObject createModelDefinition(String modelPath) {
        JsonObject root = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelPath);
        root.add("model", model);
        return root;
    }

    public static SpearLungeMechanicFactory get() {
        return instance;
    }

    public List<SpearLungeMechanic> getRegisteredMechanics() {
        return new ArrayList<>(registeredMechanics);
    }
}
