package io.th0rgal.oraxen.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@SuppressWarnings("ConstantConditions")
public class LU {

    private final String i = "%%__USER__%%";
    private final String in = "%%__NONCE__%%";
    private final String pmdl = "%%__POLYMART__%%";
    private final String pml = "%%__LICENSE__%%";
    private final String rid = "%%__RESOURCE__%%";

    public final String su = new String(Base64.getDecoder().decode("aHR0cHM6Ly9hcGkuc3BpZ290bWMub3JnL3NpbXBsZS8wLjIvaW5kZXgucGhwP2FjdGlvbj1nZXRBdXRob3ImaWQ9"), StandardCharsets.UTF_8) + i;
    public final String pu = new String(Base64.getDecoder().decode("aHR0cHM6Ly9hcGkucG9seW1hcnQub3JnL3YxL2dldEFjY291bnRJbmZvP3VzZXJfaWQ9"), StandardCharsets.UTF_8) + i;

    public void l() {
        Logs.logInfo(hr(su, "s"));
        Logs.logInfo(hr(pu, "p"));
    }

    public String hr() {
        if (VersionUtil.isCompiled()) return "";
        return hr(su, "s") + "\n" + hr(pu, "p");
    }

    public String hr(String ur, String p) {
        if (VersionUtil.isCompiled()) return "";
        String u = new String(Base64.getDecoder().decode("dXNlcm5hbWU="), StandardCharsets.UTF_8);
        String i = new String(Base64.getDecoder().decode("aWRlbnRpdGllcw=="), StandardCharsets.UTF_8);
        String d = new String(Base64.getDecoder().decode("ZGlzY29yZA=="), StandardCharsets.UTF_8);

        // Bounded timeouts: without them a black-holed connection stalls the caller forever.
        try (HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ur))
                    .timeout(Duration.ofSeconds(15))
                    .GET().build();
            String responseString;
            try {
                responseString = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            } catch (IOException e) {
                return "";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
            JsonObject jsonOutput;
            try {
                jsonOutput = JsonParser.parseString(responseString).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                return "";
            }
            if (jsonOutput.equals(new JsonObject())) {
                Logs.logInfo(p + ": jo i e");
                return "";
            }

            jsonOutput = jsonOutput.has("response") ? jsonOutput.get("response").getAsJsonObject() : jsonOutput;
            jsonOutput = jsonOutput.has("user") ? jsonOutput.get("user").getAsJsonObject() : jsonOutput;

            JsonElement ij = jsonOutput.get(i);
            String ju = String.valueOf(jsonOutput.getAsJsonObject().get(u).toString());
            String did = ij != null && ij.isJsonObject() ? String.valueOf(ij.getAsJsonObject().get(d).toString()) : "null";
            return p + ": " + this.i + " | " + ju + " | " + did;
        } catch (NullPointerException | IllegalStateException | IllegalArgumentException ex) {
            return "";
        }
    }

}
