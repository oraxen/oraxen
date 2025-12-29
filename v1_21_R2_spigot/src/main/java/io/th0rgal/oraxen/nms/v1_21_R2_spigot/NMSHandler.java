package io.th0rgal.oraxen.nms.v1_21_R2_spigot;

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

public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    private final GlyphHandler glyphHandler;
    private static boolean warningLogged = false;

    public NMSHandler() {
        this.glyphHandler = new io.th0rgal.oraxen.nms.v1_21_R2_spigot.GlyphHandler();
        if (!warningLogged) {
            Logs.logWarning("Spigot 1.21.3 detected. Some features are limited compared to Paper.");
            Logs.logWarning("For full feature support, consider using Paper 1.21.3+");
            warningLogged = true;
        }
    }

    @Override public GlyphHandler glyphHandler() { return glyphHandler; }
    @Override public boolean tripwireUpdatesDisabled() { return false; }
    @Override public boolean noteblockUpdatesDisabled() { return false; }
    @Override public boolean chorusPlantUpdatesDisabled() { return false; }
    @Override public ItemStack copyItemNBTTags(ItemStack oldItem, ItemStack newItem) { return newItem; }
    @Override public BlockData correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) { return null; }
    @Override public void customBlockDefaultTools(Player player) {}
    @Override public boolean getSupported() { return true; }
    @Override public boolean setComponent(ItemBuilder item, String componentKey, Object component) { return false; }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void foodComponent(ItemBuilder item, ConfigurationSection foodSection) {
        try {
            FoodComponent foodComponent = new ItemStack(item.getType()).getItemMeta().getFood();
            foodComponent.setNutrition(Math.max(foodSection.getInt("nutrition"), 0));
            foodComponent.setSaturation(Math.max((float) foodSection.getDouble("saturation", 0.0), 0f));
            foodComponent.setCanAlwaysEat(foodSection.getBoolean("can_always_eat", false));
            item.setFoodComponent(foodComponent);
        } catch (Exception e) {
            Logs.logWarning("Failed to set food component on Spigot: " + e.getMessage());
        }
    }

    @Override public void consumableComponent(ItemBuilder item, ConfigurationSection section) {}
    @Override public Object consumableComponent(ItemStack itemStack) { return null; }
    @Override public ItemStack consumableComponent(ItemStack itemStack, Object consumable) { return itemStack; }
    @Override public boolean supportsJukeboxPlaying() { return false; }
    @Override public void playJukeBoxSong(Location location, ItemStack itemStack) {}
    @Override public void stopJukeBox(Location location) {}
}
