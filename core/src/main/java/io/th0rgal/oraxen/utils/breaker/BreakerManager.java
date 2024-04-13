package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.custom_block.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.custom_block.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.PacketHelpers;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BreakerManager {

    private final Random SOURCE_RANDOM = new Random();
    private final Map<UUID, ActiveBreakerData> activeBreakerDataMap;

    public BreakerManager(Map<UUID, ActiveBreakerData> activeBreakerDataMap) {
        this.activeBreakerDataMap = activeBreakerDataMap;
    }

    @Nullable
    public ActiveBreakerData activeBreakerData(Player player) {
        return this.activeBreakerDataMap.get(player.getUniqueId());
    }

    public void startBlockBreak(Player player, Block block, CustomBlockMechanic mechanic) {
        OraxenPlugin.get().breakerManager().stopBlockBreak(player);

        Event customBlockEvent;
        if (mechanic instanceof NoteBlockMechanic noteMechanic)
            customBlockEvent = new OraxenNoteBlockDamageEvent(noteMechanic, block, player);
        else if (mechanic instanceof StringBlockMechanic stringMechanic)
            customBlockEvent = new OraxenStringBlockDamageEvent(stringMechanic, block, player);
        else return;
        if (!EventUtils.callEvent(customBlockEvent)) return;

        int breakTime = mechanic.breakTime(player);
        PacketHelpers.applyMiningFatigue(player);
        ActiveBreakerData activeBreakerData = new ActiveBreakerData(player, block.getLocation(), mechanic, breakTime, 0, createBreakScheduler(breakTime, player.getUniqueId()));
        activeBreakerDataMap.put(player.getUniqueId(), activeBreakerData);
    }

    public void stopBlockBreak(Player player) {
        final ActiveBreakerData activeBreakerData = activeBreakerDataMap.get(player.getUniqueId());
        if (activeBreakerData == null) return;

        activeBreakerData.bukkitTask.cancel();
        activeBreakerDataMap.remove(player.getUniqueId());
        if (player.isOnline()) {
            PacketHelpers.removeMiningFatigue(player);
            activeBreakerData.resetProgress();
            activeBreakerData.sendBreakProgress();
        }
    }

    private BukkitTask createBreakScheduler(double blockBreakTime, UUID uuid) {

        return Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () -> {
            final ActiveBreakerData activeBreakerData = this.activeBreakerDataMap.get(uuid);
            Player player = activeBreakerData.breaker;
            final Block block = activeBreakerData.location.getBlock();
            CustomBlockMechanic mechanic = OraxenBlocks.getCustomBlockMechanic(block.getBlockData());

            if (!player.isOnline() || mechanic == null || mechanic.customVariation() != activeBreakerData.mechanic.customVariation()) {
                OraxenPlugin.get().breakerManager().stopBlockBreak(player);
            } else if (!activeBreakerData.isBroken()) {
                activeBreakerData.addBreakTimeProgress(blockBreakTime / mechanic.breakTime(player));
                activeBreakerData.sendBreakProgress();
            } else if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, block.getLocation())) {
                activeBreakerData.resetProgress();
                activeBreakerData.sendBreakProgress();

                for (ActiveBreakerData alterBreakerData : activeBreakerDataMap.values()) {
                    if (alterBreakerData.breaker.getUniqueId().equals(uuid)) continue;
                    if (!alterBreakerData.location.equals(activeBreakerData.location)) continue;

                    OraxenPlugin.get().breakerManager().stopBlockBreak(alterBreakerData.breaker);
                }

                ItemUtils.damageItem(player, mechanic.drop(), player.getInventory().getItemInMainHand());
                block.setType(Material.AIR);
                activeBreakerData.bukkitTask.cancel();
                this.activeBreakerDataMap.remove(uuid);
            }
        }, (long) (blockBreakTime / 11), (long) (blockBreakTime / 11));
    }

    public static class ActiveBreakerData {
        public static final float MAX_DAMAGE = 1f;
        private final int sourceId;
        private final Player breaker;
        private final Location location;
        private final CustomBlockMechanic mechanic;
        private final int totalBreakTime;
        private double breakTimeProgress;
        private final BukkitTask bukkitTask;

        public ActiveBreakerData(
                Player breaker,
                Location location,
                CustomBlockMechanic mechanic,
                int totalBreakTime,
                int breakTimeProgress,
                BukkitTask bukkitTask
        ) {
            this.sourceId = OraxenPlugin.get().breakerManager().SOURCE_RANDOM.nextInt();
            this.breaker = breaker;
            this.location = location;
            this.mechanic = mechanic;
            this.totalBreakTime = totalBreakTime;
            this.breakTimeProgress = breakTimeProgress;
            this.bukkitTask = bukkitTask;
        }

        public int totalBreakTime() {
            return totalBreakTime;
        }

        public double breakTimeProgress() {
            return breakTimeProgress;
        }

        public void breakTimeProgress(double breakTimeProgress) {
            this.breakTimeProgress = breakTimeProgress;
        }

        public void addBreakTimeProgress(double breakTimeProgress) {
            this.breakTimeProgress = Math.min(this.breakTimeProgress + breakTimeProgress, this.totalBreakTime);
        }

        public void sendBreakProgress() {
            breaker.sendBlockDamage(location, calculateDamage(), sourceId);
        }

        public float calculateDamage() {
            final double percentage = this.breakTimeProgress / this.totalBreakTime;
            final double damage = MAX_DAMAGE * percentage;
            return (float) (Math.min(damage, MAX_DAMAGE));
        }

        public boolean isBroken() {
            return breakTimeProgress >= this.totalBreakTime;
        }

        public void resetProgress() {
            this.breakTimeProgress = 0;
        }
    }
}
