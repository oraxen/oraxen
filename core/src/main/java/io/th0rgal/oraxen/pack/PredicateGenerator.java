package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.KeyUtils;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class PredicateGenerator {

    private final ResourcePack resourcePack;

    public PredicateGenerator(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    /**
     * Generates the base model overrides for the given material
     * This looks up all ItemBuilders using this material and generates the overrides for them
     * This includes CustomModelData, ItemPredicates like pulling, blocking, charged, cast, firework and damage
     * @param material the material to generate the overrides for
     * @return the generated overrides
     */
    public List<ItemOverride> generateBaseModelOverrides(Material material) {
        LinkedHashSet<ItemBuilder> itemBuilders = OraxenItems.getItems().stream().filter(i -> i.getType() == material).collect(Collectors.toCollection(LinkedHashSet::new));
        List<ItemOverride> overrides = Optional.ofNullable(DefaultResourcePackExtractor.vanillaResourcePack.model(Key.key("item/" + material.toString().toLowerCase(Locale.ENGLISH)))).map(Model::overrides).orElse(new ArrayList<>());

        for (ItemBuilder itemBuilder : itemBuilders) {
            if (itemBuilder == null) continue;
            OraxenMeta oraxenMeta = itemBuilder.getOraxenMeta();
            if (oraxenMeta == null || !oraxenMeta.hasPackInfos()) continue;
            Key itemModelKey = oraxenMeta.modelKey();
            ItemPredicate cmdPredicate = ItemPredicate.customModelData(oraxenMeta.customModelData());
            overrides.add(ItemOverride.of(itemModelKey, cmdPredicate));

            if (oraxenMeta.hasBlockingModel()) addMissingOverrideModel(oraxenMeta.blockingModel(), oraxenMeta.parentModelKey());
            if (oraxenMeta.hasChargedModel()) addMissingOverrideModel(oraxenMeta.chargedModel(), oraxenMeta.parentModelKey());
            if (oraxenMeta.hasCastModel()) addMissingOverrideModel(oraxenMeta.castModel(), oraxenMeta.parentModelKey());
            if (oraxenMeta.hasFireworkModel()) addMissingOverrideModel(oraxenMeta.fireworkModel(), oraxenMeta.parentModelKey());
            for (Key pullingKey : oraxenMeta.pullingModels()) addMissingOverrideModel(pullingKey, oraxenMeta.parentModelKey());
            for (Key damagedKey : oraxenMeta.damagedModels()) addMissingOverrideModel(damagedKey, oraxenMeta.parentModelKey());

            if (oraxenMeta.hasBlockingModel()) overrides.add(ItemOverride.of(oraxenMeta.blockingModel(), ItemPredicate.blocking(), cmdPredicate));
            if (oraxenMeta.hasChargedModel()) overrides.add(ItemOverride.of(oraxenMeta.chargedModel(), ItemPredicate.charged(), cmdPredicate));
            if (oraxenMeta.hasCastModel()) overrides.add(ItemOverride.of(oraxenMeta.castModel(), ItemPredicate.cast(), cmdPredicate));
            if (oraxenMeta.hasFireworkModel()) overrides.add(ItemOverride.of(oraxenMeta.fireworkModel(), ItemPredicate.firework(), cmdPredicate));

            List<Key> pullingModels = oraxenMeta.pullingModels();
            if (!pullingModels.isEmpty()) for (int i = 1; i <= pullingModels.size(); i++) {
                float pull = Math.min((float) i / pullingModels.size(), 0.99f);
                overrides.add(ItemOverride.of(pullingModels.get(i-1), ItemPredicate.pulling(), ItemPredicate.pull(pull), cmdPredicate));
            }

            List<Key> damagedModels = oraxenMeta.damagedModels();
            if (!damagedModels.isEmpty()) for (int i = 1; i <= damagedModels.size(); i++) {
                float damage = Math.min((float) i / damagedModels.size(), 0.99f);
                overrides.add(ItemOverride.of(damagedModels.get(i-1), ItemPredicate.damage(damage), cmdPredicate));
            }
        }

        return overrides;
    }

    private void addMissingOverrideModel(Key modelKey, Key parentKey) {
        resourcePack.model(Optional.ofNullable(resourcePack.model(modelKey)).orElse(
                Model.model().key(modelKey).parent(parentKey)
                .textures(ModelTextures.builder().layers(ModelTexture.ofKey(KeyUtils.dropExtension(modelKey))).build())
                .build())
        );
    }
}

