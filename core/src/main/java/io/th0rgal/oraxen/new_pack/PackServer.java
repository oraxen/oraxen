package io.th0rgal.oraxen.new_pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;
import team.unnamed.creative.server.ResourcePackServer;

import java.io.IOException;

public class PackServer {
    private static ResourcePackServer packServer;

    public PackServer() {
        try {
            int port = Settings.PACK_SERVER_PORT.toInt(8080);
            String ip = Settings.PACK_SERVER_IP.toString("atlas.oraxen.com");
            packServer = ResourcePackServer.server().address(ip, port).pack(OraxenPlugin.get().packGenerator().builtPack()).build();
        } catch (IOException e) {
            Logs.logError("Failed to start Oraxen pack-server");
            if (Settings.DEBUG.toBool()) Logs.logWarning(e.getMessage());
        }
    }

    public void sendPack(Player player) {
        String hash = OraxenPlugin.get().packGenerator().builtPack().hash();
        String url = Settings.PACK_SERVER_ADDRESS.toString("http://atlas.oraxen.com:8080").replaceAll("^(?!.*/)", "") + "/" + hash + ".zip";
        //String url = "http://" + ip + ":" + port + "/" + hash + ".zip";
        if (VersionUtil.isPaperServer())
            player.setResourcePack(url, hash, Settings.SEND_PACK_MANDATORY.toBool(), AdventureUtils.MINI_MESSAGE.deserialize(Settings.SEND_PACK_PROMPT.toString()));
        else player.setResourcePack(url, hash, Settings.SEND_PACK_MANDATORY.toBool());
    }

    public void start() {
        Logs.logSuccess("Started Oraxen pack-server...");
        packServer.start();
    }

    public void stop() {
        Logs.logError("Stopping Oraxen pack-server...");
        packServer.stop(0);
    }
}
