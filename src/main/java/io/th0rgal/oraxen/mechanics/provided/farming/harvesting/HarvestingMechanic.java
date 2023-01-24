package io.th0rgal.oraxen.mechanics.provided.farming.harvesting;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class HarvestingMechanic extends Mechanic {

    private final int radius;
    private final int height;
    private final boolean lowerItemDurability;
    private final TimersFactory timersFactory;

    public HarvestingMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.radius = section.getInt("radius");
        this.height = section.getInt("height");
        this.lowerItemDurability = section.getBoolean("lower_item_durability", true);

        this.timersFactory = new TimersFactory(section.isInt("cooldown") ? section.getInt("cooldown") : 0);
    }

    public int getRadius() {
        return this.radius;
    }

    public int getHeight() {
        return height;
    }

    public boolean shouldLowerItemDurability() { return lowerItemDurability; }

    public Timer getTimer(Player player) {
        return timersFactory.getTimer(player);
    }
}
