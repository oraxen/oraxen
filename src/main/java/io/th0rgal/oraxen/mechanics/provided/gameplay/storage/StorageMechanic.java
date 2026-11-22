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
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.sound.Sound;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
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
    private static final Map<Block, ItemStack[]> pendingBlockContents = new ConcurrentHashMap<>();
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
        // Reload may run on any command thread; block PDC lives in the chunk and
        // furniture entities belong to their region, so hop to the owning
        // region/entity thread before persisting (Folia thread-checks both).
        for (Map.Entry<Block, StorageGui> entry : blockStorages.entrySet()) {
            Block block = entry.getKey();
            StorageGui gui = entry.getValue();
            if (block == null || gui == null) continue;
            SchedulerUtil.runAtLocation(block.getLocation(), () -> {
                ItemStack[] items = snapshotContents(gui);
                // Clear immediately after the snapshot so viewers cannot extract
                // already-persisted items before the deferred close (dupe).
                gui.getInventory().clear();
                BlockHelpers.getPDC(block).set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
                closeAllViewers(gui);
            });
        }
        for (Map.Entry<Entity, StorageGui> entry : frameStorages.entrySet()) {
            Entity entity = entry.getKey();
            StorageGui gui = entry.getValue();
            if (entity == null || gui == null) continue;
            SchedulerUtil.runForEntity(entity, () -> {
                ItemStack[] items = snapshotContents(gui);
                gui.getInventory().clear();
                persistEntityStorage(entity, items);
                closeAllViewers(gui);
            }, () -> {
            });
        }
        for (Player player : playerStorages) {
            persistPersonalStorage(player);
        }
        blockStorages.clear();
        frameStorages.clear();
        playerStorages.clear();
        lockedBlockStorages.clear();
        pendingBlockContents.clear();
        lockedEntityStorages.clear();
    }

    private static void persistEntityStorage(@Nullable Entity entity, ItemStack[] items) {
        if (entity == null) return;

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
        // Clear the inventory immediately after snapshot to prevent dupe exploits.
        // Without this, viewers could extract items between snapshot and async close.
        gui.getInventory().clear();
        HumanEntity[] viewers = gui.getInventory().getViewers().toArray(new HumanEntity[0]);
        for (HumanEntity viewer : viewers) {
            SchedulerUtil.runForEntity(viewer, () -> gui.close(viewer), () -> {});
        }
    }

    public StorageMechanic(ConfigurationSection section) {
        rows = section.getInt("rows", 6);
        title = section.getString("title", "Storage");
        type = parseType(section);
        openSound = section.getString("open_sound", "minecraft:block.chest.open");
        closeSound = section.getString("close_sound", "minecraft:block.chest.close");
        openAnimation = section.getString("open_animation", null);
        closeAnimation = section.getString("close_animation", null);
        volume = (float) section.getDouble("volume", 0.5);
        pitch = (float) section.getDouble("pitch", 0.95f);
    }

    private static StorageType parseType(ConfigurationSection section) {
        String value = section.getString("type", "STORAGE");
        try {
            return StorageType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logs.logWarning("Invalid storage type " + value + " in " + section.getCurrentPath() + ", using STORAGE");
            Logs.logWarning("Valid types are: " + StringUtils.join(StorageType.values(), ", "));
            return StorageType.STORAGE;
        }
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
            AdventureUtils.playSound(location, openSound, Sound.Source.MASTER, volume, pitch);
    }

    public void openDisposal(Player player, Location location, @Nullable Entity baseEntity) {
        if (type != StorageType.DISPOSAL) return;
        StorageGui storageGui = createDisposalGui(location, baseEntity);
        storageGui.open(player);
        if (baseEntity != null)
            playOpenAnimation(baseEntity, openAnimation);
        if (hasOpenSound() && location.isWorldLoaded())
            AdventureUtils.playSound(location, openSound, Sound.Source.MASTER, volume, pitch);
    }

    public void openStorage(Block block, Player player) {
        Material blockType = block.getType();
        if (blockType != Material.NOTE_BLOCK && blockType != Material.TRIPWIRE && blockType != Material.CHORUS_PLANT) return;
        if (isBlockStorageLocked(block)) return;
        StorageGui storageGui = blockStorages.computeIfAbsent(block, b -> createGui(b, null));
        if (storageGui == null) return;
        storageGui.open(player);
        if (hasOpenSound() && block.getLocation().isWorldLoaded())
            AdventureUtils.playSound(block.getLocation(), openSound, Sound.Source.MASTER, volume, pitch);
    }

    public void openStorage(Entity baseEntity, Player player) {
        if (isEntityStorageLocked(baseEntity)) return;
        StorageGui storageGui = frameStorages.computeIfAbsent(baseEntity, this::createGui);
        if (storageGui == null) return;
        storageGui.open(player);
        playOpenAnimation(baseEntity, openAnimation);
        if (hasOpenSound() && baseEntity.getLocation().isWorldLoaded())
            AdventureUtils.playSound(baseEntity.getLocation(), openSound, Sound.Source.MASTER, volume, pitch);
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

    /**
     * Moves the contents a shulker item carries into the block it was placed as.
     *
     * @param block The placed storage block
     * @param item  The item the block was placed from
     */
    public void loadStorageContent(Block block, @Nullable ItemStack item) {
        loadStorageContent(BlockHelpers.getPDC(block), item);
    }

    /**
     * Moves the contents a shulker item carries into the given container, for example a falling block.
     *
     * @param pdc  The container that holds the storage contents
     * @param item The item the block was placed from
     */
    public void loadStorageContent(PersistentDataContainer pdc, @Nullable ItemStack item) {
        ItemMeta itemMeta = isShulker() && item != null ? item.getItemMeta() : null;
        if (itemMeta == null) return;
        ItemStack[] items = itemMeta.getPersistentDataContainer().get(STORAGE_KEY, DataType.ITEM_STACK_ARRAY);
        if (items != null) pdc.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
    }

    public void dropStorageContent(Block block) {
        Mechanic mechanic = OraxenBlocks.getOraxenBlock(block.getLocation());
        dropStorageContent(block, mechanic != null ? mechanic.getItemID() : null);
    }

    /**
     * Drops the contents of a block storage. A shulker storage that holds items drops its item with the contents instead.
     *
     * @param block  The storage block
     * @param itemId The item a shulker storage drops, can be null
     * @return True if a shulker item holding the contents was dropped
     */
    public boolean dropStorageContent(Block block, @Nullable String itemId) {
        if (!lockedBlockStorages.add(block)) return false;
        try {
            StorageGui gui = blockStorages.remove(block);
            pendingBlockContents.remove(block);
            PersistentDataContainer pdc = BlockHelpers.getPDC(block);
            ItemStack[] items = gui != null ? snapshotContents(gui) : resolveBlockStorageItems(block, null);
            pdc.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
            closeGuiViewers(gui);

            boolean droppedShulker = false;
            if (isShulker()) {
                boolean hasContents = Arrays.stream(items).anyMatch(item -> item != null && !item.getType().isAir());
                ItemBuilder shulkerBuilder = itemId != null && hasContents ? OraxenItems.getItemById(itemId) : null;
                if (shulkerBuilder != null) {
                    ItemStack shulker = shulkerBuilder.build();
                    ItemUtils.editItemMeta(shulker, shulkerMeta ->
                            shulkerMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items));
                    block.getWorld().dropItemNaturally(block.getLocation(), shulker);
                    droppedShulker = true;
                }
            } else for (ItemStack item : items) {
                if (item == null) continue;
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
            pdc.remove(STORAGE_KEY);
            return droppedShulker;
        } finally {
            // blockStorages.remove() already called at start of try block
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

            closeGuiViewers(gui);

            if (isShulker()) {
                ItemStack defaultItem = OraxenItems.getItemById(mechanic.getItemID()).build();
                ItemStack shulker = FurnitureMechanic.getFurnitureItem(baseEntity);
                if (shulker != null) {
                    ItemMeta shulkerMeta = shulker.getItemMeta();
                    if (shulkerMeta != null) {
                        shulkerMeta.getPersistentDataContainer().set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items);
                        ItemMeta defaultMeta = defaultItem.getItemMeta();
                        ItemUtils.setDisplayName(shulkerMeta,
                                defaultMeta != null ? ItemUtils.getDisplayName(defaultMeta) : null);
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
            // frameStorages.remove() already called at start of try block
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
                AdventureUtils.playSound(location, closeSound, Sound.Source.MASTER, volume, pitch);
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
                SchedulerUtil.runForEntityLater(player, 3L, () -> {
                    if (!gui.getInventory().getViewers().contains(player)) return;
                    storagePDC.set(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.getInventory().getContents());
                }, () -> {});
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
                AdventureUtils.playSound(player.getLocation(), closeSound, Sound.Source.MASTER, volume, pitch);
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
            ItemStack[] pending = pendingBlockContents.get(block);
            if (pending != null) gui.getInventory().setContents(pending);
            else if (storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                gui.getInventory().setContents(storagePDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{}));
        });

        gui.setCloseGuiAction(event -> {
            ItemStack[] contents = snapshotContents(gui);
            Runnable persist = () -> {
                if (!canPersistBlockStorage(block, gui)) return;
                storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, contents);
                if (hasCloseSound() && block.getLocation().isChunkLoaded())
                    AdventureUtils.playSound(location, closeSound, Sound.Source.MASTER, volume, pitch);
                if (frame != null) playOpenAnimation(frame, closeAnimation);
            };
            if (Bukkit.isOwnedByCurrentRegion(location)) {
                // This write is the newest, so an older snapshot from another region must not win later
                pendingBlockContents.remove(block);
                persist.run();
                return;
            }
            // The viewer closed from another region (teleport, quit) and the chunk PDC belongs to the block's region.
            // Reopening before the write lands must use this snapshot, not the older PDC contents.
            pendingBlockContents.put(block, contents);
            SchedulerUtil.runAtLocation(location, () -> {
                // Skip the write if a newer close already replaced or persisted this snapshot
                if (pendingBlockContents.remove(block, contents)) persist.run();
            });
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
            if (hasCloseSound() && baseEntity.getLocation().isChunkLoaded())
                AdventureUtils.playSound(location, closeSound, Sound.Source.MASTER, volume, pitch);
            playOpenAnimation(baseEntity, closeAnimation);
        });

        return gui;
    }
}
