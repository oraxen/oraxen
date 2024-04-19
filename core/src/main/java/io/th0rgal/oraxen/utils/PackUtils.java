package io.th0rgal.oraxen.utils;

import org.apache.commons.lang3.StringUtils;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.font.Font;
import team.unnamed.creative.lang.Language;
import team.unnamed.creative.metadata.pack.PackMeta;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.sound.SoundEvent;
import team.unnamed.creative.sound.SoundRegistry;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;

public class PackUtils {

    public static String getFinalPackPath(File file) {
        String filePath = StringUtils.substringAfter(file.getAbsolutePath().replace("\\", "/"), "Oraxen/pack");
        return filePath.startsWith("assets") ? filePath : "assets/minecraft/" + filePath;
    }

    public static String getShortenedPackPath(File file) {
        String filePath = StringUtils.substringAfter(file.getAbsolutePath().replace("\\", "/"), "Oraxen/pack");
        return filePath;
    }

    public static void mergePack(ResourcePack resourcePack, ResourcePack importedPack) {
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
            model.addTo(resourcePack);
        });

        importedPack.fonts().forEach(font -> {
            Font baseFont = resourcePack.font(font.key());
            if (baseFont != null) font.providers().addAll(baseFont.providers());
            font.addTo(resourcePack);
        });

        importedPack.soundRegistries().forEach(soundRegistry -> {
            SoundRegistry baseRegistry = resourcePack.soundRegistry(soundRegistry.namespace());
            if (baseRegistry != null) {
                Collection<SoundEvent> mergedEvents = new LinkedHashSet<>(baseRegistry.sounds());
                mergedEvents.addAll(soundRegistry.sounds());
                SoundRegistry.soundRegistry(baseRegistry.namespace(), mergedEvents).addTo(resourcePack);
            } else soundRegistry.addTo(resourcePack);
        });

        importedPack.atlases().forEach(atlas -> {
            Atlas baseAtlas = resourcePack.atlas(atlas.key());
            if (baseAtlas != null) atlas.sources().forEach(source -> baseAtlas.toBuilder().addSource(source));
            atlas.addTo(resourcePack);
        });

        importedPack.languages().forEach(language -> {
            Language baseLanguage = resourcePack.language(language.key());
            if (baseLanguage != null) baseLanguage.translations().putAll(language.translations());
            language.addTo(resourcePack);
        });
    }
}
