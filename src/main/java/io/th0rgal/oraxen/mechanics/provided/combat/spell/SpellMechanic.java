package io.th0rgal.oraxen.mechanics.provided.combat.spell;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public abstract class SpellMechanic extends Mechanic {

    public final int charges;
    private final TimersFactory timersFactory;
    public static final NamespacedKey NAMESPACED_KEY = new NamespacedKey(OraxenPlugin.get(), "charges");

    protected SpellMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section,
                // Initialize charges from config -> -1 if not set for infinite charges
                item -> {
                    int initCharges =  -1;
                    if(section.contains("charges")) {
                        initCharges = section.getInt("charges");
                    }
                    return item.setCustomTag(NAMESPACED_KEY, PersistentDataType.INTEGER, initCharges);
                },
                // Initialize Lore to display charges if charges are defined and not infinite
                item -> {
                    if(!section.contains("charges") || section.getInt("charges") == -1){
                        return item;
                    }
                    List<String> lore = new ArrayList<>();
                    if(item.getLore() != null && !item.getLore().isEmpty()){
                        lore = item.getLore();
                        lore.add(0,"");
                    }
                    lore.add(0,"Charges " + section.getInt("charges") + "/" + section.getInt("charges"));
                    return item.setLore(lore);
                });

        if(section.contains("charges")){
            this.charges = section.getInt("charges");
        } else {
            this.charges = -1;
        }
        this.timersFactory = new TimersFactory(section.getLong("delay"));
    }

    public int getMaxCharges(){ return this.charges; }

    public Timer getTimer(Player player) {
        return timersFactory.getTimer(player);
    }

    public void removeCharge(ItemStack item){
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null)
            return;

        PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();

        if (persistentDataContainer.has(NAMESPACED_KEY, PersistentDataType.INTEGER)) {
            int chargesLeft = persistentDataContainer.get(NAMESPACED_KEY, PersistentDataType.INTEGER);

            if (chargesLeft == -1) return;

            if (chargesLeft == 1) {
                item.setAmount(0);
                return;
            }

            persistentDataContainer.set(NAMESPACED_KEY, PersistentDataType.INTEGER, chargesLeft - 1);

            if(!item.getItemMeta().hasLore()){
                return;
            }
            List<String> lore = item.getItemMeta().getLore();
            lore.set(0, "Charges " + (chargesLeft - 1) + "/" + this.getMaxCharges());
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);
        }
    }
}
