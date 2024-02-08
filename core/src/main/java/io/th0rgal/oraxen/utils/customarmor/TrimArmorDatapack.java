package io.th0rgal.oraxen.utils.customarmor;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TrimArmorDatapack {

    private final JsonObject datapackMeta = new JsonObject();
    public TrimArmorDatapack() {
        JsonObject data = new JsonObject();
        data.addProperty("description", "Datapack for Oraxens Custom Armor trims");
        data.addProperty("pack_format", 26);
        datapackMeta.add("pack", data);
    }

    public void generateTrimDatapack() {
        File datapacksRoot = Bukkit.getWorldContainer().toPath().resolve("datapacks").toFile();
        File datapack = datapacksRoot.toPath().resolve("trims").toFile();
        datapack.toPath().resolve("data").toFile().mkdirs();
    }

    private void writeMCMeta(File datapack) {
        try {
            File packMeta = datapack.toPath().resolve("pack.mcmeta").toFile();
            packMeta.createNewFile();
            FileUtils.writeStringToFile(packMeta, datapackMeta.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
