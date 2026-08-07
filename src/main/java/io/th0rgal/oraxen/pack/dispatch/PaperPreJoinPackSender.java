package io.th0rgal.oraxen.pack.dispatch;

import io.papermc.paper.event.connection.configuration.PlayerConnectionInitialConfigureEvent;
import io.th0rgal.oraxen.nms.NMSHandlers;
import net.kyori.adventure.audience.Audience;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

final class PaperPreJoinPackSender implements Listener {

    private final PreJoinPackProvider packProvider;

    PaperPreJoinPackSender(PreJoinPackProvider packProvider) {
        this.packProvider = packProvider;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConfigure(PlayerConnectionInitialConfigureEvent event) {
        // The NMS listener owns callback-aware pre-join dispatch when available.
        // Keep this listener only as a fallback for servers where guarded NMS setup failed.
        if (NMSHandlers.hasPackDispatchListener()) return;
        if (!PackSender.isPreJoinDispatchActive()) return;
        Object connection = event.getConnection();
        if (!PackDispatchFilter.canSendPackForConnection(connection)) return;

        packProvider.sendPack(connection, event.getConnection().getAudience());
    }

    @FunctionalInterface
    interface PreJoinPackProvider {
        void sendPack(Object connection, Audience audience);
    }
}
