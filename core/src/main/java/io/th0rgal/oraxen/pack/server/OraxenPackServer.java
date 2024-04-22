package io.th0rgal.oraxen.pack.server;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.PackListener;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface OraxenPackServer {

    PackListener packListener = new PackListener();

    static OraxenPackServer initializeServer() {
        OraxenPlugin.get().packServer().stop();
        HandlerList.unregisterAll(packListener);
        Bukkit.getPluginManager().registerEvents(packListener, OraxenPlugin.get());
        PackServerType type = Settings.PACK_SERVER_TYPE.toEnumOrGet(PackServerType.class, () -> {
            Logs.logError("Invalid PackServer-type specified: " + Settings.PACK_SERVER_TYPE);
            Logs.logError("Valid types are: " + Arrays.stream(PackServerType.values()).map(Enum::name).collect(Collectors.joining(", ")));
            return PackServerType.NONE;
        });

        Logs.logInfo("PackServer set to " + type.name());

        return switch (type) {
            case SELFHOST -> new SelfHostServer();
            case POLYMATH -> new PolymathServer();
            case NONE -> new EmptyServer();
        };
    }

    default void uploadPack() {
    }

    void sendPack(Player player);

    default void start() {
    }

    default void stop() {
    }

    static byte[] hashArray(String hash) {
        int len = hash.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hash.charAt(i), 16) << 4)
                    + Character.digit(hash.charAt(i + 1), 16));
        }
        return data;
    }
}
