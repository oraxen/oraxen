package io.th0rgal.oraxen.utils.breaker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory.getBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.FURNITURE_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener.getStringMechanic;

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

            HardnessModifier triggeredModifier = null;
            for (final HardnessModifier modifier : MODIFIERS)
                if (modifier.isTriggered(player, block, item)) {
                    triggeredModifier = modifier;
                    break;
                }
            if (triggeredModifier == null) return;
            final long period = triggeredModifier.getPeriod(player, block, item);
            if (period == 0) return;
            event.setCancelled(true);

            final Location location = block.getLocation();
            if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                                (int) (period * 11),
                                Integer.MAX_VALUE,
                                false, false, false)));
                BlockHelpers.playCustomBlockSound(block.getLocation(), getSound(block));
                if (breakerPerLocation.containsKey(location))
                    breakerPerLocation.get(location).cancelTasks(OraxenPlugin.get());
                final BukkitScheduler scheduler = Bukkit.getScheduler();
                breakerPerLocation.put(location, scheduler);

                final HardnessModifier modifier = triggeredModifier;
                scheduler.runTaskTimer(OraxenPlugin.get(), new Consumer<>() {
                    int value = 0;

                    @Override
                    public void accept(final BukkitTask bukkitTask) {
                        if (!breakerPerLocation.containsKey(location)) {
                            bukkitTask.cancel();
                            return;
                        }

                        if (item.getEnchantmentLevel(Enchantment.DIG_SPEED) >= 5)
                            value = 10;

                        for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                            if (entity instanceof Player viewer)
                                sendBlockBreak(viewer, location, value);

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
                        for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                            if (entity instanceof Player viewer)
                                sendBlockBreak(viewer, location, 10);
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
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    private void sendBlockBreak(final Player player, final Location location, final int stage) {
        Block block = location.getBlock();
        final PacketContainer fakeAnimation = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        fakeAnimation.getIntegers().write(0, location.hashCode()).write(1, stage);
        fakeAnimation.getBlockPositionModifier().write(0, new BlockPosition(location.toVector()));
        if (!breakerPlaySound.contains(block)) {
            breakerPlaySound.add(block);
            BlockHelpers.playCustomBlockSound(block.getLocation(), getSound(block));
            // Furniture is triggered more often so delay is longer
            if (block.getType() == Material.BARRIER) {
                Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () ->
                        breakerPlaySound.remove(block), 6L, 12L);
            } else {
                Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () ->
                        breakerPlaySound.remove(block), 2L, 4L);
            }
        }

        protocolManager.sendServerPacket(player, fakeAnimation);
    }

    public void registerListener() {
        protocolManager.addPacketListener(listener);
    }

    private String getSound(Block block) {
        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
                return (mechanic != null && mechanic.hasHitSound()) ? mechanic.getHitSound() : "required.wood.hit";
            }
            case MUSHROOM_STEM -> {
                BlockMechanic mechanic = getBlockMechanic(block);
                return (mechanic != null && mechanic.hasHitSound()) ? mechanic.getHitSound() : "required.wood.hit";
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = getStringMechanic(block);
                return (mechanic != null && mechanic.hasHitSound()) ? mechanic.getHitSound() : "block.tripwire.detach";
            }
            case BARRIER -> {
                FurnitureMechanic mechanic = getFurnitureMechanic(block);
                return (mechanic != null && mechanic.hasHitSound()) ? mechanic.getHitSound() : "required.stone.hit";
            }
            default -> {
                return block.getBlockData().getSoundGroup().getHitSound().getKey().toString();
            }
        }
    }

    private FurnitureMechanic getFurnitureMechanic(Block block) {
        if (block.getType() != Material.BARRIER) return null;
        final PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
        if (!customBlockData.has(FURNITURE_KEY, PersistentDataType.STRING)) return null;
        final String mechanicID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(mechanicID);
    }
}
