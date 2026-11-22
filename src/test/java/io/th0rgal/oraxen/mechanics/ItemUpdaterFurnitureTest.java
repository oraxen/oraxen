package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.AppearanceMode;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.items.ModelData;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.CustomModelDataHelper;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemUpdaterFurnitureTest extends MechanicTestSupport {

    @Test
    void entityInventoryUpdateRestoresModernGrowthStageForFurnitureWithoutAppearanceConfig() {
        ConfigurationSection section = mechanicSection("furniture", "stages", List.of(
                Map.of("model", "seedling"),
                Map.of("model", "grown")));
        FurnitureFactory factory = mock(FurnitureFactory.class);
        FurnitureMechanic mechanic = new FurnitureMechanic(factory, section);

        ItemDisplay entity = mock(ItemDisplay.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.getOrDefault(FurnitureMechanic.STAGE_INDEX_KEY, PersistentDataType.INTEGER, 0)).thenReturn(1);

        ItemStack oldItem = item(Material.PAPER);
        ItemStack updatedItem = item(Material.PAPER);
        ItemStack renderedItem = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(entity.getItemStack()).thenReturn(oldItem);
        when(updatedItem.clone()).thenReturn(renderedItem);
        when(renderedItem.getItemMeta()).thenReturn(meta);

        try (var versions = mockStatic(VersionUtil.class);
             var furniture = mockStatic(OraxenFurniture.class);
             var updater = mockStatic(ItemUpdater.class, CALLS_REAL_METHODS)) {
            versions.when(() -> VersionUtil.atOrAbove("1.21.2")).thenReturn(true);
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(entity)).thenReturn(mechanic);
            updater.when(() -> ItemUpdater.updateItem(oldItem)).thenReturn(updatedItem);

            ItemUpdater.updateEntityInventories(entity);

            verify(meta).setItemModel(new NamespacedKey("oraxen", "test_item/grown"));
            verify(renderedItem).setItemMeta(meta);
            verify(entity).setItemStack(renderedItem);
            updater.verify(() -> ItemUpdater.updateItem(oldItem));
        }
    }

    @Test
    void entityInventoryUpdatePreservesBaseModelWhenGrowthStageHasNoModelKey() {
        ConfigurationSection section = mechanicSection("furniture", "stages", List.of(
                Map.of("model", "seedling"),
                Map.of()));
        FurnitureMechanic mechanic = new FurnitureMechanic(mock(FurnitureFactory.class), section);

        ItemDisplay entity = mock(ItemDisplay.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.getOrDefault(FurnitureMechanic.STAGE_INDEX_KEY, PersistentDataType.INTEGER, 0)).thenReturn(1);

        ItemStack oldItem = item(Material.PAPER);
        ItemStack updatedItem = item(Material.PAPER);
        ItemStack renderedItem = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        NamespacedKey baseModel = new NamespacedKey("custom", "base/model");
        AtomicReference<NamespacedKey> itemModel = new AtomicReference<>(baseModel);
        when(entity.getItemStack()).thenReturn(oldItem);
        when(updatedItem.clone()).thenReturn(renderedItem);
        when(updatedItem.getItemMeta()).thenReturn(meta);
        when(renderedItem.getItemMeta()).thenReturn(meta);
        when(meta.hasItemModel()).thenAnswer(invocation -> itemModel.get() != null);
        when(meta.getItemModel()).thenAnswer(invocation -> itemModel.get());
        doAnswer(invocation -> {
            itemModel.set(invocation.getArgument(0));
            return null;
        }).when(meta).setItemModel(any());

        try (var versions = mockStatic(VersionUtil.class);
             var furniture = mockStatic(OraxenFurniture.class);
             var updater = mockStatic(ItemUpdater.class, CALLS_REAL_METHODS)) {
            versions.when(() -> VersionUtil.atOrAbove("1.21.2")).thenReturn(true);
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(entity)).thenReturn(mechanic);
            updater.when(() -> ItemUpdater.updateItem(oldItem)).thenReturn(updatedItem);

            ItemUpdater.updateEntityInventories(entity);

            assertEquals(baseModel, itemModel.get());
            verify(entity).setItemStack(renderedItem);
        }
    }

    @Test
    void entityInventoryUpdateRestoresConfiguredGrowthStageAppearance() {
        ConfigurationSection section = mechanicSection("furniture");
        ConfigurationSection appearance = section.createSection("appearance");
        appearance.set("model", "plants/seed");
        appearance.createSection("models").set("grown", "plants/grown");
        section.set("stages", List.of(Map.of("model", "seedling"), Map.of("model", "grown")));
        FurnitureMechanic mechanic = new FurnitureMechanic(mock(FurnitureFactory.class), section);
        ItemDisplay entity = mock(ItemDisplay.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(entity.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.getOrDefault(FurnitureMechanic.STAGE_INDEX_KEY, PersistentDataType.INTEGER, 0)).thenReturn(1);

        ItemStack oldItem = item(Material.PAPER);
        ItemStack updatedItem = item(Material.PAPER);
        ItemStack mechanicResult = item(Material.PAPER);
        ItemStack appearanceResult = item(Material.PAPER);
        ItemMeta appearanceMeta = mock(ItemMeta.class);
        ItemStack renderItem = mock(ItemStack.class);
        ItemMeta renderMeta = mock(ItemMeta.class);
        when(entity.getItemStack()).thenReturn(oldItem);
        when(updatedItem.clone()).thenReturn(mechanicResult);
        when(mechanicResult.clone()).thenReturn(appearanceResult);
        when(appearanceResult.getItemMeta()).thenReturn(appearanceMeta);
        when(renderItem.getItemMeta()).thenReturn(renderMeta);
        when(renderMeta.hasItemModel()).thenReturn(true);
        when(renderMeta.getItemModel()).thenReturn(new NamespacedKey("oraxen", "furniture/test_item/grown"));

        try (var versions = mockStatic(VersionUtil.class);
             var modes = mockStatic(AppearanceMode.class);
             var modelData = mockStatic(ModelData.class);
             var customData = mockStatic(CustomModelDataHelper.class);
             var furniture = mockStatic(OraxenFurniture.class);
             var updater = mockStatic(ItemUpdater.class, CALLS_REAL_METHODS);
             var builders = mockConstruction(ItemBuilder.class, (builder, context) -> {
                 doCallRealMethod().when(builder).setOraxenMeta(any());
                 doCallRealMethod().when(builder).getOraxenMeta();
                 when(builder.build()).thenReturn(renderItem);
             })) {
            versions.when(() -> VersionUtil.atOrAbove("1.21.2")).thenReturn(true);
            modelData.when(() -> ModelData.generateId(any(), any())).thenReturn(1234);
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(entity)).thenReturn(mechanic);
            updater.when(() -> ItemUpdater.updateItem(oldItem)).thenReturn(updatedItem);

            ItemUpdater.updateEntityInventories(entity);

            verify(appearanceMeta).setItemModel(new NamespacedKey("oraxen", "furniture/test_item/grown"));
            verify(entity).setItemStack(appearanceResult);
            verify(renderItem).getItemMeta();
        }
    }

    @Test
    void entityInventoryUpdateDoesNotApplyAppearanceToNoAutoUpdateFurniture() {
        ConfigurationSection section = mechanicSection("furniture");
        section.createSection("appearance").set("model", "plants/seed");
        FurnitureMechanic mechanic = spy(new FurnitureMechanic(mock(FurnitureFactory.class), section));

        ItemDisplay entity = mock(ItemDisplay.class);
        ItemStack oldItem = item(Material.PAPER);
        ItemStack appearanceItem = item(Material.PAPER);
        when(entity.getItemStack()).thenReturn(oldItem);
        ItemMeta oldMeta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(oldItem.getItemMeta()).thenReturn(oldMeta);
        when(oldMeta.getPersistentDataContainer()).thenReturn(pdc);

        ItemBuilder builder = mock(ItemBuilder.class);
        OraxenMeta oraxenMeta = new OraxenMeta();
        oraxenMeta.setNoUpdate(true);
        when(builder.getOraxenMeta()).thenReturn(oraxenMeta);
        doReturn(appearanceItem).when(mechanic).applyAppearance(entity, oldItem);

        try (var items = mockStatic(OraxenItems.class);
             var furniture = mockStatic(OraxenFurniture.class)) {
            items.when(() -> OraxenItems.getIdByItem(oldItem)).thenReturn("test_item");
            items.when(() -> OraxenItems.getOptionalItemById("test_item")).thenReturn(Optional.of(builder));
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(entity)).thenReturn(mechanic);

            assertSame(oldItem, ItemUpdater.updateItem(oldItem));
            ItemUpdater.updateEntityInventories(entity);

            verify(entity, never()).setItemStack(any());
        }
    }

    @Test
    void entityInventoryUpdateKeepsUpdatedDisplayItemWhenEntityIsNotFurniture() {
        ItemDisplay entity = mock(ItemDisplay.class);
        ItemStack oldItem = item(Material.PAPER);
        ItemStack updatedItem = item(Material.BOOK);
        when(entity.getItemStack()).thenReturn(oldItem);

        try (var furniture = mockStatic(OraxenFurniture.class);
             var updater = mockStatic(ItemUpdater.class, CALLS_REAL_METHODS)) {
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(entity)).thenReturn(null);
            updater.when(() -> ItemUpdater.updateItem(oldItem)).thenReturn(updatedItem);

            ItemUpdater.updateEntityInventories(entity);

            verify(entity).setItemStack(updatedItem);
        }
    }

    @Test
    void entityInventoryUpdateRefreshesOrdinaryArmorStandHeadWithoutFurnitureAppearance() {
        ArmorStand armorStand = mock(ArmorStand.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack oldHead = item(Material.PAPER);
        ItemStack updatedHead = item(Material.BOOK);
        when(armorStand.getEquipment()).thenReturn(equipment);
        when(equipment.getItem(EquipmentSlot.HEAD)).thenReturn(oldHead);

        try (var furniture = mockStatic(OraxenFurniture.class);
             var updater = mockStatic(ItemUpdater.class, CALLS_REAL_METHODS)) {
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(armorStand)).thenReturn(null);
            updater.when(() -> ItemUpdater.updateItem(oldHead)).thenReturn(updatedHead);

            ItemUpdater.updateEntityInventories(armorStand);

            verify(equipment).setItem(EquipmentSlot.HEAD, updatedHead);
        }
    }

    private static ItemStack item(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1);
        return item;
    }
}
