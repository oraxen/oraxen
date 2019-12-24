package io.th0rgal.oraxen.mechanics.provided.bigmining;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.event.Listener;

public class BigMiningMechanicsListener implements Listener {

    private final MechanicFactory factory;

    public BigMiningMechanicsListener(MechanicFactory factory) {
        this.factory = factory;
    }
}
