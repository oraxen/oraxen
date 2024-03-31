package io.th0rgal.oraxen.pack.server;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.PackListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public interface OraxenPackServer {

    PackListener packListener = new PackListener();

    static OraxenPackServer initializeServer() {
        OraxenPlugin.get().packServer().stop();
        HandlerList.unregisterAll(packListener);
        Bukkit.getPluginManager().registerEvents(packListener, OraxenPlugin.get());
        return switch (PackServerType.fromSetting()) {
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
