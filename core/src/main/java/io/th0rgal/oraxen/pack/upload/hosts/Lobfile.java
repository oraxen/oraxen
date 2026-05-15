package io.th0rgal.oraxen.pack.upload.hosts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.th0rgal.oraxen.utils.SHA1Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

public class Lobfile implements HostingProvider {

    private static final String UPLOAD_URL = "https://lobfile.com/api/v3/upload";
    private static final String DEFAULT_PACK_NAME = "Oraxen";

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

    private boolean uploadPackWithName(File resourcePack, String uploadPackName) {
        if (apiKey.isBlank()) {
            Logs.logError("The Lobfile resource pack could not be uploaded because Pack.upload.lobfile.api-key is not set.");
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            calculateSHA1(resourcePack);

            HttpPost request = new HttpPost(UPLOAD_URL);
            request.setHeader("X-API-Key", apiKey);
            request.setEntity(createUploadEntity(resourcePack, uploadPackName));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseString = EntityUtils.toString(response.getEntity());
                JsonObject jsonOutput = parseResponse(responseString);
                if (jsonOutput == null) return false;

                if (jsonOutput.has("success") && jsonOutput.get("success").getAsBoolean() && jsonOutput.has("url")) {
                    packUrl = jsonOutput.get("url").getAsString();
                    return true;
                }

                logUploadError(jsonOutput);
                return false;
            }
        } catch (IllegalStateException | IOException | NoSuchAlgorithmException ex) {
            Logs.logError("The resource pack has not been uploaded to Lobfile.");
            if (ex.getMessage() != null) Logs.logWarning(ex.getMessage());
            if (io.th0rgal.oraxen.config.Settings.DEBUG.toBool()) ex.printStackTrace();
            return false;
        }
    }

    private HttpEntity createUploadEntity(File resourcePack, String uploadPackName) throws IOException, NoSuchAlgorithmException {
        return MultipartEntityBuilder.create()
                .addBinaryBody("file", resourcePack, ContentType.APPLICATION_OCTET_STREAM, buildUploadFileName(uploadPackName))
                .addTextBody("sha_256", digest(resourcePack, "SHA-256"))
                .build();
    }

    private void calculateSHA1(File file) throws IOException, NoSuchAlgorithmException {
        sha1 = digest(file, "SHA-1");
        packUUID = UUID.nameUUIDFromBytes(sha1.getBytes());
    }

    private static String digest(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return SHA1Utils.bytesToHex(digest.digest());
    }

    private JsonObject parseResponse(String responseString) {
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
        return sanitized.toLowerCase(Locale.ROOT).endsWith(".zip") ? sanitized : sanitized + ".zip";
    }

    @NotNull
    static String sanitizePackName(String packName) {
        if (packName == null || packName.isBlank()) return DEFAULT_PACK_NAME;
        String sanitized = packName.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? DEFAULT_PACK_NAME : sanitized;
    }

    @NotNull
    String buildVersionedPackName(String packVersion) {
        String baseName = packName.toLowerCase(Locale.ROOT).endsWith(".zip")
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
