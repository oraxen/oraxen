package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class ModelGenerator {

    private final ResourcePack resourcePack;
    private final PredicateGenerator predicateGenerator;

    public ModelGenerator(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
        this.predicateGenerator = new PredicateGenerator(resourcePack);
    }

    public void generateBaseItemModels() {
        // Generate the baseItem model and add all needed overrides
        for (Material baseMaterial : OraxenItems.getItems().stream().map(ItemBuilder::getType).collect(Collectors.toCollection(LinkedHashSet::new))) {
            Key baseModelKey = predicateGenerator.vanillaModelKey(baseMaterial);
            Model model = resourcePack.model(baseModelKey);
            if (model == null) model = DefaultResourcePackExtractor.vanillaResourcePack.model(baseModelKey);
            //if (model == null) model = predicateGenerator.generateBaseModel(baseMaterial);

            if (model != null) model.addTo(resourcePack);
        }
    }

    public void generateItemModels() {
        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
            OraxenMeta oraxenMeta = itemBuilder.getOraxenMeta();
            if (oraxenMeta == null || !oraxenMeta.hasPackInfos() || !oraxenMeta.shouldGenerateModel()) continue;
            generateModelBuilder(oraxenMeta).addTo(resourcePack);
        }
    }

    //TODO Try and rework this to work dynamically with DefaultResourcePackExtractor#vanillaResourcePack
    private Model generateModelBuilder(OraxenMeta oraxenMeta) {
        final String parent = oraxenMeta.parentModelKey().value();
        ModelTextures.Builder textures = oraxenMeta.modelTextures().toBuilder();
        final List<ModelTexture> layers = oraxenMeta.modelTextures().layers();
        ModelTexture defaultTexture = Utils.getOrDefault(layers, 0, ModelTexture.ofKey(Key.key("")));

        if (oraxenMeta.modelTextures().variables().isEmpty()) {
            textures.layers(List.of());
            if (parent.equals("block/cube") || parent.equals("block/cube_directional") || parent.equals("block/cube_mirrored")) {
                textures.addVariable("particle", Utils.getOrDefault(layers, 2, defaultTexture));
                textures.addVariable("down", defaultTexture);
                textures.addVariable("up", Utils.getOrDefault(layers, 1, defaultTexture));
                textures.addVariable("north", Utils.getOrDefault(layers, 2, defaultTexture));
                textures.addVariable("south", Utils.getOrDefault(layers, 3, defaultTexture));
                textures.addVariable("west", Utils.getOrDefault(layers, 4, defaultTexture));
                textures.addVariable("east", Utils.getOrDefault(layers, 5, defaultTexture));
            } else if (parent.equals("block/cube_all") || parent.equals("block/cube_mirrored_all")) {
                textures.addVariable("all", defaultTexture);
            } else if (parent.equals("block/cross")) {
                textures.addVariable("cross", defaultTexture);
            } else if (parent.startsWith("block/orientable")) {
                textures.addVariable("front", defaultTexture);
                textures.addVariable("side", Utils.getOrDefault(layers, 1, defaultTexture));
                if (!parent.endsWith("vertical"))
                    textures.addVariable("top", Utils.getOrDefault(layers, 2, defaultTexture));
                if (parent.endsWith("with_bottom"))
                    textures.addVariable("bottom", Utils.getOrDefault(layers, 3, defaultTexture));
            } else if (parent.startsWith("block/cube_column")) {
                textures.addVariable("end", defaultTexture);
                textures.addVariable("side", Utils.getOrDefault(layers, 1, defaultTexture));
            } else if (parent.equals("block/cube_bottom_top") || parent.contains("block/slab") || parent.endsWith("stairs")) {
                textures.addVariable("bottom", defaultTexture);
                textures.addVariable("side", Utils.getOrDefault(layers, 1, defaultTexture));
                textures.addVariable("top", Utils.getOrDefault(layers, 2, defaultTexture));
            } else if (parent.equals("block/cube_top")) {
                textures.addVariable("top", defaultTexture);
                textures.addVariable("side", Utils.getOrDefault(layers, 1, defaultTexture));
            } else if (parent.contains("block/door_")) {
                textures.addVariable("bottom", defaultTexture);
                textures.addVariable("top", Utils.getOrDefault(layers, 1, defaultTexture));
            } else if (parent.contains("trapdoor")) {
                textures.addVariable("texture", defaultTexture);
            } else textures.layers(layers);
        }

        return Model.model()
                .key(oraxenMeta.modelKey())
                .parent(oraxenMeta.parentModelKey())
                .textures(textures.build())
                .build();
    }
}
