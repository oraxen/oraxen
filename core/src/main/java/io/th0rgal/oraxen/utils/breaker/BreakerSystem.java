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
import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
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
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BreakerSystem {

    public static final List<HardnessModifier> MODIFIERS = new ArrayList<>();
    private final Map<Location, WrappedTask> breakerPerLocation = new HashMap<>();
    private final List<Block> breakerPlaySound = new ArrayList<>();
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
            final EnumWrappers.PlayerDigType type = data.getValues().size() > 0 ?
                    data.getValues().get(0) : EnumWrappers.PlayerDigType.SWAP_HELD_ITEMS;

            final BlockPosition pos = dataTemp.getValues().get(0);
            final World world = player.getWorld();
            final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            final BlockFace blockFace = dataDirection.size() > 0 ?
                    BlockFace.valueOf(dataDirection.read(0).name()) :
                    BlockFace.UP;

            OraxenPlugin.foliaLib.getImpl().runAtLocation(block.getLocation(), (w) -> {
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
                if (block.getType() == Material.NOTE_BLOCK && !OraxenBlocks.isOraxenNoteBlock(block)) return;
                if (block.getType() == Material.TRIPWIRE && !OraxenBlocks.isOraxenStringBlock(block)) return;
                if (block.getType() == Material.BARRIER && !OraxenFurniture.isFurniture(block)) return;


                event.setCancelled(true);

                final Location location = block.getLocation();
                if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                    OraxenPlugin.foliaLib.getImpl().runAtEntity(player, (ww) -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                            (int) (period * 11),
                            Integer.MAX_VALUE,
                            false, false, false)));
                    BlockSounds blockSounds = getBlockSounds(block);
                    if (blockSounds != null)
                        BlockHelpers.playCustomBlockSound(block.getLocation(), getSound(block), blockSounds.getHitVolume(), blockSounds.getHitPitch());
                    if (breakerPerLocation.containsKey(location))
                        breakerPerLocation.get(location).cancel();

                    // Cancellation state is being ignored.
                    // However still needs to be called for plugin support.
                    final PlayerInteractEvent playerInteractEvent =
                            new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, blockFace, EquipmentSlot.HAND);
                    OraxenPlugin.foliaLib.getImpl().runNextTick((ww) -> Bukkit.getPluginManager().callEvent(playerInteractEvent));

                    // If the relevant damage event is cancelled, return
                    if (blockDamageEventCancelled(block, player)) return;

                    final HardnessModifier modifier = triggeredModifier;

                    breakerPerLocation.put(location, OraxenPlugin.foliaLib.getImpl().runAtLocationTimer(location, () -> {
                        int value = 0;

                        if (breakerPerLocation.containsKey(location))  {
                            final FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
                            final Entity furnitureBaseEntity = furnitureMechanic != null ? furnitureMechanic.getBaseEntity(block) : null;
                            final List<Location> furnitureBarrierLocations = furnitureMechanic != null && furnitureBaseEntity != null ? furnitureMechanic.getLocations(FurnitureMechanic.getFurnitureYaw(furnitureBaseEntity), furnitureBaseEntity.getLocation(), furnitureMechanic.getBarriers()) : Collections.singletonList(block.getLocation());

                            if (item.getEnchantmentLevel(Enchantment.DIG_SPEED) >= 5)
                                value = 10;

                            for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                                if (entity instanceof Player viewer) {
                                    if (furnitureMechanic != null) {
                                        for (Location barrierLoc : furnitureBarrierLocations)
                                            sendBlockBreak(viewer, barrierLoc, value);
                                    } else sendBlockBreak(viewer, location, value);
                                }

                            if (value++ < 10) return;

                            if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, block.getLocation())) {
                                modifier.breakBlock(player, block, item);
                                EventUtils.callEvent(new PlayerItemDamageEvent(player, item, 1));
                            } else breakerPlaySound.remove(block);

                            OraxenPlugin.foliaLib.getImpl().runAtEntity(player, (ww) -> player.removePotionEffect(PotionEffectType.SLOW_DIGGING));
                            for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                                if (entity instanceof Player viewer) {
                                    if (furnitureMechanic != null) {
                                        for (Location barrierLoc : furnitureBarrierLocations)
                                            sendBlockBreak(viewer, barrierLoc, value);
                                    } else sendBlockBreak(viewer, location, value);
                                }
                            }

                            if (VersionUtil.isPaperServer()) item.damage(1, player);
                            else ItemUtils.editItemMeta(item, meta -> {
                                if (meta instanceof Damageable damageable)
                                    damageable.setDamage(damageable.getDamage() + 1);
                            });

                            breakerPerLocation.remove(location).cancel();
                        }
                    }, period, period));
                } else {
                    OraxenPlugin.foliaLib.getImpl().runAtEntity(player, (ww) -> {
                        player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                        if (!ProtectionLib.canBreak(player, block.getLocation()))
                            player.sendBlockChange(block.getLocation(), block.getBlockData());

                        for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                            if (entity instanceof Player viewer)
                                sendBlockBreak(viewer, location, 10);
                        breakerPerLocation.remove(location).cancel();
                    });
                }
            });
        }
    };

    public BreakerSystem() {
        protocolManager = OraxenPlugin.get().protocolManager();
    }

    private boolean blockDamageEventCancelled(Block block, Player player) {

        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return true;
                OraxenNoteBlockDamageEvent event = new OraxenNoteBlockDamageEvent(mechanic, block, player);
                OraxenPlugin.foliaLib.getImpl().runNextTick((w) -> Bukkit.getPluginManager().callEvent(event));
                return event.isCancelled();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null) return true;
                OraxenStringBlockDamageEvent event = new OraxenStringBlockDamageEvent(mechanic, block, player);
                OraxenPlugin.foliaLib.getImpl().runNextTick((w) -> Bukkit.getPluginManager().callEvent(event));
                return event.isCancelled();
            }
            case BARRIER -> {
                try {
                    return false; //return Bukkit.getScheduler().callSyncMethod(OraxenPlugin.get(), ).get();
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
        if (!breakerPlaySound.contains(block)) {
            breakerPlaySound.add(block);
            BlockSounds blockSounds = getBlockSounds(block);
            if (blockSounds != null && blockSounds.hasHitSound())
                BlockHelpers.playCustomBlockSound(block.getLocation(), getSound(block), blockSounds.getHitVolume(), blockSounds.getHitPitch());
            OraxenPlugin.foliaLib.getImpl().runLater(() -> breakerPlaySound.remove(block), 3L);
        }

        protocolManager.sendServerPacket(player, packet);
    }

    public void registerListener() {
        //protocolManager.addPacketListener(listener);
    }

    private BlockSounds getBlockSounds(Block block) {
        ConfigurationSection soundSection = OraxenPlugin.get().configsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (soundSection == null) return null;
        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
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
        ConfigurationSection soundSection = OraxenPlugin.get().configsManager().getMechanics().getConfigurationSection("custom_block_sounds");
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
