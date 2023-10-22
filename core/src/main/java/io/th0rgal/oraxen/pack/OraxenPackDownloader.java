package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.ZipUtils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Base64;

public class OraxenPackDownloader {

    public static void downloadPack() {
        String fileUrl = "http://repo.oraxen.com:8080/private/OraxenPack.zip";
        String username = "boy";
        String password = "8d/RFJR5S0vA3wZQ3xQfsXipFMJWCkmNSnVYTwXsJ5ZgPfjeefWN6tuDVpZgQsLW";
        Path zipPath = OraxenPlugin.get().getDataFolder().toPath().resolve("pack/DefaultPack.zip");

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
            ZipUtils.extractDefaultZipPack();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error downloading the file.");
        }
    }
}
