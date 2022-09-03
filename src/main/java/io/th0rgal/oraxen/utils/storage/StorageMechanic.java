package io.th0rgal.oraxen.utils.storage;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
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
    private final String openSound;
    private final String closeSound;
    private final float volume;
    private final float pitch;

    public StorageMechanic(ConfigurationSection section) {
        rows = section.getInt("rows", 6);
        title = section.getString("title", "Storage");
        openSound = section.getString("open_sound", "minecraft:block.chest.open");
        closeSound = section.getString("close_sound", "minecraft:block.chest.close");
        volume = (float) section.getDouble("volume", 1.0);
        pitch = (float) section.getDouble("pitch", 0.8f);
    }

    public void openStorage(Block block, Player player) {
        if (block.getType() != Material.NOTE_BLOCK) return;
        PersistentDataContainer pdc = new CustomBlockData(block, OraxenPlugin.get());
        StorageGui storageGui = (blockStorages.containsKey(block) ? blockStorages.get(block) : createGui(pdc, block.getLocation()));
        storageGui.open(player);
        blockStorages.put(block, storageGui);
    }

    public void openStorage(ItemFrame frame, Player player) {
        PersistentDataContainer pdc = frame.getPersistentDataContainer();
        StorageGui storageGui = (frameStorages.containsKey(frame) ? frameStorages.get(frame) : createGui(pdc, frame.getLocation()));
        storageGui.open(player);
        frameStorages.put(frame, storageGui);
    }

    private StorageGui createGui(PersistentDataContainer storagePDC, Location soundLocation) {
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        gui.setCloseGuiAction(event -> {
            if (!soundLocation.isWorldLoaded()) return;
            if (gui.getInventory().getViewers().size() <= 1)
                storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound())
                Objects.requireNonNull(soundLocation.getWorld()).playSound(soundLocation, closeSound, volume, pitch);
        });

        return gui;
    }

    public void dropStorageContent(Block block) {
        StorageGui gui = blockStorages.get(block);
        PersistentDataContainer pdc = new CustomBlockData(block, OraxenPlugin.get());
        ItemStack[] items = blockStorages.containsKey(block)
                ? gui.getInventory().getContents() : pdc.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY);
        if (items != null) for (ItemStack item : items) {
            if (item == null) continue;
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }
        //TODO Figure out "Concurrent Modification" Error
        gui.getInventory().getViewers().forEach(HumanEntity::closeInventory);
        pdc.remove(STORAGE_KEY);
        blockStorages.remove(block);
    }

    public void dropStorageContent(ItemFrame frame) {
        StorageGui gui = frameStorages.get(frame);
        PersistentDataContainer pdc = frame.getPersistentDataContainer();
        ItemStack[] items = frameStorages.containsKey(frame)
                ? gui.getInventory().getContents() : pdc.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY);
        if (items != null) for (ItemStack item : items) {
            if (item == null) continue;
            frame.getWorld().dropItemNaturally(frame.getLocation(), item);
        }
        //TODO Figure out "Concurrent Modification" Error
        gui.getInventory().getViewers().forEach(HumanEntity::closeInventory);
        for (HumanEntity player : gui.getInventory().getViewers()) player.closeInventory();
        pdc.remove(STORAGE_KEY);
        frameStorages.remove(frame);
    }

    public int getRows() {
        return rows;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasOpenSound(){
        return openSound != null;
    }

    public String getOpenSound() {
        return openSound;
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
}
