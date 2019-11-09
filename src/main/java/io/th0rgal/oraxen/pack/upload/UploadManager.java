package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.pack.upload.hosts.TransferDotSh;
import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.utils.logs.Logs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class UploadManager {

    Plugin plugin;
    boolean enabled;
    HostingProvider hostingProvider;

    public UploadManager(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = (boolean) Pack.UPLOAD.getValue();
        this.hostingProvider = getHostingProvider();
    }

    public void uploadAsyncAndSendToPlayers(File file) {
        if (!enabled)
            return;
        long time = System.currentTimeMillis();
        Logs.log(ChatColor.GREEN, "Automatic upload of the resource pack is enabled, uploading...");
        Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
            hostingProvider.uploadPack(file);
            Logs.log(ChatColor.GREEN, "Resourcepack uploaded on url "
                    + hostingProvider.getPackURL() + " in " + (System.currentTimeMillis() - time) + "ms");
            if ((boolean) Pack.SEND.getValue())
                Bukkit.getPluginManager().registerEvents(new PackSender(hostingProvider.getPackURL()), plugin);
        });
    }

    private HostingProvider getHostingProvider() {
        return new TransferDotSh();
    }

}
