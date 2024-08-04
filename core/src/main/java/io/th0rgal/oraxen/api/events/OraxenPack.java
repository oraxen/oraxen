package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.PackGenerator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.pack.PackMeta;
import team.unnamed.creative.sound.SoundEvent;
import team.unnamed.creative.sound.SoundRegistry;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;

public class OraxenPack {

    public static void sendPack(Player player) {
        OraxenPlugin.get().packServer().sendPack(player);
    }

    public static ResourcePack resourcePack() {
        return OraxenPlugin.get().packGenerator().resourcePack();
    }

    public static void mergePackFromZip(@NotNull File zipFile) {
        if (!zipFile.exists()) return;
        ResourcePack zipPack = PackGenerator.reader.readFromZipFile(zipFile);
        mergePack(resourcePack(), zipPack);
    }

    public static void mergePackFromDirectory(@NotNull File directory) {
        if (!directory.exists() || !directory.isDirectory()) return;
        ResourcePack zipPack = PackGenerator.reader.readFromDirectory(directory);
        mergePack(resourcePack(), zipPack);
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
            Optional.ofNullable(resourcePack.model(model.key())).ifPresent(base -> model.overrides().addAll(base.overrides()));
            model.addTo(resourcePack);
        });

        importedPack.fonts().forEach(font -> {
            Optional.ofNullable(resourcePack.font(font.key())).ifPresent(base -> font.providers().addAll(base.providers()));
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
            Optional.ofNullable(resourcePack.language(language.key())).ifPresent(base -> language.translations().putAll(base.translations()));
            language.addTo(resourcePack);
        });
    }
}
