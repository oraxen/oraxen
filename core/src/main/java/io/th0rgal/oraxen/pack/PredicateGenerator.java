package io.th0rgal.oraxen.pack;

import com.google.common.collect.Lists;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.base.Vector3Float;
import team.unnamed.creative.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class PredicateGenerator {

    public static Model.Builder generateBaseModelBuilder(Material material) {
        Key modelKey = PredicateGenerator.vanillaModelKey(material);
        ModelTextures modelTextures = PredicateGenerator.vanillaModelTextures(material);

        return Model.model().key(modelKey).parent(PredicateGenerator.parentModel(material))
                .textures(modelTextures)
                .guiLight(material == Material.SHIELD ? Model.GuiLight.FRONT : null)
                .display(generateBaseModelDisplay(material))
                .overrides(generateBaseModelOverrides(material));
    }

    private record ComparableItemBuilder(ItemBuilder builder) implements Comparable<ComparableItemBuilder> {
        @Override
        public int compareTo(@NotNull ComparableItemBuilder other) {
            return Integer.compare(builder.getOraxenMeta().customModelData(), other.builder.getOraxenMeta().customModelData());
        }
    }

    /**
     * Generates the base model overrides for the given material
     * This looks up all ItemBuilders using this material and generates the overrides for them
     * This includes CustomModelData, ItemPredicates like pulling, blocking, charged, cast, firework and damage
     * @param material the material to generate the overrides for
     * @return the generated overrides
     */
    private static List<ItemOverride> generateBaseModelOverrides(Material material) {
        List<ItemOverride> overrides = Lists.newArrayList();
        List<ItemBuilder> itemBuilders = OraxenItems.getItems().stream().filter(i -> i.getType() == material).map(ComparableItemBuilder::new).sorted().map(i -> i.builder).toList();
        LinkedHashSet<ItemBuilder> itemBuilders = OraxenItems.getItems().stream().filter(i -> i.getType() == material).collect(Collectors.toCollection(LinkedHashSet::new));

        switch (material) {
            case SHIELD -> overrides.add(ItemOverride.of(Key.key("item/shield_blocking"), ItemPredicate.blocking()));
            case BOW -> {
                overrides.add(ItemOverride.of(Key.key("item/bow_pulling_0"), ItemPredicate.pulling()));
                overrides.add(ItemOverride.of(Key.key("item/bow_pulling_1"), ItemPredicate.pulling(), ItemPredicate.pull(0.65f)));
                overrides.add(ItemOverride.of(Key.key("item/bow_pulling_2"), ItemPredicate.pulling(), ItemPredicate.pull(0.9f)));
            }
            case CROSSBOW -> {
                overrides.add(ItemOverride.of(Key.key("item/crossbow_pulling_0"), ItemPredicate.pulling()));
                overrides.add(ItemOverride.of(Key.key("item/crossbow_pulling_1"), ItemPredicate.pulling(), ItemPredicate.pull(0.65f)));
                overrides.add(ItemOverride.of(Key.key("item/crossbow_pulling_2"), ItemPredicate.pulling(), ItemPredicate.pull(0.9f)));

                overrides.add(ItemOverride.of(Key.key("item/crossbow_arrow"), ItemPredicate.charged()));
                overrides.add(ItemOverride.of(Key.key("item/crossbow_firework"), ItemPredicate.firework()));
            }
        }

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
        return getVanillaTextureName(material, true);
    }

    public static ModelTextures vanillaModelTextures(Material material) {
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
        } else variables.put("layer0", ModelTexture.ofKey(baseKey));

        return ModelTextures.builder().variables(variables).layers(layers).build();
    }

    public static Key getVanillaTextureName(final Material material, final boolean model) {
        if (!model)
            if (material.isBlock()) return Key.key("block/" + material.toString().toLowerCase());
            else if (material == Material.CROSSBOW) return Key.key("item/crossbow_standby");
        return Key.key("item/" + material.toString().toLowerCase());
    }

    private static final String[] tools = new String[]{"PICKAXE", "SWORD", "HOE", "AXE", "SHOVEL"};
    public static Key parentModel(final Material material) {
        if (material.isBlock())
            return Key.key("block/cube_all");
        if (Arrays.stream(tools).anyMatch(tool -> material.toString().contains(tool)))
            return Key.key("item/handheld");
        if (material == Material.FISHING_ROD)
            return Key.key("item/handheld_rod");
        if (material == Material.SHIELD)
            return Key.key("builtin/entity");
        return Key.key("item/generated");
    }

    private static Map<ItemTransform.Type, ItemTransform> generateBaseModelDisplay(Material material) {
        Map<ItemTransform.Type, ItemTransform> display = new LinkedHashMap<>();

        switch (material) {
            case SHIELD -> {
                display.put(ItemTransform.Type.THIRDPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(0, 90, 0), new Vector3Float(10, 6, 12), new Vector3Float(1,1,1)));
                display.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 90, 0), new Vector3Float(10, 6, -4), new Vector3Float(1,1,1)));
                display.put(ItemTransform.Type.FIRSTPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(0, 180, 5), new Vector3Float(10, 0, -10), new Vector3Float(1.25f,1.25f,1.25f)));
                display.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, 180, 5), new Vector3Float(-10, 2, -10), new Vector3Float(1.25f,1.25f,1.25f)));
                display.put(ItemTransform.Type.GUI, ItemTransform.transform(new Vector3Float(15, -25, -5), new Vector3Float(2, 3, 0), new Vector3Float(0.65f,0.65f,0.65f)));
                display.put(ItemTransform.Type.FIXED, ItemTransform.transform(new Vector3Float(0, 180, 0), new Vector3Float(-4.5f, 4.5f, -5), new Vector3Float(0.55f,0.55f,0.55f)));
                display.put(ItemTransform.Type.GROUND, ItemTransform.transform(new Vector3Float(0, 0, 0), new Vector3Float(2, 4, 2), new Vector3Float(0.25f,0.25f,0.25f)));
            }
            case BOW -> {
                display.put(ItemTransform.Type.THIRDPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(-80, -280, 40), new Vector3Float(-1, -2, 2.5f), new Vector3Float(0.9f, 0.9f, 0.9f)));
                display.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(-80, 260, -40), new Vector3Float(-1, -2, 2.5f), new Vector3Float(0.9f, 0.9f, 0.9f)));
                display.put(ItemTransform.Type.FIRSTPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(0, 90, -25), new Vector3Float(1.13f, 3.2f, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));
                display.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(0, -90, 25), new Vector3Float(1.13f, 3.2f, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));
            }
            case CROSSBOW -> {
                display.put(ItemTransform.Type.THIRDPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(-90, 0, 30), new Vector3Float(2, 0.1f, -3), new Vector3Float(0.9f, 0.9f, 0.9f)));
                display.put(ItemTransform.Type.THIRDPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(-90, 0, -60), new Vector3Float(2, 0.1f, -3), new Vector3Float(0.9f, 0.9f, 0.9f)));
                display.put(ItemTransform.Type.FIRSTPERSON_LEFTHAND, ItemTransform.transform(new Vector3Float(-90, 0, 35), new Vector3Float(1.13f, 3.2f, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));
                display.put(ItemTransform.Type.FIRSTPERSON_RIGHTHAND, ItemTransform.transform(new Vector3Float(-90, 0, -55), new Vector3Float(1.13f, 3.2f, 1.13f), new Vector3Float(0.68f, 0.68f, 0.68f)));
            }
        }

        return display;
    }
}
