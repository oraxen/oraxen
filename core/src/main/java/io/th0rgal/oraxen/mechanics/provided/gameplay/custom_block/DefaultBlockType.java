package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.mechanics.MechanicFactory;

public record DefaultBlockType(String name, MechanicFactory factory) implements CustomBlockType {
}
