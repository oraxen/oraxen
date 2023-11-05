package io.th0rgal.oraxen.new_pack;

import com.google.common.collect.Lists;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import team.unnamed.creative.model.ItemOverride;
import team.unnamed.creative.model.ItemPredicate;

import java.util.List;
import java.util.Locale;

public class PredicateGenerator {

    /**
     * Generates the base model overrides for the given material
     * This looks up all ItemBuilders using this material and generates the overrides for them
     * This includes CustomModelData, ItemPredicates like pulling, blocking, charged, cast, firework and damage
     * @param material the material to generate the overrides for
     * @return the generated overrides
     */
    public static List<ItemOverride> generateBaseModelOverrides(Material material) {
        List<ItemOverride> overrides = Lists.newArrayList();
        List<ItemBuilder> itemBuilders = OraxenItems.getItems().stream().filter(i -> i.getType() == material).toList();

        for (ItemBuilder itemBuilder : itemBuilders) {
            if (itemBuilder == null) continue;
            OraxenMeta oraxenMeta = itemBuilder.getOraxenMeta();
            if (oraxenMeta == null || !oraxenMeta.hasPackInfos()) continue;
            Key itemModelKey = oraxenMeta.modelKey();
            ItemPredicate cmdPredicate = ItemPredicate.customModelData(oraxenMeta.customModelData());
            overrides.add(ItemOverride.of(itemModelKey, cmdPredicate));

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

    public static Key vanillaModelKey(Material material) {
        return Key.key(getVanillaModelName(material));
    }

    public static Key vanillaTextureKey(Material material) {
        return Key.key(getVanillaTextureName(material, false));
    }

    public static String getVanillaModelName(final Material material) {
        return getVanillaTextureName(material, true);
    }

    public static String getVanillaTextureName(final Material material, final boolean model) {
        if (!model)
            if (material.isBlock()) return "block/" + material.toString().toLowerCase(Locale.ENGLISH);
            else if (material == Material.CROSSBOW) return "item/crossbow_standby";
        return "item/" + material.toString().toLowerCase(Locale.ENGLISH);
    }
}
