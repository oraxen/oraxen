package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.custom_block.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.custom_block.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class CustomBlockMiningListener implements Listener {

    private final Map<UUID, BukkitTask> breakerPerLocation = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageCustomBlock(BlockDamageEvent event) {
        final Block block = event.getBlock();
        final Location location = block.getLocation();
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        CustomBlockMechanic mechanic = OraxenBlocks.getCustomBlockMechanic(block.getBlockData());
        if (mechanic == null || player.getGameMode() == GameMode.CREATIVE) return;
        double breakTime = mechanic.breakTime(player);
        if (breakTime == 0L) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, (int) breakTime, Integer.MAX_VALUE,
                false, false, false));

        if (breakerPerLocation.containsKey(uuid))
            breakerPerLocation.get(uuid).cancel();

        Event customBlockEvent;
        if (mechanic instanceof NoteBlockMechanic noteMechanic)
            customBlockEvent = new OraxenNoteBlockDamageEvent(noteMechanic, block, player);
        else if (mechanic instanceof StringBlockMechanic stringMechanic)
            customBlockEvent = new OraxenStringBlockDamageEvent(stringMechanic, block, player);
        else return;
        if (!EventUtils.callEvent(customBlockEvent)) return;


        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), new Consumer<BukkitTask>() {
            int value = 0;

            @Override
            public void accept(final BukkitTask bukkitTask) {
                if (!breakerPerLocation.containsKey(player.getUniqueId())) {
                    bukkitTask.cancel();
                    return;
                }

                for (final Entity entity : block.getWorld().getNearbyEntities(location, 16, 16, 16))
                    if (entity instanceof Player viewer) viewer.sendBlockDamage(location, value, location.hashCode());

                if (value++ < 10) return;
                if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, location)) {
                    player.breakBlock(block);
                }// else stopBlockHitSound(block);

                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                        player.removePotionEffect(PotionEffectType.SLOW_DIGGING));

                stopBlockBreaker(player);
                //stopBlockHitSound(block);
                for (final Entity entity : block.getWorld().getNearbyEntities(location, 16, 16, 16)) {
                    if (entity instanceof Player viewer) viewer.sendBlockDamage(location, 10, location.hashCode());
                }
                bukkitTask.cancel();
            }
        }, breakTime, breakTime);
        breakerPerLocation.put(player.getUniqueId(), task);

        event.setCancelled(true);
        Logs.debug(breakTime);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageAbort(BlockDamageAbortEvent event) {
        stopBlockBreaker(event.getPlayer());
    }

    private void stopBlockBreaker(Player player) {
        UUID uuid = player.getUniqueId();
        if (breakerPerLocation.containsKey(uuid)) {
            breakerPerLocation.get(uuid).cancel();
            breakerPerLocation.remove(uuid);
        }
    }
}
