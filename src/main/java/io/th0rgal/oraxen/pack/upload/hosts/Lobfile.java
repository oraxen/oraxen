package io.th0rgal.oraxen.pack.upload.hosts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.th0rgal.oraxen.utils.MultipartBody;
import io.th0rgal.oraxen.utils.HashUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.UUID;

public class Lobfile implements HostingProvider {

    private static final String UPLOAD_URL = "https://lobfile.com/api/v3/upload";
    private static final String DEFAULT_PACK_NAME = "Oraxen";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration UPLOAD_TIMEOUT = Duration.ofMinutes(5);

    private final String apiKey;
    private final String packName;
    private String packUrl;
    private String sha1;
    private UUID packUUID;

    public Lobfile(ConfigurationSection config) {
        this(
                config != null ? config.getString("api-key", "") : "",
                config != null ? config.getString("pack-name", DEFAULT_PACK_NAME) : DEFAULT_PACK_NAME
        );
    }

    Lobfile(String apiKey, String packName) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.packName = sanitizePackName(packName);
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        return uploadPackWithName(resourcePack, packName);
    }

    @Override
    public boolean uploadPack(File resourcePack, String packVersion) {
        return uploadPackWithName(resourcePack, buildVersionedPackName(packVersion));
    }

    @Override
    public boolean requiresNewInstancePerUpload() {
        return true;
    }

    private boolean uploadPackWithName(File resourcePack, String uploadPackName) {
        if (apiKey.isBlank()) {
            Logs.logError("The Lobfile resource pack could not be uploaded because Pack.upload.lobfile.api-key is not set.");
            return false;
        }

        try (HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()) {
            PackHashes packHashes = calculateHashes(resourcePack);
            sha1 = packHashes.sha1();
            packUUID = UUID.nameUUIDFromBytes(HashUtils.hexToBytes(sha1));

            MultipartBody body = MultipartBody.create()
                    .addPart("file", resourcePack, buildUploadFileName(uploadPackName), "application/octet-stream")
                    .addPart("sha_256", packHashes.sha256());
            HttpRequest request = HttpRequest.newBuilder(URI.create(UPLOAD_URL))
                    .timeout(UPLOAD_TIMEOUT)
                    .header("X-API-Key", apiKey)
                    .header("Content-Type", body.contentType())
                    .POST(body.bodyPublisher())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseString = response.body();
            if (statusCode < 200 || statusCode >= 300) {
                Logs.logError("Lobfile returned HTTP " + statusCode + " for the resource pack upload.");
                JsonObject errorOutput = parseResponse(responseString);
                if (errorOutput != null) logUploadError(errorOutput);
                return false;
            }
            JsonObject jsonOutput = parseResponse(responseString);
            if (jsonOutput == null) return false;

            if (jsonOutput.has("success") && jsonOutput.get("success").getAsBoolean() && jsonOutput.has("url")) {
                packUrl = jsonOutput.get("url").getAsString();
                return true;
            }

            logUploadError(jsonOutput);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Logs.logError("The resource pack has not been uploaded to Lobfile.");
            if (ex.getMessage() != null) Logs.logWarning(ex.getMessage());
            Logs.debug(ex);
            return false;
        } catch (IllegalStateException | UncheckedIOException | IOException | NoSuchAlgorithmException ex) {
            Logs.logError("The resource pack has not been uploaded to Lobfile.");
            if (ex.getMessage() != null) Logs.logWarning(ex.getMessage());
            Logs.debug(ex);
            return false;
        }
    }

    private static PackHashes calculateHashes(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                sha1.update(buffer, 0, read);
                sha256.update(buffer, 0, read);
            }
        }
        return new PackHashes(HashUtils.bytesToHex(sha1.digest()), HashUtils.bytesToHex(sha256.digest()));
    }

    private record PackHashes(String sha1, String sha256) {
    }

    private JsonObject parseResponse(String responseString) {
        if (responseString == null || responseString.isBlank()) {
            Logs.logError("The resource pack could not be uploaded to Lobfile because the response body was empty.");
            return null;
        }

        try {
            return JsonParser.parseString(responseString).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            Logs.logError("The resource pack could not be uploaded to Lobfile due to a malformed response.");
            Logs.logWarning("Response: " + responseString);
            return null;
        }
    }

    private void logUploadError(JsonObject jsonOutput) {
        if (jsonOutput.has("error")) {
            JsonElement error = jsonOutput.get("error");
            Logs.logError("Lobfile error: " + (error.isJsonPrimitive() ? error.getAsString() : error));
        } else {
            Logs.logError("Lobfile did not return an upload URL.");
        }
        Logs.logError("Response: " + jsonOutput);
    }

    @NotNull
    static String buildUploadFileName(String packName) {
        String sanitized = sanitizePackName(packName);
        return sanitized.toLowerCase().endsWith(".zip") ? sanitized : sanitized + ".zip";
    }

    @NotNull
    static String sanitizePackName(String packName) {
        if (packName == null || packName.isBlank()) return DEFAULT_PACK_NAME;
        String sanitized = packName.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? DEFAULT_PACK_NAME : sanitized;
    }

    @NotNull
    String buildVersionedPackName(String packVersion) {
        String baseName = packName.toLowerCase().endsWith(".zip")
                ? packName.substring(0, packName.length() - 4)
                : packName;
        return baseName + "_" + sanitizePackName(packVersion);
    }

    @Override
    public String getPackURL() {
        return packUrl;
    }

    @Override
    public byte[] getSHA1() {
        return sha1 != null ? HashUtils.hexToBytes(sha1) : null;
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
