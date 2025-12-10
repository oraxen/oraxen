package io.th0rgal.oraxen.mechanics.provided.combat.spear;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Listener handling the spear lunge mechanic's charge and attack logic.
 * <p>
 * When a player right-clicks with a spear item, the listener starts tracking
 * the charge state and swaps the item's model to the active pose. On release
 * (via left-click or timeout), the player lunges forward and damage is applied
 * to nearby entities in a cone.
 */
public class SpearLungeMechanicListener implements Listener {

    private final MechanicFactory factory;
    private final Map<UUID, ChargeState> chargingPlayers = new HashMap<>();
    private final Map<UUID, Long> attackCooldowns = new HashMap<>();
    // Track players who just triggered a lunge attack - suppress their default
    // melee damage
    private final Set<UUID> lungingPlayers = new HashSet<>();
    // Track entities we're intentionally damaging via lunge - allow their damage
    // events
    private final Set<UUID> intentionalDamageTargets = new HashSet<>();

    // Cooldown in ticks after an attack before player can charge again (30 ticks =
    // 1.5 seconds)
    private static final int POST_ATTACK_COOLDOWN_TICKS = 30;

    public SpearLungeMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    /**
     * Stores the state of a charging player, including the original model
     * and walk speed so they can be restored after the attack.
     */
    private record ChargeState(
            long startTick,
            SpearLungeMechanic mechanic,
            EquipmentSlot hand,
            BukkitTask task,
            int lastFrame,
            NamespacedKey originalModel,
            float originalWalkSpeed) {
        ChargeState withLastFrame(int frame) {
            return new ChargeState(startTick, mechanic, hand, task, frame, originalModel, originalWalkSpeed);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.useItemInHand() == Event.Result.DENY)
            return;
        if (BlockHelpers.isInteractable(event.getClickedBlock()) && event.useInteractedBlock() == Event.Result.ALLOW)
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;

        SpearLungeMechanic mechanic = (SpearLungeMechanic) factory.getMechanic(item);
        if (mechanic == null)
            return;

        // If already charging, ignore
        if (chargingPlayers.containsKey(player.getUniqueId()))
            return;

        // Check if player is on cooldown from a recent attack
        Long cooldownEnd = attackCooldowns.get(player.getUniqueId());
        if (cooldownEnd != null && Bukkit.getCurrentTick() < cooldownEnd) {
            return;
        }
        attackCooldowns.remove(player.getUniqueId());

        // Start charging
        startCharge(player, mechanic, event.getHand(), item, itemID);
    }

    private void startCharge(Player player, SpearLungeMechanic mechanic, EquipmentSlot hand, ItemStack item,
            String itemId) {
        // Get the original model from the item (oraxen:<itemId>)
        NamespacedKey originalModel = getItemModel(item);
        if (originalModel == null) {
            // Fallback: construct from item ID
            originalModel = new NamespacedKey(OraxenPlugin.get(), itemId);
        }

        // Store original walk speed and apply slowdown
        float originalWalkSpeed = player.getWalkSpeed();
        if (mechanic.getChargeSlowdown() > 0) {
            float slowedSpeed = originalWalkSpeed * (1.0f - (float) mechanic.getChargeSlowdown());
            player.setWalkSpeed(Math.max(0.0f, slowedSpeed));
        }

        // Switch to first animation frame if available, otherwise active model
        if (mechanic.hasSmoothAnimation()) {
            NamespacedKey firstFrame = mechanic.getIntermediateModelKey(0);
            if (firstFrame != null) {
                setItemModel(item, firstFrame);
                player.getInventory().setItem(hand, item);
            }
        } else if (mechanic.hasActiveModel()) {
            setItemModel(item, mechanic.getActiveItemModelKey());
            player.getInventory().setItem(hand, item);
        }

        // Play charge sound
        if (mechanic.hasSounds()) {
            player.playSound(player.getLocation(), mechanic.getChargeSound(), 0.8f, 1.2f);
        }

        // Start a task to monitor charging state
        BukkitTask task = new ChargeMonitorTask(player, mechanic, hand).runTaskTimer(OraxenPlugin.get(), 1L, 1L);

        ChargeState state = new ChargeState(Bukkit.getCurrentTick(), mechanic, hand, task, 0, originalModel,
                originalWalkSpeed);
        chargingPlayers.put(player.getUniqueId(), state);
    }

    private void cancelCharge(Player player, boolean performAttack) {
        ChargeState state = chargingPlayers.remove(player.getUniqueId());
        if (state == null)
            return;

        state.task().cancel();

        // Restore walk speed
        player.setWalkSpeed(state.originalWalkSpeed());

        ItemStack item = player.getInventory().getItem(state.hand());
        if (item == null || item.getType().isAir())
            return;

        // Verify it's still the spear
        String itemId = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemId))
            return;

        if (performAttack) {
            long chargedTicks = Bukkit.getCurrentTick() - state.startTick();
            double chargePercent = Math.min(1.0, (double) chargedTicks / state.mechanic().getChargeTicks());

            // Only attack if minimum charge threshold is met
            if (chargePercent >= state.mechanic().getMinChargePercent()) {
                performLungeAttack(player, state.mechanic(), chargePercent);
                // Set cooldown to prevent immediate re-charging
                attackCooldowns.put(player.getUniqueId(), (long) Bukkit.getCurrentTick() + POST_ATTACK_COOLDOWN_TICKS);
            }
        }

        // Revert to original model
        if (state.originalModel() != null) {
            setItemModel(item, state.originalModel());
            player.getInventory().setItem(state.hand(), item);
        }
    }

    private void performLungeAttack(Player player, SpearLungeMechanic mechanic, double chargePercent) {
        // Suppress default melee damage during lunge
        lungingPlayers.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(),
                () -> lungingPlayers.remove(player.getUniqueId()), 5L);

        Vector direction = player.getLocation().getDirection();
        applyLungeMovement(player, mechanic, direction, chargePercent);

        List<LivingEntity> targets = findTargetsInCone(player, mechanic, direction);
        int maxTargets = mechanic.getMaxTargets();

        for (int i = 0; i < Math.min(targets.size(), maxTargets); i++) {
            applyDamageToTarget(player, mechanic, targets.get(i), direction, chargePercent);
        }
    }

    private void applyLungeMovement(Player player, SpearLungeMechanic mechanic, Vector direction,
            double chargePercent) {
        double velocityMultiplier = mechanic.getMinChargePercent()
                + ((1.0 - mechanic.getMinChargePercent()) * chargePercent);
        double finalVelocity = mechanic.getLungeVelocity() * velocityMultiplier;

        Vector velocity = direction.clone().multiply(finalVelocity);
        velocity.setY(Math.max(velocity.getY(), 0.15));
        player.setVelocity(velocity);

        if (mechanic.hasSounds()) {
            player.getWorld().playSound(player.getLocation(), mechanic.getLungeSound(), 1.0f, 1.0f);
        }
        if (mechanic.hasParticles()) {
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(mechanic.getLungeParticle(), loc, 8, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private List<LivingEntity> findTargetsInCone(Player player, SpearLungeMechanic mechanic, Vector direction) {
        double range = mechanic.getMaxRange();
        Location eyeLocation = player.getEyeLocation();
        Vector lookDirection = direction.clone().normalize();

        List<LivingEntity> targets = new ArrayList<>();

        // Raytrace for direct hits
        RayTraceResult rayResult = player.getWorld().rayTraceEntities(
                eyeLocation, lookDirection, range, 0.5,
                entity -> entity instanceof LivingEntity && entity != player && !entity.isDead());

        if (rayResult != null && rayResult.getHitEntity() instanceof LivingEntity hitEntity) {
            targets.add(hitEntity);
        }

        // Cone check for additional targets
        for (Entity entity : player.getNearbyEntities(range + 1, range + 1, range + 1)) {
            if (!(entity instanceof LivingEntity target) || entity == player || entity.isDead())
                continue;
            if (targets.contains(target))
                continue;
            if (isInAttackCone(target, eyeLocation, lookDirection, range)) {
                targets.add(target);
            }
        }

        targets.sort(Comparator.comparingDouble(e -> eyeLocation.distanceSquared(e.getLocation())));
        return targets;
    }

    private boolean isInAttackCone(LivingEntity target, Location eyeLocation, Vector lookDirection, double range) {
        Location entityCenter = target.getLocation().add(0, target.getHeight() / 2, 0);
        double distance = eyeLocation.distance(entityCenter);

        if (distance > range + 1.5)
            return false;

        Vector toEntity = entityCenter.toVector().subtract(eyeLocation.toVector());
        if (toEntity.lengthSquared() < 0.01)
            return false;

        toEntity.normalize();
        double dot = lookDirection.dot(toEntity);
        return dot > 0.5; // ~60 degree cone
    }

    private void applyDamageToTarget(Player player, SpearLungeMechanic mechanic, LivingEntity target,
            Vector direction, double chargePercent) {
        intentionalDamageTargets.add(target.getUniqueId());

        double damage = mechanic.getDamage() * chargePercent;
        target.damage(damage, player);

        // Note: intentionalDamageTargets is cleared in onEntityDamage after allowing
        // the lunge damage. This prevents the melee swing (which fires later in the
        // same tick) from also being allowed through.

        Vector knockback = direction.clone().normalize().multiply(mechanic.getKnockback());
        knockback.setY(0.2);
        target.setVelocity(target.getVelocity().add(knockback));

        if (mechanic.hasParticles()) {
            Location hitLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
            target.getWorld().spawnParticle(mechanic.getHitParticle(), hitLoc, 5, 0.2, 0.2, 0.2, 0.05);
        }
        if (mechanic.hasSounds()) {
            target.getWorld().playSound(target.getLocation(), mechanic.getHitSound(), 1.0f, 1.0f);
        }
    }

    private NamespacedKey getItemModel(ItemStack item) {
        if (!VersionUtil.atOrAbove("1.21.2"))
            return null;
        if (item == null)
            return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasItemModel())
            return null;

        return meta.getItemModel();
    }

    private void setItemModel(ItemStack item, NamespacedKey model) {
        if (!VersionUtil.atOrAbove("1.21.2"))
            return;
        if (item == null || model == null)
            return;

        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                return;

            meta.setItemModel(model);
            item.setItemMeta(meta);
        } catch (Exception e) {
            // Silently fail if model swapping isn't supported
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        // Cancel charge if player switches slots
        cancelCharge(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDrop(PlayerDropItemEvent event) {
        // Cancel charge if player drops the item
        cancelCharge(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up on disconnect - must restore walk speed!
        Player player = event.getPlayer();
        ChargeState state = chargingPlayers.remove(player.getUniqueId());
        if (state != null) {
            state.task().cancel();
            // Restore walk speed before player disconnects
            player.setWalkSpeed(state.originalWalkSpeed());
        }
        attackCooldowns.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Clean up on death - must restore walk speed before respawn!
        Player player = event.getEntity();
        ChargeState state = chargingPlayers.remove(player.getUniqueId());
        if (state != null) {
            state.task().cancel();
            // Restore walk speed so it's correct on respawn
            player.setWalkSpeed(state.originalWalkSpeed());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        // Cancel charge if player swaps hands
        cancelCharge(event.getPlayer(), false);
    }

    /**
     * Task that monitors the charging state and detects release.
     * It also handles smooth animation frame progression and particle effects.
     */
    private class ChargeMonitorTask extends BukkitRunnable {

        private final Player player;
        private final SpearLungeMechanic mechanic;
        private final EquipmentSlot hand;
        private int ticksHeld = 0;
        private boolean fullyCharged = false;

        public ChargeMonitorTask(Player player, SpearLungeMechanic mechanic, EquipmentSlot hand) {
            this.player = player;
            this.mechanic = mechanic;
            this.hand = hand;
        }

        @Override
        public void run() {
            // Check if player is still online
            if (!player.isOnline()) {
                cancelCharge(player, false);
                return;
            }

            // Check if the item is still in hand
            ItemStack item = player.getInventory().getItem(hand);
            if (item == null || item.getType().isAir()) {
                cancelCharge(player, false);
                return;
            }

            String itemId = OraxenItems.getIdByItem(item);
            if (factory.isNotImplementedIn(itemId)) {
                cancelCharge(player, false);
                return;
            }

            ticksHeld++;

            // Show charging particles periodically
            if (mechanic.hasParticles() && ticksHeld % 3 == 0) {
                Location particleLoc = player.getLocation().add(
                        player.getLocation().getDirection().multiply(0.5)).add(0, 1.2, 0);
                player.getWorld().spawnParticle(mechanic.getChargeParticle(), particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
            }

            // Handle smooth animation frames during charge
            if (mechanic.hasSmoothAnimation() && ticksHeld < mechanic.getChargeTicks()) {
                updateAnimationFrame(item, ticksHeld);
            }

            // When fully charged, switch to active model and mark as ready
            if (!fullyCharged && ticksHeld >= mechanic.getChargeTicks()) {
                fullyCharged = true;
                if (mechanic.hasActiveModel()) {
                    setItemModel(item, mechanic.getActiveItemModelKey());
                    player.getInventory().setItem(hand, item);

                    ChargeState state = chargingPlayers.get(player.getUniqueId());
                    if (state != null) {
                        chargingPlayers.put(player.getUniqueId(), state.withLastFrame(mechanic.getSmoothFrames() + 1));
                    }
                }
                // Play a "ready" sound to indicate full charge
                if (mechanic.hasSounds()) {
                    player.playSound(player.getLocation(), mechanic.getChargeSound(), 0.5f, 1.8f);
                }
            }

            // Max hold timeout - if held too long without attacking, cancel and revert to
            // inactive
            // This gives the player 3 seconds (60 ticks by default) from charge start to
            // attack
            if (ticksHeld >= mechanic.getMaxHoldTicks()) {
                cancelCharge(player, false); // Cancel without attacking
            }
        }

        private void updateAnimationFrame(ItemStack item, int currentTick) {
            int totalFrames = mechanic.getSmoothFrames();
            if (totalFrames <= 0)
                return;

            // Calculate which frame we should be on based on charge progress
            // Frames are distributed evenly across the charge time
            // Frame 0: 0% to 33% of charge
            // Frame 1: 33% to 66% of charge
            // Active model: 66% to 100% of charge
            double chargeProgress = (double) currentTick / mechanic.getChargeTicks();
            int targetFrame;

            if (chargeProgress < 0.33) {
                targetFrame = 0;
            } else if (chargeProgress < 0.66) {
                targetFrame = Math.min(1, totalFrames - 1);
            } else {
                // Switch to active model at 66% charge
                targetFrame = totalFrames; // This will trigger active model switch
            }

            ChargeState state = chargingPlayers.get(player.getUniqueId());
            if (state == null || state.lastFrame() >= targetFrame)
                return;

            if (targetFrame < totalFrames) {
                // Use intermediate frame
                NamespacedKey frameModel = mechanic.getIntermediateModelKey(targetFrame);
                if (frameModel != null) {
                    setItemModel(item, frameModel);
                    player.getInventory().setItem(hand, item);
                    chargingPlayers.put(player.getUniqueId(), state.withLastFrame(targetFrame));
                }
            } else {
                // Switch to active model
                if (mechanic.hasActiveModel()) {
                    setItemModel(item, mechanic.getActiveItemModelKey());
                    player.getInventory().setItem(hand, item);
                    chargingPlayers.put(player.getUniqueId(), state.withLastFrame(targetFrame));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();

        // If the player is charging, left-click triggers the attack
        if (chargingPlayers.containsKey(player.getUniqueId())) {
            cancelCharge(player, true);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Cancel default melee damage from players who just performed a lunge attack
        // This prevents the sword's base damage from stacking with the lunge damage
        if (!(event.getDamager() instanceof Player player))
            return;

        if (lungingPlayers.contains(player.getUniqueId())) {
            // Allow damage if this is our intentional lunge damage (shows red hurt
            // animation), then immediately remove from tracking so any subsequent
            // damage in the same tick (melee swing) gets cancelled
            if (intentionalDamageTargets.remove(event.getEntity().getUniqueId())) {
                return; // Don't cancel - this is the lunge damage
            }
            // Cancel any other damage (sword swing from left-click)
            event.setCancelled(true);
        }
    }
}
