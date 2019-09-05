package io.th0rgal.oraxen.items.mechanics.provided.durability;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemUtils;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.items.mechanics.Mechanic;
import io.th0rgal.oraxen.listeners.EventsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class DurabilityMechanic extends Mechanic {

    private static Map<String, DurabilitySettings> settingsByItemID = new HashMap<>();

    public DurabilityMechanic(ConfigurationSection section) {
        super(section, new DurabilityModifier(new DurabilitySettings(section).getValue()));

        //this call in the constructor is safe because EventsManager is using a set so registering the same class two times is not possible
        new EventsManager(OraxenPlugin.get()).addEvents(new Events());
    }

    @Override
    public Set<String> getItems() {
        return settingsByItemID.keySet();
    }

    @Override
    public final String getMechanicID() {
        return "Durability";
    }

    public static boolean implementsDurabilityMechanic(String id) {
        return settingsByItemID.containsKey(id);
    }

    public static DurabilitySettings setDurabilitySettings(String id, DurabilitySettings durabilitySettings) {
        return settingsByItemID.put(id, durabilitySettings);
    }

    public static DurabilitySettings getDurabilitySettingsByID(String id) {
        return settingsByItemID.get(id);
    }

}

class Events implements Listener {

    @EventHandler
    public void onItemDamaged(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (itemID == null || !DurabilityMechanic.implementsDurabilityMechanic(itemID))
            return;

        DurabilitySettings durabilitySettings = DurabilityMechanic.getDurabilitySettingsByID(itemID);
        event.setCancelled(true);

        if (durabilitySettings.isVanillaDamagesEnabled()) {
            Damageable damageableMeta = (Damageable) event.getItem().getItemMeta();
            damageableMeta.setDamage(item.getType().getMaxDurability() / durabilitySettings.getValue()
                    * (int) ItemUtils.getFieldContent(item, "Durability"));
            item.setItemMeta((ItemMeta) damageableMeta);
        }

    }

}

class DurabilitySettings {

    private int value;
    private boolean vanillaDamagesEnabled;

    public DurabilitySettings(ConfigurationSection section) {
        this.value = section.getInt("value");
        this.vanillaDamagesEnabled = section.getBoolean("vanilla_damages");
        DurabilityMechanic.setDurabilitySettings(section.getParent().getParent().getName(), this);
    }

    public int getValue() {
        return value;
    }

    public boolean isVanillaDamagesEnabled() {
        return vanillaDamagesEnabled;
    }
}