package io.th0rgal.oraxen.pack.upload.hosts;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.th0rgal.oraxen.utils.HashUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SelfHost implements HostingProvider {

    private final String host;
    private final int port;
    private final String domain;
    private HttpServer httpServer;
    private ExecutorService executor;
    private final String packUrl;
    private volatile HostedPack hostedPack;

    public SelfHost(ConfigurationSection config) {
        if (config == null) {
            this.host = "0.0.0.0";
            this.port = 8080;
            this.domain = "localhost:8080";
        } else {
            this.host = config.getString("host", "0.0.0.0");
            this.port = config.getInt("port", 8080);
            this.domain = config.getString("domain", "localhost:" + this.port);
        }
        this.packUrl = "http://" + domain + "/pack.zip";
    }

    @Override
    public synchronized boolean uploadPack(File resourcePack) {
        try {
            // Generation overwrites the source ZIP. Hash and serve the same snapshot
            // so downloads cannot observe a later rewrite or a partially written pack.
            byte[] bytes = Files.readAllBytes(resourcePack.toPath());
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(bytes);
            HostedPack nextPack = new HostedPack(bytes, HashUtils.bytesToHex(hash), UUID.nameUUIDFromBytes(hash));
            if (httpServer == null) {
                startServer();
            }
            hostedPack = nextPack;
            return true;
        } catch (Exception e) {
            Logs.logError("Failed to self-host the resource pack");
            e.printStackTrace();
            return false;
        }
    }

    private void startServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        executor = Executors.newFixedThreadPool(4);
        boolean started = false;
        try {
            httpServer.setExecutor(executor);

            HttpHandler packHandler = exchange -> {
                try {
                    // Keep this request's snapshot even if another upload completes.
                    HostedPack pack = hostedPack;
                    if (pack == null) {
                        exchange.sendResponseHeaders(503, -1);
                        return;
                    }
                    byte[] fileBytes = pack.bytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/zip");
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileBytes.length));
                    exchange.sendResponseHeaders(200, fileBytes.length);
                    exchange.getResponseBody().write(fileBytes);
                } finally {
                    exchange.close();
                }
            };

            httpServer.createContext("/pack.zip", packHandler);

            HttpHandler rootHandler = exchange -> {
                String response = "Oraxen Resource Pack Server\n";
                response += "Pack URL: " + packUrl + "\n";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            };
            httpServer.createContext("/", rootHandler);

            httpServer.start();
            started = true;
            Logs.logSuccess("Self-hosted resource pack server started on " + host + ":" + port);
        } finally {
            if (!started) {
                stopServer();
            }
        }
    }

    private void stopServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    @Override
    public String getPackURL() {
        return hostedPack == null ? null : packUrl;
    }

    @Override
    public byte[] getSHA1() {
        HostedPack pack = hostedPack;
        return pack == null ? null : HashUtils.hexToBytes(pack.sha1());
    }

    @Override
    public String getOriginalSHA1() {
        HostedPack pack = hostedPack;
        return pack == null ? null : pack.sha1();
    }

    @Override
    public UUID getPackUUID() {
        HostedPack pack = hostedPack;
        return pack == null ? null : pack.uuid();
    }

    private record HostedPack(byte[] bytes, String sha1, UUID uuid) {}
}
