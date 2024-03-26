package io.th0rgal.oraxen.utils.breaker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import fr.euphyllia.energie.Energie;
import fr.euphyllia.energie.model.Scheduler;
import fr.euphyllia.energie.model.SchedulerTaskInter;
import fr.euphyllia.energie.model.SchedulerType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureDamageEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory.getBlockMechanic;

public class BreakerSystem {

    public static final List<HardnessModifier> MODIFIERS = new ArrayList<>();
    private final Map<Location, Scheduler> breakerPerLocation = new HashMap<>();
    private final Map<Location, SchedulerTaskInter> breakerPlaySound = new HashMap<>();
    private final ProtocolManager protocolManager;
    private final PacketAdapter listener = new PacketAdapter(OraxenPlugin.get(),
            ListenerPriority.LOW, PacketType.Play.Client.BLOCK_DIG) {
        @Override
        public void onPacketReceiving(final PacketEvent event) {
            final PacketContainer packet = event.getPacket();
            final Player player = event.getPlayer();
            final ItemStack item = player.getInventory().getItemInMainHand();
            if (player.getGameMode() == GameMode.CREATIVE) return;

            final StructureModifier<BlockPosition> dataTemp = packet.getBlockPositionModifier();
            final StructureModifier<EnumWrappers.Direction> dataDirection = packet.getDirections();
            final StructureModifier<EnumWrappers.PlayerDigType> data = packet
                    .getEnumModifier(EnumWrappers.PlayerDigType.class, 2);
            AtomicReference<EnumWrappers.PlayerDigType> type = new AtomicReference<>();
            try {
                type.set(data.getValues().get(0));
            } catch (IllegalArgumentException exception) {
                type.set(EnumWrappers.PlayerDigType.SWAP_HELD_ITEMS);
            }

            final BlockPosition pos = dataTemp.getValues().get(0);
            final World world = player.getWorld();
            final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            final BlockFace blockFace = dataDirection.size() > 0 ?
                    BlockFace.valueOf(dataDirection.read(0).name()) :
                    BlockFace.UP;
            final Location location = block.getLocation();
            OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, location, regionTask -> {
                HardnessModifier triggeredModifier = null;
                for (final HardnessModifier modifier : MODIFIERS) {
                    if (modifier.isTriggered(player, block, item)) {
                        triggeredModifier = modifier;
                        break;
                    }
                }
                if (triggeredModifier == null) return;
                final long period = triggeredModifier.getPeriod(player, block, item);
                if (period == 0) return;

                NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
                StringBlockMechanic stringMechanic = OraxenBlocks.getStringMechanic(block);
                FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (block.getType() == Material.NOTE_BLOCK && noteMechanic == null) return;
                if (block.getType() == Material.TRIPWIRE && stringMechanic == null) return;
                if (block.getType() == Material.BARRIER && furnitureMechanic == null) return;

                event.setCancelled(true);

                if (type.get() == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                    // Get these when block is started being broken to minimize checks & allow for proper damage checks later
                    final Drop drop;
                    if (furnitureMechanic != null)
                        drop = furnitureMechanic.getDrop() != null ? furnitureMechanic.getDrop() : Drop.emptyDrop();
                    else if (noteMechanic != null)
                        drop = noteMechanic.getDrop() != null ? noteMechanic.getDrop() : Drop.emptyDrop();
                    else if (stringMechanic != null)
                        drop = stringMechanic.getDrop() != null ? stringMechanic.getDrop() : Drop.emptyDrop();
                    else drop = null;

                    // Methods for sending multi-barrier block-breaks
                    final List<Location> furnitureBarrierLocations = furnitureBarrierLocations(furnitureMechanic, block);

                    OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, player, playerTask ->
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                                    (int) (period * 11),
                                    Integer.MAX_VALUE,
                                    false, false, false)), null);

                    if (breakerPerLocation.containsKey(location))
                        breakerPerLocation.get(location).cancelAllTask();

                    final Scheduler scheduler = OraxenPlugin.getScheduler();
                    // Cancellation state is being ignored.
                    // However still needs to be called for plugin support.
                    final PlayerInteractEvent playerInteractEvent =
                            new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, blockFace, EquipmentSlot.HAND);
                    scheduler.runTask(SchedulerType.SYNC, player, task -> Bukkit.getPluginManager().callEvent(playerInteractEvent), null);

                    // If the relevant damage event is cancelled, return
                    if (blockDamageEventCancelled(block, player)) return;

                    breakerPerLocation.put(location, scheduler);
                    final HardnessModifier modifier = triggeredModifier;
                    startBlockHitSound(block);
                    AtomicInteger value = new AtomicInteger();
                    scheduler.runAtFixedRate(SchedulerType.SYNC, location, regionTask2 ->  {
                        if (regionTask2 == null) return;
                        if (!breakerPerLocation.containsKey(location)) {
                            regionTask2.cancel();
                            return;
                        }

                        if (item.getEnchantmentLevel(Enchantment.DIG_SPEED) >= 5)
                            value.set(10);

                        for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                            if (entity instanceof Player viewer) {
                                if (furnitureMechanic != null) for (Location barrierLoc : furnitureBarrierLocations)
                                    sendBlockBreak(viewer, barrierLoc, value.get());
                                else sendBlockBreak(viewer, location, value.get());
                            }
                        }

                        if (value.getAndIncrement() < 10) return;
                        if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, location)) {
                            // Damage item with properties identified earlier
                            ItemUtils.damageItem(player, drop, item);
                            modifier.breakBlock(player, block, item);
                        } else stopBlockHitSound(block);

                        OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, player, task ->
                                player.removePotionEffect(PotionEffectType.SLOW_DIGGING), null);

                        stopBlockBreaker(block);
                        stopBlockHitSound(block);
                        for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                            if (entity instanceof Player viewer) {
                                if (furnitureMechanic != null) for (Location barrierLoc : furnitureBarrierLocations)
                                    sendBlockBreak(viewer, barrierLoc, value.get());
                                else sendBlockBreak(viewer, location, value.get());
                            }
                        }
                        regionTask2.cancel();
                    }, period, period);
                }
                else {
                    OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, player, task -> {
                        player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                        if (!ProtectionLib.canBreak(player, location))
                            player.sendBlockChange(block.getLocation(), block.getBlockData());
                        OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, location, regionTask2 -> {
                            for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                                if (entity instanceof Player viewer)
                                    sendBlockBreak(viewer, location, 10);
                            stopBlockBreaker(block);
                            stopBlockHitSound(block);
                        });

                    }, null);
                }
            });
        }
    };

    private List<Location> furnitureBarrierLocations(FurnitureMechanic furnitureMechanic, Block block) {
        AtomicReference<Entity> furnitureBaseEntity = new AtomicReference<>();
        Entity entity = furnitureMechanic != null ? furnitureMechanic.getBaseEntity(block) : null;
        if (entity != null) {
            OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, entity, task -> furnitureBaseEntity.set(entity), null);
        }
        return furnitureMechanic != null && furnitureBaseEntity.get() != null
                ? furnitureMechanic.getLocations(FurnitureMechanic.getFurnitureYaw(furnitureBaseEntity.get()),
                furnitureBaseEntity.get().getLocation(), furnitureMechanic.getBarriers())
                : Collections.singletonList(block.getLocation());
    }

    public BreakerSystem() {
        protocolManager = OraxenPlugin.get().getProtocolManager();
    }

    private boolean blockDamageEventCancelled(Block block, Player player) {

        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return true;
                OraxenNoteBlockDamageEvent event = new OraxenNoteBlockDamageEvent(mechanic, block, player);
                OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, taskInter -> Bukkit.getPluginManager().callEvent(event));
                return event.isCancelled();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null) return true;
                OraxenStringBlockDamageEvent event = new OraxenStringBlockDamageEvent(mechanic, block, player);
                OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, taskInter -> Bukkit.getPluginManager().callEvent(event));
                return event.isCancelled();
            }
            case BARRIER -> {
                try {
                    if (!Energie.isFolia()) {
                        return Bukkit.getScheduler().callSyncMethod(OraxenPlugin.get(), () -> {
                            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                            if (mechanic == null) {
                                return true;
                            }
                            Entity baseEntity = mechanic.getBaseEntity(block);
                            if (baseEntity == null) {
                                return true;
                            }
                            OraxenFurnitureDamageEvent event = new OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block);
                            Bukkit.getPluginManager().callEvent(event);
                            return event.isCancelled();
                        }).get();
                    } else {
                        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                        if (mechanic == null) {
                            return true;
                        }
                        Entity baseEntity = mechanic.getBaseEntity(block);
                        if (baseEntity == null) {
                            return true;
                        }
                        OraxenFurnitureDamageEvent event = new OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block);
                        Bukkit.getPluginManager().callEvent(event);
                        return event.isCancelled();
                    }

                } catch (Exception e) {
                    return false;
                }
            }
            case BEDROCK -> { // For BedrockBreakMechanic
                return false;
            }
            default -> {
                return true;
            }
        }
    }

    private void sendBlockBreak(final Player player, final Location location, final int stage) {
        Block block = location.getBlock();
        final PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        packet.getIntegers().write(0, location.hashCode()).write(1, stage);
        packet.getBlockPositionModifier().write(0, new BlockPosition(location.toVector()));

        protocolManager.sendServerPacket(player, packet);
    }

    private void stopBlockBreaker(Block block) {
        if (breakerPerLocation.containsKey(block.getLocation())) {
            breakerPerLocation.get(block.getLocation()).cancelAllTask();
            breakerPerLocation.remove(block.getLocation());
        }
    }

    private void startBlockHitSound(Block block) {
        BlockSounds blockSounds = getBlockSounds(block);
        if (blockSounds == null || !blockSounds.hasHitSound()) return;
        Location blockLocation = block.getLocation();
        breakerPlaySound.put(blockLocation, OraxenPlugin.getScheduler().runAtFixedRate(SchedulerType.SYNC, blockLocation, taskInter -> {
            BlockHelpers.playCustomBlockSound(blockLocation, getSound(block), blockSounds.getHitVolume(), blockSounds.getHitPitch());
            }, 0L, 4L));
    }

    private void stopBlockHitSound(Block block) {
        if (breakerPlaySound.containsKey(block.getLocation())) {
            breakerPlaySound.get(block.getLocation()).cancel();
            breakerPlaySound.remove(block.getLocation());
        }
    }

    public void registerListener() {
        protocolManager.addPacketListener(listener);
    }

    private BlockSounds getBlockSounds(Block block) {
        ConfigurationSection soundSection = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (soundSection == null) return null;
        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("noteblock_and_block")) return null;
                else return mechanic.getBlockSounds();
            }
            case MUSHROOM_STEM -> {
                BlockMechanic mechanic = getBlockMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("noteblock_and_block")) return null;
                else return mechanic.getBlockSounds();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("stringblock_and_furniture")) return null;
                else return mechanic.getBlockSounds();
            }
            case BARRIER -> {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("stringblock_and_furniture")) return null;
                else return mechanic.getBlockSounds();
            }
            default -> {
                return null;
            }
        }
    }

    private String getSound(Block block) {
        ConfigurationSection soundSection = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (soundSection == null) return null;
        BlockSounds sounds = getBlockSounds(block);
        if (sounds == null) return null;
        return switch (block.getType()) {
            case NOTE_BLOCK, MUSHROOM_STEM -> sounds.hasHitSound() ? sounds.getHitSound() : "required.wood.hit";
            case TRIPWIRE -> sounds.hasHitSound() ? sounds.getHitSound() : "block.tripwire.detach";
            case BARRIER -> sounds.hasHitSound() ? sounds.getHitSound() : "required.stone.hit";
            default -> block.getBlockData().getSoundGroup().getHitSound().getKey().toString();
        };
    }
}
