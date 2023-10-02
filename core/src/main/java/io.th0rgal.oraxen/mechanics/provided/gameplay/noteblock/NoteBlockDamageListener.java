package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NoteBlockDamageListener implements Listener {

    private final Map<Location, WrappedTask> breakerPerLocation = new HashMap<>();
    private final List<Location> breakerPlaySound = new ArrayList<>();

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        ItemStack item = event.getItemInHand();

        HardnessModifier modifier = getHardnessModifier().isTriggered(player, block, item) ? getHardnessModifier() : null;
        if (modifier == null) return;
        final long period = modifier.getPeriod(player, block, item);
        if (period == 0) return;

        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return;
        event.setCancelled(true);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                (int) (period * 11),
                Integer.MAX_VALUE,
                false, false, false));
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds != null && blockSounds.hasHitSound())
            BlockHelpers.playCustomBlockSound(location, blockSounds.getHitSound(), blockSounds.getHitVolume(), blockSounds.getHitPitch());
        if (breakerPerLocation.containsKey(location))
            breakerPerLocation.get(location).cancel();


        OraxenNoteBlockDamageEvent damageEvent = new OraxenNoteBlockDamageEvent(mechanic, block, player);
        Bukkit.getPluginManager().callEvent(damageEvent);
        if (damageEvent.isCancelled()) return;

        AtomicInteger value = new AtomicInteger(0);
        breakerPerLocation.put(location, OraxenPlugin.foliaLib.getImpl().runAtLocationTimer(location, () -> {

            if (breakerPerLocation.containsKey(block.getLocation())) {
                if (item.getEnchantmentLevel(Enchantment.DIG_SPEED) >= 5) value.set(10);

                for (Entity entity : block.getWorld().getNearbyEntities(location, 16,16,16)) {
                    if (entity instanceof Player viewer) {
                        float damage = Math.min((float) value.intValue() / 10, 1.0f);
                        viewer.sendBlockDamage(block.getLocation(), damage, location.hashCode());
                        if (!breakerPlaySound.contains(location) && blockSounds != null && blockSounds.hasHitSound()) {
                            BlockHelpers.playCustomBlockSound(location, blockSounds.getHitSound(), blockSounds.getHitVolume(), blockSounds.getHitPitch());
                            breakerPlaySound.add(location);
                            OraxenPlugin.foliaLib.getImpl().runLater(() -> breakerPlaySound.remove(location), 10L);
                        }
                    }
                }

                value.set(value.intValue() + 1);
                if (value.intValue() >= 10) {
                    BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
                    Bukkit.getPluginManager().callEvent(blockBreakEvent);

                    if (blockBreakEvent.isCancelled()) breakerPlaySound.remove(location);
                    else {
                        getHardnessModifier().breakBlock(player, block, item);
                        PlayerItemDamageEvent playerItemDamageEvent = new PlayerItemDamageEvent(player, item, 1);
                        Bukkit.getPluginManager().callEvent(playerItemDamageEvent);
                    }

                    player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
                    for (Entity entity : block.getWorld().getNearbyEntities(location, 16,16,16)) {
                        if (entity instanceof Player viewer) {
                            float damage = Math.min((float) value.intValue() / 10, 1.0f);
                            viewer.sendBlockDamage(block.getLocation(), damage, location.hashCode());
                            if (!breakerPlaySound.contains(location) && blockSounds != null && blockSounds.hasHitSound()) {
                                BlockHelpers.playCustomBlockSound(location, blockSounds.getHitSound(), blockSounds.getHitVolume(), blockSounds.getHitPitch());
                                breakerPlaySound.add(location);
                                OraxenPlugin.foliaLib.getImpl().runLater(() -> breakerPlaySound.remove(location), 10L);
                            }
                        }
                    }
                    breakerPerLocation.remove(block.getLocation());
                }
            }
        }, period, period));
    }

    @EventHandler
    public void onStopDamage(BlockDamageAbortEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
        if (!ProtectionLib.canBreak(player, block.getLocation()))
            player.sendBlockChange(block.getLocation(), block.getBlockData());

        for (final Entity entity : block.getWorld().getNearbyEntities(location, 16, 16, 16))
            if (entity instanceof Player viewer)
                viewer.sendBlockDamage(location, 0, location.hashCode());
        WrappedTask task = breakerPerLocation.remove(location);
        if (task != null) task.cancel();
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return false;

                if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
                    mechanic = mechanic.getDirectional().getParentMechanic();

                return mechanic.hasHardness();
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return 0;
                if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
                    mechanic = mechanic.getDirectional().getParentMechanic();

                final long period = mechanic.getHardness();
                double modifier = 1;
                if (mechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = mechanic.getDrop().getDiff(tool);
                    if (diff >= 1)
                        modifier *= Math.pow(0.9, diff);
                }
                return (long) (period * modifier);
            }
        };
    }
}
