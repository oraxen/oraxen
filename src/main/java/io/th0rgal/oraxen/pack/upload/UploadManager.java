package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.dispatch.PackDispatcher;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.pack.upload.hosts.Polymath;
import io.th0rgal.oraxen.pack.upload.hosts.Sh;
import io.th0rgal.oraxen.utils.logs.Logs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.ProviderNotFoundException;
import java.util.List;

public class UploadManager {

    private final Plugin plugin;
    private final boolean enabled;
    private final HostingProvider hostingProvider;

    private PackReceiver receiver;
    private PackSender sender;

    public UploadManager(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = Settings.UPLOAD.toBool();
        this.hostingProvider = getHostingProvider();
    }

    public void uploadAsyncAndSendToPlayers(ResourcePack resourcePack) {
        uploadAsyncAndSendToPlayers(resourcePack, false);
    }

    public void uploadAsyncAndSendToPlayers(ResourcePack resourcePack, boolean updateSend) {
        if (!enabled)
            return;
        if (Settings.RECEIVE_ENABLED.toBool() && receiver == null)
            Bukkit.getPluginManager().registerEvents(receiver = new PackReceiver(), plugin);
        long time = System.currentTimeMillis();
        Message.PACK_UPLOADING.log("prefix", Message.PREFIX.toString());
        Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
            if (!hostingProvider.uploadPack(resourcePack.getFile())) {
                Message.PACK_NOT_UPLOADED.log();
                return;
            }
            Message.PACK_UPLOADED.log(
                    "url", hostingProvider.getPackURL(), "delay", String.valueOf(System.currentTimeMillis() - time));

            PackDispatcher.setPackURL(hostingProvider.getPackURL());
            PackDispatcher.setSha1(hostingProvider.getSHA1());
            if ((Settings.SEND_PACK.toBool() || Settings.SEND_JOIN_MESSAGE.toBool()) && sender == null)
                Bukkit.getPluginManager().registerEvents(sender = new PackSender(), plugin);
            /* Too much pain for people trying to configure mechanics
            if ((boolean) Pack.SEND_PACK.getValue() && updateSend)
                for (Player player : Bukkit.getOnlinePlayers())
                    PackDispatcher.sendPack(player);
            */
        });
    }

    private HostingProvider getHostingProvider() {
        switch (Settings.UPLOAD_TYPE.toString().toLowerCase()) {
            case "polymath":
                return new Polymath(Settings.POLYMATH_SERVER.toString());
            case "sh":
            case "cmd":
                final ConfigurationSection opt = (ConfigurationSection) Settings.UPLOAD_OPTIONS.getValue();
                final List<String> args = opt.getStringList("args");
                if (args.isEmpty())
                    throw new ProviderNotFoundException("No command line.");
                String placeholder = opt.getString("placeholder", "${file}");
                return new Sh(Sh.path(placeholder, args));
            case "external":
                Class<?> target;
                final ConfigurationSection options = (ConfigurationSection) Settings.UPLOAD_OPTIONS.getValue();
                String klass = options.getString("class");
                if (klass == null)
                    throw new ProviderNotFoundException("No provider set.");
                try {
                    target = Class.forName(klass);
                } catch (Throwable any) {
                    ProviderNotFoundException error = new ProviderNotFoundException("Provider not found: " + klass);
                    error.addSuppressed(any);
                    throw error;
                }
                if (!HostingProvider.class.isAssignableFrom(target)) {
                    throw new ProviderNotFoundException(target + " is not a valid HostingProvider.");
                }
                Class<? extends HostingProvider> implement = target.asSubclass(HostingProvider.class);
                Constructor<? extends HostingProvider> constructor;
                try {
                    try {
                        constructor = implement.getConstructor(ConfigurationSection.class);
                    } catch (Exception notFound) {
                        try {
                            constructor = implement.getConstructor();
                        } catch (Exception ignore) {
                            // For catching reasons
                            throw (ProviderNotFoundException) new ProviderNotFoundException("Invalid provider: " + target).initCause(ignore); // Use (Lorg/bukkit/configuration/ConfigurationSection;)V to Exception
                        }
                    }
                } catch (Exception e) {
                    throw (ProviderNotFoundException) new ProviderNotFoundException("Cannot found constructor in " + target)
                            .initCause(e);
                }
                try {
                    return constructor.getParameterCount() == 0 ? constructor.newInstance()
                            : constructor.newInstance(options);
                } catch (InstantiationException e) {
                    throw (ProviderNotFoundException) new ProviderNotFoundException("Cannot alloc instance for " + target)
                            .initCause(e);
                } catch (IllegalAccessException e) {
                    throw (ProviderNotFoundException) new ProviderNotFoundException("Failed to access " + target)
                            .initCause(e);
                } catch (InvocationTargetException e) {
                    throw (ProviderNotFoundException) new ProviderNotFoundException("Exception in allocating instance.")
                            .initCause(e.getCause());
                }
            default:
                throw new ProviderNotFoundException("Unknown provider type: " + Settings.UPLOAD_TYPE);
        }

    }

}
