package io.th0rgal.oraxen.nms.v1_21_R6_spigot;

import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.nms.GlyphHandler;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NMS Handler for Spigot 1.21.11 (uses Spigot mappings).
 *
 * This is a simplified implementation that provides basic functionality.
 * Some features are not available on Spigot servers:
 * - GlobalConfiguration block update settings
 * - ChannelInitializeListenerHolder (mineable tag handling)
 * - Advanced consumable component configuration
 * - Jukebox song playing via NMS
 *
 * For full feature support, use Paper 1.21.11+ with the v1_21_R6 module.
 */
public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    private final GlyphHandler glyphHandler;
    private static boolean warningLogged = false;

    public NMSHandler() {
        this.glyphHandler = new io.th0rgal.oraxen.nms.v1_21_R6_spigot.GlyphHandler();

        if (!warningLogged) {
            Logs.logWarning("Spigot 1.21.11 detected. Some features are limited compared to Paper.");
            Logs.logWarning("For full feature support, consider using Paper 1.21.11+");
            warningLogged = true;
        }
    }

    @Override
    public GlyphHandler glyphHandler() {
        return glyphHandler;
    }

    @Override
    public boolean tripwireUpdatesDisabled() {
        // Spigot doesn't have GlobalConfiguration - block updates cannot be disabled
        return false;
    }

    @Override
    public boolean noteblockUpdatesDisabled() {
        // Spigot doesn't have GlobalConfiguration - block updates cannot be disabled
        return false;
    }

    @Override
    public boolean chorusPlantUpdatesDisabled() {
        // Spigot doesn't have GlobalConfiguration - block updates cannot be disabled
        return false;
    }

    @Override
    public ItemStack copyItemNBTTags(@NotNull ItemStack oldItem, @NotNull ItemStack newItem) {
        // For Spigot, we'll use Bukkit's PersistentDataContainer which is portable
        // This is a simplified implementation
        if (oldItem.hasItemMeta() && newItem.hasItemMeta()) {
            var oldMeta = oldItem.getItemMeta();
            var newMeta = newItem.getItemMeta();
            if (oldMeta != null && newMeta != null) {
                // Copy persistent data container
                for (var key : oldMeta.getPersistentDataContainer().getKeys()) {
                    var type = oldMeta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                    if (type != null) {
                        newMeta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, type);
                    }
                }
                newItem.setItemMeta(newMeta);
            }
        }
        return newItem;
    }

    @Override
    @Nullable
    public BlockData correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
        // This requires NMS access to properly handle block placement context
        // On Spigot, we return null and let the default behavior handle it
        return null;
    }

    @Override
    public void customBlockDefaultTools(Player player) {
        // Not supported on Spigot (requires Paper's ChannelInitializeListenerHolder)
    }

    @Override
    public boolean getSupported() {
        return true;
    }

    @Override
    public boolean setComponent(ItemBuilder item, String componentKey, Object component) {
        // Component setting requires NMS access with Spigot-mapped names
        // This is a complex operation that's not fully portable
        Logs.logWarning("setComponent is not fully supported on Spigot. Use Paper for full support.");
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void foodComponent(ItemBuilder item, ConfigurationSection foodSection) {
        try {
            FoodComponent foodComponent = new ItemStack(item.getType()).getItemMeta().getFood();

            int nutrition = Math.max(foodSection.getInt("nutrition"), 0);
            foodComponent.setNutrition(nutrition);

            float saturation = Math.max((float) foodSection.getDouble("saturation", 0.0), 0f);
            foodComponent.setSaturation(saturation);

            foodComponent.setCanAlwaysEat(foodSection.getBoolean("can_always_eat", false));

            item.setFoodComponent(foodComponent);
        } catch (Exception e) {
            Logs.logWarning("Failed to set food component on Spigot: " + e.getMessage());
        }
    }

    @Override
    public void consumableComponent(ItemBuilder item, ConfigurationSection section) {
        // Consumable component requires Paper-specific NMS classes
        Logs.logWarning("Consumable components are not fully supported on Spigot servers.");
    }

    @Override
    @Nullable
    public Object consumableComponent(final ItemStack itemStack) {
        return null; // Not supported on Spigot
    }

    @Override
    public ItemStack consumableComponent(final ItemStack itemStack, @Nullable Object consumable) {
        return itemStack; // Not supported on Spigot
    }

    @Override
    public boolean supportsJukeboxPlaying() {
        return false; // Jukebox playing requires Paper's LevelEvent constants
    }

    @Override
    public void playJukeBoxSong(Location location, ItemStack itemStack) {
        // Not supported on Spigot
    }

    @Override
    public void stopJukeBox(Location location) {
        // Not supported on Spigot
    }
}
