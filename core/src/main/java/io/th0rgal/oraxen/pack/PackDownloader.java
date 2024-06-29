package io.th0rgal.oraxen.pack;

import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
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

            try(InputStreamReader accessReader = new InputStreamReader(accessStream)) {
                YamlConfiguration accessYaml = OraxenYaml.loadConfiguration(accessReader);
                String fileUrl = "https://api.github.com/repos/oraxen/DefaultPack/zipball/main";
                String token = accessYaml.getString("token", "");
                String hash = checkDefaultPackHash(token);
                Path zipPath = PackGenerator.externalPacks.resolve("DefaultPack_" + hash + ".zip");

                if (zipPath.toFile().exists() || zipPath.resolveSibling("DefaultPack_" + hash).toFile().exists()) {
                    Logs.logSuccess("Skipped downloading DefaultPack as it is up to date!");
                    return;
                }

                try {
                    HttpURLConnection connection = (HttpURLConnection) URI.create(fileUrl).toURL().openConnection();
                    connection.setRequestProperty("Accept", "application/vnd.github+json");
                    connection.setRequestProperty("Authorization", !token.isEmpty() ? "Bearer " + token : "");
                    connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");

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
            } catch (Exception e) {
                if (Settings.DEBUG.toBool()) e.printStackTrace();
                else Logs.logWarning(e.getMessage());
                Logs.logError("Failed to download Oraxen pack...");
                Logs.logError("Please contact the developer!", true);
            }
        }
    }

    private static String checkDefaultPackHash(String token) throws MalformedURLException {
        try {
            URL url = new URL("https://api.github.com/repos/oraxen/DefaultPack/commits");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", !token.isEmpty() ? "token " + token : "");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    return JsonParser.parseReader(reader).getAsJsonArray().get(0).getAsJsonObject().get("sha").getAsString();
                }
            }
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            else Logs.logWarning(e.getMessage());
        }

        return null;
    }
}
