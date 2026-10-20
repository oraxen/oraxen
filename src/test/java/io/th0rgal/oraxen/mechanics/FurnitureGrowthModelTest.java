package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

class FurnitureGrowthModelTest extends MechanicTestSupport {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingAppearanceStageUsesLegacyStageItemOnlyOnOlderServers(boolean modern) {
        ConfigurationSection section = mechanicSection("furniture", "appearance",
                Map.of("model", "plants/seed", "generate_model", false));
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), section);
        ItemDisplay entity = mock(ItemDisplay.class);
        ItemStack currentItem = mock(ItemStack.class);
        ItemStack stageItem = mock(ItemStack.class);
        ItemStack stageClone = mock(ItemStack.class);
        ItemBuilder stageBuilder = mock(ItemBuilder.class);
        when(entity.getType()).thenReturn(EntityType.ITEM_DISPLAY);
        when(entity.getItemStack()).thenReturn(currentItem);
        when(currentItem.hasItemMeta()).thenReturn(true);
        when(stageBuilder.build()).thenReturn(stageItem);
        when(stageItem.clone()).thenReturn(stageClone);

        try (var versions = mockStatic(VersionUtil.class);
             var furniture = mockStatic(OraxenFurniture.class);
             var items = mockStatic(OraxenItems.class);
             var itemUtils = mockStatic(ItemUtils.class)) {
            versions.when(() -> VersionUtil.atOrAbove("1.21.2")).thenReturn(modern);
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic("plant")).thenReturn(mechanic);
            items.when(() -> OraxenItems.getOptionalItemById("plant_stage0"))
                    .thenReturn(Optional.of(stageBuilder));

            FurnitureMechanic.setFurnitureItemModel(entity, "plant", "stage0");

            if (modern) {
                verify(entity, never()).setItemStack(any());
                items.verifyNoInteractions();
            } else {
                verify(entity).setItemStack(stageClone);
                items.verify(() -> OraxenItems.getOptionalItemById("plant_stage0"));
            }
        }
    }
}
