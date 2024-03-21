package io.th0rgal.oraxen.pack.server;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;
import team.unnamed.creative.BuiltResourcePack;
import team.unnamed.creative.server.ResourcePackServer;
import team.unnamed.creative.server.handler.ResourcePackRequestHandler;

import java.io.IOException;
import java.util.UUID;

public class CreativeServer implements OraxenPackServer {

    private ResourcePackServer packServer;
    private final String serverIp = Settings.CREATIVE_PACK_SERVER_IP.toString("0.0.0.0").replace("localhost", "0.0.0.0");
    private String downloadAddress = Settings.CREATIVE_PACK_DOWNLOAD_ADDRESS.toString(serverIp).replace("localhost", "0.0.0.0");
    private final String prompt = Settings.SEND_PACK_PROMPT.toString();
    private final boolean mandatory = Settings.SEND_PACK_MANDATORY.toBool();

    public CreativeServer() {
        downloadAddress = (downloadAddress.startsWith("http://") || downloadAddress.startsWith("https://") ? "" : "http://") + downloadAddress + (downloadAddress.endsWith("/") ? "" : "/");
        try {
            BuiltResourcePack builtPack = OraxenPlugin.get().packGenerator().builtPack();
            ResourcePackRequestHandler handler = ResourcePackRequestHandler.fixed(builtPack);
            int serverPort = Settings.CREATIVE_PACK_SERVER_PORT.toInt(8082);
            packServer = ResourcePackServer.server().address(serverIp, serverPort).handler(handler).pack(builtPack).build();
            OraxenPlugin.get().packServer(this);
        } catch (IOException e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            else Logs.logWarning(e.getMessage(), true);
            Logs.logError("Failed to start Oraxen pack-server");
        }
    }

    @Override
    public void sendPack(Player player) {
        String hash = OraxenPlugin.get().packGenerator().builtPack().hash();
        byte[] hashArray = OraxenPackServer.hashArray(hash);
        String url = downloadAddress.replaceAll("^(?!.*/)", "") + "/" + hash + ".zip";
        UUID packUUID = UUID.nameUUIDFromBytes(hashArray);

        if (VersionUtil.atOrAbove("1.20.3")) {
            if (VersionUtil.isPaperServer()) player.setResourcePack(packUUID, url, hash, AdventureUtils.MINI_MESSAGE.deserialize(prompt), mandatory);
            else player.setResourcePack(packUUID, url, hashArray, AdventureUtils.parseLegacy(prompt), mandatory);
        }
        else if (VersionUtil.isPaperServer()) player.setResourcePack(url, hashArray, AdventureUtils.MINI_MESSAGE.deserialize(prompt), mandatory);
        else player.setResourcePack(url, hashArray, AdventureUtils.parseLegacy(prompt), mandatory);


    }

    @Override
    public void start() {
        if (packServer == null) return;
        Logs.logSuccess("Started Oraxen pack-server...");
        packServer.start();
    }

    @Override
    public void stop() {
        if (packServer == null) return;
        Logs.logError("Stopping Oraxen pack-server...");
        packServer.stop(0);
    }
}
