package io.th0rgal.oraxen.studio;

import com.sun.net.httpserver.HttpServer;
import io.th0rgal.oraxen.studio.StudioApiClient.FileEntry;
import io.th0rgal.oraxen.studio.StudioApiClient.StudioApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudioApiClientTest {

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private StudioApiClient client() {
        return new StudioApiClient("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private void respond(String path, int status, String json) {
        server.createContext(path, exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @Test
    void readsSupportedVersions() throws Exception {
        respond("/api/studio/meta", 200, "{\"api_versions\":[1,2],\"latest\":2}");
        assertEquals(java.util.List.of(1, 2), client().supportedVersions());
    }

    @Test
    void beginPushSendsFilesBaseAndForceWithBearerToken() throws Exception {
        respond("/api/v1/studio/pushes", 201,
                "{\"push_id\":\"p1\",\"uploads\":[{\"path\":\"items/a.yml\",\"url\":\"http://u\"}],\"deletes\":[\"items/old.yml\"],\"unchanged\":3}");
        StudioApiClient.PushPlan plan = client().beginPush("oxn_studio_t",
                Map.of("items/a.yml", new FileEntry("ab".repeat(32), 5)), null, true);

        assertEquals("p1", plan.pushId());
        assertEquals("items/a.yml", plan.uploads().getFirst().path());
        assertEquals(java.util.List.of("items/old.yml"), plan.deletes());
        assertEquals(3, plan.unchanged());
        assertEquals("Bearer oxn_studio_t", lastAuth.get());
        assertTrue(lastBody.get().contains("\"sha256\":\"" + "ab".repeat(32) + "\""));
        assertTrue(lastBody.get().contains("\"base\":null"));
        assertTrue(lastBody.get().contains("\"force\":true"));
    }

    @Test
    void mapsErrorResponsesToStudioApiException() {
        respond("/api/v1/studio/pushes", 409,
                "{\"error\":{\"code\":\"conflict\",\"message\":\"Files changed\",\"conflict_count\":2,\"conflicts\":[{\"path\":\"items/a.yml\"}]}}");
        StudioApiException e = assertThrows(StudioApiException.class,
                () -> client().beginPush("t", Map.of(), Map.of(), false));
        assertEquals(409, e.status());
        assertEquals("conflict", e.code());
        assertEquals("Files changed", e.getMessage());
        assertEquals(2, e.details().get("conflict_count").getAsInt());
    }

    @Test
    void reportsNonJsonErrorsWithTheirStatus() {
        respond("/api/v1/studio/manifest", 502, "<html>Bad gateway</html>");
        StudioApiException e = assertThrows(StudioApiException.class, () -> client().manifest("t"));
        assertEquals(502, e.status());
        assertEquals("http_502", e.code());
    }

    @Test
    void pollPairingReturnsNullWhilePendingAndTheTokenOnceApproved() throws Exception {
        StudioApiClient.Pairing pairing = new StudioApiClient.Pairing("ABCDEFGHJK", "poll", "http://x", 2);
        respond("/api/v1/studio/pairings/ABCDEFGHJK", 200, "{\"status\":\"pending\"}");
        assertEquals(null, client().pollPairing(pairing));
        assertEquals("Bearer poll", lastAuth.get());
        server.removeContext("/api/v1/studio/pairings/ABCDEFGHJK");
        respond("/api/v1/studio/pairings/ABCDEFGHJK", 200,
                "{\"status\":\"approved\",\"token\":\"oxn_studio_x\",\"workspace_id\":\"w\",\"workspace_name\":\"My WS\"}");
        StudioApiClient.PairingResult result = client().pollPairing(pairing);
        assertEquals("oxn_studio_x", result.token());
        assertEquals("My WS", result.workspaceName());
    }
}
