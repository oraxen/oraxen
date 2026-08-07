package io.th0rgal.oraxen.utils.blocksounds;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockSoundsTest {

    @Test
    void blockSoundsUseLegacyNoteblockKeyWhenBlockKeyIsMissing() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("noteblock_and_block", false);

        assertFalse(BlockSounds.isBlockSoundEnabled(config));
    }

    @Test
    void stringBlockSoundsUseLegacyStringFurnitureKeyWhenBlockKeyIsMissing() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("stringblock_and_furniture", false);

        assertFalse(BlockSounds.isStringBlockSoundEnabled(config));
    }

    @Test
    void newKeysTakePrecedenceOverLegacyKeys() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("block", true);
        config.set("furniture", true);
        config.set("noteblock_and_block", false);
        config.set("stringblock_and_furniture", false);

        assertTrue(BlockSounds.isBlockSoundEnabled(config));
        assertTrue(BlockSounds.isStringBlockSoundEnabled(config));
        assertTrue(BlockSounds.isFurnitureSoundEnabled(config));
    }
}
