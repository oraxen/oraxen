package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackPreUploadEvent;
import io.th0rgal.oraxen.api.events.OraxenPackUploadEvent;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.dispatch.BukkitPackSender;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.pack.upload.hosts.Polymath;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.nio.file.ProviderNotFoundException;
import java.util.Locale;

public class UploadManager {

    private static String url;
    private final Plugin plugin;
    private final boolean enabled;
    private final HostingProvider hostingProvider;
    private PackSender packSender;
    private PackReceiver receiver;

    public UploadManager(final Plugin plugin) {
        this.plugin = plugin;
        enabled = Settings.UPLOAD.toBool();
        hostingProvider = createHostingProvider();
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

        if (Settings.RECEIVE_ENABLED.toBool() && receiver == null) {
            receiver = new PackReceiver();
            Bukkit.getPluginManager().registerEvents(receiver, plugin);
        }

        final long time = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
            EventUtils.callEvent(new OraxenPackPreUploadEvent());

            Message.PACK_UPLOADING.log();
            if (!hostingProvider.uploadPack(resourcePack.getFile())) {
                Message.PACK_NOT_UPLOADED.log();
                return;
            }

            OraxenPackUploadEvent uploadEvent = new OraxenPackUploadEvent(hostingProvider);
            Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () ->
                    Bukkit.getPluginManager().callEvent(uploadEvent));

            Message.PACK_UPLOADED.log(
                    AdventureUtils.tagResolver("url", hostingProvider.getPackURL()),
                    AdventureUtils.tagResolver("delay", String.valueOf(System.currentTimeMillis() - time)));

            if (packSender == null) packSender = new BukkitPackSender(hostingProvider);
            else if (updatePackSender) {
                packSender.unregister();
                packSender = new BukkitPackSender(hostingProvider);
            }

            if (isReload && !Settings.SEND_ON_RELOAD.toBool() && packSender != null) packSender.unregister();
            else if (Settings.SEND_PACK.toBool() || Settings.SEND_JOIN_MESSAGE.toBool()) {
                packSender.register();
                if (!hostingProvider.getPackURL().equals(url))
                    for (Player player : Bukkit.getOnlinePlayers())
                        packSender.sendPack(player);
                url = hostingProvider.getPackURL();
            } else if (packSender != null) packSender.unregister();
        });
    }

    private HostingProvider createHostingProvider() {
        HostingProvider provider = switch (Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT)) {
            case "polymath" -> new Polymath(Settings.POLYMATH_SERVER.toString());
            case "external" -> createExternalProvider();
            default -> null;
        };

        if (provider == null) {
            Logs.logError("Unknown Hosting-Provider type: " + Settings.UPLOAD_TYPE);
            Logs.logError("Polymath will be used instead.");
            provider = new Polymath(Settings.POLYMATH_SERVER.toString());
        }
        return provider;
    }

    private HostingProvider createExternalProvider() {
        final Class<?> target;
        final ConfigurationSection options = (ConfigurationSection) Settings.UPLOAD_OPTIONS.getValue();
        final String klass = options.getString("class");
        if (klass == null)
            throw new ProviderNotFoundException("No provider set.");
        try {
            target = Class.forName(klass);
        } catch (final Exception any) {
            final ProviderNotFoundException error = new ProviderNotFoundException("Provider not found: " + klass);
            error.addSuppressed(any);
            throw error;
        }
        if (!HostingProvider.class.isAssignableFrom(target))
            throw new ProviderNotFoundException(target + " is not a valid HostingProvider.");
        return constructExternalHostingProvider(target, options);
    }

    private HostingProvider constructExternalHostingProvider(final Class<?> target,
                                                             final ConfigurationSection options) {
        Constructor<? extends HostingProvider> constructor = getConstructor(target);

        try {
            return constructor.getParameterCount() == 0 ? constructor.newInstance()
                    : constructor.newInstance(options);
        } catch (final InstantiationException e) {
            throw (ProviderNotFoundException) new ProviderNotFoundException("Cannot alloc instance for " + target)
                    .initCause(e);
        } catch (final IllegalAccessException e) {
            throw (ProviderNotFoundException) new ProviderNotFoundException("Failed to access " + target)
                    .initCause(e);
        } catch (final InvocationTargetException e) {
            throw (ProviderNotFoundException) new ProviderNotFoundException("Exception in allocating instance.")
                    .initCause(e.getCause());
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static Constructor<? extends HostingProvider> getConstructor(Class<?> target) {
        final Class<? extends HostingProvider> implement = target.asSubclass(HostingProvider.class);
        Constructor<? extends HostingProvider> constructor = null;
        for (final Constructor<?> implementConstructor : implement.getConstructors()) {
            Parameter[] parameters = implementConstructor.getParameters();
            if (parameters.length == 0 || (parameters.length == 1 && parameters[0].getType().equals(ConfigurationSection.class))) {
                constructor = (Constructor<? extends HostingProvider>) implementConstructor;
                break;
            }
        }

        if (constructor == null) throw new ProviderNotFoundException("Invalid provider: " + target);
        return constructor;
    }



}
