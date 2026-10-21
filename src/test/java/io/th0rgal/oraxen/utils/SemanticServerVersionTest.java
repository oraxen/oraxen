package io.th0rgal.oraxen.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SemanticServerVersionTest {

    @AfterEach
    void tearDown() {
        SchedulerUtil.resetBackend();
    }

    @Test
    void parsesStandardAndMojang2026DropVersions() {
        SemanticServerVersion v1201 = SemanticServerVersion.parse("1.20.1");
        assertEquals(1, v1201.major());
        assertEquals(20, v1201.minor());
        assertEquals(1, v1201.patch());
        assertNull(v1201.qualifier());
        assertFalse(v1201.snapshot());
        assertFalse(v1201.isModernNamespace());

        SemanticServerVersion v12111 = SemanticServerVersion.parse("1.21.11");
        assertEquals(1, v12111.major());
        assertEquals(21, v12111.minor());
        assertEquals(11, v12111.patch());
        assertTrue(v12111.isAtLeast(v1201));

        SemanticServerVersion v2612 = SemanticServerVersion.parse("26.1.2");
        assertEquals(26, v2612.major());
        assertEquals(1, v2612.minor());
        assertEquals(2, v2612.patch());
        assertTrue(v2612.isModernNamespace());
        assertTrue(v2612.isAtLeast(v12111));

        SemanticServerVersion v1262Alias = SemanticServerVersion.parse("1.26.2");
        assertTrue(v1262Alias.isModernNamespace());
        assertEquals(SemanticServerVersion.parse("26.2"), v1262Alias.toCanonicalNamespace());
    }

    @Test
    void parsesPreReleasesAndSnapshots() {
        SemanticServerVersion pre1 = SemanticServerVersion.parse("26.3-pre1");
        SemanticServerVersion rc1 = SemanticServerVersion.parse("26.3-rc1");
        SemanticServerVersion release = SemanticServerVersion.parse("26.3");

        assertTrue(pre1.isPreRelease());
        assertEquals("pre1", pre1.qualifier());
        assertTrue(pre1.compareTo(rc1) < 0);
        assertTrue(rc1.compareTo(release) < 0);
        assertTrue(pre1.targetsAtLeast(26, 3, 0));

        SemanticServerVersion snap1 = SemanticServerVersion.parse("25w03a");
        SemanticServerVersion snap2 = SemanticServerVersion.parse("25w03b");
        assertTrue(snap1.snapshot());
        assertEquals(25, snap1.snapshotYear());
        assertEquals(3, snap1.snapshotWeek());
        assertEquals('a', snap1.snapshotRevision());
        assertTrue(snap1.compareTo(snap2) < 0);
    }

    @Test
    void platformCapabilitiesMatchVersionBoundaries() {
        SemanticServerVersion v1201 = SemanticServerVersion.parse("1.20.1");
        SemanticServerVersion v1205 = SemanticServerVersion.parse("1.20.5");
        SemanticServerVersion v1212 = SemanticServerVersion.parse("1.21.2");
        SemanticServerVersion v1214 = SemanticServerVersion.parse("1.21.4");
        SemanticServerVersion v1216 = SemanticServerVersion.parse("1.21.6");
        SemanticServerVersion v12111 = SemanticServerVersion.parse("1.21.11");
        SemanticServerVersion v262 = SemanticServerVersion.parse("26.2");

        assertFalse(PlatformCapabilities.supportsDataComponents(v1201));
        assertTrue(PlatformCapabilities.supportsDataComponents(v1205));

        assertFalse(PlatformCapabilities.supportsNmsHandler(v1205));
        assertTrue(PlatformCapabilities.supportsNmsHandler(v1212));
        assertTrue(PlatformCapabilities.supportsItemModelComponent(v1212));
        assertTrue(PlatformCapabilities.supportsEquippableComponent(v1212));

        assertFalse(PlatformCapabilities.supportsModernItemDefinitions(v1212));
        assertTrue(PlatformCapabilities.supportsModernItemDefinitions(v1214));

        assertFalse(PlatformCapabilities.supportsSingleAxisFreeRotation(v1214));
        assertTrue(PlatformCapabilities.supportsSingleAxisFreeRotation(v1216));
        assertTrue(PlatformCapabilities.supportsDynamicDialogs(v1216));
        assertTrue(PlatformCapabilities.supportsHappyGhastHitbox(v1216));

        assertFalse(PlatformCapabilities.supportsSplitItemAtlas(v1216));
        assertTrue(PlatformCapabilities.supportsSplitItemAtlas(v12111));
        assertTrue(PlatformCapabilities.supportsSplitItemAtlas(v262));
        assertTrue(PlatformCapabilities.supportsFreeBlockModelRotations(v12111));
        assertTrue(PlatformCapabilities.supportsFreeBlockModelRotations(v262));
    }

    @Test
    void executionSchedulerPortAllowsDeterministicTesting() {
        AtomicBoolean executed = new AtomicBoolean(false);
        SchedulerUtil.setBackend(new ExecutionSchedulerPort() {
            private final SchedulerUtil.ScheduledTask noOpTask = new SchedulerUtil.ScheduledTask(null);

            @Override public boolean isGlobalThread() { return true; }
            @Override public SchedulerUtil.ScheduledTask runTask(@Nullable Plugin plugin, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public SchedulerUtil.ScheduledTask runTaskLater(@Nullable Plugin plugin, long delayTicks, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public SchedulerUtil.ScheduledTask runTaskTimer(@Nullable Plugin plugin, long delayTicks, long periodTicks, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public SchedulerUtil.ScheduledTask runTaskAsync(@Nullable Plugin plugin, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public SchedulerUtil.ScheduledTask runTaskLaterAsync(@Nullable Plugin plugin, long delayTicks, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public SchedulerUtil.ScheduledTask runTaskTimerAsync(@Nullable Plugin plugin, long delayTicks, long periodTicks, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public SchedulerUtil.ScheduledTask runAtLocation(@Nullable Plugin plugin, @NotNull Location location, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public SchedulerUtil.ScheduledTask runAtLocationLater(@Nullable Plugin plugin, @NotNull Location location, long delayTicks, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public SchedulerUtil.ScheduledTask runAtLocationTimer(@Nullable Plugin plugin, @NotNull Location location, long delayTicks, long periodTicks, @NotNull Runnable runnable) { runnable.run(); return noOpTask; }
            @Override public void runOnOwningThread(@NotNull Entity entity, @NotNull Runnable runnable) { runnable.run(); }
            @Override public @Nullable SchedulerUtil.ScheduledTask runForEntity(@Nullable Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, @Nullable Runnable retired) { runnable.run(); return noOpTask; }
            @Override public @Nullable SchedulerUtil.ScheduledTask runForEntityLater(@Nullable Plugin plugin, @NotNull Entity entity, long delayTicks, @NotNull Runnable runnable, @Nullable Runnable retired) { runnable.run(); return noOpTask; }
            @Override public @Nullable SchedulerUtil.ScheduledTask runForEntityTimer(@Nullable Plugin plugin, @NotNull Entity entity, long delayTicks, long periodTicks, @NotNull Runnable runnable, @Nullable Runnable retired) { runnable.run(); return noOpTask; }
        });

        SchedulerUtil.runTask(() -> executed.set(true));
        assertTrue(executed.get());
        assertTrue(SchedulerUtil.isGlobalThread());
    }
}
