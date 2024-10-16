package io.th0rgal.oraxen.pack.server;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.entity.Player;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.server.ResourcePackServer;
import team.unnamed.creative.server.handler.ResourcePackRequestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SelfHostServer implements OraxenPackServer {

    private ResourcePackServer packServer;
    private final String publicAddress;

    public SelfHostServer() {
        this.publicAddress = publicAddress();
        try {
            int serverPort = Settings.SELFHOST_PACK_SERVER_PORT.toInt(8082);
            packServer = ResourcePackServer.server().address(serverPort).handler(handler).build();
        } catch (IOException e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            else Logs.logWarning(e.getMessage(), true);
            Logs.logError("Failed to start Oraxen pack-server");
        }
    }

    private byte[] builtPackArray = null;
    private final ResourcePackRequestHandler handler = (request, exchange) -> {
        Writable packData = OraxenPlugin.get().packGenerator().builtPack().data();
        if (builtPackArray == null) builtPackArray = packData.toByteArray();
        exchange.getResponseHeaders().put("Content-Type", Collections.singletonList("application/zip"));
        exchange.sendResponseHeaders(200, builtPackArray.length);
        exchange.getResponseBody().write(builtPackArray);
    };

    @Override
    public String packUrl() {
        String hash = OraxenPlugin.get().packGenerator().builtPack().hash();
        int serverPort = Settings.SELFHOST_PACK_SERVER_PORT.toInt(8082);
        return "http://" + publicAddress + ":" + serverPort + "/" + hash + ".zip";
    }

    @Override
    public void sendPack(Player player) {
        OraxenPlugin.get().packGenerator().packGenFuture.thenRun(() -> {
            String hash = OraxenPlugin.get().packGenerator().builtPack().hash();
            byte[] hashArray = OraxenPackServer.hashArray(hash);
            String url = packUrl();
            UUID packUUID = UUID.nameUUIDFromBytes(hashArray);
            OraxenPackServer.allPackUUIDs.add(packUUID);
            Set<UUID> oldPackUUIDs = new HashSet<>(OraxenPackServer.allPackUUIDs);
            oldPackUUIDs.remove(packUUID);

            ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                    .required(mandatory).replace(false).prompt(prompt)
                    .packs(ResourcePackInfo.resourcePackInfo(packUUID, URI.create(url), hash)).build();
            player.removeResourcePacks(oldPackUUIDs);
            player.sendResourcePacks(request);
        });
    }

    @Override
    public boolean isPackUploaded() {
        return true;
    }

    @Override
    public CompletableFuture<Void> uploadPack() {
        String hashPart = "/" + OraxenPlugin.get().packGenerator().builtPack().hash() + ".zip";
        if (Settings.DEBUG.toBool()) Logs.logSuccess("Resourcepack uploaded and will be dispatched with publicAddress http://" + this.publicAddress + ":" + packServer.address().getPort() + hashPart);
        else Logs.logSuccess("Resourcepack has been uploaded to SelfHost!");

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void start() {
        if (packServer == null) return;
        Logs.logSuccess("Started Self-Host Pack-Server...");
        packServer.start();
    }

    @Override
    public void stop() {
        if (packServer == null) return;
        Logs.logError("Stopping Self-Host Pack-Server...");
        packServer.stop(0);
        packServer = null;
    }

    private String publicAddress() {
        String urlString = "http://checkip.amazonaws.com/";
        String publicAddress;
        try {
            URL url = URI.create(urlString).toURL();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                publicAddress = br.readLine();
            }
        } catch (IOException e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            Logs.logError("Failed to get publicAddress for SELFHOST server...");
            Logs.logWarning("You can manually set it in `settings.yml` at ");
            publicAddress = "0.0.0.0";
        }
        return Settings.SELFHOST_PUBLIC_ADDRESS.toString(publicAddress);
    }
}
