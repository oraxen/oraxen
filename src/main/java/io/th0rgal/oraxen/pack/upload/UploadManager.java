package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.dispatch.PackDispatcher;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.pack.upload.hosts.Polymath;
import io.th0rgal.oraxen.pack.upload.hosts.TransferDotSh;
import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.utils.logs.Logs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

public class UploadManager {

    private Plugin plugin;
    private boolean enabled;
    private HostingProvider hostingProvider;

    public UploadManager(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = (boolean) Pack.UPLOAD.getValue();
        this.hostingProvider = getHostingProvider();
    }

    public void uploadAsyncAndSendToPlayers(ResourcePack resourcePack) {
        if (!enabled)
            return;
        long time = System.currentTimeMillis();
        Logs.log(ChatColor.GREEN, "Automatic upload of the resource pack is enabled, uploading...");
        Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
            if (!hostingProvider.uploadPack(resourcePack.getFile())) {
                Logs.log(ChatColor.RED, "Resourcepack not uploaded");
                return;
            }
            Logs.log(ChatColor.GREEN, "Resourcepack uploaded on url "
                    + hostingProvider.getPackURL() + " in " + (System.currentTimeMillis() - time) + "ms");
            PackDispatcher.setPackURL(hostingProvider.getPackURL());
            PackDispatcher.setSha1(hostingProvider.getSHA1());
            if ((boolean) Pack.SEND_PACK.getValue() || (boolean) Pack.SEND_WELCOME_MESSAGE.getValue())
                Bukkit.getPluginManager().registerEvents(new PackSender(), plugin);
        });
    }

    private HostingProvider getHostingProvider() {
        switch (Pack.UPLOAD_TYPE.toString().toLowerCase()) {
            case "transfer.sh":
                return new TransferDotSh();
            case "polymath":
                return new Polymath(Pack.POLYMATH_SERVER.toString());
            default:
                throw new RuntimeException();
        }

    }

}
