package io.th0rgal.oraxen.pack.upload.hosts;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.MineHttpd;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

public class LocalHost implements HostingProvider {
    private int port;
    private String ip;
    private static MineHttpd httpd;
    private String sha1;

    public LocalHost() {
        port = OraxenPlugin.get().getConfigsManager().getSettings().getInt("Pack.upload.localhost.port", 8080);
        ip = getIp();
        ip = ip == null ? Bukkit.getIp() : ip;
    }

    private static String getIp() {
        try {
            URL url = new URL("https://api.ipify.org");
            InputStream stream = url.openStream();
            Scanner s = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A");
            String ip = s.next();
            s.close();
            stream.close();
            return ip;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        startHttpd();
        return OraxenPlugin.get().getResourcePack().getFile().exists();
    }

    @Override
    public String getPackURL() {
        return "https://" + ip + ":" + port + "/pack.zip";
    }

    @Override
    public String getMinecraftPackURL() {
        return "http://" + ip + ":" + port + "/pack.zip";
    }

    @Override
    public byte[] getSHA1() {
        try {
            return calcSHA1(OraxenPlugin.get().getResourcePack().getFile());
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public String getOriginalSHA1() {
        return bytesToHexString(getSHA1());
    }

    private static byte[] calcSHA1(File file) throws IOException, NoSuchAlgorithmException {
        FileInputStream fileInputStream = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest);
        byte[] bytes = new byte[1024];
        // read all file content
        while (digestInputStream.read(bytes) > 0) ;

        byte[] resultByteArry = digest.digest();
        digestInputStream.close();
        return resultByteArry;
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (value < 16) {
                // if value less than 16, then it's hex String will be only
                // one character, so we need to append a character of '0'
                sb.append("0");
            }
            sb.append(Integer.toHexString(value).toUpperCase());
        }
        return sb.toString();
    }

    public static void stopHttpd() {
        if (httpd != null) httpd.terminate();
    }

    private void startHttpd() {
        try {
            httpd = new MineHttpd(port) {
                @Override
                public File requestFileCallback(MineConnection connection) {
                    Player player = WebUtil.getAddress(connection);
                    if (player == null) {
                        Logs.logWarning("Unknown connection from '" + connection.getClient().getInetAddress() + "'. Aborting...");
                        return null;
                    }

                    return OraxenPlugin.get().getResourcePack().getFile();
                }
            };
            // Start the web server
            httpd.start();
            Logs.logSuccess("Successfully started the mini http daemon!");
        } catch (IOException e1) {
            Logs.logError("Unable to start the mini http daemon! Disabling...");
        }
    }

    public static class WebUtil {
        public static Player getAddress( MineHttpd.MineConnection connection ) {
            byte[] mac = connection.getClient().getInetAddress().getAddress();
            for ( Player player : Bukkit.getOnlinePlayers() ) {
                if ( Arrays.equals( player.getAddress().getAddress().getAddress(), mac ) ) {
                    return player;
                }
            }
            return null;
        }

        public static byte[] getMAC( InetAddress address ) {
            try {
                return NetworkInterface.getByInetAddress( address ).getHardwareAddress();
            } catch ( SocketException e ) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
