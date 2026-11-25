package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EntityUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

final class FurnitureBeds implements Listener {

    static final int DEEP_SLEEP_TICKS = 100;
    private static final long DAY_LENGTH = 24000L;
    private static final double MAX_STAND_UP_DISTANCE_SQUARED = 4.0;
    private static final NamespacedKey CARRIER_KEY = new NamespacedKey(OraxenPlugin.get(), "bed_carrier");
    private static final Map<UUID, Sleeper> sleepers = new ConcurrentHashMap<>();
    private static final AtomicBoolean checkingNightSkip = new AtomicBoolean();
    private static final GameRule<Integer> sleepingPercentage = gameRule("players_sleeping_percentage", "playersSleepingPercentage");
    private static final GameRule<Boolean> advanceTime = gameRule("advance_time", "doDaylightCycle");
    private static final GameRule<Boolean> advanceWeather = gameRule("advance_weather", "doWeatherCycle");

    record SleepState(boolean spectator, boolean sleeping, boolean deepSleeping, boolean sleepingIgnored) {
        static boolean enoughSleeping(List<SleepState> states, int percentage) {
            int active = 0;
            int sleeping = 0;
            int deepSleeping = 0;
            for (SleepState state : states) {
                if (state == null || state.spectator()) continue;
                if (state.deepSleeping()) return false;
                active++;
                if (state.sleeping() || state.sleepingIgnored()) sleeping++;
                if (state.sleepingIgnored()) deepSleeping++;
            }
            int needed = Math.max(1, (int) Math.ceil(active * percentage / 100.0));
            return sleeping >= needed && deepSleeping >= needed;
        }
    }

    private static final class Sleeper {
        private final UUID baseId;
        private final int index;
        private final FurnitureBed bed;
        private final Entity carrier;
        private final World world;
        private final Location head;
        private final boolean wasSleepingIgnored;
        private volatile int ticks;
        private volatile SchedulerUtil.ScheduledTask task;

        private Sleeper(UUID baseId, int index, FurnitureBed bed, Entity carrier, Location head, boolean wasSleepingIgnored) {
            this.baseId = baseId;
            this.index = index;
            this.bed = bed;
            this.carrier = carrier;
            this.world = head.getWorld();
            this.head = head;
            this.wasSleepingIgnored = wasSleepingIgnored;
        }
    }

    static void lieDown(Player player, Entity baseEntity, FurnitureMechanic mechanic, Location target) {
        if (sleepers.containsKey(player.getUniqueId()) || player.isInsideVehicle() || player.isSleeping()) return;
        float yaw = FurnitureMechanic.getFurnitureYaw(baseEntity);
        Location root = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        List<FurnitureBed> beds = mechanic.getBeds();
        int index = -1;
        Location head = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < beds.size(); i++) {
            if (isOccupied(baseEntity.getUniqueId(), i)) continue;
            Location spot = root.clone().add(FurnitureMechanic.rotateGroundOffset(beds.get(i).offset(), yaw));
            double distance = spot.distanceSquared(target);
            if (distance < bestDistance) {
                bestDistance = distance;
                index = i;
                head = spot;
            }
        }
        if (head == null || !Bukkit.isOwnedByCurrentRegion(head)) return;

        FurnitureBed bed = beds.get(index);
        if (bed.nightOnly() && head.getWorld().isDayTime()) {
            player.sendActionBar(Component.translatable("block.minecraft.bed.no_sleep"));
            return;
        }

        head.setYaw(yaw);
        ItemDisplay carrier = EntityUtils.spawnEntity(head, ItemDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.getPersistentDataContainer().set(CARRIER_KEY, PersistentDataType.BOOLEAN, true);
        });
        if (carrier == null) return;
        if (!carrier.addPassenger(player)) {
            carrier.remove();
            return;
        }

        Sleeper sleeper = new Sleeper(baseEntity.getUniqueId(), index, bed, carrier, head.clone(),
                player.isSleepingIgnored());
        sleepers.put(player.getUniqueId(), sleeper);
        if (bed.skipNight()) player.setSleepingIgnored(true);
        if (bed.resetPhantoms()) player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        if (!NMSHandlers.getHandler().showFurnitureBed(player, head, yaw)) {
            leave(player);
            return;
        }
        if (bed.setRespawn()) {
            player.setRespawnLocation(standUpLocation(head), true);
            player.sendMessage(Component.translatable("block.minecraft.set_spawn"));
        }
        sleeper.task = SchedulerUtil.runForEntityTimer(player, 20L, 20L, () -> tick(player),
                () -> sleepers.remove(player.getUniqueId(), sleeper));
    }

    private static boolean isOccupied(UUID baseId, int index) {
        for (Sleeper sleeper : sleepers.values())
            if (sleeper.baseId.equals(baseId) && sleeper.index == index) return true;
        return false;
    }

    private static void tick(Player player) {
        Sleeper sleeper = sleepers.get(player.getUniqueId());
        if (sleeper == null) return;
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !vehicle.getUniqueId().equals(sleeper.carrier.getUniqueId())
                || sleeper.bed.nightOnly() && player.getWorld().isDayTime()) {
            leave(player);
            return;
        }
        sleeper.ticks += 20;
        if (sleeper.bed.resetPhantoms()) player.setStatistic(Statistic.TIME_SINCE_REST, 0);
    }

    static void leave(Player player) {
        leave(player, false, false);
    }

    private static void leave(Player player, boolean dismounting, boolean immediate) {
        Sleeper sleeper = sleepers.remove(player.getUniqueId());
        if (sleeper == null) return;
        if (sleeper.task != null) sleeper.task.cancel();
        NMSHandlers.getHandler().hideFurnitureBed(player);
        if (sleeper.bed.skipNight()) player.setSleepingIgnored(sleeper.wasSleepingIgnored);
        if (sleeper.bed.resetPhantoms()) player.setStatistic(Statistic.TIME_SINCE_REST, 0);

        Entity carrier = sleeper.carrier;
        if (!dismounting && carrier.getPassengers().contains(player)) player.leaveVehicle();
        if (immediate) {
            if (Bukkit.isOwnedByCurrentRegion(carrier)) carrier.remove();
            standUp(player, sleeper);
        } else {
            SchedulerUtil.runOnOwningThread(carrier, carrier::remove);
            SchedulerUtil.runForEntityLater(player, 1L, () -> standUp(player, sleeper));
        }
    }

    private static void standUp(Player player, Sleeper sleeper) {
        if (!player.isOnline() || player.isDead() || player.isInsideVehicle()
                || !player.getWorld().equals(sleeper.world)
                || player.getLocation().distanceSquared(sleeper.head) > MAX_STAND_UP_DISTANCE_SQUARED) return;
        Location target = standUpLocation(sleeper.head);
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        player.teleportAsync(target);
    }

    static void leave(Entity baseEntity) {
        UUID baseId = baseEntity.getUniqueId();
        leaveIf(sleeper -> sleeper.baseId.equals(baseId));
    }

    static void shutdown() {
        sleepers.forEach((playerId, sleeper) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && Bukkit.isOwnedByCurrentRegion(player)) leave(player, false, true);
            else {
                sleepers.remove(playerId, sleeper);
                if (Bukkit.isOwnedByCurrentRegion(sleeper.carrier)) sleeper.carrier.remove();
            }
        });
    }

    private static void wake(World world) {
        leaveIf(sleeper -> sleeper.world.equals(world) && sleeper.bed.skipNight());
    }

    private static void leaveIf(Predicate<Sleeper> filter) {
        sleepers.forEach((playerId, sleeper) -> {
            if (filter.test(sleeper)) leaveLater(playerId, sleeper);
        });
    }

    private static void leaveLater(UUID playerId, Sleeper sleeper) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || SchedulerUtil.runForEntity(player, () -> leave(player), null) == null) {
            sleepers.remove(playerId, sleeper);
            SchedulerUtil.runOnOwningThread(sleeper.carrier, sleeper.carrier::remove);
        }
    }

    private static Location standUpLocation(Location head) {
        Block origin = head.getBlock();
        List<Block> candidates = new ArrayList<>();
        for (int dy = 0; dy >= -1; dy--)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dz != 0) candidates.add(origin.getRelative(dx, dy, dz));
        candidates.sort(Comparator.comparingDouble(block -> block.getLocation().add(0.5, 0, 0.5).distanceSquared(head)));
        candidates.add(origin.getRelative(BlockFace.UP));
        for (Block feet : candidates)
            if (feet.isPassable() && feet.getRelative(BlockFace.UP).isPassable() && !feet.getRelative(BlockFace.DOWN).isPassable())
                return feet.getLocation().add(0.5, 0, 0.5);
        return head.clone();
    }

    static SchedulerUtil.ScheduledTask startNightSkipTask() {
        return SchedulerUtil.runTaskTimer(20L, 20L, FurnitureBeds::checkNightSkip);
    }

    private static void checkNightSkip() {
        Set<World> worlds = new HashSet<>();
        for (Sleeper sleeper : sleepers.values())
            if (sleeper.bed.skipNight() && sleeper.ticks >= DEEP_SLEEP_TICKS) worlds.add(sleeper.world);
        worlds.removeIf(world -> world.isFixedTime() || world.isDayTime());
        if (worlds.isEmpty() || !checkingNightSkip.compareAndSet(false, true)) return;

        Map<World, List<CompletableFuture<SleepState>>> states = new ConcurrentHashMap<>();
        List<CompletableFuture<SleepState>> pending = new ArrayList<>();
        for (World world : worlds) {
            List<CompletableFuture<SleepState>> worldStates = world.getPlayers().stream().map(FurnitureBeds::sleepState).toList();
            states.put(world, worldStates);
            pending.addAll(worldStates);
        }
        CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).whenComplete((ignored, error) -> {
            if (SchedulerUtil.runTask(() -> {
                try {
                    states.forEach((world, worldStates) -> skipNight(world,
                            worldStates.stream().map(state -> state.getNow(null)).toList()));
                } finally {
                    checkingNightSkip.set(false);
                }
            }) == null) checkingNightSkip.set(false);
        });
    }

    private static CompletableFuture<SleepState> sleepState(Player player) {
        CompletableFuture<SleepState> state = new CompletableFuture<>();
        if (SchedulerUtil.runForEntity(player, () -> state.complete(new SleepState(
                player.getGameMode() == GameMode.SPECTATOR, player.isSleeping(),
                player.isSleeping() && player.getSleepTicks() >= DEEP_SLEEP_TICKS, player.isSleepingIgnored())
        ), () -> state.complete(null)) == null) state.complete(null);
        return state;
    }

    private static void skipNight(World world, List<SleepState> states) {
        if (world.isDayTime() || sleepers.values().stream().noneMatch(sleeper -> sleeper.world.equals(world)
                && sleeper.bed.skipNight() && sleeper.ticks >= DEEP_SLEEP_TICKS)) return;
        Integer percentage = sleepingPercentage == null ? null : world.getGameRuleValue(sleepingPercentage);
        if (!SleepState.enoughSleeping(states, percentage == null ? 100 : percentage)) return;

        if (isEnabled(world, advanceTime)) {
            long time = world.getFullTime();
            long morning = time + DAY_LENGTH - Math.floorMod(time + DAY_LENGTH, DAY_LENGTH);
            TimeSkipEvent event = FurnitureTimeSkip.nightSkip(world, morning - time);
            if (event.callEvent()) world.setFullTime(world.getFullTime() + event.getSkipAmount());
        } else wake(world);

        if (isEnabled(world, advanceWeather) && world.hasStorm()) {
            world.setWeatherDuration(0);
            world.setStorm(false);
            world.setThunderDuration(0);
            world.setThundering(false);
        }
    }

    private static boolean isEnabled(World world, GameRule<Boolean> rule) {
        return rule == null || !Boolean.FALSE.equals(world.getGameRuleValue(rule));
    }

    @SuppressWarnings({"unchecked", "removal"})
    private static <T> GameRule<T> gameRule(String key, String legacyName) {
        try {
            GameRule<?> rule = Registry.GAME_RULE.get(NamespacedKey.minecraft(key));
            if (rule != null) return (GameRule<T>) rule;
        } catch (LinkageError ignored) {
        }
        try {
            return (GameRule<T>) GameRule.getByName(legacyName);
        } catch (LinkageError ignored) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getDismounted().getPersistentDataContainer().has(CARRIER_KEY)) leave(player, true, false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        leave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNightSkip(TimeSkipEvent event) {
        if (FurnitureTimeSkip.isNightSkip(event)) wake(event.getWorld());
    }
}
