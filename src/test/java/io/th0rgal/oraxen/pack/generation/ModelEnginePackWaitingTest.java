package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.provided.modelengine.ModelEngineCompatibility;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ModelEnginePackWaitingTest {
    @TempDir
    Path tempDir;

    private final YamlConfiguration settings = new YamlConfiguration();
    private final List<Runnable> scheduled = new ArrayList<>();
    private final ModelEngineCompatibility compatibility = mock(ModelEngineCompatibility.class);
    private final CompletableFuture<Void> ready = new CompletableFuture<>();
    private OraxenPlugin plugin;
    private ResourcePack pack;
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<CompatibilitiesManager> compatibilities;
    private MockedStatic<SchedulerUtil> scheduler;
    private MockedStatic<PackSender> dispatch;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void setUp() {
        plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(plugin.isEnabled()).thenReturn(true);
        when(configs.getSettings()).thenReturn(settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        compatibilities = mockStatic(CompatibilitiesManager.class);
        compatibilities.when(() -> CompatibilitiesManager.getActiveCompatibility("ModelEngine"))
                .thenReturn(compatibility);
        when(compatibility.getPackReady()).thenReturn(ready);
        scheduler = mockStatic(SchedulerUtil.class);
        scheduler.when(() -> SchedulerUtil.runTask(any(Runnable.class))).thenAnswer(invocation -> {
            scheduled.add(invocation.getArgument(0));
            return null;
        });
        dispatch = mockStatic(PackSender.class);
        // Stop at the start of asset generation, after the real waiting path has run.
        dispatch.when(PackSender::resetDispatchNormalization).thenThrow(new GenerationStarted());
        logs = mockStatic(Logs.class);
        settings.set(Settings.IMPORT_MODEL_ENGINE.getPath(), true);
        Settings.invalidateCache();
        pack = new ResourcePack();
    }

    @AfterEach
    void tearDown() {
        pack.shutdown();
        Settings.invalidateCache();
        logs.close();
        dispatch.close();
        scheduler.close();
        compatibilities.close();
        pluginAccess.close();
    }

    @Test
    void waitsForReadinessAndIgnoresDuplicateGenerationRequests() {
        pack.generate();
        pack.generate();
        assertTrue(scheduled.isEmpty());
        dispatch.verifyNoInteractions();

        ready.complete(null);

        assertEquals(1, scheduled.size());
        dispatch.verifyNoInteractions();
        assertThrows(GenerationStarted.class, scheduled.removeFirst()::run);
    }

    @Test
    void generatesImmediatelyWhenAlreadyReady() {
        ready.complete(null);
        assertThrows(GenerationStarted.class, pack::generate);
        assertTrue(scheduled.isEmpty());
    }

    @Test
    void disabledImportDoesNotWaitForModelEngine() {
        settings.set(Settings.IMPORT_MODEL_ENGINE.getPath(), false);
        Settings.invalidateCache();

        assertThrows(GenerationStarted.class, pack::generate);

        compatibilities.verifyNoInteractions();
        assertFalse(ready.isDone());
    }

    @Test
    void timeoutContinuesGenerationWithoutCompletingModelEngineReadiness() {
        CompletableFuture<Void> source = spy(ready);
        CompletableFuture<Void> timeout = spy(new CompletableFuture<Void>());
        doReturn(timeout).when(source).copy();
        doReturn(timeout).when(timeout).orTimeout(300, TimeUnit.SECONDS);
        when(compatibility.getPackReady()).thenReturn(source);

        pack.generate();
        verify(timeout).orTimeout(300, TimeUnit.SECONDS);
        timeout.completeExceptionally(new TimeoutException());

        assertFalse(source.isDone());
        assertEquals(1, scheduled.size());
        logs.verify(() -> Logs.logWarning("ModelEngine resource pack was not ready within 300 seconds; continuing pack generation"));
        assertThrows(GenerationStarted.class, scheduled.removeFirst()::run);
    }

    @Test
    void shutdownWhileWaitingDoesNotScheduleGeneration() {
        pack.generate();
        pack.shutdown();
        ready.complete(null);

        assertTrue(scheduled.isEmpty());
        dispatch.verifyNoInteractions();
    }

    @Test
    void shutdownAfterReadinessPreventsQueuedGeneration() {
        pack.generate();
        ready.complete(null);
        assertEquals(1, scheduled.size());
        pack.shutdown();
        scheduled.removeFirst().run();

        dispatch.verifyNoInteractions();
    }

    @Test
    void disabledPluginDoesNotScheduleGenerationWhenReady() {
        pack.generate();
        when(plugin.isEnabled()).thenReturn(false);
        ready.complete(null);

        assertTrue(scheduled.isEmpty());
        dispatch.verifyNoInteractions();
    }

    private static class GenerationStarted extends RuntimeException {}
}
