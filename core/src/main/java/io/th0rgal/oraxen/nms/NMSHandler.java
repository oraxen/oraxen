package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.items.helpers.EmptyItemPropertyHandler;
import io.th0rgal.oraxen.items.helpers.ItemPropertyHandler;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface NMSHandler {

    default ItemPropertyHandler itemPropertyHandler() {
        return new EmptyItemPropertyHandler();
    }

    GlyphHandler glyphHandler();

    boolean noteblockUpdatesDisabled();

    boolean tripwireUpdatesDisabled();

    /**
     * Copies over all NBT-Tags from oldItem to newItem
     * Useful for plugins that might register their own NBT-Tags outside
     * the ItemStacks PersistentDataContainer
     *
     * @param oldItem The old ItemStack to copy the NBT-Tags from
     * @param newItem The new ItemStack to copy the NBT-Tags to
     * @return The new ItemStack with the copied NBT-Tags
     */
    ItemStack copyItemNBTTags(@NotNull ItemStack oldItem, @NotNull ItemStack newItem);

    /**
     * Corrects the BlockData of a placed block.
     * Mainly fired when placing a block against an OraxenNoteBlock due to vanilla behaviour requiring Sneaking
     *
     * @param player          The player that placed the block
     * @param slot            The hand the player placed the block with
     * @param itemStack       The ItemStack the player placed the block with
     * @return The corrected BlockData
     */
    @Nullable BlockData correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack);

    /**Removes mineable/axe tag from noteblocks for custom blocks */
    void customBlockDefaultTools(Player player);

    /**
     * Keys that are used by vanilla Minecraft and should therefore be skipped
     * Some are accessed through API methods, others are just used internally
     */
    Set<String> vanillaKeys = Set.of("PublicBukkitValues", "display", "CustomModelData", "Damage", "AttributeModifiers",
            "Unbreakable", "CanDestroy", "slot", "count", "HideFlags", "CanPlaceOn", "Enchantments", "StoredEnchantments",
            "RepairCost", "CustomPotionEffects", "Potion", "CustomPotionColor", "Trim", "EntityTag",
            "pages", "filtered_pages", "filtered_title", "resolved", "generation", "author", "title",
            "BucketVariantTag", "Items", "LodestoneTracked", "LodestoneDimension", "LodestonePos",
            "ChargedProjectiles", "Charged", "DebugProperty", "Fireworks", "Explosion", "Flight",
            "map", "map_scale_direction", "map_to_lock", "Decorations", "SkullOwner", "Effects", "BlockEntityTag", "BlockStateTag");

    default boolean getSupported() {
        return false;
    }

    class EmptyNMSHandler implements NMSHandler {

        @Override
        public GlyphHandler glyphHandler() {
            return new GlyphHandler.EmptyGlyphHandler();
        }

        @Override
        public boolean noteblockUpdatesDisabled() {
            return false;
        }

        @Override
        public boolean tripwireUpdatesDisabled() {
            return false;
        }

        @Override
        public ItemStack copyItemNBTTags(@NotNull ItemStack oldItem, @NotNull ItemStack newItem) {
            return newItem;
        }

        @Nullable
        @Override
        public BlockData correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
            return null;
        }

        @Override
        public void customBlockDefaultTools(Player player) {

        }
    }
}
