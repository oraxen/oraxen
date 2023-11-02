package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import team.unnamed.creative.BuiltResourcePack;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

public class PackGenerator {

    public static Path packImports = OraxenPlugin.get().packPath().resolve("imports");
    static Path assetsFolder = OraxenPlugin.get().packPath().resolve("assets");
    ResourcePack resourcePack;
    BuiltResourcePack builtPack;

    public PackGenerator() {
        packImports.toFile().mkdirs();
        assetsFolder.toFile().mkdirs();
        PackDownloader.downloadDefaultPack();
    }

    public void generatePack() {
        resourcePack = MinecraftResourcePackReader.minecraft().readFromDirectory(OraxenPlugin.get().packPath().toFile());
        OraxenPlugin.get().resourcePack(resourcePack);
        resourcePack.removeUnknownFile("token.secret");
        resourcePack.removeUnknownFile("pack.zip");
        for (Map.Entry<String, Writable> entry : new HashSet<>(resourcePack.unknownFiles().entrySet()))
            if (entry.getKey().startsWith("imports/")) resourcePack.removeUnknownFile(entry.getKey());

        MinecraftResourcePackWriter.minecraft().writeToZipFile(OraxenPlugin.get().packPath().resolve("pack.zip").toFile(), resourcePack);

        builtPack = MinecraftResourcePackWriter.minecraft().build(resourcePack);
    }

    public BuiltResourcePack getBuiltPack() {
        return builtPack;
    }

    private void addItemPackFiles() {
        for (ItemBuilder builder : OraxenItems.getItems()) {
            if (builder == null || !builder.hasOraxenMeta()) continue;
            OraxenMeta meta = builder.getOraxenMeta();
            if (meta.getCustomModelData() == 0) return;
        }
    }
}
