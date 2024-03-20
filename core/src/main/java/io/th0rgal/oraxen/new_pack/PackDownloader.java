package io.th0rgal.oraxen.new_pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PackDownloader {

    public static void downloadDefaultPack() {
        Logs.logInfo("Downloading default resourcepack...");
        if (VersionUtil.isCompiled()) Logs.logWarning("Skipping download of Oraxen pack, compiled versions do not include assets");
        else if (VersionUtil.isLeaked()) Logs.logError("Skipping download of Oraxen pack, pirated versions do not include assets");
        else {
            InputStream accessStream = OraxenPlugin.get().getResource("pack/token.secret");
            if (accessStream == null) {
                Logs.logWarning("Failed to download Default-Pack...");
                Logs.logWarning("Missing token-file, please contact the developer!");
                return;
            }

            YamlConfiguration accessYaml = OraxenYaml.loadConfiguration(new InputStreamReader(accessStream));
            String fileUrl = "https://api.github.com/repos/oraxen/DefaultPack/zipball/main";
            String token = accessYaml.getString("token", "");
            //String hash = accessYaml.getString("hash", "");
            Path zipPath = PackGenerator.externalPacks.resolve("DefaultPack.zip");

            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", !token.isEmpty() ? "token " + token : "");

                // Open input stream from the connection
                ZipInputStream zis = new ZipInputStream(connection.getInputStream());

                // Save the file to local disk
                try (FileOutputStream fos = new FileOutputStream(zipPath.toString())) {
                    ZipOutputStream zos = new ZipOutputStream(fos);
                    ZipEntry entry = zis.getNextEntry();

                    while (entry != null) {
                        if (!entry.isDirectory()) {
                            zos.putNextEntry(new ZipEntry(StringUtils.substringAfter(entry.getName(), "/")));

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = zis.read(buffer)) > 0) {
                                zos.write(buffer, 0, bytesRead);
                            }

                        }

                        zis.closeEntry();
                        entry = zis.getNextEntry();
                    }

                    zos.close();
                }
                zis.close();
                connection.disconnect();

                accessStream.close();
            } catch (IOException e) {
                if (Settings.DEBUG.toBool()) e.printStackTrace();
                else Logs.logWarning(e.getMessage());
                Logs.logError("Failed to download Oraxen pack...");
                Logs.logError("Please contact the developer!", true);
            }
        }
    }
}
