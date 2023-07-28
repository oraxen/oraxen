package io.th0rgal.oraxen.pack.upload.hosts;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.util.JavalinLogger;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;

import javax.xml.bind.DatatypeConverter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LocalHost implements HostingProvider {
    private static Javalin app;
    private final String uploadDir;
    private final String packUrl;
    private final String ip;

    public LocalHost() {
        JavalinLogger.enabled = false; // Disables Javalin Server start messages
        Logs.logInfo("<blue>Starting Javalin server for hosting ResourcePack locally...");
        int port = OraxenPlugin.get().getConfigsManager().getSettings().getInt("Pack.upload.localhost.port", 8080);
        ip = OraxenPlugin.get().getConfigsManager().getSettings().getString("Pack.upload.localhost.ip", "localhost");
        app = Javalin.create().start(port);
        uploadDir = OraxenPlugin.get().getResourcePack().getFile().getParent();
        packUrl = "http://" + ip + ":" + port + "/pack.zip";
        setupEndpoints();
        Logs.logSuccess("Local Javalin server started on port " + port);
    }

    private void setupEndpoints() {
        app.post("/upload", ctx -> {
            UploadedFile file = ctx.uploadedFile("pack.zip");
            if (file != null) {
                Path path = Paths.get(uploadDir.replace(ip, "localhost"), file.filename());
                try (InputStream inputStream = file.content()) {
                    Files.createDirectories(path.getParent());
                    Files.delete(path);
                    Files.copy(inputStream, path);
                    Logs.logWarning("File uploaded successfully!");
                } catch (IOException e) {
                    Logs.logWarning("Error uploading file: " + e.getMessage());
                }
            } else {
                Logs.logWarning("No file uploaded");
            }
        });
        app.get("/pack.zip", ctx -> {
            File file = new File(uploadDir.replace(ip, "localhost"), "pack.zip");
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                ctx.result(fileBytes)
                        .header("Content-Type", "application/zip")
                        .header("Content-Disposition", "attachment; filename=" + file.getName());
            } catch (IOException e) {
                Logs.logWarning("Error downloading file: " + e.getMessage());
            }
        });
    }

    public static void stop() {
        if (app != null) app.stop();
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        try {
            String boundary = Long.toHexString(System.currentTimeMillis());
            String CRLF = "\r\n";
            URL url = new URL(packUrl.replace("/pack.zip", "/upload"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                out.writeBytes("--" + boundary + CRLF);
                out.writeBytes("Content-Disposition: form-data; name=\"pack.zip\"; filename=\"" + resourcePack.getName() + "\"" + CRLF);
                out.writeBytes(CRLF);
                try (FileInputStream in = new FileInputStream(resourcePack)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                out.writeBytes(CRLF);
                out.writeBytes("--" + boundary + "--" + CRLF);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            Logs.logWarning("Error uploading pack to localhost: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getPackURL() {
        return packUrl;
    }

    @Override
    public String getMinecraftPackURL() {
        return packUrl;
    }

    @Override
    public byte[] getSHA1() {
        try {
            File file = new File(uploadDir, "pack.zip");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(fileBytes);
            return md.digest();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getOriginalSHA1() {
        return DatatypeConverter.printHexBinary(getSHA1()).toLowerCase();
    }
}
