package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanicFactory;
import org.jetbrains.annotations.Nullable;

public interface CustomBlockType {
    String name();
    @Nullable MechanicFactory factory();

    CustomBlockType NOTEBLOCK = new DefaultBlockType("NOTEBLOCK", NoteBlockMechanicFactory.isEnabled() ? NoteBlockMechanicFactory.get() : null);
    CustomBlockType STRINGBLOCK = new DefaultBlockType("STRINGBLOCK", StringBlockMechanicFactory.isEnabled() ? StringBlockMechanicFactory.get() : null);
}


