package io.th0rgal.oraxen.commands;

import gs.mclo.java.APIResponse;
import gs.mclo.java.Log;
import gs.mclo.java.MclogsAPI;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.LU;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LogDumpCommand {

    OraxenCommand getLogDumpCommand() {
        return new OraxenCommand("dump-log")
                .withPermission("oraxen.command.dumplog")
                .executes((sender, args) -> {
                    SchedulerUtil.runTaskAsync(() -> {
                        String packUrl = "http://atlas.oraxen.com:8080/.*";
                        String logfile;

                        try {
                            Path path = OraxenPlugin.get().getDataFolder().getAbsoluteFile().getParentFile().getParentFile()
                                    .toPath().resolve("logs/latest.log");
                            logfile = Files.readString(path).replaceAll(packUrl, "*redacted pack url*");
                            logfile += "\n\n" + new LU().hr();
                        } catch (Exception e) {
                            report(sender, Message.MISSING_LOGS);
                            if (Settings.DEBUG.toBool())
                                e.printStackTrace();
                            return;
                        }

                        try {
                            APIResponse post = MclogsAPI.share(new Log(logfile));
                            report(sender, Message.LOGFILE_DUMPED, AdventureUtils.tagResolver("uri", post.url));
                        } catch (IOException e) {
                            report(sender, Message.LOGFILE_MCLOG_ERROR);
                            if (Settings.DEBUG.toBool())
                                e.printStackTrace();
                            try {
                                report(sender, Message.LOGFILE_DUMPED,
                                        AdventureUtils.tagResolver("uri", postToPasteBin(logfile)));
                            } catch (IOException ex) {
                                report(sender, Message.LOGFILE_PASTEBIN_ERROR);
                                if (Settings.DEBUG.toBool())
                                    ex.printStackTrace();
                            }
                        }
                    });
                });

    }

    /**
     * Reports the outcome to the console and, when the command was not run from the console,
     * to the sender as well so a player does not get a silent no-op.
     */
    private void report(CommandSender sender, Message message, TagResolver... placeholders) {
        message.log(placeholders);
        if (sender != null && !(sender instanceof ConsoleCommandSender))
            message.send(sender, placeholders);
    }

    private String postToPasteBin(String text) throws IOException {
        byte[] postData = text.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;

        String requestURL = "https://hastebin.com/documents";
        URL url = URI.create(requestURL).toURL();
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
            Logs.logWarning("Failed to read hastebin result.");
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
        } finally {
            if (inputReader != null)
                inputReader.close();
            if (wr != null)
                wr.close();
            if (reader != null)
                reader.close();
        }

        if (response != null && response.contains("key")) {
            response = response.substring(response.indexOf(":") + 2, response.length() - 2);

            String postURL = "https://hastebin.com/raw/";
            response = postURL + response;
        }

        return response;
    }

}
