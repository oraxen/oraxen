package io.th0rgal.oraxen.utils.storage;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
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

import java.util.*;

public class StorageMechanic {

    public static Set<Player> playerStorages = new HashSet<>();
    public static Map<Block, StorageGui> blockStorages = new HashMap<>();
    public static Map<ItemFrame, StorageGui> frameStorages = new HashMap<>();
    public static final NamespacedKey STORAGE_KEY = new NamespacedKey(OraxenPlugin.get(), "storage");
    public static final NamespacedKey PERSONAL_STORAGE_KEY =  new NamespacedKey(OraxenPlugin.get(), "personal_storage");
    private final int rows;
    private final String title;
    private final StorageType type;
    private final String openSound;
    private final String closeSound;
    private final float volume;
    private final float pitch;

    public StorageMechanic(ConfigurationSection section) {
        rows = section.getInt("rows", 6);
        title = section.getString("title", "Storage");
        type = StorageType.valueOf(section.getString("type", "STORAGE"));
        openSound = section.getString("open_sound", "minecraft:block.chest.open");
        closeSound = section.getString("close_sound", "minecraft:block.chest.close");
        volume = (float) section.getDouble("volume", 1.0);
        pitch = (float) section.getDouble("pitch", 0.8f);
    }

    public enum StorageType {
        STORAGE, PERSONAL, ENDERCHEST
    }

    public void openPersonalStorage(Player player) {
        if (type != StorageType.PERSONAL) return;
        StorageGui storageGui = createPersonalGui(player);
        storageGui.open(player);
    }

    public void openStorage(Block block, Player player) {
        if (block.getType() != Material.NOTE_BLOCK) return;
        StorageGui storageGui = (blockStorages.containsKey(block) ? blockStorages.get(block) : createGui(block));
        storageGui.open(player);
        blockStorages.put(block, storageGui);
    }

    public void openStorage(ItemFrame frame, Player player) {
        StorageGui storageGui = (frameStorages.containsKey(frame) ? frameStorages.get(frame) : createGui(frame));
        storageGui.open(player);
        frameStorages.put(frame, storageGui);
    }

    public void dropStorageContent(Block block) {
        StorageGui gui = blockStorages.get(block);
        PersistentDataContainer pdc = new CustomBlockData(block, OraxenPlugin.get());
        // If shutdown the gui isn't saved and map is empty, so use pdc storage
        ItemStack[] items = blockStorages.containsKey(block)
                ? gui.getInventory().getContents() : pdc.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY);
        if (items != null) for (ItemStack item : items) {
            if (item == null) continue;
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }
        if (gui != null) {
            HumanEntity[] players = gui.getInventory().getViewers().toArray(new HumanEntity[0]);
            for (HumanEntity player : players) gui.close(player);
        }
        pdc.remove(STORAGE_KEY);
        blockStorages.remove(block);
    }

    public void dropStorageContent(ItemFrame frame) {
        StorageGui gui = frameStorages.get(frame);
        PersistentDataContainer pdc = frame.getPersistentDataContainer();
        // If shutdown the gui isn't saved and map is empty, so use pdc storage
        ItemStack[] items = frameStorages.containsKey(frame)
                ? gui.getInventory().getContents() : pdc.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY);
        if (items != null) for (ItemStack item : items) {
            if (item == null) continue;
            frame.getWorld().dropItemNaturally(frame.getLocation(), item);
        }
        if (gui != null) {
            HumanEntity[] players = gui.getInventory().getViewers().toArray(new HumanEntity[0]);
            for (HumanEntity player : players) gui.close(player);
        }
        pdc.remove(STORAGE_KEY);
        frameStorages.remove(frame);
    }

    public int getRows() {
        return rows;
    }

    public String getTitle() {
        return title;
    }

    public StorageType getStorageType() {
        return type;
    }

    public boolean hasOpenSound() {
        return openSound != null;
    }

    public String getOpenSound() {
        return openSound;
    }

    public boolean hasCloseSound() {
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

    private StorageGui createPersonalGui(Player player) {
        PersistentDataContainer storagePDC = player.getPersistentDataContainer();
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                    storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                }, 3L);
            }
        });

        gui.setOpenGuiAction(event -> {
            playerStorages.add(player);
            if (storagePDC.has(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                gui.getInventory().setContents(Objects.requireNonNull(storagePDC.get(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY)));
        });

        gui.setCloseGuiAction(event -> {
            playerStorages.remove(player);
            storagePDC.set(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound())
                Objects.requireNonNull(player.getLocation().getWorld()).playSound(player.getLocation(), closeSound, volume, pitch);
        });

        return gui;
    }

    private StorageGui createGui(Block block) {
        Location location = block.getLocation();
        PersistentDataContainer storagePDC = new CustomBlockData(block, OraxenPlugin.get());
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                    storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                }, 3L);
            }
        });
        gui.setOpenGuiAction(event -> {
            if (storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                gui.getInventory().setContents(Objects.requireNonNull(storagePDC.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY)));
        });

        gui.setCloseGuiAction(event -> {
            storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound() && location.isWorldLoaded() && block.getWorld().isChunkLoaded(block.getChunk()))
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
        });

        return gui;
    }

    private StorageGui createGui(ItemFrame frame) {
        Location location = frame.getLocation();
        PersistentDataContainer storagePDC = frame.getPersistentDataContainer();
        StorageGui gui = Gui.storage().title(Utils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                    storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                }, 3L);
            }
        });
        gui.setOpenGuiAction(event -> {
            if (storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                gui.getInventory().setContents(Objects.requireNonNull(storagePDC.get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY)));
        });

        gui.setCloseGuiAction(event -> {
            if (gui.getInventory().getViewers().size() <= 1)
                storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound() && location.isWorldLoaded() && frame.getWorld().isChunkLoaded(frame.getLocation().getChunk()))
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
        });

        return gui;
    }

    // Closes any open storage gui on plugin/server shutdown to force save content to pdc
    public static void forceCloseStorages() {
        blockStorages.values().forEach(gui -> {
            HumanEntity[] players = gui.getInventory().getViewers().toArray(HumanEntity[]::new);
            for (HumanEntity player : players) player.closeInventory();
        });

        frameStorages.values().forEach(gui -> {
            HumanEntity[] players = gui.getInventory().getViewers().toArray(HumanEntity[]::new);
            for (HumanEntity player : players) player.closeInventory();
        });

        playerStorages.forEach(Player::closeInventory);
    }
}
