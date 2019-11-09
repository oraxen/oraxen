package io.th0rgal.oraxen.pack.upload.hosts;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TransferDotSh implements HostingProvider {

    private String packURL;

    @Override
    public void uploadPack(File resourcePack) {
        String url = "https://transfer.sh/pack.zip";
        String charset = "UTF-8";
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("PUT");
        } catch (IOException e) {
            e.printStackTrace();
        }

        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = connection.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true)) {
            // Send binary file.
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"").append(resourcePack.getName()).append("\"").append(CRLF);
            writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(resourcePack.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            Files.copy(resourcePack.toPath(), output);
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // End of multipart/form-data.
            writer.append("--").append(boundary).append("--").append(CRLF).flush();
            // Request is lazily fired whenever you need to obtain information about response.

            if (connection.getResponseCode() == 200) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = connection.getInputStream().read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                buffer.flush();
                byte[] byteArray = buffer.toByteArray();

                this.packURL = new String(byteArray, StandardCharsets.UTF_8).replace("https://transfer.sh/", "https://transfer.sh/get/");
            } else {
                throw new RuntimeException();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPackURL() {
        return packURL;
    }

}
