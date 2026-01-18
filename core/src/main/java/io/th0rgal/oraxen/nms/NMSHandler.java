package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface NMSHandler {

    GlyphHandler glyphHandler();

    boolean noteblockUpdatesDisabled();

    boolean tripwireUpdatesDisabled();

    boolean chorusPlantUpdatesDisabled();

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
     * Mainly fired when placing a block against an OraxenNoteBlock due to vanilla
     * behaviour requiring Sneaking
     *
     * @param player    The player that placed the block
     * @param slot      The hand the player placed the block with
     * @param itemStack The ItemStack the player placed the block with
     * @return The corrected BlockData
     */
    @Nullable
    BlockData correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack);

    /**
     * Removes mineable/axe tag from noteblocks for custom blocks
     */
    void customBlockDefaultTools(Player player);

    default void foodComponent(ItemBuilder itemBuilder, ConfigurationSection foodSection) {

    }

    default Object consumableComponent(ItemStack itemStack) {
        return itemStack;
    }

    default ItemStack consumableComponent(ItemStack itemStack, Object consumableComponent) {
        return itemStack;
    }

    default void consumableComponent(ItemBuilder itemBuilder, ConfigurationSection consumableSection) {

    }

    default boolean supportsJukeboxPlaying() {
        return false;
    }
    default void playJukeBoxSong(Location location, ItemStack itemStack) {
    }

    default void stopJukeBox(Location location) {
    }

    // Backpack cosmetic packet methods

    /**
     * Get the next available entity ID for packet-based entities
     */
    default int getNextEntityId() {
        return -1;
    }

    /**
     * Spawn an invisible armor stand for backpack display
     */
    default void spawnBackpackArmorStand(Player viewer, int entityId, Location location, ItemStack displayItem, boolean small) {
    }

    /**
     * Send entity teleport packet
     */
    default void sendEntityTeleport(Player viewer, int entityId, Location location) {
    }

    /**
     * Send entity head rotation packet
     */
    default void sendEntityHeadRotation(Player viewer, int entityId, float yaw) {
    }

    /**
     * Send entity destroy packet
     */
    default void sendEntityDestroy(Player viewer, int... entityIds) {
    }

    /**
     * Send mount/ride packet (make entity ride another)
     */
    default void sendMountPacket(Player viewer, int vehicleId, int... passengerIds) {
    }

    /**
     * Keys that are used by vanilla Minecraft and should therefore be skipped
     * Some are accessed through API methods, others are just used internally
     */
    Set<String> vanillaKeys = Set.of("PublicBukkitValues", "display", "CustomModelData", "Damage", "AttributeModifiers",
        "Unbreakable", "CanDestroy", "slot", "count", "HideFlags", "CanPlaceOn", "Enchantments",
        "StoredEnchantments",
        "RepairCost", "CustomPotionEffects", "Potion", "CustomPotionColor", "Trim", "EntityTag",
        "pages", "filtered_pages", "filtered_title", "resolved", "generation", "author", "title",
        "BucketVariantTag", "Items", "LodestoneTracked", "LodestoneDimension", "LodestonePos",
        "ChargedProjectiles", "Charged", "DebugProperty", "Fireworks", "Explosion", "Flight",
        "map", "map_scale_direction", "map_to_lock", "Decorations", "SkullOwner", "Effects", "BlockEntityTag",
        "BlockStateTag");

    default boolean getSupported() {
        return false;
    }

    /**
     * Sets a component on an item using the DataComponents registry
     *
     * @param item         The ItemBuilder to modify
     * @param componentKey The component key (e.g. "food", "tool", etc.)
     * @param component    The component data
     * @return true if the component was successfully set
     */
    boolean setComponent(ItemBuilder item, String componentKey, Object component);

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
        public boolean chorusPlantUpdatesDisabled() {
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

        @Override
        public void foodComponent(ItemBuilder item, ConfigurationSection foodSection) {
        }

        @Override
        public void consumableComponent(ItemBuilder item, ConfigurationSection section) {
        }

        @Override
        public Object consumableComponent(ItemStack itemStack) {
            return null;
        }

        @Override
        public ItemStack consumableComponent(ItemStack itemStack, Object consumable) {
            return itemStack;
        }

        @Override
        public boolean setComponent(ItemBuilder item, String componentKey, Object component) {
            return false;
        }
    }
}
