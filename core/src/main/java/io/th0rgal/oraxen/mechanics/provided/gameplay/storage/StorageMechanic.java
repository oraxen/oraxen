package io.th0rgal.oraxen.mechanics.provided.gameplay.storage;

import com.jeff_media.morepersistentdatatypes.DataType;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StorageMechanic {

    // Use thread-safe collections for Folia compatibility (concurrent region thread access)
    public static Set<Player> playerStorages = ConcurrentHashMap.newKeySet();
    public static Map<Block, StorageGui> blockStorages = new ConcurrentHashMap<>();
    public static Map<Entity, StorageGui> frameStorages = new ConcurrentHashMap<>();
    public static final NamespacedKey STORAGE_KEY = new NamespacedKey(OraxenPlugin.get(), "storage");
    public static final NamespacedKey PERSONAL_STORAGE_KEY = new NamespacedKey(OraxenPlugin.get(), "personal_storage");
    private final int rows;
    private final String title;
    private final StorageType type;
    private final String openSound;
    private final String closeSound;
    private final String openAnimation;
    private final String closeAnimation;
    private final float volume;
    private final float pitch;

    public StorageMechanic(ConfigurationSection section) {
        rows = section.getInt("rows", 6);
        title = section.getString("title", "Storage");
        type = StorageType.valueOf(section.getString("type", "STORAGE"));
        openSound = section.getString("open_sound", "minecraft:block.chest.open");
        closeSound = section.getString("close_sound", "minecraft:block.chest.close");
        openAnimation = section.getString("open_animation", null);
        closeAnimation = section.getString("close_animation", null);
        volume = (float) section.getDouble("volume", 0.5);
        pitch = (float) section.getDouble("pitch", 0.95f);
    }

    public enum StorageType {
        STORAGE, PERSONAL, ENDERCHEST, DISPOSAL, SHULKER
    }

    public void openPersonalStorage(Player player, Location location, @Nullable Entity baseEntity) {
        if (type != StorageType.PERSONAL) return;
        StorageGui storageGui = createPersonalGui(player, baseEntity);
        storageGui.open(player);
        if (baseEntity != null)
            playOpenAnimation(baseEntity, openAnimation);
        if (hasOpenSound() && location.isWorldLoaded())
            Objects.requireNonNull(location.getWorld()).playSound(location, openSound, volume, pitch);
    }

    public void openDisposal(Player player, Location location, @Nullable Entity baseEntity) {
        if (type != StorageType.DISPOSAL) return;
        StorageGui storageGui = createDisposalGui(location, baseEntity);
        storageGui.open(player);
        if (baseEntity != null)
            playOpenAnimation(baseEntity, openAnimation);
        if (hasOpenSound() && location.isWorldLoaded())
            Objects.requireNonNull(location.getWorld()).playSound(location, openSound, volume, pitch);
    }

    public void openStorage(Block block, Player player) {
        Material blockType = block.getType();
        if (blockType != Material.NOTE_BLOCK && blockType != Material.TRIPWIRE && blockType != Material.CHORUS_PLANT) return;
        StorageGui storageGui = blockStorages.computeIfAbsent(block, b -> createGui(b, null));
        if (storageGui == null) return;
        storageGui.open(player);
        if (hasOpenSound() && block.getLocation().isWorldLoaded())
            Objects.requireNonNull(block.getWorld()).playSound(block.getLocation(), openSound, volume, pitch);
    }

    public void openStorage(Entity baseEntity, Player player) {
        StorageGui storageGui = frameStorages.computeIfAbsent(baseEntity, this::createGui);
        if (storageGui == null) return;
        storageGui.open(player);
        playOpenAnimation(baseEntity, openAnimation);
        if (hasOpenSound() && baseEntity.getLocation().isWorldLoaded())
            Objects.requireNonNull(baseEntity.getWorld()).playSound(baseEntity.getLocation(), openSound, volume, pitch);
    }

    private void playOpenAnimation(Entity baseEntity, String animation) {
        if (baseEntity == null || animation == null) return;
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        if (pdc.has(FurnitureMechanic.MODELENGINE_KEY, DataType.UUID)) {
            UUID uuid = pdc.get(FurnitureMechanic.MODELENGINE_KEY, DataType.UUID);
            if (uuid != null) {
                ModeledEntity modelEntity = ModelEngineAPI.getModeledEntity(uuid);
                if (modelEntity != null) {
                    for (ActiveModel model : modelEntity.getModels().values()) {
                        model.getAnimationHandler().forceStopAllAnimations();
                        model.getAnimationHandler().playAnimation(animation, 0, 0, 1, true);
                    }
                }
            }
        }
    }

    public void dropStorageContent(Block block) {
        StorageGui gui = blockStorages.get(block);
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        // If shutdown the gui isn't saved and map is empty, so use pdc storage
        ItemStack[] items = (blockStorages.containsKey(block) && gui != null)
                ? gui.getInventory().getContents() : pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});

        if (isShulker()) {
            NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
            if (mechanic == null) return;

            ItemStack shulker = OraxenItems.getItemById(mechanic.getItemID()).build();
            ItemMeta shulkerMeta = shulker.getItemMeta();

            if (shulkerMeta != null)
                shulkerMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);

            shulker.setItemMeta(shulkerMeta);
            block.getWorld().dropItemNaturally(block.getLocation(), shulker);
        } else for (ItemStack item : items) {
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

    public void dropStorageContent(FurnitureMechanic mechanic, Entity baseEntity) {
        StorageGui gui = frameStorages.get(baseEntity);
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        // If shutdown the gui isn't saved and map is empty, so use pdc storage
        ItemStack[] items = (frameStorages.containsKey(baseEntity) && gui != null)
                ? gui.getInventory().getContents() : pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
        if (isShulker()) {
            ItemStack defaultItem = OraxenItems.getItemById(mechanic.getItemID()).build();
            ItemStack shulker = FurnitureMechanic.getFurnitureItem(baseEntity);
            ItemMeta shulkerMeta = shulker.getItemMeta();

            if (shulkerMeta != null) {
                shulkerMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
                shulkerMeta.setDisplayName(defaultItem.getItemMeta() != null ? defaultItem.getItemMeta().getDisplayName() : null);
                shulker.setItemMeta(shulkerMeta);
            }
            baseEntity.getWorld().dropItemNaturally(baseEntity.getLocation(), shulker);
        } else for (ItemStack item : items) {
            if (item == null) continue;
            baseEntity.getWorld().dropItemNaturally(baseEntity.getLocation(), item);
        }

        if (gui != null) {
            HumanEntity[] players = gui.getInventory().getViewers().toArray(new HumanEntity[0]);
            for (HumanEntity player : players) gui.close(player);
        }
        pdc.remove(STORAGE_KEY);
        frameStorages.remove(baseEntity);
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

    public boolean isStorage() {
        return type == StorageType.STORAGE;
    }

    public boolean isPersonal() {
        return type == StorageType.PERSONAL;
    }

    public boolean isEnderchest() {
        return type == StorageType.ENDERCHEST;
    }

    public boolean isDisposal() {
        return type == StorageType.DISPOSAL;
    }

    public boolean isShulker() {
        return type == StorageType.SHULKER;
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

    private StorageGui createDisposalGui(Location location, @Nullable Entity baseEntity) {
        StorageGui gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        gui.setOpenGuiAction(event -> {
            gui.getInventory().clear();
        });

        gui.setCloseGuiAction(event -> {
            gui.getInventory().clear();
            if (hasCloseSound() && location.isWorldLoaded())
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
            if (baseEntity != null) playOpenAnimation(baseEntity, closeAnimation);
        });
        return gui;
    }

    private StorageGui createPersonalGui(Player player, @Nullable Entity baseEntity) {
        PersistentDataContainer storagePDC = player.getPersistentDataContainer();
        StorageGui gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                SchedulerUtil.runForEntityLater(player, 3L, () ->
                        storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents()), () -> {});
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
            if (hasCloseSound() && player.getLocation().isWorldLoaded())
                Objects.requireNonNull(player.getLocation().getWorld()).playSound(player.getLocation(), closeSound, volume, pitch);
            if (baseEntity != null) playOpenAnimation(baseEntity, closeAnimation);
        });

        return gui;
    }

    private StorageGui createGui(Block block, @Nullable ItemFrame frame) {
        Location location = block.getLocation();
        PersistentDataContainer storagePDC = BlockHelpers.getPDC(block);
        StorageGui gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                SchedulerUtil.runAtLocationLater(location, 3L, () ->
                        storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents()));
            }
        });
        gui.setOpenGuiAction(event -> {
            if (storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                gui.getInventory().setContents(storagePDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{}));
        });

        gui.setCloseGuiAction(event -> {
            storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound() && BlockHelpers.isLoaded(block.getLocation()))
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
            if (frame != null) playOpenAnimation(frame, closeAnimation);
        });

        return gui;
    }

    private StorageGui createGui(Entity baseEntity) {
        Location location = baseEntity.getLocation();
        ItemStack furnitureItem = FurnitureMechanic.getFurnitureItem(baseEntity);
        if (furnitureItem == null) return null;
        PersistentDataContainer storagePDC = baseEntity.getPersistentDataContainer();
        PersistentDataContainer itemPDC = furnitureItem.getItemMeta().getPersistentDataContainer();
        boolean shulker = isShulker();
        PersistentDataContainer shulkerPDC = shulker ? itemPDC : null;
        StorageGui gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(title)).rows(rows).create();

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction(event -> {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR || event.getCurrentItem() != null) {
                SchedulerUtil.runForEntityLater(baseEntity, 3L, () ->
                        storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents()), () -> {});
            }
        });

        // If it's a shulker, get the itemstack array of the items pdc, otherwise use the frame pdc
        gui.setOpenGuiAction(event -> {
            if (gui.getInventory().getViewers().size() > 1) return;
            gui.getInventory().setContents(!shulker && storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY)
                    ? storagePDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{})
                    : (shulker && shulkerPDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                    ? shulkerPDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{})
                    : new ItemStack[]{});
        });

        gui.setCloseGuiAction(event -> {
            if (gui.getInventory().getViewers().size() <= 1) {
                if (shulker) {
                    shulkerPDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                } else {
                    storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                }
            }
            if (hasCloseSound() && BlockHelpers.isLoaded(baseEntity.getLocation()))
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
            playOpenAnimation(baseEntity, closeAnimation);
        });

        return gui;
    }
}
