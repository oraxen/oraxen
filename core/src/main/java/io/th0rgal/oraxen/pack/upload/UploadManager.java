package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.api.events.OraxenPackPreUploadEvent;
import io.th0rgal.oraxen.api.events.OraxenPackUploadEvent;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.dispatch.BukkitPackSender;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class UploadManager {

    private static String url;
    private static String previousSHA1;
    private static final Object trackingLock = new Object();
    private final Plugin plugin;
    private final boolean enabled;
    private final HostingProvider hostingProvider;
    private PackSender packSender;
    private PackReceiver receiver;
    private volatile boolean cancelled = false;

    public UploadManager(final Plugin plugin) {
        this.plugin = plugin;
        enabled = Settings.UPLOAD.toBool();
        hostingProvider = HostingProviderFactory.createHostingProvider(true);
    }

    public HostingProvider getHostingProvider() {
        return hostingProvider;
    }

    public PackSender getSender() {
        return packSender;
    }

    public void uploadAsyncAndSendToPlayers(final ResourcePack resourcePack, final boolean updatePackSender, final boolean isReload) {
        if (!enabled)
            return;

        cancelled = false;

        if (Settings.RECEIVE_ENABLED.toBool() && receiver == null) {
            receiver = new PackReceiver();
            Bukkit.getPluginManager().registerEvents(receiver, plugin);
        }

        final long time = System.currentTimeMillis();
        SchedulerUtil.runTaskAsync(() -> {
            if (cancelled) return;

            EventUtils.callEvent(new OraxenPackPreUploadEvent());

            if (cancelled) return;

            Message.PACK_UPLOADING.log();
            if (!hostingProvider.uploadPack(resourcePack.getFile())) {
                Message.PACK_NOT_UPLOADED.log();
                return;
            }

            OraxenPackUploadEvent uploadEvent = new OraxenPackUploadEvent(hostingProvider);
            SchedulerUtil.runTask(() ->
                    Bukkit.getPluginManager().callEvent(uploadEvent));

            Message.PACK_UPLOADED.log(
                    AdventureUtils.tagResolver("url", hostingProvider.getPackURL()),
                    AdventureUtils.tagResolver("delay", String.valueOf(System.currentTimeMillis() - time)));

            // Update tracking variables after successful upload, regardless of send settings
            // This ensures tracking stays current even if settings are disabled
            // Synchronize to prevent race conditions when multiple uploads occur concurrently
            String currentSHA1 = hostingProvider.getOriginalSHA1();
            String currentURL = hostingProvider.getPackURL();
            boolean urlChanged;
            boolean sha1Changed;
            synchronized (trackingLock) {
                urlChanged = !Objects.equals(currentURL, url);
                sha1Changed = !Objects.equals(currentSHA1, previousSHA1);
                url = currentURL;
                previousSHA1 = currentSHA1;
            }

            if (cancelled) return;

            if (packSender == null) packSender = new BukkitPackSender(hostingProvider);
            else if (updatePackSender) {
                packSender.unregister();
                packSender = new BukkitPackSender(hostingProvider);
            }

            if (cancelled) return;

            if (isReload && !Settings.SEND_ON_RELOAD.toBool() && packSender != null) packSender.unregister();
            else if (Settings.SEND_PACK.toBool() || Settings.SEND_JOIN_MESSAGE.toBool()) {
                packSender.register();
                // Send pack if URL changed OR SHA1 changed (for self-hosted packs, URL doesn't change but SHA1 does)
                if (urlChanged || sha1Changed) {
                    for (Player player : Bukkit.getOnlinePlayers())
                        packSender.sendPack(player);
                }
            } else if (packSender != null) packSender.unregister();
        });
    }

    public void unregister() {
        cancelled = true;
        if (packSender != null) {
            packSender.unregister();
            packSender = null;
        }
        if (receiver != null) {
            HandlerList.unregisterAll(receiver);
            receiver = null;
        }
    }

}
