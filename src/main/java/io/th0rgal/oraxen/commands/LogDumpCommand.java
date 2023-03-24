package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import gs.mclo.api.Log;
import gs.mclo.api.MclogsClient;
import gs.mclo.api.response.UploadLogResponse;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.http.protocol.RequestUserAgent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LogDumpCommand {

    public CommandAPICommand getLogDumpCommand() {
        return new CommandAPICommand("dump_log")
                .withPermission("oraxen.command.dumplog")
                .executes((sender, args) -> {
                    String packUrl = OraxenPlugin.get().getUploadManager().getHostingProvider().getPackURL();
                    String logfile;

                    try {
                        String path = OraxenPlugin.get().getDataFolder().getAbsoluteFile().getParentFile().getParentFile().getAbsolutePath();
                        logfile = Files.readString(Path.of(path + "/logs/latest.log"));
                        logfile = logfile.replace(packUrl, "[REDACTED]");
                    } catch (Exception e) {
                        Logs.logError("Failed to read latest.log, is it missing?");
                        return;
                    }

                    try {
                        UploadLogResponse post = new MclogsClient(new RequestUserAgent().toString()).uploadLog(new Log(logfile));
                        Logs.logSuccess("Logfile has been dumped to: " + post.getUrl());
                    } catch (IOException e) {
                        Logs.logWarning("Failed to upload logfile to mclo.gs, attempting to using pastebin");
                        try {
                            Logs.logSuccess("Logfile has been dumped to: " + postToPasteBin(logfile, true));
                        } catch (IOException ex) {
                            Logs.logError("Failed to use backup solution with pastebin");
                            e.printStackTrace();
                        }
                    }
                });


    }

    private String postToPasteBin(String text, boolean raw) throws IOException {
        byte[] postData = text.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;

        String requestURL = "https://hastebin.com/documents";
        URL url = new URL(requestURL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "Hastebin Java Api");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);

        String response = null;
        DataOutputStream wr = null;
        InputStreamReader inputReader = null;
        BufferedReader reader = null;
        try {
            wr = new DataOutputStream(conn.getOutputStream());
            wr.write(postData);
            inputReader = new InputStreamReader(conn.getInputStream());
            reader = new BufferedReader(inputReader);
            response = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputReader != null) inputReader.close();
            if (wr != null) wr.close();
            if (reader != null) reader.close();
        }

        if (response != null && response.contains("key")) {
            response = response.substring(response.indexOf(":") + 2, response.length() - 2);

            String postURL = raw ? "https://hastebin.com/raw/" : "https://hastebin.com/";
            response = postURL + response;
        }

        return response;
    }

}
