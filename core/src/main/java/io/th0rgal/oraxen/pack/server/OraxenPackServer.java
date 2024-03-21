package io.th0rgal.oraxen.pack.server;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface OraxenPackServer {

    @Nullable
    static OraxenPackServer initializeServer() {
        return switch (PackServerType.fromSetting()) {
            case CREATIVE -> new CreativeServer();
            case POLYMATH -> {
                Logs.logWarning("Polymath-type server is not yet setup. Changing to CREATIVE");
                yield null;
            }
            case NONE -> null;
        };
    }

    void sendPack(Player player);

    default void start() {}

    default void stop() {}
}
