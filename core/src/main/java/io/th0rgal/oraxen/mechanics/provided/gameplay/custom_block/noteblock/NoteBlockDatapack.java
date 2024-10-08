package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.TagKey;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.key.Key;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

public class NoteBlockDatapack {

    private static final World defaultWorld = Bukkit.getWorlds().getFirst();
    private static final Path oraxenDatapack = defaultWorld.getWorldFolder().toPath().resolve("datapacks/oraxen_custom_blocks");

    public void generateDatapack() {
        oraxenDatapack.resolve("data").toFile().mkdirs();
        writeMcMeta();
        removeFromMineableTag();

        Bukkit.getDatapackManager().getPacks().stream().filter(d -> d.getName().equals("file/oraxen_custom_blocks")).findFirst().ifPresent(d -> d.setEnabled(true));
    }

    private void writeMcMeta() {
        try {
            File packMeta = oraxenDatapack.resolve("pack.mcmeta").toFile();
            JsonObject jsonObject = new JsonObject();
            JsonObject packObject = new JsonObject();

            packObject.addProperty("description", "Datapack for Oraxen");
            packObject.addProperty("pack_format", 48);

            jsonObject.add("pack", packObject);

            FileUtils.writeStringToFile(packMeta, jsonObject.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeFromMineableTag() {
        try {
            File tagFile = oraxenDatapack.resolve("data/minecraft/tags/block/mineable/axe.json").toFile();
            tagFile.getParentFile().mkdirs();
            tagFile.createNewFile();

            String content = FileUtils.readFileToString(tagFile, StandardCharsets.UTF_8);
            JsonObject tagObject = content.isEmpty() ? new JsonObject() : JsonParser.parseString(content).getAsJsonObject();
            JsonArray tagArray = Optional.ofNullable(tagObject.getAsJsonArray("values")).orElseGet(() -> {
                JsonArray jsonArray = new JsonArray();
                if (VersionUtil.atOrAbove("1.20.5")) {
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK).getTag(TagKey.create(RegistryKey.BLOCK, Key.key("minecraft:mineable/axe")))
                            .values().forEach(t -> jsonArray.add(t.key().asString()));
                } else Tag.MINEABLE_AXE.getValues().forEach(m -> jsonArray.add(m.key().asString()));
                return jsonArray;
            });

            tagArray.remove(new JsonPrimitive("minecraft:note_block"));

            tagObject.addProperty("replace", true);
            tagObject.add("values", tagArray);

            FileUtils.writeStringToFile(tagFile, tagObject.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
