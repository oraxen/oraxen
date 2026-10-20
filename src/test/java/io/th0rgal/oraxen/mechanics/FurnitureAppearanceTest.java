package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.configs.AppearanceMode;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ModelData;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureAppearance;
import io.th0rgal.oraxen.utils.CustomModelDataHelper;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FurnitureAppearanceTest extends MechanicTestSupport {

    @Test
    void scaledExternalModelsUseSeparateWrappersWithoutChangingInventoryModels() {
        ConfigurationSection config = standaloneSection("model", "custom:block/chair", "generate_model", false,
                "textures", List.of("custom:chair"), "gui_model", "custom:item/chair_icon",
                "models", Map.of("grown", "custom:block/grown"));
        FurnitureAppearance appearance = new FurnitureAppearance("chair", config, new Vector(2, 3, 4));
        try (var versions = mockStatic(VersionUtil.class);
             var plugins = mockStatic(OraxenPlugin.class);
             var modelData = mockStatic(ModelData.class);
             var builders = mockConstruction(ItemBuilder.class, (builder, context) -> {
                 doCallRealMethod().when(builder).setOraxenMeta(any());
                 doCallRealMethod().when(builder).getOraxenMeta();
             })) {
            OraxenPlugin plugin = mock(OraxenPlugin.class);
            ConfigsManager configs = mock(ConfigsManager.class);
            plugins.when(OraxenPlugin::get).thenReturn(plugin);
            when(plugin.getConfigsManager()).thenReturn(configs);
            YamlConfiguration settings = new YamlConfiguration();
            settings.set("Pack.generation.auto_generated_models_follow_texture_path", false);
            when(configs.getSettings()).thenReturn(settings);
            Settings.invalidateCache();
            Map<String, ItemBuilder> renders = appearance.getRenderItems(Material.PAPER);
            var base = renders.get("furniture/chair").getOraxenMeta();
            var stage = renders.get("furniture/chair/grown").getOraxenMeta();
            assertTrue(base.shouldGenerateModel());
            assertEquals("furniture/chair", base.getModelName());
            assertEquals("custom:block/chair", base.getParentModel());
            assertEquals("custom:item/chair_icon", base.getGuiModel());
            assertEquals(new Vector(2, 3, 4), base.getArmorStandHeadScale());
            assertFalse(base.hasLayers());
            assertTrue(stage.shouldGenerateModel());
            assertEquals("furniture/chair/grown", stage.getModelName());
            assertEquals("custom:block/grown", stage.getParentModel());
            assertFalse(stage.hasLayers());
            assertEquals("custom:block/chair", config.getString("model"));
            assertFalse(config.getBoolean("generate_model"));
            assertEquals(List.of("custom:chair"), config.getStringList("textures"));
        } finally {
            Settings.invalidateCache();
            Map<String, Integer> data = ModelData.DATAS.get(Material.PAPER);
            if (data != null) {
                data.remove("furniture/chair");
                data.remove("furniture/chair/grown");
            }
        }
    }

    @Test
    void implicitModelKeepsLegacyItemPathWhenGenerationIsDisabled() {
        FurnitureAppearance appearance = new FurnitureAppearance("chair", standaloneSection(), null);
        try (var versions = mockStatic(VersionUtil.class);
             var modelData = mockStatic(ModelData.class);
             var builders = mockConstruction(ItemBuilder.class, (builder, context) -> {
                 doCallRealMethod().when(builder).setOraxenMeta(any());
                 doCallRealMethod().when(builder).getOraxenMeta();
             })) {
            ItemBuilder render = appearance.getRenderItems(Material.PAPER).get("furniture/chair");
            assertEquals("chair", render.getOraxenMeta().getModelName());
            assertFalse(render.getOraxenMeta().shouldGenerateModel());
        } finally {
            Map<String, Integer> data = ModelData.DATAS.get(Material.PAPER);
            if (data != null) data.remove("furniture/chair");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"item_model", "strings", "floats", "legacy"})
    void assignsFurnitureIdentifiersWithoutReusingInventoryModelData(String mode) {
        ConfigurationSection config = standaloneSection("model", "inventory/chair", "generate_model", true,
                "custom_model_data", 1234, "generated_model_path", "custom:generated");
        try (var versions = mockStatic(VersionUtil.class);
             var modes = mockStatic(AppearanceMode.class);
             var modelData = mockStatic(ModelData.class);
             var builders = mockConstruction(ItemBuilder.class, (builder, context) -> {
                 doCallRealMethod().when(builder).setOraxenMeta(any());
                 doCallRealMethod().when(builder).getOraxenMeta();
             })) {
            versions.when(() -> VersionUtil.atOrAbove(anyString())).thenReturn(!mode.equals("legacy"));
            modes.when(AppearanceMode::isItemPropertiesEnabled).thenReturn(mode.equals("item_model"));
            modes.when(AppearanceMode::isModelDataIdsEnabled).thenReturn(mode.equals("strings"));
            modes.when(AppearanceMode::isModelDataFloatEnabled).thenReturn(mode.equals("floats"));
            modelData.when(() -> ModelData.generateId("furniture/chair", Material.PAPER)).thenReturn(4321);

            FurnitureAppearance appearance = new FurnitureAppearance("chair", config, new Vector(2, 3, 4));
            ItemBuilder render = appearance.getRenderItems(Material.PAPER).get("furniture/chair");

            assertEquals("furniture/chair", render.getOraxenMeta().getModelName());
            assertEquals("assets/custom/models/generated", render.getOraxenMeta().getModelPath());
            assertEquals(new Vector(2, 3, 4), render.getOraxenMeta().getArmorStandHeadScale());
            assertEquals("inventory/chair", config.getString("model"));
            assertEquals(1234, config.getInt("custom_model_data"));
            if (mode.equals("item_model"))
                verify(render).setItemModel(new NamespacedKey("oraxen", "furniture/chair"));
            if (mode.equals("strings"))
                verify(render).setCustomModelDataStrings(List.of("oraxen:furniture/chair"));
            if (mode.equals("floats")) {
                verify(render).setCustomModelDataFloats(List.of(4321.0f));
                verify(render).setCustomModelData(4321);
            }
            if (mode.equals("legacy")) verify(render).setCustomModelData(4321);
            verify(render, never()).setCustomModelData(1234);
            assertSame(render, appearance.getRenderItems(Material.PAPER).get("furniture/chair"));
        } finally {
            Map<String, Integer> data = ModelData.DATAS.get(Material.PAPER);
            if (data != null) data.remove("furniture/chair");
        }
    }

    @Test
    void stagesKeepTheirConfiguredModelAndBaseKeepsExternalModel() {
        ConfigurationSection config = standaloneSection("model", "custom:block/chair", "generate_model", false,
                "models", Map.of("grown", "custom:block/grown.json"));
        FurnitureAppearance appearance = new FurnitureAppearance("plant", config, null);
        config.set("models.grown", "changed_after_construction");
        try (var versions = mockStatic(VersionUtil.class);
             var modelData = mockStatic(ModelData.class);
             var builders = mockConstruction(ItemBuilder.class, (builder, context) -> {
                 doCallRealMethod().when(builder).setOraxenMeta(any());
                 doCallRealMethod().when(builder).getOraxenMeta();
             })) {
            modelData.when(() -> ModelData.generateId(anyString(), any())).thenReturn(5678);
            Map<String, ItemBuilder> renders = appearance.getRenderItems(Material.PAPER);

            assertTrue(appearance.hasStage("grown"));
            assertFalse(appearance.hasStage("missing"));
            assertEquals("custom:block/chair", renders.get("furniture/plant").getOraxenMeta().getModelName());
            assertEquals("custom:block/grown", renders.get("furniture/plant/grown").getOraxenMeta().getModelName());
            assertFalse(renders.get("furniture/plant/grown").getOraxenMeta().shouldGenerateModel());
            assertFalse(renders.get("furniture/plant").getOraxenMeta().hasAdditionalModels());
        } finally {
            Map<String, Integer> data = ModelData.DATAS.get(Material.PAPER);
            if (data != null) {
                data.remove("furniture/plant");
                data.remove("furniture/plant/grown");
            }
        }
    }

    @Test
    void swapsStageAndResetsBaseVisualsOnACloneWithoutReplacingOtherMetadata() {
        FurnitureAppearance appearance = new FurnitureAppearance("plant", standaloneSection("model", "plant/base",
                "models", Map.of("grown", "plant/grown")), null);
        ItemStack original = mock(ItemStack.class);
        ItemStack clone = mock(ItemStack.class);
        ItemMeta target = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(original.clone()).thenReturn(clone);
        when(original.getType()).thenReturn(Material.PAPER);
        when(clone.getItemMeta()).thenReturn(target);
        when(target.getPersistentDataContainer()).thenReturn(pdc);

        try (var versions = mockStatic(VersionUtil.class);
             var modes = mockStatic(AppearanceMode.class);
             var modelData = mockStatic(ModelData.class);
             var customData = mockStatic(CustomModelDataHelper.class);
             var builders = mockConstruction(ItemBuilder.class)) {
            versions.when(() -> VersionUtil.atOrAbove(anyString())).thenReturn(true);
            Map<String, ItemBuilder> renders = appearance.getRenderItems(Material.PAPER);
            ItemMeta base = visualMeta(renders.get("furniture/plant"), "furniture/plant");
            ItemMeta grown = visualMeta(renders.get("furniture/plant/grown"), "furniture/plant/grown");
            customData.when(() -> CustomModelDataHelper.getCustomModelData(base)).thenReturn(4001);
            customData.when(() -> CustomModelDataHelper.getCustomModelData(grown)).thenReturn(4002);
            customData.when(() -> CustomModelDataHelper.getComponentData(grown)).thenReturn(
                    new CustomModelDataHelper.ComponentData(List.of("grown"), List.of(4002f), List.of(), List.of()));

            assertSame(clone, appearance.apply(original, "grown"));
            verify(target).setItemModel(new NamespacedKey("oraxen", "furniture/plant/grown"));
            customData.verify(() -> CustomModelDataHelper.setComponentData(target, List.of("grown"), List.of(4002f)));
            assertSame(clone, appearance.apply(original, null));
            verify(target).setItemModel(new NamespacedKey("oraxen", "furniture/plant"));
            customData.verify(() -> CustomModelDataHelper.setCustomModelData(target, 4001));
            verify(clone, times(2)).setItemMeta(target);
            verifyNoInteractions(pdc);
            verify(target, never()).displayName(any());
            verify(target, never()).lore(any());
            verify(original, never()).setItemMeta(any());

            clearInvocations(clone, target);
            assertSame(clone, appearance.apply(original, "missing"));
            verify(clone, never()).setItemMeta(any());
        } finally {
            Map<String, Integer> data = ModelData.DATAS.get(Material.PAPER);
            if (data != null) {
                data.remove("furniture/plant");
                data.remove("furniture/plant/grown");
            }
        }
    }

    private static ItemMeta visualMeta(ItemBuilder builder, String model) {
        ItemStack stack = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(builder.build()).thenReturn(stack);
        when(stack.getItemMeta()).thenReturn(meta);
        when(meta.hasItemModel()).thenReturn(true);
        when(meta.getItemModel()).thenReturn(new NamespacedKey("oraxen", model));
        return meta;
    }
}
