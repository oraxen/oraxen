package io.th0rgal.oraxen.items;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.jeff_media.morepersistentdatatypes.DataType;
import com.jeff_media.persistentdataserializer.PersistentDataSerializer;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import static io.th0rgal.oraxen.items.ItemBuilder.ORIGINAL_NAME_KEY;
import static io.th0rgal.oraxen.items.ItemBuilder.UNSTACKABLE_KEY;

public class ItemUpdater implements Listener {

    private static final int STARTUP_ENTITY_BATCH_SIZE = 50;
    private static final int STARTUP_CHUNK_BATCH_SIZE = 10;
    private static final int CHUNK_LOAD_TILE_ENTITY_BATCH_SIZE = 5;

    private static final Object STARTUP_SCAN_LOCK = new Object();
    private static final Object TILE_ENTITY_CHUNK_QUEUE_LOCK = new Object();
    private static final Queue<Chunk> pendingTileEntityChunks = new ArrayDeque<>();
    private static final Set<ChunkKey> pendingTileEntityChunkKeys = new HashSet<>();
    private static SchedulerUtil.ScheduledTask startupContentsTask;
    private static SchedulerUtil.ScheduledTask startupEntityScanTask;
    private static SchedulerUtil.ScheduledTask startupChunkScanTask;
    private static SchedulerUtil.ScheduledTask tileEntityChunkQueueTask;

    public ItemUpdater() {
        resetQueuedTasks();
        if (!Settings.UPDATE_ITEMS.toBool()) return;
        if (VersionUtil.isPaperServer()) Bukkit.getPluginManager().registerEvents(new PaperEntityLoadListener(), OraxenPlugin.get());
        replaceStartupContentsTask(SchedulerUtil.runTaskLater(OraxenPlugin.get(), 2L, () -> {
            clearStartupContentsTask();
            updateLoadedContents();
        }));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!Settings.UPDATE_ITEMS.toBool()) return;

        Player player = event.getPlayer();
        SchedulerUtil.runForEntity(player, () -> {
            updateInventory(player.getInventory());
            updateInventory(player.getEnderChest());
        });
    }

    private static final class PaperEntityLoadListener implements Listener {
        @EventHandler
        public void onEntityLoad(EntityAddToWorldEvent event) {
            if (!Settings.UPDATE_ITEMS.toBool() || !Settings.UPDATE_ENTITY_CONTENTS.toBool()) return;

            Entity entity = event.getEntity();
            if (!shouldUpdateEntityContents(entity)) return;

            SchedulerUtil.runForEntityLater(entity, 2L, () -> {
                if (!entity.isValid()) return;
                updateEntityInventories(entity);
            }, () -> {});
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!Settings.UPDATE_ITEMS.toBool() || !Settings.UPDATE_TILE_ENTITY_CONTENTS.toBool() || event.isNewChunk()) return;

        queueTileEntityChunkUpdate(event.getChunk());
    }

    @EventHandler
    public void onPlayerPickUp(EntityPickupItemEvent event) {
        if (!Settings.UPDATE_ITEMS.toBool()) return;
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack oldItem = event.getItem().getItemStack();
        ItemStack newItem = ItemUpdater.updateItem(oldItem);
        if (oldItem.equals(newItem)) return;
        event.getItem().setItemStack(newItem);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemEnchant(PrepareItemEnchantEvent event) {
        String id = OraxenItems.getIdByItem(event.getItem());
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null || !builder.hasOraxenMeta()) return;

        if (builder.getOraxenMeta().isDisableEnchanting()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemEnchant(PrepareAnvilEvent event) {
        ItemStack item = event.getInventory().getItem(0);
        ItemStack result = event.getResult();
        String id = OraxenItems.getIdByItem(item);
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null || !builder.hasOraxenMeta()) return;

        if (builder.getOraxenMeta().isDisableEnchanting()) {
            if (result == null || item == null) return;
            if (!result.getEnchantments().equals(item.getEnchantments()))
                event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUseMaxDamageItem(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (!VersionUtil.atOrAbove("1.20.5") || player.getGameMode() == GameMode.CREATIVE) return;
        if (ItemUtils.isEmpty(itemStack) || ItemUtils.isTool(itemStack)) return;
        if (!(itemStack.getItemMeta() instanceof Damageable damageable) || !damageable.hasMaxDamage()) return;

        Optional.ofNullable(OraxenItems.getBuilderByItem(itemStack)).ifPresent(i -> {
                if (i.isDamagedOnBlockBreak()) itemStack.damage(1, player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUseMaxDamageItem(EntityDamageByEntityEvent event) {
        if (!VersionUtil.atOrAbove("1.20.5") || VersionUtil.atOrAbove("1.21.2")) return;
        if (!(event.getDamager() instanceof LivingEntity entity)) return;
        ItemStack itemStack = Optional.ofNullable(entity.getEquipment()).map(EntityEquipment::getItemInMainHand).orElse(null);

        if (entity instanceof Player player && player.getGameMode() == GameMode.CREATIVE) return;
        if (ItemUtils.isEmpty(itemStack) || ItemUtils.isTool(itemStack)) return;
        if (!(itemStack.getItemMeta() instanceof Damageable damageable) || !damageable.hasMaxDamage()) return;

        Optional.ofNullable(OraxenItems.getBuilderByItem(itemStack)).ifPresent(i -> {
            if (i.isDamagedOnEntityHit()) itemStack.damage(1, entity);
        });
    }

    // Until Paper changes getReplacement to use food-component, this is the best way
    @EventHandler(ignoreCancelled = true)
    public void onUseConvertedTo(PlayerItemConsumeEvent event) {
        ItemStack itemStack = event.getItem();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!VersionUtil.atOrAbove("1.21") && itemMeta == null) return;
        ItemStack usingConvertsTo = ItemUtils.getUsingConvertsTo(itemMeta);
        if (usingConvertsTo == null || !itemStack.isSimilar(ItemUpdater.updateItem(usingConvertsTo))) return;

        PlayerInventory inventory = event.getPlayer().getInventory();
        if (inventory.firstEmpty() == -1) event.setItem(event.getItem().add(usingConvertsTo.getAmount()));
        else SchedulerUtil.runForEntity(event.getPlayer(), () -> {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack oldItem = inventory.getItem(i);
                ItemStack newItem = ItemUpdater.updateItem(oldItem);
                if (!itemStack.isSimilar(newItem)) continue;

                // Remove the item and add it to fix stacking
                inventory.setItem(i, null);
                inventory.addItem(newItem);
            }
        }, () -> {});
    }

    private static final NamespacedKey IF_UUID = Objects.requireNonNull(NamespacedKey.fromString("oraxen:if-uuid"));
    private static final NamespacedKey MF_GUI = Objects.requireNonNull(NamespacedKey.fromString("oraxen:mf-gui"));

    public static void updateLoadedEntityContents() {
        if (!Settings.UPDATE_ITEMS.toBool() || !Settings.UPDATE_ENTITY_CONTENTS.toBool()) return;
        if (VersionUtil.isFoliaServer()) {
            Logs.debug("Skipping loaded entity item updates on Folia; entities are updated when they load instead.");
            return;
        }

        SchedulerUtil.runTask(() -> {
            List<Entity> entities = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!shouldUpdateEntityContents(entity)) continue;
                    entities.add(entity);
                }
            }
            processLoadedEntities(entities);
        });
    }

    public static void updateLoadedTileEntityContents() {
        if (!Settings.UPDATE_ITEMS.toBool() || !Settings.UPDATE_TILE_ENTITY_CONTENTS.toBool()) return;
        if (VersionUtil.isFoliaServer()) {
            Logs.debug("Skipping loaded tile entity item updates on Folia; tile entities are updated when their chunks load instead.");
            return;
        }

        SchedulerUtil.runTask(() -> {
            List<Chunk> chunks = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    chunks.add(chunk);
                }
            }
            processLoadedChunks(chunks);
        });
    }

    private static void processLoadedEntities(List<Entity> entities) {
        replaceStartupEntityScanTask(null);
        if (entities.isEmpty()) return;

        final int[] index = {0};
        final StartupScanTask task = new StartupScanTask();
        // The timer may theoretically finish before its handle is registered on non-standard schedulers.
        // registerStartupEntityScanTask checks task.finished after assigning the handle and cancels it in that case.
        SchedulerUtil.ScheduledTask scheduledTask = SchedulerUtil.runTaskTimer(1L, 1L, () -> {
            try {
                int batchEnd = Math.min(index[0] + STARTUP_ENTITY_BATCH_SIZE, entities.size());
                while (index[0] < batchEnd) {
                    Entity entity = entities.get(index[0]++);
                    if (!entity.isValid() || !shouldUpdateEntityContents(entity)) continue;
                    SchedulerUtil.runForEntity(entity, () -> updateEntityInventories(entity), () -> {});
                }
                if (index[0] >= entities.size()) finishStartupEntityScanTask(task);
            } catch (RuntimeException | Error throwable) {
                finishStartupEntityScanTask(task);
                throw throwable;
            }
        });
        registerStartupEntityScanTask(task, scheduledTask);
    }

    private static void processLoadedChunks(List<Chunk> chunks) {
        replaceStartupChunkScanTask(null);
        if (chunks.isEmpty()) return;

        final int[] index = {0};
        final StartupScanTask task = new StartupScanTask();
        SchedulerUtil.ScheduledTask scheduledTask = SchedulerUtil.runTaskTimer(1L, 1L, () -> {
            try {
                int batchEnd = Math.min(index[0] + STARTUP_CHUNK_BATCH_SIZE, chunks.size());
                while (index[0] < batchEnd) {
                    Chunk chunk = chunks.get(index[0]++);
                    SchedulerUtil.runAtLocation(chunkLocation(chunk), () -> {
                        if (!chunk.isLoaded()) return;
                        updateTileEntityInventories(chunk);
                    });
                }
                if (index[0] >= chunks.size()) finishStartupChunkScanTask(task);
            } catch (RuntimeException | Error throwable) {
                finishStartupChunkScanTask(task);
                throw throwable;
            }
        });
        registerStartupChunkScanTask(task, scheduledTask);
    }

    private static void replaceStartupContentsTask(SchedulerUtil.ScheduledTask task) {
        SchedulerUtil.ScheduledTask oldTask;
        synchronized (STARTUP_SCAN_LOCK) {
            oldTask = startupContentsTask;
            startupContentsTask = task;
        }
        cancelTask(oldTask);
    }

    private static void clearStartupContentsTask() {
        synchronized (STARTUP_SCAN_LOCK) {
            startupContentsTask = null;
        }
    }

    private static void replaceStartupEntityScanTask(SchedulerUtil.ScheduledTask task) {
        SchedulerUtil.ScheduledTask oldTask;
        synchronized (STARTUP_SCAN_LOCK) {
            oldTask = startupEntityScanTask;
            startupEntityScanTask = task;
        }
        cancelTask(oldTask);
    }

    private static void registerStartupEntityScanTask(StartupScanTask task, SchedulerUtil.ScheduledTask scheduledTask) {
        SchedulerUtil.ScheduledTask oldTask;
        boolean finished;
        synchronized (STARTUP_SCAN_LOCK) {
            task.scheduledTask = scheduledTask;
            finished = task.finished;
            oldTask = startupEntityScanTask;
            startupEntityScanTask = finished ? null : scheduledTask;
        }
        cancelTask(oldTask);
        if (finished) cancelTask(scheduledTask);
    }

    private static void replaceStartupChunkScanTask(SchedulerUtil.ScheduledTask task) {
        SchedulerUtil.ScheduledTask oldTask;
        synchronized (STARTUP_SCAN_LOCK) {
            oldTask = startupChunkScanTask;
            startupChunkScanTask = task;
        }
        cancelTask(oldTask);
    }

    private static void registerStartupChunkScanTask(StartupScanTask task, SchedulerUtil.ScheduledTask scheduledTask) {
        SchedulerUtil.ScheduledTask oldTask;
        boolean finished;
        synchronized (STARTUP_SCAN_LOCK) {
            task.scheduledTask = scheduledTask;
            finished = task.finished;
            oldTask = startupChunkScanTask;
            startupChunkScanTask = finished ? null : scheduledTask;
        }
        cancelTask(oldTask);
        if (finished) cancelTask(scheduledTask);
    }

    private static void finishStartupEntityScanTask(StartupScanTask task) {
        synchronized (STARTUP_SCAN_LOCK) {
            task.finished = true;
            if (startupEntityScanTask == task.scheduledTask) startupEntityScanTask = null;
        }
        cancelTask(task.scheduledTask);
    }

    private static void finishStartupChunkScanTask(StartupScanTask task) {
        synchronized (STARTUP_SCAN_LOCK) {
            task.finished = true;
            if (startupChunkScanTask == task.scheduledTask) startupChunkScanTask = null;
        }
        cancelTask(task.scheduledTask);
    }

    private static void cancelTask(SchedulerUtil.ScheduledTask task) {
        if (task != null) task.cancel();
    }

    public static void resetQueuedTasks() {
        SchedulerUtil.ScheduledTask contentsTask;
        SchedulerUtil.ScheduledTask entityTask;
        SchedulerUtil.ScheduledTask chunkTask;
        synchronized (STARTUP_SCAN_LOCK) {
            contentsTask = startupContentsTask;
            entityTask = startupEntityScanTask;
            chunkTask = startupChunkScanTask;
            startupContentsTask = null;
            startupEntityScanTask = null;
            startupChunkScanTask = null;
        }
        cancelTask(contentsTask);
        cancelTask(entityTask);
        cancelTask(chunkTask);

        SchedulerUtil.ScheduledTask tileEntityTask;
        synchronized (TILE_ENTITY_CHUNK_QUEUE_LOCK) {
            tileEntityTask = tileEntityChunkQueueTask;
            tileEntityChunkQueueTask = null;
            pendingTileEntityChunks.clear();
            pendingTileEntityChunkKeys.clear();
        }
        cancelTask(tileEntityTask);
    }

    private static void updateLoadedContents() {
        updateLoadedEntityContents();
        updateLoadedTileEntityContents();
    }

    private static void queueTileEntityChunkUpdate(Chunk chunk) {
        ChunkKey key = ChunkKey.from(chunk);
        synchronized (TILE_ENTITY_CHUNK_QUEUE_LOCK) {
            if (!pendingTileEntityChunkKeys.add(key)) return;
            pendingTileEntityChunks.add(chunk);
            if (tileEntityChunkQueueTask != null) return;

            tileEntityChunkQueueTask = SchedulerUtil.runTaskTimer(1L, 1L, ItemUpdater::processQueuedTileEntityChunks);
        }
    }

    private static void processQueuedTileEntityChunks() {
        SchedulerUtil.ScheduledTask taskToCancel = null;
        for (int i = 0; i < CHUNK_LOAD_TILE_ENTITY_BATCH_SIZE; i++) {
            Chunk chunk;
            synchronized (TILE_ENTITY_CHUNK_QUEUE_LOCK) {
                chunk = pendingTileEntityChunks.poll();
                if (chunk == null) {
                    taskToCancel = finishTileEntityChunkQueueTask();
                    break;
                }
                pendingTileEntityChunkKeys.remove(ChunkKey.from(chunk));
            }

            SchedulerUtil.runAtLocationLater(chunkLocation(chunk), 1L, () -> {
                if (!chunk.isLoaded()) return;
                updateTileEntityInventories(chunk);
            });
        }

        if (taskToCancel == null) {
            synchronized (TILE_ENTITY_CHUNK_QUEUE_LOCK) {
                if (pendingTileEntityChunks.isEmpty()) taskToCancel = finishTileEntityChunkQueueTask();
            }
        }
        cancelTask(taskToCancel);
    }

    private static SchedulerUtil.ScheduledTask finishTileEntityChunkQueueTask() {
        // Called inside TILE_ENTITY_CHUNK_QUEUE_LOCK; caller must cancel the returned task outside the lock.
        SchedulerUtil.ScheduledTask task = tileEntityChunkQueueTask;
        tileEntityChunkQueueTask = null;
        return task;
    }

    private static Location chunkLocation(Chunk chunk) {
        return new Location(chunk.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16);
    }

    private static void updateTileEntityInventories(Chunk chunk) {
        for (BlockState tileEntity : chunk.getTileEntities()) {
            if (!(tileEntity instanceof InventoryHolder holder)) continue;
            updateInventory(holder.getInventory());
        }
    }

    private static final class StartupScanTask {
        private volatile SchedulerUtil.ScheduledTask scheduledTask;
        private volatile boolean finished;
    }

    private record ChunkKey(UUID worldId, int x, int z) {

        private static ChunkKey from(Chunk chunk) {
            return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    public static void updateEntityInventories(Entity entity) {
        if (entity instanceof ItemFrame itemFrame) {
            ItemStack oldItem = itemFrame.getItem();
            ItemStack newItem = updateItem(oldItem);
            if (!Objects.equals(oldItem, newItem)) itemFrame.setItem(newItem, false);
        }
        if (entity instanceof ItemDisplay itemDisplay) {
            ItemStack oldItem = itemDisplay.getItemStack();
            ItemStack newItem = updateItem(oldItem);
            if (!Objects.equals(oldItem, newItem)) itemDisplay.setItemStack(newItem);
        }
        if (entity instanceof Item item) {
            ItemStack oldItem = item.getItemStack();
            ItemStack newItem = updateItem(oldItem);
            if (!Objects.equals(oldItem, newItem)) item.setItemStack(newItem);
        }
        if (entity instanceof InventoryHolder holder && !(entity instanceof ItemFrame)) updateInventory(holder.getInventory());
        if (entity instanceof LivingEntity livingEntity) updateEquipment(livingEntity);
    }

    private static boolean shouldUpdateEntityContents(Entity entity) {
        if (entity instanceof Player) return false;
        return entity instanceof ItemFrame
                || entity instanceof ItemDisplay
                || entity instanceof Item
                || entity instanceof InventoryHolder
                || entity instanceof LivingEntity livingEntity && hasEquipment(livingEntity);
    }

    private static boolean hasEquipment(LivingEntity livingEntity) {
        EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) return false;

        if (!ItemUtils.isEmpty(equipment.getItemInMainHand()) || !ItemUtils.isEmpty(equipment.getItemInOffHand())) return true;
        for (ItemStack itemStack : equipment.getArmorContents()) {
            if (!ItemUtils.isEmpty(itemStack)) return true;
        }
        return false;
    }

    private static void updateEquipment(LivingEntity livingEntity) {
        EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                ItemStack oldItem = equipment.getItem(slot);
                if (oldItem == null) continue;
                ItemStack newItem = updateItem(oldItem);
                if (oldItem.equals(newItem)) continue;
                equipment.setItem(slot, newItem);
            } catch (IllegalArgumentException ignored) {
                // Some entity types do not support every slot exposed by the API.
            }
        }
    }

    public static void updateInventory(Inventory inventory) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack oldItem = inventory.getItem(i);
            ItemStack newItem = updateItem(oldItem);
            if (oldItem == null || oldItem.equals(newItem)) continue;
            inventory.setItem(i, newItem);
        }
    }

    public static ItemStack updateItem(ItemStack oldItem) {
        String id = OraxenItems.getIdByItem(oldItem);
        if (id == null) return oldItem;

        // Oraxens Inventory adds a dumb PDC entry to items, this will remove them
        // Done here over [ItemsView] as this method is called anyway and supports old items
        ItemUtils.editItemMeta(oldItem, itemMeta -> {
            itemMeta.getPersistentDataContainer().remove(IF_UUID);
            itemMeta.getPersistentDataContainer().remove(MF_GUI);
        });

        Optional<ItemBuilder> optionalBuilder = OraxenItems.getOptionalItemById(id);
        if (optionalBuilder.isEmpty() || optionalBuilder.get().getOraxenMeta().isNoUpdate()) return oldItem;
        ItemBuilder newItemBuilder = optionalBuilder.get();

        ItemStack newItem = NMSHandlers.getHandler() != null ? NMSHandlers.getHandler().copyItemNBTTags(oldItem, newItemBuilder.build()) : newItemBuilder.build();
        newItem.setAmount(oldItem.getAmount());

        ItemUtils.editItemMeta(newItem, itemMeta -> {
            ItemMeta oldMeta = oldItem.getItemMeta();
            ItemMeta newMeta = newItem.getItemMeta();
            if (oldMeta == null || newMeta == null) return;
            PersistentDataContainer oldPdc = oldMeta.getPersistentDataContainer();
            PersistentDataContainer itemPdc = itemMeta.getPersistentDataContainer();

            // Transfer over all PDC entries from oldItem to newItem
            List<Map<?, ?>> oldPdcMap = PersistentDataSerializer.toMapList(oldPdc);
            PersistentDataSerializer.fromMapList(oldPdcMap, itemPdc);

            // If oldItem had all enchantments removed, don't restore configured enchantments
            if (oldMeta.getEnchants().isEmpty() && !newMeta.getEnchants().isEmpty()) {
                // Remove all configured enchantments since user intentionally unenchanted the item
                for (Enchantment enchantment : newMeta.getEnchants().keySet())
                    itemMeta.removeEnchant(enchantment);
            } else {
                // Add all enchantments from oldItem and add all from newItem as long as it is not the same Enchantments
                for (Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet())
                    itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                for (Map.Entry<Enchantment, Integer> entry : newMeta.getEnchants().entrySet().stream().filter(e -> !oldMeta.getEnchants().containsKey(e.getKey())).toList())
                    itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            }

            Integer cmd = newMeta.hasCustomModelData() ? (Integer) newMeta.getCustomModelData() : oldMeta.hasCustomModelData() ? (Integer) oldMeta.getCustomModelData() : null;
            itemMeta.setCustomModelData(cmd);

            // If OraxenItem has no lore, we should assume that 3rd-party plugin has added lore
            if (Settings.OVERRIDE_ITEM_LORE.toBool()) {
                if (VersionUtil.isPaperServer()) itemMeta.lore(newMeta.lore());
                else itemMeta.setLore(newMeta.getLore());
            } else {
                if (VersionUtil.isPaperServer()) itemMeta.lore(oldMeta.lore());
                else itemMeta.setLore(oldMeta.getLore());
            }

            // Only change AttributeModifiers if the new item has some
            if (newMeta.hasAttributeModifiers()) itemMeta.setAttributeModifiers(newMeta.getAttributeModifiers());
            else if (oldMeta.hasAttributeModifiers()) itemMeta.setAttributeModifiers(oldMeta.getAttributeModifiers());

            // Transfer over durability from old item
            if (itemMeta instanceof Damageable damageable && oldMeta instanceof Damageable oldDmg) {
                if (oldDmg.hasDamage()) damageable.setDamage(oldDmg.getDamage());
            }

            if (oldMeta.isUnbreakable()) itemMeta.setUnbreakable(true);

            if (itemMeta instanceof LeatherArmorMeta leatherMeta && oldMeta instanceof LeatherArmorMeta oldLeatherMeta && newMeta instanceof LeatherArmorMeta newLeatherMeta) {
                // If it is not custom armor, keep color
                if (oldItem.getType() == Material.LEATHER_HORSE_ARMOR) leatherMeta.setColor(oldLeatherMeta.getColor());
                // If it is custom armor we use newLeatherMeta color, since the builder would have been altered
                // in the process of creating the shader images. Then we just save the builder to update the config
                else {
                    leatherMeta.setColor(newLeatherMeta.getColor());
                    newItemBuilder.save();
                }
            }

            if (itemMeta instanceof PotionMeta potionMeta && oldMeta instanceof PotionMeta oldPotionMeta) {
                potionMeta.setColor(oldPotionMeta.getColor());
            }

            if (itemMeta instanceof MapMeta mapMeta && oldMeta instanceof MapMeta oldMapMeta) {
                mapMeta.setColor(oldMapMeta.getColor());
            }

            if (VersionUtil.atOrAbove("1.20") && itemMeta instanceof ArmorMeta armorMeta && oldMeta instanceof ArmorMeta oldArmorMeta) {
                armorMeta.setTrim(oldArmorMeta.getTrim());
            }

            if (VersionUtil.atOrAbove("1.20.5")) {
                if (newMeta.hasFood()) itemMeta.setFood(newMeta.getFood());
                else if (oldMeta.hasFood()) itemMeta.setFood(oldMeta.getFood());

                if (newMeta.hasEnchantmentGlintOverride()) itemMeta.setEnchantmentGlintOverride(newMeta.getEnchantmentGlintOverride());
                else if (oldMeta.hasEnchantmentGlintOverride()) itemMeta.setEnchantmentGlintOverride(oldMeta.getEnchantmentGlintOverride());

                if (newMeta.hasMaxStackSize()) itemMeta.setMaxStackSize(newMeta.getMaxStackSize());
                else if (oldMeta.hasMaxStackSize()) itemMeta.setMaxStackSize(oldMeta.getMaxStackSize());

                if (VersionUtil.isPaperServer()) {
                    if (newMeta.hasItemName()) itemMeta.itemName(newMeta.itemName());
                    else if (oldMeta.hasItemName()) itemMeta.itemName(oldMeta.itemName());
                } else {
                    if (newMeta.hasItemName()) itemMeta.setItemName(newMeta.getItemName());
                    else if (oldMeta.hasItemName()) itemMeta.setItemName(oldMeta.getItemName());
                }
            }

            if (VersionUtil.atOrAbove("1.21")) {
                if (newMeta.hasJukeboxPlayable()) itemMeta.setJukeboxPlayable(newMeta.getJukeboxPlayable());
                else if (oldMeta.hasJukeboxPlayable()) itemMeta.setJukeboxPlayable(oldMeta.getJukeboxPlayable());
            }

            if (VersionUtil.atOrAbove("1.21.2")) {
                if (newMeta.hasEquippable()) itemMeta.setEquippable(newMeta.getEquippable());
                // Preserve old item's equippable data when the new template has none.
                else if (oldMeta.hasEquippable()) itemMeta.setEquippable(oldMeta.getEquippable());

                if (newMeta.isGlider()) itemMeta.setGlider(true);
                else if (oldMeta.isGlider()) itemMeta.setGlider(true);

                if (newMeta.hasItemModel()) itemMeta.setItemModel(newMeta.getItemModel());
                else if (oldMeta.hasItemModel()) itemMeta.setItemModel(oldMeta.getItemModel());

                if (newMeta.hasUseCooldown()) itemMeta.setUseCooldown(newMeta.getUseCooldown());
                else if (oldMeta.hasUseCooldown()) itemMeta.setUseCooldown(oldMeta.getUseCooldown());

                if (newMeta.hasUseRemainder()) itemMeta.setUseRemainder(newMeta.getUseRemainder());
                else if (oldMeta.hasUseRemainder()) itemMeta.setUseRemainder(oldMeta.getUseRemainder());

                if (newMeta.hasDamageResistant()) itemMeta.setDamageResistant(newMeta.getDamageResistant());
                else if (oldMeta.hasDamageResistant()) itemMeta.setDamageResistant(oldMeta.getDamageResistant());

                if (newMeta.hasTooltipStyle()) itemMeta.setTooltipStyle(newMeta.getTooltipStyle());
                else if (oldMeta.hasTooltipStyle()) itemMeta.setTooltipStyle(oldMeta.getTooltipStyle());

                if (newMeta.hasEnchantable()) itemMeta.setEnchantable(newMeta.getEnchantable());
                else if (oldMeta.hasEnchantable()) itemMeta.setEnchantable(oldMeta.getEnchantable());
            }

            // On 1.20.5+ we use ItemName which is different from userchanged displaynames
            if (!VersionUtil.atOrAbove("1.20.5")) {

                String oldDisplayName = oldMeta.hasDisplayName() ? AdventureUtils.parseLegacy(VersionUtil.isPaperServer() ? AdventureUtils.MINI_MESSAGE.serialize(oldMeta.displayName()) : AdventureUtils.parseLegacy(oldMeta.getDisplayName())) : null;
                String originalName = AdventureUtils.parseLegacy(oldPdc.getOrDefault(ORIGINAL_NAME_KEY, DataType.STRING, ""));

                if (Settings.OVERRIDE_RENAMED_ITEMS.toBool()) {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(newMeta.displayName());
                    else itemMeta.setDisplayName(newMeta.getDisplayName());
                } else if (!originalName.equals(oldDisplayName)) {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(oldMeta.displayName());
                    else itemMeta.setDisplayName(oldMeta.getDisplayName());
                } else {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(newMeta.displayName());
                    else itemMeta.setDisplayName(newMeta.getDisplayName());
                }

                originalName = newMeta.hasDisplayName() ? VersionUtil.isPaperServer()
                        ? AdventureUtils.MINI_MESSAGE.serialize(newMeta.displayName())
                        : newMeta.getDisplayName()
                        : null;
                if (originalName != null) itemPdc.set(ORIGINAL_NAME_KEY, DataType.STRING, originalName);
            } else { // Set the displayName/customName if it exists on an item before
                if (newMeta.hasDisplayName() && !newMeta.getDisplayName().isEmpty()) {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(newMeta.displayName());
                    else itemMeta.setDisplayName(newMeta.getDisplayName());
                } else {
                    if (VersionUtil.isPaperServer()) itemMeta.displayName(oldMeta.displayName());
                    else itemMeta.setDisplayName(oldMeta.getDisplayName());
                }
            }


            // If the item is not unstackable, we should remove the unstackable tag
            // Also remove it on 1.20.5+ due to maxStackSize component
            if (VersionUtil.atOrAbove("1.20.5") || !newItemBuilder.isUnstackable()) itemPdc.remove(UNSTACKABLE_KEY);
            else itemPdc.set(UNSTACKABLE_KEY, DataType.UUID, UUID.randomUUID());
        });

        Optional.ofNullable(NMSHandlers.getHandler()).ifPresent(nmsHandler ->
            nmsHandler.consumableComponent(newItem, Optional.ofNullable(nmsHandler.consumableComponent(newItem)).orElse(nmsHandler.consumableComponent(oldItem)))
        );

        return newItem;
    }

}
