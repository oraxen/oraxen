package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeIngredientsTest {
    private MockedStatic<OraxenPlugin> plugin;

    @BeforeEach
    void initializePluginNamespace() {
        OraxenPlugin instance = mock(OraxenPlugin.class);
        when(instance.getName()).thenReturn("Oraxen");
        when(instance.namespace()).thenReturn("oraxen");
        plugin = mockStatic(OraxenPlugin.class);
        plugin.when(OraxenPlugin::get).thenReturn(instance);
    }

    @AfterEach
    void clearTags() {
        RecipeIngredients.load(null);
        plugin.close();
    }

    @Test
    void shorthandUsesIdentityWhileLegacyMaterialKeepsMaterialMatching() {
        ItemStack custom = new ItemStack(Material.PAPER, 2);
        ItemStack vanilla = new ItemStack(Material.PAPER);
        ItemBuilder builder = mock(ItemBuilder.class);
        when(builder.build()).thenReturn(custom);
        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            items.when(() -> OraxenItems.getItemById("ticket")).thenReturn(builder);
            items.when(() -> OraxenItems.getIdByItem(custom)).thenReturn("ticket");
            // Separate instances have the same material but different identity.
            RecipeChoice oraxen = RecipeIngredients.choice("oraxen:ticket");
            assertTrue(oraxen.test(custom));
            items.when(() -> OraxenItems.getIdByItem(vanilla)).thenReturn(null);
            assertFalse(oraxen.test(vanilla));
            RecipeChoice material = RecipeIngredients.choice(Map.of("minecraft_type", "PAPER"));
            assertTrue(material.test(custom));
            items.when(() -> OraxenItems.getIdByItem(custom)).thenReturn("ticket");
            assertFalse(RecipeIngredients.choice("minecraft:paper").test(custom));
        }
    }

    @Test
    void modernSelectorsWinWhenLegacySpellingsAreAlsoPresent() {
        assertEquals(Material.DIAMOND, RecipeIngredients.result(Map.of(
                "minecraft-type", "DIAMOND", "minecraft_type", "EMERALD")).getType());
        assertTrue(RecipeIngredients.choice(Map.of("minecraft-type", "DIAMOND", "minecraft_type", "EMERALD"))
                .test(new ItemStack(Material.DIAMOND)));
        assertFalse(RecipeIngredients.choice(Map.of("minecraft-type", "DIAMOND", "minecraft_type", "EMERALD"))
                .test(new ItemStack(Material.EMERALD)));
    }

    @Test
    void alternativesRetainMatchersAfterCloning() {
        RecipeChoice choice = RecipeIngredients.choice(List.of(
                Map.of("minecraft-type", "DIAMOND"), Map.of("minecraft_type", "EMERALD")));
        assertTrue(choice.clone().test(new ItemStack(Material.DIAMOND)));
        assertTrue(choice.clone().test(new ItemStack(Material.EMERALD)));
        assertFalse(choice.clone().test(new ItemStack(Material.COAL)));
    }

    @Test
    void customTagsResolveForwardReferencesAndPublishAtomically() {
        YamlConfiguration initial = new YamlConfiguration();
        initial.set("oraxen:gems", List.of("#oraxen:diamonds", Map.of("minecraft-type", "EMERALD")));
        initial.set("oraxen:diamonds", List.of(Map.of("minecraft-type", "DIAMOND")));
        Map<String, RecipeChoice> prepared = RecipeIngredients.prepare(initial);
        RecipeIngredients.publish(prepared);
        assertTrue(RecipeIngredients.choice("#oraxen:gems").test(new ItemStack(Material.DIAMOND)));
        assertTrue(RecipeIngredients.choice("#oraxen:gems").test(new ItemStack(Material.EMERALD)));
        YamlConfiguration cyclic = new YamlConfiguration();
        cyclic.set("oraxen:a", List.of("#oraxen:b"));
        cyclic.set("oraxen:b", List.of("#oraxen:a"));
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.load(cyclic));
        assertTrue(RecipeIngredients.choice("#oraxen:gems").test(new ItemStack(Material.DIAMOND)));
    }

    @Test
    void resultSupportsShorthandAndSelectorAmountsWithoutMutatingTemplates() {
        ItemStack template = new ItemStack(Material.PAPER, 1);
        ItemBuilder builder = mock(ItemBuilder.class);
        when(builder.build()).thenReturn(template);
        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            items.when(() -> OraxenItems.getItemById("ticket")).thenReturn(builder);
            assertEquals(3, RecipeIngredients.result(Map.of("item", "oraxen:ticket", "amount", 3)).getAmount());
            assertEquals(1, template.getAmount());
        }
        assertEquals(Material.DIAMOND, RecipeIngredients.result("minecraft:diamond").getType());
        assertEquals(2, RecipeIngredients.result(Map.of("minecraft-type", "DIAMOND", "amount", 2)).getAmount());
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.result(List.of("minecraft:diamond")));
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.result("#minecraft:planks"));
    }

    @Test
    void backpackMatchingRequiresIdentityButAllowsChangedContents() {
        ItemBuilder builder = mock(ItemBuilder.class);
        ItemStack emptyBackpack = mock(ItemStack.class);
        when(emptyBackpack.getType()).thenReturn(Material.LEATHER_HORSE_ARMOR);
        when(emptyBackpack.clone()).thenReturn(emptyBackpack);
        when(builder.build()).thenReturn(emptyBackpack);
        MechanicFactory factory = mock(MechanicFactory.class);
        when(factory.getMechanic("bag")).thenReturn(mock(Mechanic.class));
        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class);
             MockedStatic<MechanicsManager> mechanics = mockStatic(MechanicsManager.class);
             MockedStatic<ItemUpdater> updater = mockStatic(ItemUpdater.class)) {
            items.when(() -> OraxenItems.getItemById("bag")).thenReturn(builder);
            mechanics.when(() -> MechanicsManager.getMechanicFactory("backpack")).thenReturn(factory);
            updater.when(() -> ItemUpdater.updateItem(any(ItemStack.class))).thenAnswer(invocation -> invocation.getArgument(0));
            RecipeChoice backpack = RecipeIngredients.choice(Map.of("oraxen_item", "bag"), true);
            ItemStack changedContents = new ItemStack(Material.LEATHER_HORSE_ARMOR);
            items.when(() -> OraxenItems.getIdByItem(changedContents)).thenReturn("bag");
            assertTrue(backpack.test(changedContents));
            YamlConfiguration definitions = new YamlConfiguration();
            definitions.set("oraxen:bags", List.of(Map.of("oraxen-item", "bag")));
            definitions.set("oraxen:nested", List.of("#oraxen:bags"));
            RecipeIngredients.load(definitions);
            assertTrue(RecipeIngredients.choice("#oraxen:nested", true).clone().test(changedContents));
            assertFalse(RecipeIngredients.choice("#oraxen:nested", true).test(new ItemStack(Material.PAPER)));
            assertFalse(RecipeIngredients.choice("#oraxen:nested", false).test(changedContents));
            assertFalse(backpack.test(new ItemStack(Material.LEATHER_HORSE_ARMOR)));
            assertInstanceOf(RecipeChoice.ExactChoice.class,
                    RecipeIngredients.choice(Map.of("oraxen-item", "bag"), false));
            assertInstanceOf(RecipeIngredients.MatchingChoice.class,
                    RecipeIngredients.choice("oraxen:bag", true));
        }
    }

    @Test
    void enchantmentsRequireMinimumLevelAndBaseItem() {
        Enchantment sharpness = mock(Enchantment.class);
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(sword.getEnchantmentLevel(sharpness)).thenReturn(3);
        try (MockedStatic<Enchantment> enchantments = mockStatic(Enchantment.class)) {
            enchantments.when(() -> Enchantment.getByKey(NamespacedKey.minecraft("sharpness"))).thenReturn(sharpness);
            RecipeChoice choice = RecipeIngredients.choice(Map.of("minecraft-type", "DIAMOND_SWORD",
                    "predicates", List.of(Map.of("type", "enchantment", "enchantments", Map.of("minecraft:sharpness", 2)))));
            assertTrue(choice.test(sword));
            when(sword.getEnchantmentLevel(sharpness)).thenReturn(1);
            assertFalse(choice.test(sword));
            when(sword.getEnchantmentLevel(sharpness)).thenReturn(3);
            when(sword.getType()).thenReturn(Material.IRON_SWORD);
            assertFalse(choice.test(sword));
        }
    }

    @Test
    void typedExactDamageAcceptsSnbtAndRejectsDifferentOrUnsupportedValues() {
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        org.bukkit.inventory.meta.Damageable meta = mock(org.bukkit.inventory.meta.Damageable.class);
        when(sword.getItemMeta()).thenReturn(meta);
        when(meta.getDamage()).thenReturn(0);
        RecipeChoice choice = RecipeIngredients.choice(Map.of("minecraft-type", "DIAMOND_SWORD",
                "predicates", List.of(Map.of("type", "exact", "component", "minecraft:damage", "value", "snbt:0"))));
        assertTrue(choice.test(sword));
        when(meta.getDamage()).thenReturn(1);
        assertFalse(choice.clone().test(sword));
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.choice(Map.of("minecraft-type", "DIAMOND_SWORD",
                "predicates", List.of(Map.of("type", "exact", "component", "minecraft:damage", "value", "snbt:{bad:1}")))));
    }

    @Test
    void invalidAmountsAndUnsupportedPredicatesFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.choice(Map.of("item", "minecraft:diamond", "amount", 0)));
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.amount(Map.of("amount", 1.5)));
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.choice(Map.of(
                "minecraft-type", "DIAMOND", "predicates", Map.of("unknown", true))));
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.choice(Map.of(
                "minecraft-type", "DIAMOND", "predicates", Map.of("exact", Map.of("unknown:component", 1)))));
        assertThrows(IllegalArgumentException.class, () -> RecipeIngredients.choice(List.of()));
    }
}
