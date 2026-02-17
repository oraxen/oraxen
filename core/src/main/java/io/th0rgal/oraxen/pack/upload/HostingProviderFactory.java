package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.pack.upload.hosts.Polymath;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.nio.file.ProviderNotFoundException;
import java.util.Locale;

/**
 * Factory for creating hosting provider instances.
 * Shared between UploadManager (single-pack) and MultiVersionUploadManager.
 */
public final class HostingProviderFactory {

    private HostingProviderFactory() {
    }

    /**
     * Creates a hosting provider based on current settings.
     * Falls back to Polymath if the configured provider is invalid.
     *
     * @param allowSelfHost Whether to allow self-host provider (not supported for multi-version)
     * @return A configured hosting provider
     */
    @NotNull
    public static HostingProvider createHostingProvider(boolean allowSelfHost) {
        HostingProvider provider = switch (Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT)) {
            case "polymath" -> new Polymath(Settings.POLYMATH_SERVER.toString());
            case "self-host" -> {
                if (!allowSelfHost) {
                    Logs.logError("SelfHost cannot be used with multi-version packs");
                    yield null;
                }
                ConfigurationSection selfHostConfig = OraxenPlugin.get().getConfigsManager().getSettings()
                        .getConfigurationSection("Pack.upload.self-host");
                yield new io.th0rgal.oraxen.pack.upload.hosts.SelfHost(selfHostConfig);
            }
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

    private static HostingProvider createExternalProvider() {
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

    private static HostingProvider constructExternalHostingProvider(final Class<?> target,
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
