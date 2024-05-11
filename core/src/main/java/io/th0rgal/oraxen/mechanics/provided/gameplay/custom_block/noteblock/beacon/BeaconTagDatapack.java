package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.beacon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class BeaconTagDatapack {

    private static final World defaultWorld = Bukkit.getWorlds().get(0);
    private static final Key datapackKey = Key.key("minecraft:file/oraxen_custom_blocks");
    private static final File customBlocksDatapack = defaultWorld.getWorldFolder().toPath().resolve("datapacks/oraxen_custom_blocks").toFile();


    public static void generateDatapack() {
        customBlocksDatapack.toPath().resolve("data").toFile().mkdirs();
        writeMCMeta();
        writeTagFile();
    }

    private static void writeTagFile() {
        try {
            File tagFile = customBlocksDatapack.toPath().resolve("data/minecraft/tags/blocks/beacon_base_blocks.json").toFile();
            tagFile.getParentFile().mkdirs();
            tagFile.createNewFile();

            JsonObject tagObject = new JsonObject();
            JsonArray values = new JsonArray();

            tagObject.addProperty("replace", false);
            values.add("minecraft:note_block");
            tagObject.add("values", values);

            FileUtils.writeStringToFile(tagFile, tagObject.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeMCMeta() {
        try {
            File packMeta = customBlocksDatapack.toPath().resolve("pack.mcmeta").toFile();
            packMeta.createNewFile();

            JsonObject datapackMeta = new JsonObject();
            JsonObject data = new JsonObject();
            data.addProperty("description", "Datapack for Oraxens Custom Blocks");
            data.addProperty("pack_format", 26);
            datapackMeta.add("pack", data);
            FileUtils.writeStringToFile(packMeta, datapackMeta.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
