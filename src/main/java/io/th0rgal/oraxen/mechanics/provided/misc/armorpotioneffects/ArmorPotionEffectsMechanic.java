package io.th0rgal.oraxen.mechanics.provided.misc.armorpotioneffects;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ArmorPotionEffectsMechanic extends Mechanic {

    private final Set<PotionEffect> effects = new HashSet<>();
    private final Set<PotionEffectType> overridedTypes = new HashSet<>();
    private final Map<UUID, Set<PotionEffect>> previousPlayerEffects = new HashMap<>();

    public ArmorPotionEffectsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        for (String effectSection : section.getKeys(false))
            if (section.isConfigurationSection(effectSection))
                registersEffectFromSection(section.getConfigurationSection(effectSection));
    }

    public void registersEffectFromSection(ConfigurationSection section) {
        PotionEffectType effectType = PotionEffectType.getByName(section.getName());
        int amplifier = 0;
        boolean ambient = false;
        boolean particles = true;
        boolean icon = true;
        if (section.isInt("amplifier"))
            amplifier = section.getInt("amplifier");
        if (section.isBoolean("ambient"))
            ambient = section.getBoolean("ambient");
        if (section.isBoolean("particles"))
            particles = section.getBoolean("particles");
        if (section.isBoolean("icon"))
            icon = section.getBoolean("icon");
        PotionEffect potionEffect = new PotionEffect(effectType, Integer.MAX_VALUE, amplifier, ambient, particles,
                icon);
        effects.add(potionEffect);
        overridedTypes.add(potionEffect.getType());
    }

    public void onItemPlaced(Player player) {
        Collection<PotionEffect> activeEffects = player.getActivePotionEffects();
        Set<PotionEffect> currentConflictingEffects = new HashSet<>();
        for (PotionEffect potionEffect : activeEffects)
            // to avoid player lose their previous effects or can usebug
            if (potionEffect.getDuration() < 1728000 && !overridedTypes.contains(potionEffect.getType()))
                currentConflictingEffects.add(potionEffect);

        if (!currentConflictingEffects.isEmpty())
            previousPlayerEffects.put(player.getUniqueId(), currentConflictingEffects);

        player.addPotionEffects(effects);
    }

    public void onItemRemoved(Player player) {
        for (PotionEffectType potionEffectType : overridedTypes)
            player.removePotionEffect(potionEffectType);

        if (previousPlayerEffects.containsKey(player.getUniqueId())) {
            player.addPotionEffects(previousPlayerEffects.get(player.getUniqueId()));
            previousPlayerEffects.remove(player.getUniqueId());
        }
    }

}
