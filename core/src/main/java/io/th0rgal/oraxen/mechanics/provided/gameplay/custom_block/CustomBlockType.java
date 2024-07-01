package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

public interface CustomBlockType {
    String name();

    CustomBlockType NOTEBLOCK = new DefaultBlockType("NOTEBLOCK");
    CustomBlockType STRINGBLOCK = new DefaultBlockType("STRINGBLOCK");
}


