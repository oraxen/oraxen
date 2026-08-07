package io.th0rgal.oraxen.mechanics.provided.combat.spell.witherskull;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.combat.spell.SpellMechanic;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class WitherSkullMechanic extends SpellMechanic {

    private final TimersFactory timersFactory;
    public final boolean charged;

    public WitherSkullMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.timersFactory = new TimersFactory(section.getLong("delay"));
        this.charged = section.getBoolean("charged");
    }

    @Override
    public Timer getTimer(Player player) {
        return timersFactory.getTimer(player);
    }

}
