package io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.ThreadLocalRandom;

public class BedrockBreakMechanic extends Mechanic {

    final long delay;
    final long period;
    final int probability;

    public BedrockBreakMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic
         */
        super(mechanicFactory, section);
        this.delay = section.getLong("delay");
        this.period = section.getLong("hardness");
        this.probability = (int) (1D / section.getDouble("probability"));
    }

    public long getPeriod() {
        return period;
    }

    public boolean bernouilliTest() {
        return ThreadLocalRandom.current().nextInt(probability) == 0;
    }
}
