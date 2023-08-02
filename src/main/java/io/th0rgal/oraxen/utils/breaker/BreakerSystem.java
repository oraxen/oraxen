package io.th0rgal.oraxen.utils.breaker;

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
import io.th0rgal.oraxen.protocol.MinecraftVersion;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.ProtocolMapping;
import io.th0rgal.oraxen.protocol.handler.NMSPacketHandler;
import io.th0rgal.oraxen.protocol.packet.BlockDestroyStage;
import io.th0rgal.oraxen.protocol.packet.PacketFlow;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.CacheInvoker;
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory.getBlockMechanic;

public class BreakerSystem implements NMSPacketHandler {

    private static final ProtocolMapping<Integer> PLAYER_ACTION_ID_MAPPING =
            new ProtocolMapping<Integer>()
                    .add(MinecraftVersion.MINECRAFT_1_8, 0x07)
                    .add(MinecraftVersion.MINECRAFT_1_9, 0x13)
                    .add(MinecraftVersion.MINECRAFT_1_12, 0x14)
                    .add(MinecraftVersion.MINECRAFT_1_13, 0x18)
                    .add(MinecraftVersion.MINECRAFT_1_14, 0x1A)
                    .add(MinecraftVersion.MINECRAFT_1_16, 0x1B)
                    .add(MinecraftVersion.MINECRAFT_1_17, 0x1A)
                    .add(MinecraftVersion.MINECRAFT_1_19, 0x1C)
                    .add(MinecraftVersion.MINECRAFT_1_19_1, 0x1D)
                    .add(MinecraftVersion.MINECRAFT_1_19_3, 0x1C)
                    .add(MinecraftVersion.MINECRAFT_1_19_4, 0x1D)
                    .build();

    private static final CacheInvoker INVOKER = CacheInvoker.get();

    public static final List<HardnessModifier> MODIFIERS = new ArrayList<>();
    private final Map<Location, BukkitScheduler> breakerPerLocation = new HashMap<>();
    private final List<Block> breakerPlaySound = new ArrayList<>();
    private final ProtocolInjector protocolInjector;
    private final int playerActionId;
    private MethodHandle positionGetter;
    private MethodHandle[] positionFieldGetters;
    private MethodHandle directionGetter;
    private MethodHandle directionNameGetter;
    private MethodHandle statusGetter;

    public BreakerSystem() {
        protocolInjector = OraxenPlugin.get().getProtocolInjector();
        playerActionId = PLAYER_ACTION_ID_MAPPING.get(protocolInjector.getServerVersion());
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
        if (!breakerPlaySound.contains(block)) {
            breakerPlaySound.add(block);
            BlockSounds blockSounds = getBlockSounds(block);
            if (blockSounds != null && blockSounds.hasHitSound())
                BlockHelpers.playCustomBlockSound(block.getLocation(), getSound(block), blockSounds.getHitVolume(), blockSounds.getHitPitch());
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () ->
                    breakerPlaySound.remove(block), 3L);
        }

        protocolInjector.sendPacket(player, new BlockDestroyStage(location.hashCode(), location, stage));
    }

    public void registerListener() {
        protocolInjector.addNMSHandler(this, PacketFlow.SERVERBOUND, playerActionId);
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

    @Override
    public void initialize() {
        try {
            Class<?> packetClass = protocolInjector.getNMSPacketClass(PacketFlow.SERVERBOUND, playerActionId);
            Class<?> actionClass = packetClass.getClasses()[0];
            MethodHandles.Lookup packetLookup = MethodHandles.privateLookupIn(packetClass, MethodHandles.lookup());
            for (Field field : packetClass.getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (fieldType == actionClass && statusGetter == null) {
                    statusGetter = packetLookup.unreflectGetter(field);
                } else if (fieldType.isEnum() && directionGetter == null) {
                    Class<?> directionClass = field.getType();
                    directionGetter = packetLookup.unreflectGetter(field);
                    for (Field directionField : directionClass.getDeclaredFields()) {
                        if (directionField.getType() != String.class) {
                            continue;
                        }

                        directionNameGetter = MethodHandles.privateLookupIn(directionClass, MethodHandles.lookup())
                                .unreflectGetter(directionField);
                        break;
                    }
                } else if (!fieldType.isPrimitive() && positionGetter == null) {
                    Class<?> positionClass = field.getType();
                    MethodHandles.Lookup positionLookup = MethodHandles.privateLookupIn(positionClass, MethodHandles.lookup());
                    positionGetter = packetLookup.unreflectGetter(field);
                    positionFieldGetters = new MethodHandle[3];
                    int currentField = 0;
                    for (Field positionField : positionClass.getSuperclass().getDeclaredFields()) {
                        if (Modifier.isStatic(positionField.getModifiers())) {
                            continue;
                        }

                        if (positionField.getType() != int.class) {
                            continue;
                        }

                        positionField.setAccessible(true);
                        positionFieldGetters[currentField++] = positionLookup.unreflectGetter(positionField);
                        if (currentField == 3) {
                            break;
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean handle(Player player, PacketFlow flow, int id, Object packet) {
        try {
            final ItemStack item = player.getInventory().getItemInMainHand();
            if (player.getGameMode() == GameMode.CREATIVE) {
                return true;
            }


            Enum<?> status = (Enum<?>) INVOKER.cache(statusGetter).invoke(packet);
            int statusId = status.ordinal();

            final World world = player.getWorld();
            Object position = INVOKER.cache(positionGetter).invoke(packet);
            final Block block = world.getBlockAt(
                    (int) INVOKER.cache(positionFieldGetters[0]).invoke(position),
                    (int) INVOKER.cache(positionFieldGetters[1]).invoke(position),
                    (int) INVOKER.cache(positionFieldGetters[2]).invoke(position)
            );

            Object direction = INVOKER.cache(directionGetter).invoke(packet);
            String directionName = (String) INVOKER.cache(directionNameGetter).invoke(direction);
            final BlockFace blockFace = BlockFace.valueOf(directionName.toUpperCase());

            HardnessModifier triggeredModifier = null;
            for (final HardnessModifier modifier : MODIFIERS) {
                if (modifier.isTriggered(player, block, item)) {
                    triggeredModifier = modifier;
                    break;
                }
            }
            if (triggeredModifier == null) {
                return true;
            }

            final long period = triggeredModifier.getPeriod(player, block, item);

            if (period == 0) {
                return true;
            }

            if (block.getType() == Material.NOTE_BLOCK && !OraxenBlocks.isOraxenNoteBlock(block)) {
                return true;
            }

            if (block.getType() == Material.TRIPWIRE_HOOK && !OraxenBlocks.isOraxenStringBlock(block)) {
                return true;
            }

            if (block.getType() == Material.BARRIER && !OraxenFurniture.isFurniture(block)) {
                return true;
            }

            final Location location = block.getLocation();
            if (statusId == 0) {
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
                if (blockDamageEventCancelled(block, player)) {
                    return true;
                }

                breakerPerLocation.put(location, scheduler);
                final HardnessModifier modifier = triggeredModifier;

                scheduler.runTaskTimer(OraxenPlugin.get(), new Consumer<>() {
                    int value = 0;

                    @Override
                    public void accept(final BukkitTask bukkitTask) {
                        // Methods for sending multi-barrier block-breaks
                        final FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
                        final Entity furnitureBaseEntity = furnitureMechanic != null ? furnitureMechanic.getBaseEntity(block) : null;
                        final List<Location> furnitureBarrierLocations =
                                furnitureMechanic != null && furnitureBaseEntity != null
                                        ? furnitureMechanic.getLocations(
                                                FurnitureMechanic.getFurnitureYaw(furnitureBaseEntity),
                                                furnitureBaseEntity.getLocation(),
                                                furnitureMechanic.getBarriers()
                                        )
                                        : Collections.singletonList(block.getLocation());

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

            return false;
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        }

        return true;
    }
}
