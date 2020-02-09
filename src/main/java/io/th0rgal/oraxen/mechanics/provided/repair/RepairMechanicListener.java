package io.th0rgal.oraxen.mechanics.provided.repair;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.event.Listener;

public class RepairMechanicListener implements Listener {
    private final MechanicFactory factory;

    public RepairMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

}