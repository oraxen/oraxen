package io.th0rgal.oraxen.new_pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;

import java.util.Map;
import java.util.stream.Collectors;

public class ModelGenerator {

    public static void generateBaseItemModels() {
        // Generate the baseItem model and add all needed overrides
        for (Material baseMaterial : OraxenItems.getItems().stream().map(ItemBuilder::getType).collect(Collectors.toSet())) {
            Key baseModelKey = PredicateGenerator.vanillaModelKey(baseMaterial);
            Key baseTextureKey = PredicateGenerator.vanillaTextureKey(baseMaterial);
            // Get the baseModel if it exists in the pack
            Model baseModel = OraxenPlugin.get().resourcePack().model(baseModelKey);
            if (baseModel == null) baseModel = Model.model().key(baseModelKey).parent(Key.key("item/generated"))
                    .textures(ModelTextures.builder().layers(ModelTexture.ofKey(baseTextureKey)).build()).build();
            Model.Builder modelBuilder = Model.model().key(baseModel.key()).parent(baseModel.parent()).textures(baseModel.textures());
            OraxenPlugin.get().resourcePack().model(modelBuilder.overrides(PredicateGenerator.generateBaseModelOverrides(baseMaterial)).build());
        }
    }

    public static void generateItemModels() {
        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
            OraxenMeta oraxenMeta = itemBuilder.getOraxenMeta();
            if (oraxenMeta == null || !oraxenMeta.hasPackInfos() || !oraxenMeta.shouldGenerateModel()) continue;
            OraxenPlugin.get().resourcePack().model(ModelGenerator.generateModelBuilder(oraxenMeta).build());
        }
    }

    public static Model.Builder generateModelBuilder(OraxenMeta oraxenMeta) {
        return Model.model()
                .key(oraxenMeta.modelKey())
                .parent(oraxenMeta.parentModelKey())
                .textures(oraxenMeta.modelTextures());
    }
}
