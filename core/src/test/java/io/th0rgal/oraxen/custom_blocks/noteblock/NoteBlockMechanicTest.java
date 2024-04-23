package io.th0rgal.oraxen.custom_blocks.noteblock;

import io.th0rgal.oraxen.OraxenMockPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NoteBlockMechanicTest extends OraxenMockPlugin {

    @Test
    void test_noteblock_variation() throws IOException {
        NoteBlockMechanic amethystMechanic;
        try (InputStream inputStream = NoteBlockMechanicTest.class.getClassLoader().getResourceAsStream("blocks.yml")) {
            YamlConfiguration blocksConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
            ConfigurationSection amethystOreSection = blocksConfig.getConfigurationSection("amethyst_ore");
            String amethystId = OraxenItems.getIdByItem(new ItemParser(amethystOreSection).buildItem());
            amethystMechanic = OraxenBlocks.getNoteBlockMechanic(amethystId);
        }

        NoteBlock amethystBlockData = (NoteBlock) Material.NOTE_BLOCK.createBlockData();
        amethystBlockData.setNote(new Note(1 % 25));

        assertNotNull(amethystMechanic, "No NoteBlockMechanic found");
        assertEquals(amethystMechanic.blockData(), amethystBlockData);
    }
}
