package io.th0rgal.oraxen.custom_blocks.noteblock;

import io.th0rgal.oraxen.OraxenMockPlugin;
import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

class NoteBlockMechanicTest extends OraxenMockPlugin {

    @Test
    void test_noteblock_variation() {
        URL url = Thread.currentThread().getContextClassLoader().getResource("blocks.yml");
        YamlConfiguration blocksConfig = OraxenYaml.loadConfiguration(new File(url.getPath()));
        //System.out.println(new ItemParser(blocksConfig).buildItem());
    }
}
