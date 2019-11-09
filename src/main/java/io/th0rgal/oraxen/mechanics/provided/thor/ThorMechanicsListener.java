package io.th0rgal.oraxen.mechanics.provided.thor;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.event.Listener;

public class ThorMechanicsListener implements Listener {

    private final MechanicFactory factory;

    public ThorMechanicsListener(MechanicFactory factory) {
        this.factory = factory;
    }

}
