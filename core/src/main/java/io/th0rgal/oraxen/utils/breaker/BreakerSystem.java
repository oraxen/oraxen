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
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
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
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BreakerSystem {

    public static final List<HardnessModifier> MODIFIERS = new ArrayList<>();
    private final Map<Location, BukkitScheduler> breakerPerLocation = new HashMap<>();
    private final Map<Location, BukkitTask> breakerPlaySound = new HashMap<>();
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

            NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
            StringBlockMechanic stringMechanic = OraxenBlocks.getStringMechanic(block);
            if (block.getType() == Material.NOTE_BLOCK && noteMechanic == null) return;
            if (block.getType() == Material.TRIPWIRE && stringMechanic == null) return;

            event.setCancelled(true);

            final Location location = block.getLocation();
            if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
                // Get these when block is started being broken to minimize checks & allow for proper damage checks later
                final Drop drop;
                if (noteMechanic != null)
                    drop = noteMechanic.drop() != null ? noteMechanic.drop() : Drop.emptyDrop();
                else if (stringMechanic != null)
                    drop = stringMechanic.drop() != null ? stringMechanic.drop() : Drop.emptyDrop();
                else drop = null;

                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                                (int) (period * 11),
                                Integer.MAX_VALUE,
                                false, false, false)));

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
                startBlockHitSound(block);

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
                            if (entity instanceof Player viewer) sendBlockBreak(viewer, location, value);

                        if (value++ < 10) return;
                        if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, location)) {
                            // Damage item with properties identified earlier
                            ItemUtils.damageItem(player, drop, item);
                            modifier.breakBlock(player, block, item);
                        } else stopBlockHitSound(block);

                        Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                                player.removePotionEffect(PotionEffectType.SLOW_DIGGING));

                        stopBlockBreaker(block);
                        stopBlockHitSound(block);
                        for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                            if (entity instanceof Player viewer) sendBlockBreak(viewer, location, value);
                        }
                        bukkitTask.cancel();
                    }
                }, period, period);
            } else {
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                    player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                    if (!ProtectionLib.canBreak(player, location))
                        player.sendBlockChange(block.getLocation(), block.getBlockData());

                    for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                        if (entity instanceof Player viewer)
                            sendBlockBreak(viewer, location, 10);
                    stopBlockBreaker(block);
                    stopBlockHitSound(block);
                });
            }
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
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> Bukkit.getPluginManager().callEvent(event));
                return event.isCancelled();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null) return true;
                OraxenStringBlockDamageEvent event = new OraxenStringBlockDamageEvent(mechanic, block, player);
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> Bukkit.getPluginManager().callEvent(event));
                return event.isCancelled();
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
        final PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        packet.getIntegers().write(0, location.hashCode()).write(1, stage);
        packet.getBlockPositionModifier().write(0, new BlockPosition(location.toVector()));

        protocolManager.sendServerPacket(player, packet);
    }

    private void stopBlockBreaker(Block block) {
        if (breakerPerLocation.containsKey(block.getLocation())) {
            breakerPerLocation.get(block.getLocation()).cancelTasks(OraxenPlugin.get());
            breakerPerLocation.remove(block.getLocation());
        }
    }

    private void startBlockHitSound(Block block) {
        BlockSounds blockSounds = getBlockSounds(block);
        if (blockSounds == null || !blockSounds.hasHitSound()) return;
        breakerPlaySound.put(block.getLocation(), Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(),
                () -> BlockHelpers.playCustomBlockSound(block.getLocation(), getSound(block), blockSounds.getHitVolume(), blockSounds.getHitPitch())
                , 0L, 4L));
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
        ConfigurationSection soundSection = OraxenPlugin.get().configsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (soundSection == null) return null;
        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("noteblock", true)) return null;
                else return mechanic.blockSounds();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("stringblock", true)) return null;
                else return mechanic.blockSounds();
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
            case NOTE_BLOCK -> sounds.hasHitSound() ? sounds.getHitSound() : "required.wood.hit";
            case TRIPWIRE -> sounds.hasHitSound() ? sounds.getHitSound() : "block.tripwire.detach";
            default -> block.getBlockData().getSoundGroup().getHitSound().getKey().toString();
        };
    }
}
