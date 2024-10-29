package io.th0rgal.oraxen.pack;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.atlas.AtlasSource;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class AtlasGenerator {

    public static void generateAtlasFile(ResourcePack resourcePack) {
        List<AtlasSource> sources = new ArrayList<>();
        for (Model model : resourcePack.models()) {
            addKey(model.textures().layers().stream().map(ModelTexture::key), sources);
            addKey(model.textures().variables().values().stream().map(ModelTexture::key), sources);

            Optional.ofNullable(model.textures().particle()).map(ModelTexture::key)
                    .ifPresent((key) -> addKey(Stream.of(key), sources));
        }

        Atlas.atlas(Atlas.BLOCKS, sources).addTo(resourcePack);
    }

    private static void addKey(@NotNull Stream<Key> keys, List<AtlasSource> sources) {
        keys.forEach(key -> {
            if (key == null) return;
            String str = key.asMinimalString();
            if (str.startsWith("item/") || str.startsWith("block/")) return;
            sources.add(AtlasSource.single(key));
        });
    }
}
