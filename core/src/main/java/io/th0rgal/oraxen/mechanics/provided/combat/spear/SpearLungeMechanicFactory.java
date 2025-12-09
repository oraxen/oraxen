package io.th0rgal.oraxen.mechanics.provided.combat.spear;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
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

/**
 * Factory for creating SpearLungeMechanic instances.
 * <p>
 * This factory also handles resource pack integration by registering
 * additional item model definitions for the active spear models.
 * The model definitions are added during pack generation to ensure
 * the item_model component can properly reference the active model.
 */
public class SpearLungeMechanicFactory extends MechanicFactory implements Listener {

    private static SpearLungeMechanicFactory instance;
    private final List<SpearLungeMechanic> registeredMechanics = new ArrayList<>();

    public SpearLungeMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SpearLungeMechanicListener(this));
        
        // Register this factory as a listener for pack generation events
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        SpearLungeMechanic mechanic = new SpearLungeMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        registeredMechanics.add(mechanic);
        return mechanic;
    }

    /**
     * Listens to pack generation and adds item model definitions for active spear models.
     * This ensures the item_model component can reference the active model using
     * the oraxen namespace (e.g., oraxen:lotr_pike_active).
     */
    @EventHandler
    public void onPackGeneration(OraxenPackGeneratedEvent event) {
        // Only generate model definitions if using the item_model system (1.21.4+)
        if (!VersionUtil.atOrAbove("1.21.4") || !Settings.APPEARANCE_ITEM_MODEL.toBool()) {
            return;
        }

        List<VirtualFile> output = event.getOutput();
        
        for (SpearLungeMechanic mechanic : registeredMechanics) {
            String definitionPath = "assets/oraxen/items";
            
            // Create item model definition for the active model
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
            
            // Create item model definitions for intermediate animation frames
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

    /**
     * Creates a JSON item model definition that points to the specified model path.
     * Format follows Minecraft 1.21.4+ item model definition structure.
     */
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

    /**
     * Returns all registered spear lunge mechanics.
     * Useful for debugging and pack generation.
     */
    public List<SpearLungeMechanic> getRegisteredMechanics() {
        return new ArrayList<>(registeredMechanics);
    }
}
