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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StorageMechanic {

    // Use thread-safe collections for Folia compatibility (concurrent region thread access)
    // Private to enforce all access goes through lock-aware methods and prevent external dupe vectors
    private static final Set<Player> playerStorages = ConcurrentHashMap.newKeySet();
    private static final Map<Block, StorageGui> blockStorages = new ConcurrentHashMap<>();
    private static final Map<Entity, StorageGui> frameStorages = new ConcurrentHashMap<>();
    private static final Set<Block> lockedBlockStorages = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> lockedEntityStorages = ConcurrentHashMap.newKeySet();
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

    public static void clearRuntimeCaches() {
        for (Map.Entry<Block, StorageGui> entry : blockStorages.entrySet()) {
            persistBlockStorage(entry.getKey(), entry.getValue());
            closeAllViewers(entry.getValue());
        }
        for (Map.Entry<Entity, StorageGui> entry : frameStorages.entrySet()) {
            persistEntityStorage(entry.getKey(), entry.getValue());
            closeAllViewers(entry.getValue());
        }
        for (Player player : playerStorages) {
            persistPersonalStorage(player);
        }
        blockStorages.clear();
        frameStorages.clear();
        playerStorages.clear();
        lockedBlockStorages.clear();
        lockedEntityStorages.clear();
    }

    private static void persistBlockStorage(@Nullable Block block, @Nullable StorageGui gui) {
        if (block == null) return;
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        pdc.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, resolveBlockStorageItems(block, gui));
    }

    private static void persistEntityStorage(@Nullable Entity entity, @Nullable StorageGui gui) {
        if (entity == null) return;

        ItemStack[] items = resolveEntityStorageItems(entity, gui);
        entity.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);

        ItemStack furnitureItem = FurnitureMechanic.getFurnitureItem(entity);
        if (furnitureItem == null) return;

        ItemMeta itemMeta = furnitureItem.getItemMeta();
        if (itemMeta == null) return;

        PersistentDataContainer itemPdc = itemMeta.getPersistentDataContainer();
        if (!itemPdc.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY)) return;

        itemPdc.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
        furnitureItem.setItemMeta(itemMeta);
        FurnitureMechanic.setFurnitureItem(entity, furnitureItem);
    }

    private static void persistPersonalStorage(@Nullable Player player) {
        if (player == null || !player.isOnline()) return;

        SchedulerUtil.runForEntity(player, () -> {
            ItemStack[] items = player.getOpenInventory().getTopInventory().getContents();
            player.getPersistentDataContainer().set(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
        }, () -> {
        });
    }

    private static ItemStack[] resolveBlockStorageItems(@NotNull Block block, @Nullable StorageGui gui) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        return (blockStorages.containsKey(block) && gui != null)
                ? gui.getInventory().getContents()
                : pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
    }

    private static ItemStack[] resolveEntityStorageItems(@NotNull Entity baseEntity, @Nullable StorageGui gui) {
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        return (frameStorages.containsKey(baseEntity) && gui != null)
                ? gui.getInventory().getContents()
                : pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
    }

    private static void closeAllViewers(@Nullable StorageGui gui) {
        if (gui == null) return;
        HumanEntity[] viewers = gui.getInventory().getViewers().toArray(new HumanEntity[0]);
        for (HumanEntity viewer : viewers) {
            SchedulerUtil.runForEntityLater(viewer, 1L, () -> gui.close(viewer), () -> {
            });
        }
    }

    private static boolean isBlockStorageLocked(@Nullable Block block) {
        return block != null && lockedBlockStorages.contains(block);
    }

    private static boolean isEntityStorageLocked(@Nullable Entity entity) {
        return entity != null && lockedEntityStorages.contains(entity.getUniqueId());
    }

    private static boolean canPersistBlockStorage(@NotNull Block block, @Nullable StorageGui gui) {
        return gui != null && blockStorages.get(block) == gui && !isBlockStorageLocked(block);
    }

    private static boolean canPersistEntityStorage(@NotNull Entity entity, @Nullable StorageGui gui) {
        return gui != null && frameStorages.get(entity) == gui && !isEntityStorageLocked(entity);
    }

    private static ItemStack[] snapshotContents(@NotNull StorageGui gui) {
        return Arrays.copyOf(gui.getInventory().getContents(), gui.getInventory().getSize());
    }

    private static void closeGuiViewers(@Nullable StorageGui gui) {
        if (gui == null) return;
        HumanEntity[] viewers = gui.getInventory().getViewers().toArray(new HumanEntity[0]);
        for (HumanEntity viewer : viewers) {
            SchedulerUtil.runForEntity(viewer, () -> gui.close(viewer), () -> {});
        }
    }

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
        if (isBlockStorageLocked(block)) return;
        StorageGui storageGui = blockStorages.computeIfAbsent(block, b -> createGui(b, null));
        if (storageGui == null) return;
        storageGui.open(player);
        if (hasOpenSound() && block.getLocation().isWorldLoaded())
            Objects.requireNonNull(block.getWorld()).playSound(block.getLocation(), openSound, volume, pitch);
    }

    public void openStorage(Entity baseEntity, Player player) {
        if (isEntityStorageLocked(baseEntity)) return;
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
        if (!lockedBlockStorages.add(block)) return;
        try {
            StorageGui gui = blockStorages.remove(block);
            PersistentDataContainer pdc = BlockHelpers.getPDC(block);
            ItemStack[] items = gui != null ? snapshotContents(gui) : resolveBlockStorageItems(block, null);
            pdc.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
            closeGuiViewers(gui);

            if (isShulker()) {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic != null) {
                    ItemStack shulker = OraxenItems.getItemById(mechanic.getItemID()).build();
                    ItemMeta shulkerMeta = shulker.getItemMeta();

                    if (shulkerMeta != null)
                        shulkerMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);

                    shulker.setItemMeta(shulkerMeta);
                    block.getWorld().dropItemNaturally(block.getLocation(), shulker);
                }
            } else for (ItemStack item : items) {
                if (item == null) continue;
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
            pdc.remove(STORAGE_KEY);
        } finally {
            blockStorages.remove(block);
            lockedBlockStorages.remove(block);
        }
    }

    public void dropStorageContent(FurnitureMechanic mechanic, Entity baseEntity) {
        UUID entityId = baseEntity.getUniqueId();
        if (!lockedEntityStorages.add(entityId)) return;
        try {
            StorageGui gui = frameStorages.remove(baseEntity);
            PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
            ItemStack[] items = gui != null ? snapshotContents(gui) : resolveEntityStorageItems(baseEntity, null);
            pdc.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);

            ItemStack furnitureItem = FurnitureMechanic.getFurnitureItem(baseEntity);
            if (furnitureItem != null) {
                ItemMeta furnitureMeta = furnitureItem.getItemMeta();
                if (furnitureMeta != null) {
                    furnitureMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
                    furnitureItem.setItemMeta(furnitureMeta);
                    FurnitureMechanic.setFurnitureItem(baseEntity, furnitureItem);
                }
            }

            closeGuiViewers(gui);

            if (isShulker()) {
                ItemStack defaultItem = OraxenItems.getItemById(mechanic.getItemID()).build();
                ItemStack shulker = FurnitureMechanic.getFurnitureItem(baseEntity);
                if (shulker != null) {
                    ItemMeta shulkerMeta = shulker.getItemMeta();
                    if (shulkerMeta != null) {
                        shulkerMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
                        shulkerMeta.setDisplayName(defaultItem.getItemMeta() != null ? defaultItem.getItemMeta().getDisplayName() : null);
                        shulker.setItemMeta(shulkerMeta);
                    }
                    baseEntity.getWorld().dropItemNaturally(baseEntity.getLocation(), shulker);
                }
            } else for (ItemStack item : items) {
                if (item == null) continue;
                baseEntity.getWorld().dropItemNaturally(baseEntity.getLocation(), item);
            }

            pdc.remove(STORAGE_KEY);
        } finally {
            frameStorages.remove(baseEntity);
            lockedEntityStorages.remove(entityId);
        }
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

    /**
     * Gets the stored items for an entity-based furniture storage without requiring player interaction.
     * Reads directly from the entity's PersistentDataContainer.
     *
     * @param baseEntity The furniture base entity
     * @return The stored items, or an empty array if no items are stored
     */
    public ItemStack[] getStorageContents(Entity baseEntity) {
        StorageGui gui = frameStorages.get(baseEntity);
        if (gui != null) {
            return gui.getInventory().getContents();
        }
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        if (pdc.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY)) {
            return pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
        }
        if (isShulker()) {
            ItemStack furnitureItem = FurnitureMechanic.getFurnitureItem(baseEntity);
            if (furnitureItem != null) {
                ItemMeta itemMeta = furnitureItem.getItemMeta();
                if (itemMeta != null) {
                    PersistentDataContainer shulkerPDC = itemMeta.getPersistentDataContainer();
                    return shulkerPDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
                }
            }
        }
        return pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
    }

    /**
     * Gets the stored items for a block-based storage without requiring player interaction.
     * Reads directly from the block's PersistentDataContainer.
     *
     * @param block The storage block
     * @return The stored items, or an empty array if no items are stored
     */
    public ItemStack[] getStorageContents(Block block) {
        StorageGui gui = blockStorages.get(block);
        if (gui != null) {
            return gui.getInventory().getContents();
        }
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        return pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
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
                        {
                            if (!canPersistBlockStorage(block, gui)) return;
                            storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                        });
            }
        });
        gui.setOpenGuiAction(event -> {
            if (isBlockStorageLocked(block)) return;
            if (storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                gui.getInventory().setContents(storagePDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{}));
        });

        gui.setCloseGuiAction(event -> {
            if (!canPersistBlockStorage(block, gui)) return;
            storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            if (hasCloseSound() && BlockHelpers.isLoaded(block.getLocation()))
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
            if (frame != null) playOpenAnimation(frame, closeAnimation);
        });

        return gui;
    }

    private void persistEntityStorageLater(Entity baseEntity, StorageGui gui, PersistentDataContainer storagePDC) {
        SchedulerUtil.runForEntityLater(baseEntity, 3L, () -> {
            if (!canPersistEntityStorage(baseEntity, gui)) return;
            storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
        }, () -> {});
    }

    private void loadEntityStorageContents(StorageGui gui, Entity baseEntity, PersistentDataContainer storagePDC,
                                           boolean shulker, @Nullable PersistentDataContainer shulkerPDC) {
        if (isEntityStorageLocked(baseEntity)) return;
        if (gui.getInventory().getViewers().size() > 1) return;

        ItemStack[] contents = !shulker && storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY)
                ? storagePDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{})
                : shulker && shulkerPDC != null && shulkerPDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY)
                ? shulkerPDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{})
                : new ItemStack[]{};
        gui.getInventory().setContents(contents);
    }

    private void persistEntityStorageOnClose(StorageGui gui, Entity baseEntity, PersistentDataContainer storagePDC,
                                             boolean shulker, @Nullable PersistentDataContainer shulkerPDC) {
        if (!canPersistEntityStorage(baseEntity, gui)) return;
        if (gui.getInventory().getViewers().size() <= 1) {
            if (shulker && shulkerPDC != null) {
                shulkerPDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            } else {
                storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
            }
        }
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
                persistEntityStorageLater(baseEntity, gui, storagePDC);
            }
        });

        // If it's a shulker, get the itemstack array of the items pdc, otherwise use the frame pdc
        gui.setOpenGuiAction(event -> loadEntityStorageContents(gui, baseEntity, storagePDC, shulker, shulkerPDC));

        gui.setCloseGuiAction(event -> {
            persistEntityStorageOnClose(gui, baseEntity, storagePDC, shulker, shulkerPDC);
            if (hasCloseSound() && BlockHelpers.isLoaded(baseEntity.getLocation()))
                Objects.requireNonNull(location.getWorld()).playSound(location, closeSound, volume, pitch);
            playOpenAnimation(baseEntity, closeAnimation);
        });

        return gui;
    }
}
