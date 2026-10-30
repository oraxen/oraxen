package io.th0rgal.oraxen.compatibilities.provided.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

public class ModelEngineCompatibility extends CompatibilityProvider<Plugin> {

    private volatile CompletableFuture<Void> packReady = new CompletableFuture<>();

    @Override
    public void enable(String pluginName) {
        super.enable(pluginName);
        if (ModelEngineAPI.getAPI().getModelGenerator().isInitialized()) {
            packReady.complete(null);
        }
    }

    @EventHandler
    public void onModelRegistration(ModelRegistrationEvent event) {
        ModelGenerator.Phase phase = event.getPhase();
        if ((phase == ModelGenerator.Phase.PRE_IMPORT
                || phase == ModelGenerator.Phase.PRE_ASSETS
                || phase == ModelGenerator.Phase.PRE_ZIPPING) && packReady.isDone()) {
            packReady = new CompletableFuture<>();
        } else if (event.getPhase() == ModelGenerator.Phase.FINISHED) {
            packReady.complete(null);
        }
    }

    public CompletableFuture<Void> getPackReady() {
        return packReady;
    }

    @Override
    public void disable() {
        super.disable();
        packReady.complete(null);
    }
}
