package io.th0rgal.oraxen.nms.handler.java21;

import io.th0rgal.oraxen.recipes.loaders.RecipeLoader;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RecipeBookDisplays {
    // Ghost packets carry the original display instance, but no recipe ID.
    private final Map<RecipeDisplayId, Replacement> replacements = new HashMap<>();

    ClientboundRecipeBookAddPacket add(ClientboundRecipeBookAddPacket packet) {
        if (packet.replace()) replacements.clear();
        var recipes = MinecraftServer.getServer().getRecipeManager();
        List<ClientboundRecipeBookAddPacket.Entry> entries = null;
        for (int i = 0; i < packet.entries().size(); i++) {
            var entry = packet.entries().get(i);
            var contents = entry.contents();
            replacements.remove(contents.id());
            var info = recipes.getRecipeFromDisplay(contents.id());
            // Display IDs can be reused when the recipe manager is rebuilt.
            if (info == null || info.display() != contents) continue;
            var overrides = RecipeLoader.ingredientDisplays(CraftNamespacedKey.fromResourceKey(info.parent().id()));
            if (overrides.isEmpty()) continue;

            RecipeDisplay display = withIngredients(contents.display(), overrides);
            if (display == contents.display()) continue;
            replacements.put(contents.id(), new Replacement(contents.display(), display));
            var rewritten = new RecipeDisplayEntry(contents.id(), display, contents.group(),
                    contents.category(), contents.craftingRequirements());
            if (entries == null) entries = new ArrayList<>(packet.entries());
            entries.set(i, new ClientboundRecipeBookAddPacket.Entry(rewritten, entry.flags()));
        }
        return entries == null ? packet : new ClientboundRecipeBookAddPacket(entries, packet.replace());
    }

    void remove(ClientboundRecipeBookRemovePacket packet) {
        packet.recipes().forEach(replacements::remove);
    }

    ClientboundPlaceGhostRecipePacket ghost(ClientboundPlaceGhostRecipePacket packet) {
        for (Replacement replacement : replacements.values()) {
            if (replacement.original() == packet.recipeDisplay())
                return new ClientboundPlaceGhostRecipePacket(packet.containerId(), replacement.display());
        }
        return packet;
    }

    private record Replacement(RecipeDisplay original, RecipeDisplay display) {}

    private static RecipeDisplay withIngredients(RecipeDisplay display, Map<Integer, List<ItemStack>> overrides) {
        return switch (display) {
            case ShapedCraftingRecipeDisplay shaped -> new ShapedCraftingRecipeDisplay(shaped.width(), shaped.height(),
                    replaceSlots(shaped.ingredients(), overrides), shaped.result(), shaped.craftingStation());
            case ShapelessCraftingRecipeDisplay shapeless -> new ShapelessCraftingRecipeDisplay(
                    replaceSlots(shapeless.ingredients(), overrides), shapeless.result(), shapeless.craftingStation());
            case SmithingRecipeDisplay smithing -> {
                var slots = replaceSlots(List.of(smithing.template(), smithing.base(), smithing.addition()), overrides);
                yield new SmithingRecipeDisplay(slots.get(0), slots.get(1), slots.get(2),
                        smithing.result(), smithing.craftingStation());
            }
            default -> display;
        };
    }

    private static List<SlotDisplay> replaceSlots(List<SlotDisplay> original, Map<Integer, List<ItemStack>> overrides) {
        List<SlotDisplay> slots = new ArrayList<>(original);
        overrides.forEach((index, items) -> {
            if (index >= slots.size() || slots.get(index) == SlotDisplay.Empty.INSTANCE) return;
            List<SlotDisplay> alternatives = items.stream().map(RecipeBookDisplays::displayItem).toList();
            slots.set(index, alternatives.size() == 1 ? alternatives.getFirst() : new SlotDisplay.Composite(alternatives));
        });
        return slots;
    }

    private static SlotDisplay displayItem(ItemStack item) {
        SlotDisplay display = new SlotDisplay.ItemStackSlotDisplay(CraftItemStack.asNMSCopy(item));
        var remainder = item.getType().getCraftingRemainingItem();
        return remainder == null ? display : new SlotDisplay.WithRemainder(display,
                new SlotDisplay.ItemStackSlotDisplay(CraftItemStack.asNMSCopy(new ItemStack(remainder))));
    }
}
