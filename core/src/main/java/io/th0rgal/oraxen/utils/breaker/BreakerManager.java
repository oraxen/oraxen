package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.custom_block.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.custom_block.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureDamageEvent;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.BreakableMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.ParseUtils;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
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

    public void startFurnitureBreak(Player player, ItemDisplay baseEntity, FurnitureMechanic mechanic, Block block) {
        OraxenPlugin.get().breakerManager().stopBlockBreak(player);

        OraxenFurnitureDamageEvent damageEvent = new OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block);
        if (!EventUtils.callEvent(damageEvent)) return;

        int breakTime = mechanic.breakable().breakTime(player);
        NMSHandlers.getHandler().applyMiningEffect(player);
        ActiveBreakerData activeBreakerData = new ActiveBreakerData(player, block.getLocation(), mechanic, mechanic.breakable(), breakTime, 0, createBreakScheduler(breakTime, player.getUniqueId()), createBreakSoundScheduler(player.getUniqueId()));
        activeBreakerDataMap.put(player.getUniqueId(), activeBreakerData);
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

        int breakTime = mechanic.breakable().breakTime(player);
        NMSHandlers.getHandler().applyMiningEffect(player);
        ActiveBreakerData activeBreakerData = new ActiveBreakerData(player, block.getLocation(), mechanic, mechanic.breakable(), breakTime, 0, createBreakScheduler(breakTime, player.getUniqueId()), createBreakSoundScheduler(player.getUniqueId()));
        activeBreakerDataMap.put(player.getUniqueId(), activeBreakerData);
    }

    public void stopBlockBreak(Player player) {
        final ActiveBreakerData activeBreakerData = activeBreakerDataMap.get(player.getUniqueId());
        if (activeBreakerData == null) return;

        activeBreakerData.cancelTasks();
        activeBreakerDataMap.remove(player.getUniqueId());
        if (player.isOnline()) {
            NMSHandlers.getHandler().removeMiningEffect(player);
            activeBreakerData.resetProgress();
            activeBreakerData.sendBreakProgress();
        }
    }

    private BukkitTask createBreakScheduler(double blockBreakTime, UUID breakerUUID) {

        return Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () -> {
            final ActiveBreakerData activeBreakerData = this.activeBreakerDataMap.get(breakerUUID);
            if (activeBreakerData == null) return;
            Player player = activeBreakerData.breaker;
            final Block block = activeBreakerData.location.getBlock();
            CustomBlockMechanic blockMechanic = OraxenBlocks.getCustomBlockMechanic(block.getBlockData());
            FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
            BreakableMechanic breakable = blockMechanic != null ? blockMechanic.breakable() : furnitureMechanic != null ? furnitureMechanic.breakable() : null;

            if (!player.isOnline() || breakable == null) {
                OraxenPlugin.get().breakerManager().stopBlockBreak(player);
            } else if (!activeBreakerData.mechanic.equals(blockMechanic) && !activeBreakerData.mechanic.equals(furnitureMechanic)) {
                OraxenPlugin.get().breakerManager().stopBlockBreak(player);
            } else if (blockMechanic != null && activeBreakerData.mechanic instanceof CustomBlockMechanic cm && blockMechanic.customVariation() != cm.customVariation()) {
                OraxenPlugin.get().breakerManager().stopBlockBreak(player);
            } else if (furnitureMechanic != null && activeBreakerData.mechanic instanceof FurnitureMechanic fm && !fm.getItemID().equals(furnitureMechanic.getItemID())) {
                OraxenPlugin.get().breakerManager().stopBlockBreak(player);
            } else if (!activeBreakerData.isBroken()) {
                activeBreakerData.addBreakTimeProgress(blockBreakTime / breakable.breakTime(player));
                activeBreakerData.sendBreakProgress();
            } else if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, block.getLocation())) {
                NMSHandlers.getHandler().removeMiningEffect(player);
                activeBreakerData.resetProgress();
                activeBreakerData.sendBreakProgress();

                for (ActiveBreakerData alterBreakerData : activeBreakerDataMap.values()) {
                    if (alterBreakerData.breaker.getUniqueId().equals(breakerUUID)) continue;
                    if (!alterBreakerData.location.equals(activeBreakerData.location)) continue;

                    OraxenPlugin.get().breakerManager().stopBlockBreak(alterBreakerData.breaker);
                }

                ItemUtils.damageItem(player, breakable.drop(), player.getInventory().getItemInMainHand());
                block.setType(Material.AIR);
                activeBreakerData.cancelTasks();
                this.activeBreakerDataMap.remove(breakerUUID);
            } else OraxenPlugin.get().breakerManager().stopBlockBreak(player);
        }, 1,1);
    }

    private BukkitTask createBreakSoundScheduler(UUID breakerUUID) {
        return Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () -> {
            final ActiveBreakerData activeBreakerData = this.activeBreakerDataMap.get(breakerUUID);
            if (activeBreakerData == null) return;
            Player player = activeBreakerData.breaker;
            final Block block = activeBreakerData.location.getBlock();
            CustomBlockMechanic blockMechanic = OraxenBlocks.getCustomBlockMechanic(block.getBlockData());
            FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);


            if (!player.isOnline() || (blockMechanic == null && furnitureMechanic == null)) {
                OraxenPlugin.get().breakerManager().stopBlockBreak(player);
            } else if (blockMechanic != null) {
                if (!(activeBreakerData.mechanic instanceof CustomBlockMechanic cm)) {
                    OraxenPlugin.get().breakerManager().stopBlockBreak(player);
                } else if (blockMechanic.customVariation() != cm.customVariation()) {
                    OraxenPlugin.get().breakerManager().stopBlockBreak(player);
                } else if (!blockMechanic.hasBlockSounds() || !blockMechanic.blockSounds().hasHitSound()) {
                    activeBreakerData.breakerSoundTask.cancel();
                } else {
                    //TODO Allow for third party blocks to handle this somehow
                    String sound = "";
                    if (blockMechanic.type() == CustomBlockFactory.get().NOTEBLOCK)
                        sound = blockMechanic.hasBlockSounds() && blockMechanic.blockSounds().hasHitSound() ? blockMechanic.blockSounds().getHitSound() : "required.wood.hit";
                    else if (blockMechanic.type() == CustomBlockFactory.get().STRINGBLOCK)
                        sound = blockMechanic.hasBlockSounds() && blockMechanic.blockSounds().hasHitSound() ? blockMechanic.blockSounds().getHitSound() : "block.tripwire.detach";
                    BlockHelpers.playCustomBlockSound(block.getLocation(), sound, blockMechanic.blockSounds().getHitVolume(), blockMechanic.blockSounds().getHitPitch());
                }
            } else {
                if (!(activeBreakerData.mechanic instanceof FurnitureMechanic fm)) {
                    OraxenPlugin.get().breakerManager().stopBlockBreak(player);
                } else if (!furnitureMechanic.getItemID().equals(fm.getItemID())) {
                    activeBreakerData.breakerSoundTask.cancel();
                } else if (furnitureMechanic.hasBlockSounds() && furnitureMechanic.blockSounds().hasHitSound()) {
                    String sound = furnitureMechanic.blockSounds().getHitSound();
                }
            }
        }, 0, 4L);
    }

    public static class ActiveBreakerData {
        public static final float MAX_DAMAGE = 1f;
        public static final float MIN_DAMAGE = 0f;
        private final int sourceId;
        private final Player breaker;
        private final Location location;
        private final Mechanic mechanic;
        private final BreakableMechanic breakable;
        private final int totalBreakTime;
        private double breakTimeProgress;
        private final BukkitTask breakerTask;
        private final BukkitTask breakerSoundTask;

        public ActiveBreakerData(
                Player breaker,
                Location location,
                Mechanic mechanic,
                BreakableMechanic breakable,
                int totalBreakTime,
                int breakTimeProgress,
                BukkitTask breakerTask,
                BukkitTask breakerSoundTask
        ) {
            this.sourceId = OraxenPlugin.get().breakerManager().SOURCE_RANDOM.nextInt();
            this.breaker = breaker;
            this.location = location;
            this.mechanic = mechanic;
            this.breakable = breakable;
            this.totalBreakTime = totalBreakTime;
            this.breakTimeProgress = breakTimeProgress;
            this.breakerTask = breakerTask;
            this.breakerSoundTask = breakerSoundTask;
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
            final float damage = (float) (MAX_DAMAGE * percentage);
            return ParseUtils.clamp(damage, MIN_DAMAGE, MAX_DAMAGE);
        }

        public boolean isBroken() {
            return breakTimeProgress >= this.totalBreakTime;
        }

        public void resetProgress() {
            this.breakTimeProgress = 0;
        }

        public void cancelTasks() {
            if (breakerTask != null) breakerTask.cancel();
            if (breakerSoundTask != null) breakerSoundTask.cancel();
        }
    }
}
