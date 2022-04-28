package io.th0rgal.oraxen.mechanics.provided.farming.watering;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class WateringMechanic extends Mechanic {

    private final String emptyCanItem;
    private final String filledCanItem;

    public WateringMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        emptyCanItem = section.getString("emptyCanItem");
        filledCanItem = section.getString("filledCanItem");
    }

    public boolean isEmpty() { return (emptyCanItem == null && filledCanItem != null); }

    public boolean isFilled() { return (filledCanItem == null && emptyCanItem != null); }

    public String getEmptyCanItem() { return this.emptyCanItem; }

    public String getFilledCanItem() { return this.filledCanItem; }
}
