package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import gs.mclo.java.APIResponse;
import gs.mclo.java.Log;
import gs.mclo.java.MclogsAPI;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;

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

    CommandAPICommand getLogDumpCommand() {
        return new CommandAPICommand("dump_log")
                .withPermission("oraxen.command.dumplog")
                .executes((sender, args) -> {
                    String packUrl = "http://atlas.oraxen.com:8080/.*";
                    String logfile;

                    try {
                        Path path = OraxenPlugin.get().getDataFolder().getAbsoluteFile().getParentFile().getParentFile().toPath().resolve("logs/latest.log");
                        logfile = Files.readString(path).replaceAll(packUrl, "[REDACTED]");
                        if (VersionUtil.isLeaked()) logfile = logfile + "\n\nThis server is running a leaked version of Oraxen";
                    } catch (Exception e) {
                        Logs.logError("Failed to read latest.log, is it missing?");
                        if (Settings.DEBUG.toBool()) e.printStackTrace();
                        return;
                    }

                    try {
                        APIResponse post = MclogsAPI.share(new Log(logfile));
                        Logs.logSuccess("Logfile has been dumped to: " + post.url);
                    } catch (IOException e) {
                        Logs.logWarning("Failed to upload logfile to mclo.gs, attempting to using pastebin");
                        if (Settings.DEBUG.toBool()) e.printStackTrace();
                        try {
                            Logs.logSuccess("Logfile has been dumped to: " + postToPasteBin(logfile));
                        } catch (IOException ex) {
                            Logs.logError("Failed to use backup solution with pastebin");
                            if (Settings.DEBUG.toBool()) e.printStackTrace();
                        }
                    }
                });


    }

    private String postToPasteBin(String text) throws IOException {
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
            Logs.logWarning("Failed to read hastebin result");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        } finally {
            if (inputReader != null) inputReader.close();
            if (wr != null) wr.close();
            if (reader != null) reader.close();
        }

        if (response != null && response.contains("key")) {
            response = response.substring(response.indexOf(":") + 2, response.length() - 2);

            String postURL = "https://hastebin.com/raw/";
            response = postURL + response;
        }

        return response;
    }

}
