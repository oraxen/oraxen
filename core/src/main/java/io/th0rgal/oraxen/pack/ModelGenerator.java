package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.items.OraxenTexturesMeta;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.texture.Texture;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class ModelGenerator {

    public static void generateBaseItemModels() {
        // Generate the baseItem model and add all needed overrides
        for (Material baseMaterial : OraxenItems.getItems().stream().map(ItemBuilder::getType).collect(Collectors.toCollection(LinkedHashSet::new))) {
            Key baseModelKey = PredicateGenerator.vanillaModelKey(baseMaterial);
            // Get the baseModel if it exists in the pack
            Model existingBaseModel = OraxenPlugin.get().packGenerator().resourcePack().model(baseModelKey);
            Model.Builder baseModel = PredicateGenerator.generateBaseModelBuilder(baseMaterial);
            if (existingBaseModel != null) mergeBaseItemModels(existingBaseModel, baseModel);
            OraxenPlugin.get().packGenerator().resourcePack().model(baseModel.build());
        }
    }

    private static void mergeBaseItemModels(@NotNull Model existing, Model.Builder generated) {
        existing.overrides().forEach(generated::addOverride);
        generated.textures(existing.textures());
        generated.parent(existing.parent());
    }

    public static void generateItemModels() {
        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
            OraxenMeta oraxenMeta = itemBuilder.getOraxenMeta();
            if (oraxenMeta == null || !oraxenMeta.hasPackInfos() || !oraxenMeta.shouldGenerateModel()) continue;
            OraxenTexturesMeta texturesMeta = oraxenMeta.texturesMeta();
            if (texturesMeta == null) continue;
            List<Texture> textures = texturesMeta.textures();
            if (textures != null) textures.forEach(texture -> OraxenPlugin.get().packGenerator().resourcePack().texture(texture));
            OraxenPlugin.get().packGenerator().resourcePack().model(texturesMeta.model().build());
        }
    }
//    public static Model.Builder generateModelBuilder(OraxenMeta oraxenMeta) {
//        final String parent = oraxenMeta.parentModelKey().asMinimalString();
//        ModelTextures.Builder textures = oraxenMeta.modelTextures().toBuilder();
//
//        if (oraxenMeta.modelTextures().variables().isEmpty()) {
//            final List<ModelTexture> layers = oraxenMeta.modelTextures().layers();
//            textures.layers(List.of());
//            if (parent.equals("block/cube") || parent.equals("block/cube_directional") || parent.equals("block/cube_mirrored")) {
//                textures.addVariable("particle", layers.get(2));
//                textures.addVariable("down", layers.get(0));
//                textures.addVariable("up", layers.get(1));
//                textures.addVariable("north", layers.get(2));
//                textures.addVariable("south", layers.get(3));
//                textures.addVariable("west", layers.get(4));
//                textures.addVariable("east", layers.get(5));
//            } else if (parent.equals("block/cube_all") || parent.equals("block/cube_mirrored_all")) {
//                textures.addVariable("all", layers.get(0));
//            } else if (parent.equals("block/cross")) {
//                textures.addVariable("cross", layers.get(0));
//            } else if (parent.startsWith("block/orientable")) {
//                textures.addVariable("front", layers.get(0));
//                textures.addVariable("side", layers.get(1));
//                if (!parent.endsWith("vertical"))
//                    textures.addVariable("top", layers.get(2));
//                if (parent.endsWith("with_bottom"))
//                    textures.addVariable("bottom", layers.get(3));
//            } else if (parent.startsWith("block/cube_column")) {
//                textures.addVariable("end", layers.get(0));
//                textures.addVariable("side", layers.get(1));
//            } else if (parent.equals("block/cube_bottom_top")) {
//                textures.addVariable("top", layers.get(0));
//                textures.addVariable("side", layers.get(1));
//                textures.addVariable("bottom", layers.get(2));
//            } else if (parent.equals("block/cube_top")) {
//                textures.addVariable("top", layers.get(0));
//                textures.addVariable("side", layers.get(1));
//            } else textures.layers(layers);
//        }
//
//        return Model.model()
//                .key(oraxenMeta.modelKey())
//                .parent(oraxenMeta.parentModelKey())
//                .textures(textures.build());
//    }
}
