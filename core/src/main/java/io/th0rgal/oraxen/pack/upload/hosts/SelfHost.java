package io.th0rgal.oraxen.pack.upload.hosts;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.th0rgal.oraxen.utils.SHA1Utils;
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
    private String packUrl;
    private String sha1;
    private UUID packUUID;
    private File packFile;

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
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        try {
            this.packFile = resourcePack;
            stopServer();
            calculateSHA1(resourcePack);
            this.packUrl = "http://" + domain + "/pack.zip";
            startServer(resourcePack);
            return true;
        } catch (Exception e) {
            Logs.logError("Failed to self-host the resource pack");
            e.printStackTrace();
            return false;
        }
    }

    private void calculateSHA1(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        this.sha1 = SHA1Utils.bytesToHex(hashBytes);
        this.packUUID = UUID.nameUUIDFromBytes(sha1.getBytes());
    }

    private void startServer(File packFile) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        executor = Executors.newFixedThreadPool(4);
        boolean started = false;
        try {
            httpServer.setExecutor(executor);

            HttpHandler packHandler = exchange -> {
                try {
                    byte[] fileBytes = Files.readAllBytes(packFile.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "application/zip");
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileBytes.length));
                    exchange.sendResponseHeaders(200, fileBytes.length);
                    exchange.getResponseBody().write(fileBytes);
                    exchange.getResponseBody().close();
                } catch (IOException e) {
                    exchange.sendResponseHeaders(500, -1);
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
            if (!started && executor != null) {
                executor.shutdownNow();
                executor = null;
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
