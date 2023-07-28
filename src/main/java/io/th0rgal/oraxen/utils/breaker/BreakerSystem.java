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
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory.getBlockMechanic;

public class BreakerSystem {

    public static final List<HardnessModifier> MODIFIERS = new ArrayList<>();
    private final Map<Location, BukkitScheduler> breakerPerLocation = new HashMap<>();
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
            EnumWrappers.PlayerDigType type;
            try {
                type = data.getValues().get(0);
            } catch (IllegalArgumentException exception) {
                type = EnumWrappers.PlayerDigType.SWAP_HELD_ITEMS;
            }

            final BlockPosition pos = dataTemp.getValues().get(0);
            final World world = player.getWorld();
            final Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            final BlockFace blockFace = dataDirection.size() > 0 ?
                    BlockFace.valueOf(dataDirection.read(0).name()) :
                    BlockFace.UP;

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
            if (block.getType() == Material.TRIPWIRE_HOOK && !OraxenBlocks.isOraxenStringBlock(block)) return;
            if (block.getType() == Material.BARRIER && !OraxenFurniture.isFurniture(block)) return;

            event.setCancelled(true);

            final Location location = block.getLocation();
            if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                                (int) (period * 11),
                                Integer.MAX_VALUE,
                                false, false, false)));
                BlockSounds blockSounds = getBlockSounds(block);
                if (blockSounds != null)
                    BlockHelpers.playCustomBlockSound(block.getLocation(), getSound(block), blockSounds.getHitVolume(), blockSounds.getHitPitch());
                if (breakerPerLocation.containsKey(location))
                    breakerPerLocation.get(location).cancelTasks(OraxenPlugin.get());

                final BukkitScheduler scheduler = Bukkit.getScheduler();
                // Cancellation state is being ignored.
                // However still needs to be called for plugin support.
                final PlayerInteractEvent playerInteractEvent =
                        new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, blockFace, EquipmentSlot.HAND);
                scheduler.runTask(OraxenPlugin.get(), () -> Bukkit.getPluginManager().callEvent(playerInteractEvent));

                // If the relevant damage event is cancelled, return
                if (blockDamageEventCancelled(block, player)) return;

                breakerPerLocation.put(location, scheduler);
                final HardnessModifier modifier = triggeredModifier;

                scheduler.runTaskTimer(OraxenPlugin.get(), new Consumer<>() {
                    int value = 0;

                    @Override
                    public void accept(final BukkitTask bukkitTask) {
                        // Methods for sending multi-barrier block-breaks
                        final FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
                        final Entity furnitureBaseEntity = furnitureMechanic != null ? furnitureMechanic.getBaseEntity(block) : null;
                        final List<Location> furnitureBarrierLocations = furnitureMechanic != null && furnitureBaseEntity != null ? furnitureMechanic.getLocations(FurnitureMechanic.getFurnitureYaw(furnitureBaseEntity), furnitureBaseEntity.getLocation(), furnitureMechanic.getBarriers()) : Collections.singletonList(block.getLocation());

                        if (!breakerPerLocation.containsKey(location)) {
                            bukkitTask.cancel();
                            return;
                        }

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

                        final BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
                        Bukkit.getPluginManager().callEvent(blockBreakEvent);

                        if (!blockBreakEvent.isCancelled() && ProtectionLib.canBreak(player, block.getLocation())) {
                            modifier.breakBlock(player, block, item);
                            PlayerItemDamageEvent playerItemDamageEvent = new PlayerItemDamageEvent(player, item, 1);
                            Bukkit.getPluginManager().callEvent(playerItemDamageEvent);
                        } else breakerPlaySound.remove(block);

                        Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                                player.removePotionEffect(PotionEffectType.SLOW_DIGGING));
                        breakerPerLocation.remove(location);
                        for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                            if (entity instanceof Player viewer) {
                                if (furnitureMechanic != null) {
                                    for (Location barrierLoc : furnitureBarrierLocations)
                                        sendBlockBreak(viewer, barrierLoc, value);
                                } else sendBlockBreak(viewer, location, value);
                            }
                        }
                        bukkitTask.cancel();
                    }
                }, period, period);
            } else {
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                    player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                    if (!ProtectionLib.canBreak(player, block.getLocation()))
                        player.sendBlockChange(block.getLocation(), block.getBlockData());

                    for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                        if (entity instanceof Player viewer)
                            sendBlockBreak(viewer, location, 10);
                    breakerPerLocation.remove(location);
                });
            }
        }
    };

    public BreakerSystem() {
        protocolManager = OraxenPlugin.get().getProtocolManager();
    }

    private boolean blockDamageEventCancelled(Block block, Player player) {

        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return true;
                OraxenNoteBlockDamageEvent event = new OraxenNoteBlockDamageEvent(mechanic, block, player);
                io.th0rgal.oraxen.api.events.OraxenNoteBlockDamageEvent deprecatedEvent = new io.th0rgal.oraxen.api.events.OraxenNoteBlockDamageEvent(mechanic, block, player);
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                    Bukkit.getPluginManager().callEvent(event);
                    Bukkit.getPluginManager().callEvent(deprecatedEvent);
                });
                return event.isCancelled() || deprecatedEvent.isCancelled();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null) return true;
                OraxenStringBlockDamageEvent event = new OraxenStringBlockDamageEvent(mechanic, block, player);
                io.th0rgal.oraxen.api.events.OraxenStringBlockDamageEvent deprecatedEvent = new io.th0rgal.oraxen.api.events.OraxenStringBlockDamageEvent(mechanic, block, player);
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                    Bukkit.getPluginManager().callEvent(event);
                    Bukkit.getPluginManager().callEvent(deprecatedEvent);
                });
                return event.isCancelled() || deprecatedEvent.isCancelled();
            }
            case BARRIER -> {
                try {
                    return Bukkit.getScheduler().callSyncMethod(OraxenPlugin.get(), () -> {
                        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                        if (mechanic == null) return true;
                        Entity baseEntity = mechanic.getBaseEntity(block);
                        if (baseEntity == null) return true;
                        OraxenFurnitureDamageEvent event = new OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block);
                        io.th0rgal.oraxen.api.events.OraxenFurnitureDamageEvent deprecatedEvent = new io.th0rgal.oraxen.api.events.OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block);
                        Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                            Bukkit.getPluginManager().callEvent(event);
                            Bukkit.getPluginManager().callEvent(deprecatedEvent);
                        });
                        return event.isCancelled() || deprecatedEvent.isCancelled();
                    }).get();
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
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                    breakerPlaySound.remove(block), 3L);
        }

        protocolManager.sendServerPacket(player, packet);
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
