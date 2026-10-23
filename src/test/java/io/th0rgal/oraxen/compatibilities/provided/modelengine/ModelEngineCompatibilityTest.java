package io.th0rgal.oraxen.compatibilities.provided.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ModelEngineCompatibilityTest {

    @Test
    void waitsThroughRegistrationPhasesUntilFinished() {
        ModelEngineCompatibility compatibility = new ModelEngineCompatibility();
        CompletableFuture<Void> ready = compatibility.getPackReady();
        CompletableFuture<Boolean> waiter = ready.thenApply(ignored -> true);

        for (ModelGenerator.Phase phase : ModelGenerator.Phase.values()) {
            if (phase == ModelGenerator.Phase.FINISHED) continue;
            register(compatibility, phase);
            assertSame(ready, compatibility.getPackReady());
            assertFalse(waiter.isDone(), "Pack became ready during " + phase);
        }

        register(compatibility, ModelGenerator.Phase.FINISHED);

        assertTrue(waiter.join());
        assertFalse(ready.isCompletedExceptionally());
    }

    @ParameterizedTest
    @EnumSource(value = ModelGenerator.Phase.class, names = {"PRE_IMPORT", "PRE_ASSETS", "PRE_ZIPPING"})
    void startsANewWaitForEachRegistrationCycle(ModelGenerator.Phase startingPhase) {
        ModelEngineCompatibility compatibility = new ModelEngineCompatibility();
        register(compatibility, ModelGenerator.Phase.FINISHED);
        CompletableFuture<Void> previous = compatibility.getPackReady();

        register(compatibility, startingPhase);
        CompletableFuture<Void> next = compatibility.getPackReady();
        assertNotSame(previous, next);
        assertTrue(previous.isDone());
        assertFalse(next.isDone());

        register(compatibility, startingPhase);
        assertSame(next, compatibility.getPackReady());
        register(compatibility, ModelGenerator.Phase.FINISHED);
        assertTrue(next.isDone());
        assertFalse(next.isCompletedExceptionally());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void enablingUsesTheGeneratorsInitializationState(boolean initialized) {
        ModelEngineAPI api = mock(ModelEngineAPI.class);
        ModelGenerator generator = mock(ModelGenerator.class);
        PluginManager plugins = mock(PluginManager.class);
        when(api.getModelGenerator()).thenReturn(generator);
        when(generator.isInitialized()).thenReturn(initialized);

        try (MockedStatic<ModelEngineAPI> modelEngine = mockStatic(ModelEngineAPI.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            modelEngine.when(ModelEngineAPI::getAPI).thenReturn(api);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            ModelEngineCompatibility compatibility = new ModelEngineCompatibility();

            compatibility.enable("ModelEngine");

            assertTrue(compatibility.isEnabled());
            assertEquals(initialized, compatibility.getPackReady().isDone());
            if (!initialized) {
                register(compatibility, ModelGenerator.Phase.FINISHED);
                assertTrue(compatibility.getPackReady().isDone());
            }
        }
    }

    @Test
    void disablingReleasesPendingWaiters() {
        ModelEngineCompatibility compatibility = new ModelEngineCompatibility();
        CompletableFuture<Boolean> waiter = compatibility.getPackReady().thenApply(ignored -> true);

        try (MockedStatic<HandlerList> handlers = mockStatic(HandlerList.class)) {
            compatibility.disable();

            assertTrue(waiter.join());
            assertFalse(compatibility.isEnabled());
            handlers.verify(() -> HandlerList.unregisterAll(compatibility));
        }
    }

    private static void register(ModelEngineCompatibility compatibility, ModelGenerator.Phase phase) {
        ModelRegistrationEvent event = mock(ModelRegistrationEvent.class);
        when(event.getPhase()).thenReturn(phase);
        compatibility.onModelRegistration(event);
    }
}
