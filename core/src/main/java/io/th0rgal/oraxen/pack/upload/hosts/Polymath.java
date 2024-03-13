package io.th0rgal.oraxen.pack.upload.hosts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Polymath implements HostingProvider {

    private final String serverAddress;
    private String packUrl;
    private String minecraftPackURL;
    private String sha1;
    private UUID packUUID;

    public Polymath(String serverAddress) {
        this.serverAddress = (serverAddress.startsWith("http://") || serverAddress.startsWith("https://") ? "" : "https://") + serverAddress + (serverAddress.endsWith("/") ? "" : "/");
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(serverAddress + "upload");

            HttpEntity httpEntity = MultipartEntityBuilder
                    .create().addTextBody("id", Settings.POLYMATH_SECRET.toString())
                    .addBinaryBody("pack", resourcePack)
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
                return false;
            }
            if (jsonOutput.has("url") && jsonOutput.has("sha1")) {
                packUrl = jsonOutput.get("url").getAsString();
                minecraftPackURL = packUrl.replace("https://", "http://");
                sha1 = jsonOutput.get("sha1").getAsString();
                packUUID = UUID.nameUUIDFromBytes(sha1.getBytes());
                return true;
            }

            if (jsonOutput.has("error"))
                Logs.logError("Error: " + jsonOutput.get("error").getAsString());
            Logs.logError("Response: " + jsonOutput);
            Logs.logError("The resource pack has not been uploaded to the server. Usually this is due to an excessive size.");
            return false;
        } catch(IllegalStateException | IOException ex) {
            Logs.logError("The resource pack has not been uploaded to the server. Usually this is due to an excessive size.");
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public String getPackURL() {
        return packUrl;
    }

    @Override
    public String getMinecraftPackURL() {
        return minecraftPackURL;
    }

    @Override
    public byte[] getSHA1() {
        int len = sha1.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(sha1.charAt(i), 16) << 4)
                    + Character.digit(sha1.charAt(i + 1), 16));
        }
        return data;
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
