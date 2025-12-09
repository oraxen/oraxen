package io.th0rgal.oraxen.mechanics.provided.combat.spear;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.Settings;
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

    private static SpearLungeMechanicFactory instance;
    private final List<SpearLungeMechanic> registeredMechanics = new ArrayList<>();

    public SpearLungeMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SpearLungeMechanicListener(this));
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
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
        if (!VersionUtil.atOrAbove("1.21.4") || !Settings.APPEARANCE_ITEM_MODEL.toBool()) {
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
