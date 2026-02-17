package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackPreUploadEvent;
import io.th0rgal.oraxen.api.events.OraxenPackUploadEvent;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.dispatch.MultiVersionPackSender;
import io.th0rgal.oraxen.pack.dispatch.PlayerVersionDetector;
import io.th0rgal.oraxen.pack.generation.PackVersion;
import io.th0rgal.oraxen.pack.generation.PackVersionManager;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.io.IOException;
import java.nio.file.ProviderNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages uploading and distributing multiple pack versions to players.
 */
public class MultiVersionUploadManager {

    private final OraxenPlugin plugin;
    private final Map<PackVersion, HostingProvider> hostingProviders = new HashMap<>();
    private MultiVersionPackSender packSender;
    private PackReceiver receiver;
    private volatile boolean cancelled = false;

    public MultiVersionUploadManager(OraxenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Uploads all pack versions and sends appropriate packs to online players.
     *
     * @param versionManager Pack version manager with all pack versions
     * @param reload Whether this is a reload operation
     * @param sendToPlayers Whether to send packs to online players
     */
    public void uploadAndSendToPlayers(PackVersionManager versionManager, boolean reload, boolean sendToPlayers) {
        // Reset cancelled flag for new upload operation
        // This is critical when reusing the manager after unregister() was called
        cancelled = false;

        // Check if upload is enabled
        if (!Settings.UPLOAD.toBool()) {
            Logs.logWarning("Pack upload is disabled in settings");
            return;
        }

        // Validate hosting provider compatibility
        // Note: SelfHost compatibility is checked earlier in ResourcePack.generate()
        // This is a safety check in case this method is called directly
        if (isSelfHost()) {
            Logs.logError("SelfHost is incompatible with multi-version packs!");
            Logs.logError("This should have been caught earlier - no packs will be uploaded.");
            return;
        }

        // Initialize player version detection
        PlayerVersionDetector.initialize();

        // Register PackReceiver for pack status events (accepted/denied/loaded etc.)
        if (Settings.RECEIVE_ENABLED.toBool() && receiver == null) {
            receiver = new PackReceiver();
            Bukkit.getPluginManager().registerEvents(receiver, plugin);
        }

        SchedulerUtil.runTaskAsync(() -> {
            try {
                if (cancelled) return;

                uploadAllVersions(versionManager);

                if (cancelled) return;

                SchedulerUtil.runTask(() -> {
                    if (cancelled) return;

                    if (packSender != null) {
                        packSender.unregister();
                    }

                    packSender = new MultiVersionPackSender(versionManager);

                    if (reload && !Settings.SEND_ON_RELOAD.toBool()) {
                        packSender.unregister();
                    } else if (Settings.SEND_PACK.toBool() || Settings.SEND_JOIN_MESSAGE.toBool()) {
                        packSender.register();

                        if (sendToPlayers && Settings.SEND_PACK.toBool()) {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                packSender.sendPack(player);
                            }
                        }
                    } else {
                        packSender.unregister();
                    }

                    Logs.logSuccess("Multi-version pack upload and distribution complete");
                });
            } catch (Exception e) {
                Logs.logError("Failed to upload and send multi-version packs: " + e.getMessage());
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void uploadAllVersions(PackVersionManager versionManager) {
        Collection<PackVersion> versions = versionManager.getAllVersions();
        Logs.logInfo("Uploading " + versions.size() + " pack versions...");

        for (PackVersion packVersion : versions) {
            try {
                uploadPackVersion(packVersion);
            } catch (Exception e) {
                Logs.logError("Failed to upload pack for " + packVersion.getMinecraftVersion() + ": " + e.getMessage());
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void uploadPackVersion(PackVersion packVersion) throws IOException {
        Logs.logInfo("Uploading pack for Minecraft " + packVersion.getMinecraftVersion() + "...");

        // Fire pre-upload event
        OraxenPackPreUploadEvent event = new OraxenPackPreUploadEvent();
        EventUtils.callEvent(event);

        // Create hosting provider for this version
        // Note: SelfHost is not supported for multi-version (validated earlier)
        HostingProvider provider = createHostingProvider();

        // Upload pack (provider calculates SHA-1 internally)
        boolean success = provider.uploadPack(packVersion.getPackFile());
        if (!success) {
            throw new IOException("Failed to upload pack");
        }

        // Store URL, SHA1, and UUID from provider
        packVersion.setPackURL(provider.getPackURL());
        packVersion.setPackSHA1(provider.getSHA1());
        packVersion.setPackUUID(provider.getPackUUID()); // Use provider's content-based UUID

        // Store provider
        hostingProviders.put(packVersion, provider);

        // Fire upload event on main thread (matches UploadManager behavior)
        OraxenPackUploadEvent uploadEvent = new OraxenPackUploadEvent(provider);
        SchedulerUtil.runTask(() -> Bukkit.getPluginManager().callEvent(uploadEvent));

        Logs.logSuccess("  Uploaded: " + packVersion.getMinecraftVersion() + " -> " + provider.getPackURL());
    }

    private boolean isSelfHost() {
        String uploadType = Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT);
        return "self-host".equals(uploadType);
    }

    private HostingProvider createHostingProvider() {
        HostingProvider provider = switch (Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT)) {
            case "polymath" -> new io.th0rgal.oraxen.pack.upload.hosts.Polymath(Settings.POLYMATH_SERVER.toString());
            case "self-host" -> {
                Logs.logError("SelfHost cannot be used with multi-version packs");
                yield null;
            }
            case "external" -> createExternalProvider();
            default -> null;
        };

        if (provider == null) {
            String uploadType = Settings.UPLOAD_TYPE.toString();
            Logs.logError("Upload type '" + uploadType + "' is not supported for multi-version packs");
            Logs.logError("Polymath will be used instead.");
            provider = new io.th0rgal.oraxen.pack.upload.hosts.Polymath(Settings.POLYMATH_SERVER.toString());
        }
        return provider;
    }

    private HostingProvider createExternalProvider() {
        final Class<?> target;
        final org.bukkit.configuration.ConfigurationSection options = 
                (org.bukkit.configuration.ConfigurationSection) Settings.UPLOAD_OPTIONS.getValue();
        final String klass = options.getString("class");
        if (klass == null) {
            Logs.logError("No external provider class specified in settings");
            return null;
        }
        try {
            target = Class.forName(klass);
        } catch (final Exception any) {
            Logs.logError("External provider not found: " + klass + " - " + any.getMessage());
            return null;
        }
        if (!HostingProvider.class.isAssignableFrom(target)) {
            Logs.logError(target + " is not a valid HostingProvider");
            return null;
        }
        return constructExternalHostingProvider(target, options);
    }

    @SuppressWarnings("unchecked")
    private HostingProvider constructExternalHostingProvider(final Class<?> target,
                                                              final org.bukkit.configuration.ConfigurationSection options) {
        java.lang.reflect.Constructor<? extends HostingProvider> constructor = getConstructor(target);

        try {
            return constructor.getParameterCount() == 0 
                    ? constructor.newInstance()
                    : constructor.newInstance(options);
        } catch (final java.lang.InstantiationException e) {
            throw (ProviderNotFoundException) new ProviderNotFoundException("Cannot alloc instance for " + target)
                    .initCause(e);
        } catch (final java.lang.IllegalAccessException e) {
            throw (ProviderNotFoundException) new ProviderNotFoundException("Failed to access " + target)
                    .initCause(e);
        } catch (final java.lang.reflect.InvocationTargetException e) {
            throw (ProviderNotFoundException) new ProviderNotFoundException("Exception in allocating instance.")
                    .initCause(e.getCause());
        }
    }

    @org.jetbrains.annotations.NotNull
    @SuppressWarnings("unchecked")
    private static java.lang.reflect.Constructor<? extends HostingProvider> getConstructor(Class<?> target) {
        final Class<? extends HostingProvider> implement = target.asSubclass(HostingProvider.class);
        java.lang.reflect.Constructor<? extends HostingProvider> constructor = null;
        for (final java.lang.reflect.Constructor<?> implementConstructor : implement.getConstructors()) {
            java.lang.reflect.Parameter[] parameters = implementConstructor.getParameters();
            if (parameters.length == 0 || 
                    (parameters.length == 1 && parameters[0].getType().equals(org.bukkit.configuration.ConfigurationSection.class))) {
                constructor = (java.lang.reflect.Constructor<? extends HostingProvider>) implementConstructor;
                break;
            }
        }

        if (constructor == null) {
            throw new ProviderNotFoundException("Invalid external provider: " + target + " - no valid constructor found");
        }
        return constructor;
    }

    public MultiVersionPackSender getPackSender() {
        return packSender;
    }

    public void unregister() {
        cancelled = true;
        if (packSender != null) {
            packSender.unregister();
        }

        if (receiver != null) {
            HandlerList.unregisterAll(receiver);
            receiver = null;
        }

        hostingProviders.clear();
    }
}
