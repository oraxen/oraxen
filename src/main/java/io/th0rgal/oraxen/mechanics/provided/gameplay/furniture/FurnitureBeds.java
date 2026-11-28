package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
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
import org.bukkit.Statistic;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.Event;
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
    private static final ThreadLocal<TimeSkipEvent> pendingNightSkip = new ThreadLocal<>();

    @SuppressWarnings("removal")
    private static final class NightSkipRules {
        private static final GameRule<Integer> sleepingPercentage = GameRule.PLAYERS_SLEEPING_PERCENTAGE;
        private static final GameRule<Boolean> advanceTime = GameRule.DO_DAYLIGHT_CYCLE;
        private static final GameRule<Boolean> advanceWeather = GameRule.DO_WEATHER_CYCLE;
    }

    record SleepState(boolean spectator, boolean sleeping, boolean deepSleeping, boolean sleepingIgnored,
                      boolean deepSleepingIgnored) {
        static boolean enoughSleeping(List<SleepState> states, int percentage) {
            int active = 0;
            int sleeping = 0;
            int deepSleeping = 0;
            for (SleepState state : states) {
                if (state == null || state.spectator()) continue;
                if (state.deepSleeping()) return false;
                active++;
                if (state.sleeping() || state.sleepingIgnored()) sleeping++;
                if (state.sleepingIgnored() && state.deepSleepingIgnored()) deepSleeping++;
            }
            int needed = Math.max(1, (int) Math.ceil(active * percentage / 100.0));
            return sleeping >= needed && deepSleeping >= needed;
        }
    }

    FurnitureBeds() {
        registerClockTimeSkipListener();
    }

    @SuppressWarnings("unchecked")
    private void registerClockTimeSkipListener() {
        Class<? extends Event> clockTimeSkipEvent;
        try {
            Class<?> type = Class.forName("org.bukkit.event.world.ClockTimeSkipEvent");
            if (!Event.class.isAssignableFrom(type)) return;
            clockTimeSkipEvent = (Class<? extends Event>) type;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return;
        }
        Bukkit.getPluginManager().registerEvent(clockTimeSkipEvent, this, EventPriority.MONITOR,
                (listener, event) -> onClockNightSkip(event), OraxenPlugin.get(), true);
    }

    private void onClockNightSkip(Event event) {
        if (event instanceof TimeSkipEvent || !FurnitureTimeSkip.isNightSkip(event)) return;
        leaveIf(sleeper -> sleeper.bed.skipNight() && FurnitureTimeSkip.affectsWorld(event, sleeper.world));
    }

    private static boolean isSleepingTime(World world) {
        return !world.isFixedTime() && !world.isDayTime();
    }

    private static final class Sleeper {
        private final UUID baseId;
        private final int index;
        private final FurnitureBed bed;
        private final Entity carrier;
        private final int playerEntityId;
        private final World world;
        private final Location head;
        private final boolean wasSleepingIgnored;
        private final Pose previousPose;
        private final boolean wasFixedPose;
        private volatile int ticks;
        private volatile SchedulerUtil.ScheduledTask task;

        private Sleeper(UUID baseId, int index, FurnitureBed bed, Entity carrier, int playerEntityId, Location head,
                        boolean wasSleepingIgnored, Pose previousPose, boolean wasFixedPose) {
            this.baseId = baseId;
            this.index = index;
            this.bed = bed;
            this.carrier = carrier;
            this.playerEntityId = playerEntityId;
            this.world = head.getWorld();
            this.head = head;
            this.wasSleepingIgnored = wasSleepingIgnored;
            this.previousPose = previousPose;
            this.wasFixedPose = wasFixedPose;
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
        if (bed.nightOnly() && !isSleepingTime(head.getWorld())) {
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

        UUID playerId = player.getUniqueId();
        Sleeper sleeper = new Sleeper(baseEntity.getUniqueId(), index, bed, carrier, player.getEntityId(), head.clone(),
                player.isSleepingIgnored(), player.getPose(), player.hasFixedPose());
        sleepers.put(playerId, sleeper);
        if (bed.skipNight()) player.setSleepingIgnored(true);
        if (bed.resetPhantoms()) player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        player.setPose(Pose.SLEEPING, true);
        if (!NMSHandlers.getHandler().showFurnitureBed(player, head, yaw)) {
            leave(player);
            return;
        }
        if (bed.setRespawn()) {
            Location respawn = standUpLocation(head);
            if (respawn != null) {
                player.setRespawnLocation(respawn, true);
                player.sendMessage(Component.translatable("block.minecraft.set_spawn"));
            }
        }
        sleeper.task = SchedulerUtil.runForEntityTimer(player, 20L, 20L, () -> tick(player),
                () -> discard(playerId, sleeper));
        // A Folia region may finish mounting after the old factory has been unregistered.
        if (MechanicsManager.getMechanicFactory("furniture") != mechanic.getFactory()) leave(player);
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
                || sleeper.bed.nightOnly() && !isSleepingTime(player.getWorld())) {
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
        leave(player, sleepers.get(player.getUniqueId()), dismounting, immediate);
    }

    private static void leave(Player player, Sleeper sleeper, boolean dismounting, boolean immediate) {
        if (sleeper == null || !sleepers.remove(player.getUniqueId(), sleeper)) return;
        if (sleeper.task != null) sleeper.task.cancel();
        NMSHandlers.getHandler().hideFurnitureBed(player);
        player.setPose(sleeper.previousPose, sleeper.wasFixedPose);
        if (sleeper.bed.skipNight()) player.setSleepingIgnored(sleeper.wasSleepingIgnored);
        if (sleeper.bed.resetPhantoms()) player.setStatistic(Statistic.TIME_SINCE_REST, 0);

        Entity carrier = sleeper.carrier;
        if (!dismounting && carrier.equals(player.getVehicle())) player.leaveVehicle();
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
        if (target == null) return;
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        player.teleportAsync(target);
    }

    static void leave(Entity baseEntity) {
        UUID baseId = baseEntity.getUniqueId();
        leaveIf(sleeper -> sleeper.baseId.equals(baseId));
    }

    static void leaveAll() {
        leaveIf(sleeper -> true);
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
        if (player == null || SchedulerUtil.runForEntity(player, () -> leave(player, sleeper, false, false),
                () -> discard(playerId, sleeper)) == null) discard(playerId, sleeper);
    }

    private static void discard(UUID playerId, Sleeper sleeper) {
        if (!sleepers.remove(playerId, sleeper)) return;
        if (sleeper.task != null) sleeper.task.cancel();
        NMSHandlers.getHandler().forgetFurnitureBed(sleeper.playerEntityId);
        // A retired callback cannot remove entities directly.
        SchedulerUtil.runForEntity(sleeper.carrier, sleeper.carrier::remove);
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
            if (isSafeStandUpBlock(feet)) return feet.getLocation().add(0.5, 0, 0.5);
        return null;
    }

    private static boolean isSafeStandUpBlock(Block feet) {
        Block head = feet.getRelative(BlockFace.UP);
        Block below = feet.getRelative(BlockFace.DOWN);
        return feet.isPassable() && head.isPassable() && !below.isPassable()
                && !isDangerous(feet) && !isDangerous(head) && !isDangerous(below)
                && !Tag.INVALID_SPAWN_INSIDE.isTagged(feet.getType())
                && !Tag.INVALID_SPAWN_INSIDE.isTagged(head.getType());
    }

    private static boolean isDangerous(Block block) {
        return Tag.FIRE.isTagged(block.getType())
                || switch (block.getType()) {
                    case LAVA, MAGMA_BLOCK, LAVA_CAULDRON, WITHER_ROSE, SWEET_BERRY_BUSH, CACTUS, POWDER_SNOW -> true;
                    default -> Tag.CAMPFIRES.isTagged(block.getType())
                            && block.getBlockData() instanceof Lightable lightable && lightable.isLit();
                };
    }

    static SchedulerUtil.ScheduledTask startNightSkipTask() {
        return SchedulerUtil.runTaskTimer(20L, 20L, FurnitureBeds::checkNightSkip);
    }

    private static void checkNightSkip() {
        Set<World> worlds = new HashSet<>();
        for (Sleeper sleeper : sleepers.values())
            if (sleeper.bed.skipNight() && sleeper.ticks >= DEEP_SLEEP_TICKS) worlds.add(sleeper.world);
        worlds.removeIf(world -> !isSleepingTime(world));
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
        if (SchedulerUtil.runForEntity(player, () -> {
            Sleeper sleeper = sleepers.get(player.getUniqueId());
            state.complete(new SleepState(
                    player.getGameMode() == GameMode.SPECTATOR, player.isSleeping(),
                    player.isSleeping() && player.getSleepTicks() >= DEEP_SLEEP_TICKS, player.isSleepingIgnored(),
                    sleeper == null || sleeper.ticks >= DEEP_SLEEP_TICKS));
        }, () -> state.complete(null)) == null) state.complete(null);
        return state;
    }

    private static void skipNight(World world, List<SleepState> states) {
        if (!isSleepingTime(world) || sleepers.values().stream().noneMatch(sleeper -> sleeper.world.equals(world)
                && sleeper.bed.skipNight() && sleeper.ticks >= DEEP_SLEEP_TICKS)) return;
        Integer percentage = world.getGameRuleValue(NightSkipRules.sleepingPercentage);
        if (!SleepState.enoughSleeping(states, percentage == null ? 100 : percentage)) return;

        if (isEnabled(world, NightSkipRules.advanceTime)) {
            long time = world.getFullTime();
            long morning = time + DAY_LENGTH - Math.floorMod(time + DAY_LENGTH, DAY_LENGTH);
            TimeSkipEvent event = FurnitureTimeSkip.nightSkip(world, morning - time);
            pendingNightSkip.set(event);
            boolean accepted;
            try {
                accepted = event.callEvent();
            } finally {
                pendingNightSkip.remove();
            }
            if (!accepted || !NMSHandlers.getHandler().advanceTimeWithoutEvent(world, event.getSkipAmount())) return;
        }

        wake(world);
        if (isEnabled(world, NightSkipRules.advanceWeather) && world.hasStorm()) {
            world.setWeatherDuration(0);
            world.setStorm(false);
            world.setThunderDuration(0);
            world.setThundering(false);
        }
    }

    private static boolean isEnabled(World world, GameRule<Boolean> rule) {
        return !Boolean.FALSE.equals(world.getGameRuleValue(rule));
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
        if (event != pendingNightSkip.get() && FurnitureTimeSkip.isNightSkip(event)) wake(event.getWorld());
    }
}
