package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.dispatch.PackDispatcher;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.pack.upload.hosts.Polymath;
import io.th0rgal.oraxen.pack.upload.hosts.Sh;
import io.th0rgal.oraxen.settings.Pack;
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
            case "polymath":
                return new Polymath(Pack.POLYMATH_SERVER.toString());
            case "sh":
            case "cmd":
                final ConfigurationSection opt = (ConfigurationSection) Pack.UPLOAD_OPTIONS.getValue();
                final List<String> args = opt.getStringList("args");
                if (args == null || args.isEmpty())
                    throw new ProviderNotFoundException("No command line.");
                String placeholder = opt.getString("placeholder", "${file}");
                return new Sh(Sh.path(placeholder, args));
            case "external":
                Class<?> target;
                final ConfigurationSection options = (ConfigurationSection) Pack.UPLOAD_OPTIONS.getValue();
                String klass = options.getString("class");
                if (klass == null) throw new ProviderNotFoundException("No provider set.");
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
                            throw notFound; // Use (Lorg/bukkit/configuration/ConfigurationSection;)V to Exception
                        }
                    }
                } catch (Exception e) {
                    throw (ProviderNotFoundException) new ProviderNotFoundException("Cannot found constructor in " + target).initCause(e);
                }
                try {
                    return constructor.getParameterCount() == 0 ? constructor.newInstance() : constructor.newInstance(options);
                } catch (InstantiationException e) {
                    throw (ProviderNotFoundException) new ProviderNotFoundException("Cannot alloc instance for " + target).initCause(e);
                } catch (IllegalAccessException e) {
                    throw (ProviderNotFoundException) new ProviderNotFoundException("Failed to access " + target).initCause(e);
                } catch (InvocationTargetException e) {
                    throw (ProviderNotFoundException) new ProviderNotFoundException("Exception in allocating instance.").initCause(e.getCause());
                }
            default:
                throw new ProviderNotFoundException("Unknown provider type: " + Pack.UPLOAD_TYPE);
        }

    }

}
