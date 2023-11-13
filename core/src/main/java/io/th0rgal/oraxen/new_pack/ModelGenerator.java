package io.th0rgal.oraxen.new_pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTextures;

import java.util.stream.Collectors;

public class ModelGenerator {

    public static void generateBaseItemModels() {
        // Generate the baseItem model and add all needed overrides
        for (Material baseMaterial : OraxenItems.getItems().stream().map(ItemBuilder::getType).collect(Collectors.toSet())) {
            Key baseModelKey = PredicateGenerator.vanillaModelKey(baseMaterial);
            // Get the baseModel if it exists in the pack
            Model existingBaseModel = OraxenPlugin.get().resourcePack().model(baseModelKey);
            Model.Builder baseModel = PredicateGenerator.generateBaseModelBuilder(baseMaterial);
            if (existingBaseModel != null) mergeBaseItemModels(existingBaseModel, baseModel);
            OraxenPlugin.get().resourcePack().model(baseModel.build());
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
