package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import team.unnamed.creative.server.ResourcePackRequestHandler;
import team.unnamed.creative.server.ResourcePackServer;

import java.io.IOException;

public class PackServer {
    private static ResourcePackServer packServer;
    private final String ip = "127.0.0.1";
    private final int port = 8080;

    public PackServer() {


        try {
            packServer = ResourcePackServer.builder().address(ip, port).pack(OraxenPlugin.get().getPackGenerator().builtPack).build();
        } catch (IOException e) {
            Logs.logError("Failed to start Oraxen pack-server");
            if (Settings.DEBUG.toBool()) Logs.logWarning(e.getMessage());
        }
    }

    public void sendPack(Player player) {
        String hash = OraxenPlugin.get().getPackGenerator().builtPack.hash();
        String url = "http://" + ip + ":" + port + "/" + hash + ".zip";
        player.setResourcePack(url, hash, false, null);
    }

    public void start() {
        Logs.logSuccess("Started Oraxen pack-server...");
        packServer.start();
    }

    public void stop() {
        Logs.logError("Stopping Oraxen pack-server...");
        packServer.stop(0);
    }

    private ResourcePackRequestHandler handler = (request, exchange) -> {
        Player player = Bukkit.getPlayer(request.uuid());
    };
}
