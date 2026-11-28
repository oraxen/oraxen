package io.th0rgal.oraxen.mechanics.provided.combat.spell.witherskull;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.combat.spell.SpellMechanic;
import org.bukkit.configuration.ConfigurationSection;

public class WitherSkullMechanic extends SpellMechanic {

    public final boolean charged;

    public WitherSkullMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.charged = section.getBoolean("charged");
    }

}
