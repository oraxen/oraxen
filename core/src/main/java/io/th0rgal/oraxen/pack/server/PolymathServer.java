package io.th0rgal.oraxen.pack.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PolymathServer implements OraxenPackServer {

    private final String serverAddress;
    private String packUrl;
    private String hash;
    private UUID packUUID;
    private CompletableFuture<Void> uploadFuture;

    public PolymathServer() {
        String address = Settings.POLYMATH_SERVER.toString("atlas.oraxen.com");
        this.serverAddress = (address.startsWith("http://") || address.startsWith("https://") ? "" : "https://") + address + (address.endsWith("/") ? "" : "/");
    }

    @Override
    public String packUrl() {
        return packUrl;
    }

    @Override
    public CompletableFuture<Void> uploadPack() {
        if (!Objects.equals(hash, OraxenPlugin.get().packGenerator().builtPack().hash())) {
            if (uploadFuture != null) uploadFuture.cancel(true);
            uploadFuture = null;
        }

        if (uploadFuture == null) uploadFuture = CompletableFuture.runAsync(() -> {
            try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost request = new HttpPost(serverAddress + "upload");

                HttpEntity httpEntity = MultipartEntityBuilder.create()
                        .addTextBody("id", Settings.POLYMATH_SECRET.toString())
                        .addPart("pack", new ByteArrayBody(OraxenPlugin.get().packGenerator().builtPack().data().toByteArray(), "pack"))
                        .build();

                request.setEntity(httpEntity);

                CloseableHttpResponse response = httpClient.execute(request);
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity);
                JsonObject jsonOutput;
                try {
                    jsonOutput = JsonParser.parseString(responseString).getAsJsonObject();
                } catch (JsonSyntaxException e) {
                    Logs.logError("The resource pack could not be uploaded due to a malformed response.");
                    Logs.logWarning("This is usually due to the resourcepack server being down.");
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                    else Logs.logWarning(e.getMessage());
                    return;
                }
                if (jsonOutput.has("url") && jsonOutput.has("sha1")) {
                    packUrl = jsonOutput.get("url").getAsString();
                    hash = jsonOutput.get("sha1").getAsString();
                    packUUID = UUID.nameUUIDFromBytes(hash.getBytes());

                    Logs.logSuccess("ResourcePack has been uploaded to " + packUrl);
                    return;
                }

                if (jsonOutput.has("error"))
                    Logs.logError("Error: " + jsonOutput.get("error").getAsString());
                Logs.logError("Response: " + jsonOutput);
                Logs.logError("The resource pack has not been uploaded to the server. Usually this is due to an excessive size.");
            } catch(IllegalStateException | IOException ex) {
                Logs.logError("The resource pack has not been uploaded to the server. Usually this is due to an excessive size.");
                if (Settings.DEBUG.toBool()) ex.printStackTrace();
                else Logs.logWarning(ex.getMessage());
            }
        });

        return uploadFuture;
    }

    @Override
    public void sendPack(Player player) {
        if (uploadFuture.isDone()) {
            byte[] hashArray = OraxenPackServer.hashArray(hash);

            if (VersionUtil.isPaperServer()) {
                ResourcePackRequest request = ResourcePackRequest.resourcePackRequest().required(mandatory).replace(true).prompt(prompt)
                        .packs(ResourcePackInfo.resourcePackInfo(packUUID, URI.create(packUrl), hash)).build();
                player.sendResourcePacks(request);
            } else player.setResourcePack(packUUID, packUrl, hashArray, legacyPrompt, mandatory);
        }
    }
}
