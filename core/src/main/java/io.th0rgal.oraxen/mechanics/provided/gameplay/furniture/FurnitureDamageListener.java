package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureDamageEvent;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FurnitureDamageListener implements Listener {

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

        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        Entity furnitureEntity = furnitureMechanic != null ? furnitureMechanic.getBaseEntity(block) : null;
        if (furnitureMechanic == null || furnitureEntity == null) return;
        event.setCancelled(true);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,
                (int) (period * 11),
                Integer.MAX_VALUE,
                false, false, false));
        BlockSounds blockSounds = furnitureMechanic.getBlockSounds();
        if (blockSounds != null && blockSounds.hasHitSound())
            BlockHelpers.playCustomBlockSound(location, blockSounds.getHitSound(), blockSounds.getHitVolume(), blockSounds.getHitPitch());
        if (breakerPerLocation.containsKey(location))
            breakerPerLocation.get(location).cancel();


        OraxenFurnitureDamageEvent damageEvent = new OraxenFurnitureDamageEvent(furnitureMechanic, furnitureEntity, player);
        Bukkit.getPluginManager().callEvent(damageEvent);
        if (damageEvent.isCancelled()) return;

        AtomicInteger value = new AtomicInteger(0);
        breakerPerLocation.put(location, OraxenPlugin.foliaLib.getImpl().runAtLocationTimer(location, () -> {

            if (breakerPerLocation.containsKey(block.getLocation())) {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                Entity baseEntity = mechanic != null ? mechanic.getBaseEntity(block) : null;
                List<Location> barrierLocations = mechanic != null && baseEntity != null ? mechanic.getLocations(FurnitureMechanic.getFurnitureYaw(baseEntity), baseEntity.getLocation(), mechanic.getBarriers()) : Collections.singletonList(block.getLocation());

                if (item.getEnchantmentLevel(Enchantment.DIG_SPEED) >= 5) value.set(10);

                for (Entity entity : block.getWorld().getNearbyEntities(location, 16,16,16)) {
                    if (entity instanceof Player viewer) {
                        float damage = Math.min((float) value.intValue() / 10, 1.0f);
                        if (mechanic != null) {
                            for (Location barrierLoc : barrierLocations)
                                viewer.sendBlockDamage(barrierLoc, damage, location.hashCode());
                        } else viewer.sendBlockDamage(block.getLocation(), damage, location.hashCode());

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
                            if (mechanic != null) {
                                for (Location barrierLoc : barrierLocations)
                                    viewer.sendBlockDamage(barrierLoc, damage, location.hashCode());
                            } else viewer.sendBlockDamage(block.getLocation(), damage, location.hashCode());

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
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);

                return mechanic != null && mechanic.hasHardness();
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (mechanic == null) return 0;

                final long hardness = mechanic.getHardness();
                double modifier = 1;
                if (mechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = mechanic.getDrop().getDiff(tool);
                    if (diff >= 1) modifier *= Math.pow(0.9, diff);
                }
                long period = (long) (hardness * modifier);
                return period == 0 && mechanic.hasHardness() ? 1 : period;
            }
        };
    }
}
