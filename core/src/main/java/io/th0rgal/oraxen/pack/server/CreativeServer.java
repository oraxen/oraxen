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

    public CreativeServer() {
        try {
            String ip = Settings.PACK_SERVER_IP.toString("0.0.0.0");
            int port = Settings.PACK_SERVER_PORT.toInt(8082);
            BuiltResourcePack builtPack = OraxenPlugin.get().packGenerator().builtPack();
            ResourcePackRequestHandler handler = ResourcePackRequestHandler.fixed(builtPack);
            packServer = ResourcePackServer.server().address(ip, port).handler(handler).pack(builtPack).build();
            OraxenPlugin.get().packServer(this);
        } catch (IOException e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            else Logs.logWarning(e.getMessage(), true);
            Logs.logError("Failed to start Oraxen pack-server");
        }
    }

    @Override
    public void sendPack(Player player) {
        String prompt = Settings.SEND_PACK_PROMPT.toString();
        boolean mandatory = Settings.SEND_PACK_MANDATORY.toBool();
        String hash = OraxenPlugin.get().packGenerator().builtPack().hash();
        byte[] hashArray = OraxenPackServer.hashArray(hash);
        String url = Settings.PACK_SERVER_ADDRESS.toString("http://0.0.0.0:8082").replaceAll("^(?!.*/)", "") + "/" + hash + ".zip";
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
