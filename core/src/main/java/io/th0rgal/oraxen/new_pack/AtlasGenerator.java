package io.th0rgal.oraxen.new_pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.atlas.AtlasSource;
import team.unnamed.creative.atlas.SingleAtlasSource;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AtlasGenerator {

    public static void generateAtlasFile() {
        List<AtlasSource> sources = new ArrayList<>();
        for (Model model : OraxenPlugin.get().resourcePack().models()) {
            sources.addAll(model.textures().layers().stream().map(t -> AtlasSource.single(t.key())).toList());
            sources.addAll(model.textures().variables().values().stream().map(t -> AtlasSource.single(t.key())).toList());
            if (model.textures().particle() != null) sources.add(AtlasSource.single(model.textures().particle().key()));
        }

        // Remove everything in the item and block folders, as vanilla already has them
        sources.stream().map(SingleAtlasSource.class::cast).filter(r -> {
            String resource = r.resource().asString().replace("minecraft:", "");
            return resource.startsWith("item/") || resource.startsWith("block/");
        }).toList().forEach(sources::remove);

        OraxenPlugin.get().resourcePack().atlas(Atlas.atlas(Atlas.BLOCKS, sources));
    }
}
