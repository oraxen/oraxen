package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.pack.upload.hosts.TransferDotSh;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;

public class UploadManager {

    boolean enabled;
    HostingProvider hostingProvider;

    public UploadManager() {
        this.enabled = true;
        this.hostingProvider = getHostingProvider();
    }

    public void uploadAsyncIfEnabled(File file) {
        if (!enabled)
            return;
        long time = System.currentTimeMillis();
        Logs.log(ChatColor.GREEN, "Automatic upload of the resource pack is enabled, uploading...");
        Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
            hostingProvider.uploadPack(file);
            Logs.log(ChatColor.GREEN, "Resourcepack uploaded on url "
                    + hostingProvider.getPackURL() + " in " + (System.currentTimeMillis() - time) + "ms");
        });
    }

    private HostingProvider getHostingProvider() {
        return new TransferDotSh();
    }

}
