package io.th0rgal.oraxen.utils.storage;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StorageMechanic {

    public static Map<Block, StorageGui> blockStorages = new HashMap<>();
    public static Map<ItemFrame, StorageGui> frameStorages = new HashMap<>();
    public static final NamespacedKey STORAGE_KEY = new NamespacedKey(OraxenPlugin.get(), "storage");
    private final int rows;
    private final String title;
    private final boolean isLockable;
    //private final ItemStack lockItem; //TODO Implement
    private final String openSound;
    private final String lockedSound;
    private final String closeSound;
    private final float volume;
    private final float pitch;
    //private Pair<String, Object> lockItemConfig;

    public StorageMechanic(ConfigurationSection section) {
        rows = section.getInt("rows", 6);
        title = section.getString("title", "Storage");
        isLockable = section.getBoolean("is_lockable", false);
        //lockItem = section.getString("lockItem");
        openSound = section.getString("open_sound", "minecraft:block.chest.open");
        lockedSound = section.getString("locked_sound", "minecraft:block.chest.locked");
        closeSound = section.getString("close_sound", "minecraft:block.chest.close");
        volume = (float) section.getDouble("volume", 1.0);
        pitch = (float) section.getDouble("pitch", 0.8f);
    }

    public void openStorage(Block block, Player player) {
        PersistentDataContainer pdc = new CustomBlockData(block, OraxenPlugin.get());
        StorageGui storageGui =
                (blockStorages.containsKey(block) ? blockStorages.get(block) : createGui(pdc, block.getLocation()));
        Bukkit.broadcastMessage("isStorageGui: " + blockStorages.containsKey(block));
        storageGui.open(player);
        blockStorages.put(block, storageGui);
    }

    public void openStorage(ItemFrame frame, Player player) {
        PersistentDataContainer pdc = frame.getPersistentDataContainer();
        StorageGui storageGui =
                (frameStorages.containsKey(frame) ? frameStorages.get(frame) : createGui(pdc, frame.getLocation()));
        Bukkit.broadcastMessage("isStorageGui: " + frameStorages.containsKey(frame));
        storageGui.open(player);
        frameStorages.put(frame, storageGui);
    }

    private StorageGui createGui(PersistentDataContainer storagePDC, Location soundLocation) {
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        gui.setOutsideClickAction(event -> event.setCancelled(true));
        gui.setOpenGuiAction(event -> {
            if (!soundLocation.isWorldLoaded()) return;
            ItemStack[] contents = storagePDC.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY);
            if (contents != null) gui.getInventory().setContents(contents);
            if (hasOpenSound())
                Objects.requireNonNull(soundLocation.getWorld()).playSound(soundLocation, openSound, volume, pitch);
        });

        gui.setCloseGuiAction(event -> {
            if (!soundLocation.isWorldLoaded()) return;
            storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound())
                Objects.requireNonNull(soundLocation.getWorld()).playSound(soundLocation, closeSound, volume, pitch);
        });

        return gui;
    }

    public void dropStorageContent(Block block) {
        PersistentDataContainer container = new CustomBlockData(block, OraxenPlugin.get());
        ItemStack[] items = blockStorages.containsKey(block)
                ? blockStorages.get(block).getInventory().getContents()
                : container.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY);
        if (items != null) for (ItemStack item : items) {
            if (item == null) continue;
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }

        container.remove(STORAGE_KEY);
        blockStorages.remove(block);
    }

    public void dropStorageContent(ItemFrame frame) {
        PersistentDataContainer container = frame.getPersistentDataContainer();
        ItemStack[] items = frameStorages.containsKey(frame)
                ? frameStorages.get(frame).getInventory().getContents()
                : container.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY);
        if (items != null) for (ItemStack item : items)
            frame.getWorld().dropItemNaturally(frame.getLocation(), item);
        container.remove(STORAGE_KEY);
        frameStorages.remove(frame);
    }

    public int getRows() {
        return rows;
    }

    public String getTitle() {
        return title;
    }

    public boolean isLockable() {
        return isLockable;
    }

    public boolean hasOpenSound(){
        return openSound != null;
    }

    public String getOpenSound() {
        return openSound;
    }

    public boolean hasLockedSound(){
        return lockedSound != null;
    }

    public String getLockedSound() {
        return lockedSound;
    }

    public boolean hasCloseSound(){
        return closeSound != null;
    }

    public String getCloseSound() {
        return closeSound;
    }

    public float getPitch() {
        return pitch;
    }

    public float getVolume() {
        return volume;
    }

//    private ItemStack getLockItem(String type) {
//        if (type.contains("oraxen_item")) {
//            String itemId = config.get("oraxen_item").toString();
//            itemStack = OraxenItems.getItemById(itemId).build();
//        } else if (type.contains("crucible_item")) {
//            String crucibleID = type.get("crucible_item").toString();
//            itemStack = MythicCrucible.core().getItemManager().getItemStack(crucibleID);
//        } else if (type.contains("minecraft_type")) {
//            String itemType = config.get("minecraft_type").toString();
//            Material material = Material.getMaterial(itemType);
//            itemStack = new ItemStack(material);
//        } else itemStack = (ItemStack) config.get("minecraft_item");
//        return itemStack;
//    }
}
