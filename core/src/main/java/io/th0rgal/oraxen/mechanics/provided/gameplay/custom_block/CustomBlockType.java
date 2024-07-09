package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.jetbrains.annotations.Nullable;

public interface CustomBlockType {
    String name();
    @Nullable MechanicFactory factory();
}


