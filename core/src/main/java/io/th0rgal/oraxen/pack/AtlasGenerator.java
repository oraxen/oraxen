package io.th0rgal.oraxen.pack;

import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.atlas.AtlasSource;
import team.unnamed.creative.atlas.SingleAtlasSource;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtlasGenerator {

    public static void generateAtlasFile(ResourcePack resourcePack) {
        List<AtlasSource> sources = new ArrayList<>();
        for (Model model : resourcePack.models()) {
            sources.addAll(model.textures().layers().stream().filter(t -> t.key() != null).map(t -> AtlasSource.single(t.key())).collect(Collectors.toCollection(LinkedHashSet::new)));
            sources.addAll(model.textures().variables().values().stream().filter(t -> t.key() != null).map(t -> AtlasSource.single(t.key())).collect(Collectors.toCollection(LinkedHashSet::new)));

            Optional.ofNullable(model.textures().particle()).map(ModelTexture::key)
                    .ifPresent((key) -> sources.add(AtlasSource.single(key)));
        }

        // Remove everything in the item and block folders, as vanilla already has them
        for (AtlasSource source : new ArrayList<>(sources)) {
            SingleAtlasSource singleAtlasSource = (SingleAtlasSource) source;
            String resource = singleAtlasSource.resource().asMinimalString();
            if (resource.startsWith("item/") || resource.startsWith("block/")) {
                sources.remove(singleAtlasSource);
            }
        }

        Atlas.atlas(Atlas.BLOCKS, sources).addTo(resourcePack);
    }
}
