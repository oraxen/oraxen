package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum CustomBlockType {
    NOTEBLOCK, STRINGBLOCK;

    @Nullable
    public static CustomBlockType fromString(String type) {
        try {
            return valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String[] names() {
        return Arrays.stream(values()).map(Enum::name).toList().toArray(new String[0]);
    }
}
