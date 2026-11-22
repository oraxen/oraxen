package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackPreUploadEvent;
import io.th0rgal.oraxen.api.events.OraxenPackUploadEvent;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.dispatch.BukkitPackSender;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UploadManager {

    private static String url;
    private static String previousSHA1;
    private static final Object trackingLock = new Object();
    private final Plugin plugin;
    private final boolean enabled;
    private final Map<String, Object> uploadSettings;
    private final HostingProvider hostingProvider;
    private volatile PackSender packSender;
    private PackReceiver receiver;
    private volatile boolean cancelled = false;

    public UploadManager(final Plugin plugin) {
        this.plugin = plugin;
        enabled = Settings.UPLOAD.toBool();
        uploadSettings = readUploadSettings();
        hostingProvider = HostingProviderFactory.createHostingProvider(true);
    }

    /** Whether the pack.upload settings still match the ones this manager was created with. */
    public boolean usesCurrentUploadSettings() {
        return uploadSettings.equals(readUploadSettings());
    }

    private static Map<String, Object> readUploadSettings() {
        Map<String, Object> values = new HashMap<>();
        ConfigurationSection section = OraxenPlugin.get().getConfigsManager().getSettings().getConfigurationSection("pack.upload");
        if (section != null) section.getValues(true).forEach((key, value) -> {
            if (!(value instanceof ConfigurationSection)) values.put(key, value);
        });
        return values;
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
        registerReceiverIfNeeded();

        final long time = System.currentTimeMillis();
        SchedulerUtil.runTaskAsync(() -> {
            if (cancelled) return;

            if (!new OraxenPackPreUploadEvent().callEvent() || cancelled) return;

            if (!uploadPack(resourcePack, time)) return;

            boolean contentChanged = updateTrackingState();
            if (cancelled) return;

            // Bukkit listener registration is not thread-safe, so swap and
            // register the pack sender on the global region thread.
            SchedulerUtil.runTask(() -> {
                if (cancelled) return;
                updatePackSenderInstance(updatePackSender);
                distributePacksToPlayers(contentChanged);
            });
        });
    }

    private void registerReceiverIfNeeded() {
        if (Settings.RECEIVE_ENABLED.toBool() && receiver == null) {
            receiver = new PackReceiver();
            Bukkit.getPluginManager().registerEvents(receiver, plugin);
        }
    }

    private boolean uploadPack(ResourcePack resourcePack, long startTime) {
        Message.PACK_UPLOADING.log();
        if (!hostingProvider.uploadPack(resourcePack.getFile())) {
            Message.PACK_NOT_UPLOADED.log();
            return false;
        }

        OraxenPackUploadEvent uploadEvent = new OraxenPackUploadEvent(hostingProvider);
        SchedulerUtil.runTask(uploadEvent::callEvent);

        Message.PACK_UPLOADED.log(
                AdventureUtils.tagResolver("url", hostingProvider.getPackURL()),
                AdventureUtils.tagResolver("delay", String.valueOf(System.currentTimeMillis() - startTime)));
        return true;
    }

    /** Updates SHA1/URL tracking state and returns true if content changed. */
    private boolean updateTrackingState() {
        String currentSHA1 = hostingProvider.getOriginalSHA1();
        String currentURL = hostingProvider.getPackURL();
        synchronized (trackingLock) {
            boolean changed = !Objects.equals(currentURL, url) || !Objects.equals(currentSHA1, previousSHA1);
            url = currentURL;
            previousSHA1 = currentSHA1;
            return changed;
        }
    }

    private void updatePackSenderInstance(boolean updatePackSender) {
        if (packSender == null) {
            packSender = new BukkitPackSender(hostingProvider);
        } else if (updatePackSender) {
            packSender.unregister();
            packSender = new BukkitPackSender(hostingProvider);
        }
    }

    private void distributePacksToPlayers(boolean contentChanged) {
        if (PackSender.isAnyDispatchEnabled()) {
            PackSender sender = packSender;
            sender.register();
            if (contentChanged) {
                for (Player player : Bukkit.getOnlinePlayers())
                    SchedulerUtil.runForEntity(player, () -> {
                        if (!cancelled) sender.sendPack(player);
                    });
            }
        } else if (packSender != null) {
            packSender.unregister();
        }
    }

    public void unregister() {
        cancelled = true;
        if (hostingProvider instanceof io.th0rgal.oraxen.pack.upload.hosts.SelfHost selfHost) {
            selfHost.stopServer();
        }
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
