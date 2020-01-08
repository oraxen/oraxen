package io.th0rgal.oraxen.pack.upload.hosts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxstudio.utils.CUrl;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Polymath implements HostingProvider {

    private final String serverAddress;
    private String packUrl;
    private String sha1;

    public Polymath(String serverAddress) {
        this.serverAddress = "http://" + serverAddress + "/";
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        try {
            CUrl curl = new CUrl(serverAddress + "upload")
                    .form("id", "%%__USER__%%")
                    .form("pack", new CUrl.FileIO(resourcePack.getPath()));
            JsonObject jsonOutput = (JsonObject) new JsonParser().parse(new String(curl.exec(), StandardCharsets.UTF_8));
            packUrl = jsonOutput.get("url").getAsString();
            sha1 = jsonOutput.get("sha1").getAsString();
            return true;

        } catch (Exception exception) { //if upload failed
            exception.printStackTrace();
            return false;
        }
    }

    @Override
    public String getPackURL() {
        return packUrl;
    }

    @Override
    public byte[] getSHA1() {
        int len = sha1.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(sha1.charAt(i), 16) << 4)
                    + Character.digit(sha1.charAt(i+1), 16));
        }
        return data;
    }

}
