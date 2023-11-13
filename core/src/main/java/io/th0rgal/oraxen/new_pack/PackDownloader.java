package io.th0rgal.oraxen.new_pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.ZipUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Base64;

public class PackDownloader {

    public static void downloadDefaultPack() {
        OraxenPlugin.get().saveResource("pack/token.secret", true);
        if (!VersionUtil.isPremium()) {
            if (VersionUtil.isCompiled()) Logs.logWarning("Skipping download of Oraxen pack, compiled versions do not include assets");
            else Logs.logError("Skipping download of Oraxen pack, pirated versions do not include assets");
            return;
        }

        YamlConfiguration accessYaml = OraxenYaml.loadConfiguration(OraxenPlugin.get().packPath().resolve("token.secret").toFile());
        String fileUrl = "https://repo.oraxen.com/assets/defaultPack/DefaultPack.zip";
        String username = accessYaml.getString("username", "");
        String password = accessYaml.getString("password", "");
        Path zipPath = PackGenerator.externalPacks.resolve("DefaultPack.zip");

        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set up basic authentication
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            String authHeaderValue = "Basic " + encodedAuth;
            connection.setRequestProperty("Authorization", authHeaderValue);

            // Open input stream from the connection
            InputStream inputStream = new BufferedInputStream(connection.getInputStream());

            // Save the file to local disk
            try (FileOutputStream fos = new FileOutputStream(zipPath.toString())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            //ZipUtils.extractDefaultZipPack();
        } catch (IOException e) {
            Logs.logError("Failed to download Oraxen pack");
        }
    }
}
