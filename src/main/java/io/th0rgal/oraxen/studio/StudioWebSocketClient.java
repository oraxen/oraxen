package io.th0rgal.oraxen.studio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * Outbound Phoenix-channel WebSocket client for Supabase Realtime used by Oraxen Studio.
 */
public class StudioWebSocketClient {

    private final String realtimeWsUrl;
    private final String channelName;
    private final BiConsumer<String, JsonObject> onBroadcastEvent;
    private final Runnable onConnected;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong refCounter = new AtomicLong(1);

    private volatile WebSocket webSocket;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatFuture;

    public StudioWebSocketClient(
            String realtimeWsUrl,
            String channelName,
            BiConsumer<String, JsonObject> onBroadcastEvent,
            Runnable onConnected) {
        this.realtimeWsUrl = realtimeWsUrl;
        this.channelName = channelName;
        this.onBroadcastEvent = onBroadcastEvent;
        this.onConnected = onConnected;
    }

    public void start() {
        if (closed.get()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Oraxen-Studio-WS-" + channelName);
            t.setDaemon(true);
            return t;
        });
        connectNow();
    }

    private void connectNow() {
        if (closed.get()) return;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(realtimeWsUrl), new Listener())
                    .whenComplete((ws, err) -> {
                        if (err != null || closed.get()) {
                            connected.set(false);
                            if (ws != null) {
                                try {
                                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "closed");
                                } catch (Exception ignored) {
                                }
                            }
                            if (!closed.get()) scheduleReconnect();
                            return;
                        }
                        this.webSocket = ws;
                        sendJoin(ws);
                        startHeartbeat(ws);
                    });
        } catch (Exception e) {
            connected.set(false);
            scheduleReconnect();
        }
    }

    private void sendJoin(WebSocket ws) {
        String topic = channelName.startsWith("realtime:") ? channelName : "realtime:" + channelName;
        JsonObject config = new JsonObject();
        JsonObject broadcast = new JsonObject();
        broadcast.addProperty("self", false);
        broadcast.addProperty("ack", false);
        config.add("broadcast", broadcast);

        JsonObject payload = new JsonObject();
        payload.add("config", config);

        JsonObject joinMsg = new JsonObject();
        joinMsg.addProperty("topic", topic);
        joinMsg.addProperty("event", "phx_join");
        joinMsg.add("payload", payload);
        joinMsg.addProperty("ref", String.valueOf(refCounter.getAndIncrement()));

        ws.sendText(joinMsg.toString(), true);
    }

    private void startHeartbeat(WebSocket ws) {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
        }
        if (scheduler == null || scheduler.isShutdown()) return;
        heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            if (closed.get() || ws.isOutputClosed()) return;
            JsonObject hb = new JsonObject();
            hb.addProperty("topic", "phoenix");
            hb.addProperty("event", "heartbeat");
            hb.add("payload", new JsonObject());
            hb.addProperty("ref", String.valueOf(refCounter.getAndIncrement()));
            ws.sendText(hb.toString(), true);
        }, 25, 25, TimeUnit.SECONDS);
    }

    private void scheduleReconnect() {
        if (closed.get() || scheduler == null || scheduler.isShutdown()) return;
        scheduler.schedule(this::connectNow, 5, TimeUnit.SECONDS);
    }

    public boolean isConnected() {
        return connected.get() && !closed.get();
    }

    public String getChannelName() {
        return channelName;
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        connected.set(false);
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
        WebSocket ws = this.webSocket;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "closing");
            } catch (Exception ignored) {
            }
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    static void dispatchIncomingRealtimeFrame(String rawJson, Runnable onJoinAck, BiConsumer<String, JsonObject> onBroadcast) {
        try {
            JsonElement parsed = JsonParser.parseString(rawJson);
            if (!parsed.isJsonObject()) return;
            JsonObject root = parsed.getAsJsonObject();
            String event = getStringOrDefault(root, "event", "");
            if ("phx_reply".equals(event)) {
                handleJoinReply(root, onJoinAck);
            } else if ("broadcast".equals(event) && onBroadcast != null) {
                handleBroadcastFrame(root, onBroadcast);
            }
        } catch (Exception e) {
            Logs.debug(e);
        }
    }

    private static void handleJoinReply(JsonObject root, Runnable onJoinAck) {
        if (onJoinAck == null) return;
        JsonObject payload = getObjectOrNull(root, "payload");
        if (payload != null && "ok".equals(getStringOrDefault(payload, "status", ""))) {
            onJoinAck.run();
        }
    }

    private static void handleBroadcastFrame(JsonObject root, BiConsumer<String, JsonObject> onBroadcast) {
        JsonObject outerPayload = getObjectOrNull(root, "payload");
        if (outerPayload == null) {
            onBroadcast.accept("sync", new JsonObject());
            return;
        }
        String innerEvent = getStringOrDefault(outerPayload, "event", "sync");
        JsonObject innerPayload = getObjectOrNull(outerPayload, "payload");
        onBroadcast.accept(innerEvent, innerPayload != null ? innerPayload : outerPayload);
    }

    private static String getStringOrDefault(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        return obj.get(key).getAsString();
    }

    private static JsonObject getObjectOrNull(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonObject()) return null;
        return obj.getAsJsonObject(key);
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                dispatchIncomingRealtimeFrame(
                        message,
                        () -> {
                            if (connected.compareAndSet(false, true) && onConnected != null) {
                                onConnected.run();
                            }
                        },
                        onBroadcastEvent);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected.set(false);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected.set(false);
            scheduleReconnect();
        }
    }
}
