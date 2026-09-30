package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.ConfigPropertyValues;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BackpackMechanic extends Mechanic {

    public static final NamespacedKey BACKPACK_KEY = new NamespacedKey(OraxenPlugin.get(), "backpack");
    private final int rows;
    private final String title;
    private final String openSound;
    private final String closeSound;
    private final float volume;
    private final float pitch;
    private final Set<String> blockedOraxenItems = new HashSet<>();
    private final Set<String> blockedVanillaItems = new HashSet<>();

    public BackpackMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        rows = ConfigPropertyValues.integer(BackpackMechanicFactory.class, section, BackpackMechanicFactory.PROP_ROWS);
        title = ConfigPropertyValues.text(BackpackMechanicFactory.class, section, BackpackMechanicFactory.PROP_TITLE);
        openSound = ConfigPropertyValues.text(BackpackMechanicFactory.class, section, BackpackMechanicFactory.PROP_OPEN_SOUND);
        closeSound = ConfigPropertyValues.text(BackpackMechanicFactory.class, section, BackpackMechanicFactory.PROP_CLOSE_SOUND);
        volume = (float) ConfigPropertyValues.decimal(BackpackMechanicFactory.class, section, BackpackMechanicFactory.PROP_VOLUME);
        pitch = (float) ConfigPropertyValues.decimal(BackpackMechanicFactory.class, section, BackpackMechanicFactory.PROP_PITCH);
        for (String item : section.getStringList("blocked-items")) {
            String normalized = item.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("oraxen:"))
                blockedOraxenItems.add(normalized.substring("oraxen:".length()));
            else
                blockedVanillaItems.add(normalized.startsWith("minecraft:")
                        ? normalized.substring("minecraft:".length()) : normalized);
        }
    }

    public int getRows() {
        return rows;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasOpenSound(){
        return openSound != null;
    }

    public String getOpenSound() {
        return openSound;
    }

    public boolean hasCloseSound(){
        return closeSound != null;
    }

    public String getCloseSound() {
        return closeSound;
    }

    public float getPitch() {
        return pitch;
    }

    public float getVolume() {
        return volume;
    }

    public boolean isBlocked(ItemStack item) {
        if (item == null) return false;

        String oraxenId = OraxenItems.getIdByItem(item);
        if (oraxenId != null) return blockedOraxenItems.contains(oraxenId.toLowerCase(Locale.ROOT));

        String material = item.getType().getKey().getKey();
        return blockedVanillaItems.contains(material)
                || (material.endsWith("_shulker_box") && blockedVanillaItems.contains("shulker_box"));
    }
}
