package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockDurability {

    private static final Map<UUID, Integer> PENDING_VANILLA_DAMAGE_CANCELLATIONS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> SUPPRESS_VANILLA_DAMAGE_CANCELLATION = ThreadLocal.withInitial(() -> false);

    private BlockDurability() {
    }

    public static void applyConfigured(Player player, ItemStack item, @Nullable BlockBreaking.DurabilityAction action) {
        if (action == null || player.getGameMode() == GameMode.CREATIVE || ItemUtils.isInvalidItem(item)) return;

        if (action.add() > 0) repairItem(player, item, action.add());
        if (action.remove() > 0 && !ItemUtils.isInvalidItem(item)) damageItem(player, item, action.remove());

        registerPendingVanillaDamageCancellation(player);
    }

    public static void setSuppressVanillaDamageCancellation(boolean suppress) {
        if (suppress) SUPPRESS_VANILLA_DAMAGE_CANCELLATION.set(true);
        else SUPPRESS_VANILLA_DAMAGE_CANCELLATION.remove();
    }

    public static boolean cancelPendingVanillaDamage(PlayerItemDamageEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Integer pending = PENDING_VANILLA_DAMAGE_CANCELLATIONS.get(playerId);
        if (pending == null || pending <= 0) return false;

        event.setCancelled(true);
        decrementPending(playerId);
        return true;
    }

    private static void registerPendingVanillaDamageCancellation(Player player) {
        if (SUPPRESS_VANILLA_DAMAGE_CANCELLATION.get()) return;

        UUID playerId = player.getUniqueId();
        PENDING_VANILLA_DAMAGE_CANCELLATIONS.merge(playerId, 1, Integer::sum);
        SchedulerUtil.runForEntityLater(player, 2L, () -> decrementPending(playerId), () -> decrementPending(playerId));
    }

    private static void decrementPending(UUID playerId) {
        PENDING_VANILLA_DAMAGE_CANCELLATIONS.compute(playerId, (uuid, pending) -> {
            if (pending == null || pending <= 1) return null;
            return pending - 1;
        });
    }

    private static void repairItem(Player player, ItemStack item, int amount) {
        DurabilityMechanic mechanic = getDurabilityMechanic(item);
        if (mechanic != null && mechanic.changeDurability(player, item, amount)) return;

        if (!(item.getItemMeta() instanceof Damageable damageable)) return;
        damageable.setDamage(Math.max(0, damageable.getDamage() - amount));
        item.setItemMeta(damageable);
    }

    private static void damageItem(Player player, ItemStack item, int amount) {
        PlayerItemDamageEvent damageEvent = new PlayerItemDamageEvent(player, item, amount);
        if (!EventUtils.callEvent(damageEvent)) return;

        int damage = damageEvent.getDamage();
        if (damage <= 0 || ItemUtils.isInvalidItem(item)) return;
        if (getDurabilityMechanic(item) != null) return;

        if (!(item.getItemMeta() instanceof Damageable damageable)) return;
        int maxDamage = damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
        if (maxDamage <= 0) return;

        int newDamage = damageable.getDamage() + damage;
        if (newDamage >= maxDamage) {
            EventUtils.callEvent(new PlayerItemBreakEvent(player, item));
            item.setAmount(0);
            return;
        }

        damageable.setDamage(newDamage);
        item.setItemMeta(damageable);
    }

    @Nullable
    private static DurabilityMechanic getDurabilityMechanic(ItemStack item) {
        DurabilityMechanicFactory factory = DurabilityMechanicFactory.get();
        return factory != null ? factory.getMechanic(item) : null;
    }
}
