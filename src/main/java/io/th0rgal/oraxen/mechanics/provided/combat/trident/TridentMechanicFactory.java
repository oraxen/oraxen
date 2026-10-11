package io.th0rgal.oraxen.mechanics.provided.combat.trident;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.NestedProperty;
import io.th0rgal.oraxen.mechanics.PropertyType;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@MechanicInfo(category = "combat", description = "Custom trident models and sounds with vanilla throwing and enchantments")
public class TridentMechanicFactory extends MechanicFactory implements Listener {
    @ConfigProperty(type = PropertyType.OBJECT, nested = {
            @NestedProperty(name = "throw", type = PropertyType.STRING, description = "Sound when thrown"),
            @NestedProperty(name = "hit", type = PropertyType.STRING, description = "Sound when hitting an entity"),
            @NestedProperty(name = "hit-ground", type = PropertyType.STRING, description = "Defaults to hit"),
            @NestedProperty(name = "return", type = PropertyType.STRING, description = "Defaults to throw")})
    public static final String soundsProperty = "sounds";

    @ConfigProperty(type = PropertyType.OBJECT, nested = {
            @NestedProperty(name = "model", type = PropertyType.STRING, description = "Handheld model"),
            @NestedProperty(name = "charging-model", type = PropertyType.STRING, description = "Model while charging, defaults to model"),
            @NestedProperty(name = "thrown-model", type = PropertyType.STRING, description = "Thrown model"),
            @NestedProperty(name = "transform", type = PropertyType.ENUM, defaultValue = "NONE", enumRef = "ItemDisplayTransform")})
    public static final String appearanceProperty = "appearance";

    private static volatile TridentMechanicFactory instance;
    private final Map<String, TridentMechanic> mechanics = new ConcurrentHashMap<>();

    public TridentMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), this);
    }

    public static TridentMechanicFactory get() { return instance; }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        if (!VersionUtil.atOrAbove("1.21.4")) {
            Logs.logWarning("The trident mechanic requires Paper 1.21.4 or newer.");
            return null;
        }
        try {
            TridentMechanic mechanic = new TridentMechanic(this, section);
            mechanics.put(mechanic.getItemID(), mechanic);
            return mechanic;
        } catch (IllegalArgumentException exception) {
            Logs.logWarning("Invalid trident mechanic for " + section.getParent().getParent().getName()
                    + ".\n" + exception.getMessage());
            return null;
        }
    }

    public void clearMechanics() {
        mechanics.clear();
    }

    @Override
    public void onUnregister() {
        if (instance == this) instance = null;
    }

    @Override
    public Set<String> getItems() { return mechanics.keySet(); }

    @Override
    public TridentMechanic getMechanic(String itemId) { return itemId == null ? null : mechanics.get(itemId); }

    @Override
    public TridentMechanic getMechanic(ItemStack item) { return getMechanic(OraxenItems.getIdByItem(item)); }

    @Override
    public boolean isNotImplementedIn(String itemId) { return getMechanic(itemId) == null; }

    @Override
    public boolean isNotImplementedIn(ItemStack item) { return getMechanic(item) == null; }

    @EventHandler
    public void onPackGenerated(OraxenPackGeneratedEvent event) {
        for (String itemId : getItems()) {
            TridentMechanic mechanic = getMechanic(itemId);
            addDefinition(event.getOutput(), "oraxen", itemId, mechanic.modelDefinition(false).toString());
            addDefinition(event.getOutput(), mechanic.getThrownItemModel().getNamespace(),
                    mechanic.getThrownItemModel().getKey(), mechanic.modelDefinition(true).toString());
        }
    }

    private static void addDefinition(List<VirtualFile> output, String namespace, String itemId, String json) {
        String path = "assets/" + namespace + "/items/" + itemId + ".json";
        output.removeIf(file -> file.getPath().equals(path));
        output.add(new VirtualFile("assets/" + namespace + "/items", itemId + ".json",
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }
}
