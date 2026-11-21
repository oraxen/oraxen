package io.th0rgal.oraxen.nms.handler.java21;

import io.th0rgal.oraxen.recipes.loaders.RecipeLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RecipeBookDisplaysTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void resourceKeyLocationResolvesPaper12111IdentifierAccessor() {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.parse("oraxen:gold_backpack"));
        Object location = NmsAccessors.resourceKeyLocation(key);
        assertEquals(Identifier.parse("oraxen:gold_backpack"), location);
        assertEquals(new NamespacedKey("oraxen", "gold_backpack"),
                CraftNamespacedKey.fromString(location.toString()));
    }

    @Test
    void resolveResourceKeyLocationMethodHandlesBothSidesOfPaper12111Rename() throws Exception {
        Method identifierMethod = NmsAccessors.resolveResourceKeyLocationMethod(
                IdentifierResourceKey.class, Identifier.class);
        assertEquals("identifier", identifierMethod.getName());
        assertEquals(new NamespacedKey("oraxen", "gold_backpack"),
                CraftNamespacedKey.fromString(identifierMethod.invoke(new IdentifierResourceKey("oraxen:gold_backpack")).toString()));

        Method locationMethod = NmsAccessors.resolveResourceKeyLocationMethod(
                LegacyResourceKey.class, ResourceLocation.class);
        assertEquals("location", locationMethod.getName());
        assertEquals(new NamespacedKey("oraxen", "gold_backpack"),
                CraftNamespacedKey.fromString(locationMethod.invoke(new LegacyResourceKey("oraxen:gold_backpack")).toString()));
    }

    @Test
    void rewritesRecipeBookAndGhostPacketsAndClearsOnRemove() {
        NamespacedKey recipeKey = new NamespacedKey("oraxen", "gold_backpack");
        ResourceKey<Recipe<?>> nmsKey = ResourceKey.create(Registries.RECIPE, Identifier.parse("oraxen:gold_backpack"));
        RecipeDisplayId displayId = new RecipeDisplayId(7);

        SlotDisplay originalGold = new SlotDisplay.ItemSlotDisplay(Items.GOLD_INGOT);
        SlotDisplay originalPaper = new SlotDisplay.ItemSlotDisplay(Items.PAPER);
        SlotDisplay resultSlot = new SlotDisplay.ItemSlotDisplay(Items.PAPER);
        SlotDisplay stationSlot = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        ShapedCraftingRecipeDisplay originalDisplay = new ShapedCraftingRecipeDisplay(
                1, 2, List.of(originalGold, originalPaper), resultSlot, stationSlot);

        RecipeDisplayEntry entry = new RecipeDisplayEntry(
                displayId, originalDisplay, OptionalInt.empty(), new RecipeBookCategory(), Optional.empty());
        RecipeManager.ServerDisplayInfo info = new RecipeManager.ServerDisplayInfo(
                entry, new RecipeHolder<>(nmsKey, mock(Recipe.class)));

        ItemStack ironBackpack = mock(ItemStack.class);
        when(ironBackpack.getType()).thenReturn(Material.PAPER);
        net.minecraft.world.item.ItemStack nmsBackpack = new net.minecraft.world.item.ItemStack(Items.PAPER);

        MinecraftServer server = mock(MinecraftServer.class);
        RecipeManager recipes = mock(RecipeManager.class);
        when(server.getRecipeManager()).thenReturn(recipes);
        when(recipes.getRecipeFromDisplay(displayId)).thenReturn(info);

        RecipeBookDisplays displays = new RecipeBookDisplays();
        ClientboundRecipeBookAddPacket addPacket = new ClientboundRecipeBookAddPacket(
                List.of(new ClientboundRecipeBookAddPacket.Entry(entry, (byte) 0)), false);

        try (MockedStatic<MinecraftServer> minecraftServer = mockStatic(MinecraftServer.class);
             MockedStatic<RecipeLoader> recipeLoader = mockStatic(RecipeLoader.class);
             MockedStatic<CraftItemStack> craftItemStack = mockStatic(CraftItemStack.class)) {
            minecraftServer.when(MinecraftServer::getServer).thenReturn(server);
            recipeLoader.when(() -> RecipeLoader.ingredientDisplays(recipeKey))
                    .thenReturn(Map.of(1, List.of(ironBackpack)));
            craftItemStack.when(() -> CraftItemStack.asNMSCopy(ironBackpack)).thenReturn(nmsBackpack);

            ClientboundRecipeBookAddPacket rewritten = displays.add(addPacket);
            assertNotSame(addPacket, rewritten);
            ShapedCraftingRecipeDisplay rewrittenDisplay = assertInstanceOf(
                    ShapedCraftingRecipeDisplay.class, rewritten.entries().getFirst().contents().display());
            assertSame(originalGold, rewrittenDisplay.ingredients().get(0));
            SlotDisplay.ItemStackSlotDisplay replacedSlot = assertInstanceOf(
                    SlotDisplay.ItemStackSlotDisplay.class, rewrittenDisplay.ingredients().get(1));
            assertSame(nmsBackpack, replacedSlot.stack());

            ClientboundPlaceGhostRecipePacket ghost = new ClientboundPlaceGhostRecipePacket(3, originalDisplay);
            ClientboundPlaceGhostRecipePacket rewrittenGhost = displays.ghost(ghost);
            assertEquals(3, rewrittenGhost.containerId());
            assertSame(rewrittenDisplay, rewrittenGhost.recipeDisplay());

            displays.remove(new ClientboundRecipeBookRemovePacket(List.of(displayId)));
            assertSame(ghost, displays.ghost(ghost));
        }
    }

    @Test
    void buildsCompositeAlternativesAndSkipsStaleOrReplacedEntries() {
        NamespacedKey recipeKey = new NamespacedKey("oraxen", "upgrade_backpack");
        ResourceKey<Recipe<?>> nmsKey = ResourceKey.create(Registries.RECIPE, Identifier.parse("oraxen:upgrade_backpack"));
        RecipeDisplayId displayId = new RecipeDisplayId(9);

        SlotDisplay originalSlot = new SlotDisplay.ItemSlotDisplay(Items.PAPER);
        SlotDisplay resultSlot = new SlotDisplay.ItemSlotDisplay(Items.PAPER);
        SlotDisplay stationSlot = new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
        ShapedCraftingRecipeDisplay originalDisplay = new ShapedCraftingRecipeDisplay(
                1, 1, List.of(originalSlot), resultSlot, stationSlot);

        RecipeDisplayEntry entry = new RecipeDisplayEntry(
                displayId, originalDisplay, OptionalInt.empty(), new RecipeBookCategory(), Optional.empty());
        RecipeDisplayEntry staleEntry = new RecipeDisplayEntry(
                displayId, originalDisplay, OptionalInt.empty(), new RecipeBookCategory(), Optional.empty());
        RecipeManager.ServerDisplayInfo info = new RecipeManager.ServerDisplayInfo(
                entry, new RecipeHolder<>(nmsKey, mock(Recipe.class)));

        ItemStack ironBackpack = mock(ItemStack.class);
        when(ironBackpack.getType()).thenReturn(Material.PAPER);
        ItemStack leather = mock(ItemStack.class);
        when(leather.getType()).thenReturn(Material.LEATHER);
        net.minecraft.world.item.ItemStack nmsBackpack = new net.minecraft.world.item.ItemStack(Items.PAPER);
        net.minecraft.world.item.ItemStack nmsLeather = new net.minecraft.world.item.ItemStack(Items.LEATHER);

        MinecraftServer server = mock(MinecraftServer.class);
        RecipeManager recipes = mock(RecipeManager.class);
        when(server.getRecipeManager()).thenReturn(recipes);
        when(recipes.getRecipeFromDisplay(displayId)).thenReturn(info);

        RecipeBookDisplays displays = new RecipeBookDisplays();
        ClientboundRecipeBookAddPacket addPacket = new ClientboundRecipeBookAddPacket(
                List.of(new ClientboundRecipeBookAddPacket.Entry(entry, (byte) 0)), false);
        ClientboundRecipeBookAddPacket stalePacket = new ClientboundRecipeBookAddPacket(
                List.of(new ClientboundRecipeBookAddPacket.Entry(staleEntry, (byte) 0)), false);
        ClientboundRecipeBookAddPacket replacePacket = new ClientboundRecipeBookAddPacket(List.of(), true);

        try (MockedStatic<MinecraftServer> minecraftServer = mockStatic(MinecraftServer.class);
             MockedStatic<RecipeLoader> recipeLoader = mockStatic(RecipeLoader.class);
             MockedStatic<CraftItemStack> craftItemStack = mockStatic(CraftItemStack.class)) {
            minecraftServer.when(MinecraftServer::getServer).thenReturn(server);
            recipeLoader.when(() -> RecipeLoader.ingredientDisplays(recipeKey))
                    .thenReturn(Map.of(0, List.of(ironBackpack, leather)));
            craftItemStack.when(() -> CraftItemStack.asNMSCopy(ironBackpack)).thenReturn(nmsBackpack);
            craftItemStack.when(() -> CraftItemStack.asNMSCopy(leather)).thenReturn(nmsLeather);

            ClientboundRecipeBookAddPacket rewritten = displays.add(addPacket);
            ShapedCraftingRecipeDisplay rewrittenDisplay = assertInstanceOf(
                    ShapedCraftingRecipeDisplay.class, rewritten.entries().getFirst().contents().display());
            SlotDisplay.Composite composite = assertInstanceOf(
                    SlotDisplay.Composite.class, rewrittenDisplay.ingredients().getFirst());
            assertEquals(2, composite.contents().size());
            assertSame(nmsBackpack, assertInstanceOf(SlotDisplay.ItemStackSlotDisplay.class, composite.contents().get(0)).stack());
            assertSame(nmsLeather, assertInstanceOf(SlotDisplay.ItemStackSlotDisplay.class, composite.contents().get(1)).stack());

            assertSame(stalePacket, displays.add(stalePacket));
            ClientboundPlaceGhostRecipePacket ghost = new ClientboundPlaceGhostRecipePacket(1, originalDisplay);
            assertSame(ghost, displays.ghost(ghost));

            displays.add(addPacket);
            assertNotSame(ghost, displays.ghost(ghost));
            assertSame(replacePacket, displays.add(replacePacket));
            assertSame(ghost, displays.ghost(ghost));
        }
    }

    private record IdentifierResourceKey(String value) {
        public String identifier() {
            return value;
        }
    }

    private record LegacyResourceKey(String value) {
        public String location() {
            return value;
        }
    }

    private static final class ResourceLocation {
    }
}
