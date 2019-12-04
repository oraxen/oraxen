package io.th0rgal.oraxen.mechanics.provided.bottledexp;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.event.Listener;

public class BottledExpMechanicListener implements Listener {

    private final MechanicFactory factory;

    public BottledExpMechanicListener(BottledExpMechanicFactory factory) {
        this.factory = factory;
    }

}
