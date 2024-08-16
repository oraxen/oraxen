package io.th0rgal.oraxen.pack;

import com.google.common.collect.Lists;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.KeyUtils;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class PredicateGenerator {

    private final ResourcePack resourcePack;

    public PredicateGenerator(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    public Key vanillaModelKey(Material material) {
        return getVanillaTextureName(material, true);
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
        List<ItemOverride> overrides = OverrideProperties.fromMaterial(material);

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
                .textures(ModelTextures.builder().layers(ModelTexture.ofKey(KeyUtils.dropPngSuffix(modelKey))).build())
                .build())
        );
    }

    private ModelTextures vanillaModelTextures(Material material) {
        Key baseKey = getVanillaTextureName(material, false);
        List<ModelTexture> layers = Lists.newArrayList();
        Map<String, ModelTexture> variables = new LinkedHashMap<>();
        ItemMeta exampleMeta = new ItemStack(material).getItemMeta();

        if (exampleMeta instanceof PotionMeta) {
            variables.put("layer0", ModelTexture.ofKey(Key.key("item/potion" + "_overlay")));
            variables.put("layer1", ModelTexture.ofKey(baseKey));
        } else if (exampleMeta instanceof LeatherArmorMeta && material != Material.LEATHER_HORSE_ARMOR) {
            variables.put("layer0", ModelTexture.ofKey(baseKey));
            variables.put("layer1", ModelTexture.ofKey(Key.key(baseKey.asString() + "_overlay")));
        } else if (material == Material.DECORATED_POT) {
            variables.put("particle", ModelTexture.ofKey(Key.key("entity/decorated_pot/decorated_pot_side")));
        } else variables.put("layer0", ModelTexture.ofKey(baseKey));

        return ModelTextures.builder().variables(variables).layers(layers).build();
    }

    private Key getVanillaTextureName(final Material material, final boolean model) {
        if (model) return Key.key("item/" + material.toString().toLowerCase());

        if (material.isBlock()) return Key.key("block/" + material.toString().toLowerCase());
        else if (material == Material.CROSSBOW) return Key.key("item/crossbow_standby");
        else if (material == Material.SPYGLASS) return Key.key("item/spyglass_in_hand");
        else if (material == Material.TRIDENT) return Key.key("item/trident_in_hand");
        return Key.key("item/" + material.toString().toLowerCase());
    }

    private Key parentModel(final Material material) {
        if (material.isBlock())
            return Key.key("block/cube_all");
        if (ItemUtils.isTool(material))
            return Key.key("item/handheld");
        if (material == Material.FISHING_ROD)
            return Key.key("item/handheld_rod");
        if (material == Material.SHIELD)
            return Key.key("builtin/entity");
        return Key.key("item/generated");
    }
}

