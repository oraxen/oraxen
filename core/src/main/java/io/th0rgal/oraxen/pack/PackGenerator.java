package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.logs.Logs;
import team.unnamed.creative.BuiltResourcePack;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.font.Font;
import team.unnamed.creative.lang.Language;
import team.unnamed.creative.metadata.pack.PackMeta;
import team.unnamed.creative.model.ItemOverride;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;
import team.unnamed.creative.sound.SoundRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PackGenerator {

    public static Path packImports = OraxenPlugin.get().packPath().resolve("imports");
    static Path assetsFolder = OraxenPlugin.get().packPath().resolve("assets");
    ResourcePack resourcePack;
    BuiltResourcePack builtPack;

    public PackGenerator() {
        generateDefaultPaths();
        PackDownloader.downloadDefaultPack();
    }

    public void generatePack() {
        resourcePack = MinecraftResourcePackReader.minecraft().readFromDirectory(OraxenPlugin.get().packPath().toFile());
        addImportPacks();
        OraxenPlugin.get().resourcePack(resourcePack);
        resourcePack.removeUnknownFile("token.secret");
        resourcePack.removeUnknownFile("pack.zip");
        for (Map.Entry<String, Writable> entry : new HashSet<>(resourcePack.unknownFiles().entrySet()))
            if (entry.getKey().startsWith("imports/")) resourcePack.removeUnknownFile(entry.getKey());

        MinecraftResourcePackWriter.minecraft().writeToZipFile(OraxenPlugin.get().packPath().resolve("pack.zip").toFile(), resourcePack);

        builtPack = MinecraftResourcePackWriter.minecraft().build(resourcePack);
    }

    private void addImportPacks() {
        for (File file : new ArrayList<File>().toArray(packImports.toFile().listFiles())) {
            if (file == null) continue;
            if (file.isDirectory()) {
                Logs.logInfo("Importing pack " + file.getName() + "...");
                mergePack(MinecraftResourcePackReader.minecraft().readFromDirectory(file));
            }
            else if (file.getName().endsWith(".zip")) {
                Logs.logInfo("Importing pack " + file.getName() + "...");
                mergePack(MinecraftResourcePackReader.minecraft().readFromZipFile(file));
            }
            else {
                Logs.logError("Skipping unknown file " + file.getName() + " in imports folder");
                Logs.logError("File is neither a directory nor a zip file");
            }
        }
    }

    private void mergePack(ResourcePack importedPack) {
        importedPack.textures().forEach(resourcePack::texture);
        importedPack.sounds().forEach(resourcePack::sound);
        importedPack.unknownFiles().forEach(resourcePack::unknownFile);

        PackMeta packMeta = importedPack.packMeta() != null ? importedPack.packMeta() : resourcePack.packMeta();
        if (packMeta != null) resourcePack.packMeta(packMeta);
        Writable packIcon = importedPack.icon() != null ? importedPack.icon() : resourcePack.icon();
        if (packIcon != null) resourcePack.icon(packIcon);

        importedPack.models().forEach(model -> {
            Model baseModel = resourcePack.model(model.key());
            if (baseModel != null) model.overrides().addAll(baseModel.overrides());
            resourcePack.model(model);
        });

        importedPack.fonts().forEach(font -> {
            Font baseFont = resourcePack.font(font.key());
            if (baseFont != null) font.providers().addAll(baseFont.providers());
            resourcePack.font(font);
        });

        importedPack.soundRegistries().forEach(soundRegistry -> {
            SoundRegistry baseRegistry = resourcePack.soundRegistry(soundRegistry.namespace());
            if (baseRegistry != null) soundRegistry.sounds().addAll(baseRegistry.sounds());
            resourcePack.soundRegistry(soundRegistry);
        });

        importedPack.atlases().forEach(atlas -> {
            Atlas baseAtlas = resourcePack.atlas(atlas.key());
            if (baseAtlas != null) atlas.sources().forEach( source -> baseAtlas.toBuilder().addSource(source));
            resourcePack.atlas(atlas);
        });

        importedPack.languages().forEach(language -> {
            Language baseLanguage = resourcePack.language(language.key());
            if (baseLanguage != null) baseLanguage.translations().putAll(language.translations());
            resourcePack.language(language);
        });
    }

    public BuiltResourcePack builtPack() {
        return builtPack;
    }

    private void generateDefaultPaths() {
        packImports.toFile().mkdirs();
        assetsFolder.resolve("minecraft/textures").toFile().mkdirs();
        assetsFolder.resolve("minecraft/models").toFile().mkdirs();
        assetsFolder.resolve("minecraft/sounds").toFile().mkdirs();
        assetsFolder.resolve("minecraft/font").toFile().mkdirs();
        assetsFolder.resolve("minecraft/lang").toFile().mkdirs();
    }

    private void addItemPackFiles() {
        for (ItemBuilder builder : OraxenItems.getItems()) {
            if (builder == null || !builder.hasOraxenMeta()) continue;
            OraxenMeta meta = builder.getOraxenMeta();
            if (meta.getCustomModelData() == 0) return;
        }
    }
}
