package io.th0rgal.oraxen.pack.upload.hosts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.MultipartBody;
import io.th0rgal.oraxen.utils.SHA1Utils;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public class Polymath implements HostingProvider {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration UPLOAD_TIMEOUT = Duration.ofMinutes(5);

    private final String serverAddress;
    private String packUrl;
    private String sha1;
    private UUID packUUID;

    public Polymath(String serverAddress) {
        this.serverAddress = (serverAddress.startsWith("http://") || serverAddress.startsWith("https://") ? "" : "https://") + serverAddress + (serverAddress.endsWith("/") ? "" : "/");
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        try (HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()) {
            MultipartBody body = MultipartBody.create()
                    .addPart("id", Settings.POLYMATH_SECRET.toString())
                    .addPart("pack", resourcePack);
            HttpRequest request = HttpRequest.newBuilder(URI.create(serverAddress + "upload"))
                    .timeout(UPLOAD_TIMEOUT)
                    .header("Content-Type", body.contentType())
                    .POST(body.bodyPublisher())
                    .build();

            String responseString = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            JsonObject jsonOutput;
            try {
                jsonOutput = JsonParser.parseString(responseString).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                Logs.logError("The resource pack could not be uploaded due to a malformed response.");
                Logs.logWarning("This is usually due to the resourcepack server being down.");
                return false;
            }
            if (jsonOutput.has("url") && jsonOutput.has("sha1")) {
                packUrl = jsonOutput.get("url").getAsString();
                sha1 = jsonOutput.get("sha1").getAsString();
                packUUID = UUID.nameUUIDFromBytes(SHA1Utils.hexToBytes(sha1));
                return true;
            }

            if (jsonOutput.has("error"))
                Logs.logError("Error: " + jsonOutput.get("error").getAsString());
            Logs.logError("Response: " + jsonOutput);
            Logs.logError("The resource pack has not been uploaded to the server. Usually this is due to an excessive size.");
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Logs.logError("The resource pack has not been uploaded to the server. Usually this is due to an excessive size.");
            if (ex.getMessage() != null) Logs.logWarning(ex.getMessage());
            Logs.debug(ex);
            return false;
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException | IOException ex) {
            Logs.logError("The resource pack has not been uploaded to the server. Usually this is due to an excessive size.");
            if (ex.getMessage() != null) Logs.logWarning(ex.getMessage());
            Logs.debug(ex);
            return false;
        }
    }

    @Override
    public String getPackURL() {
        return packUrl;
    }

    @Override
    public byte[] getSHA1() {
        return SHA1Utils.hexToBytes(sha1);
    }

    @Override
    public String getOriginalSHA1() {
        return sha1;
    }

    @Override
    public UUID getPackUUID() {
        return packUUID;
    }

}
