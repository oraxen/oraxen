package io.th0rgal.oraxen.pack.server;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.PackListener;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.BuiltResourcePack;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface OraxenPackServer {

    PackListener packListener = new PackListener();
    boolean mandatory = Settings.PACK_SEND_MANDATORY.toBool();
    Component prompt = AdventureUtils.MINI_MESSAGE.deserialize(Settings.PACK_SEND_PROMPT.toString());
    String legacyPrompt = AdventureUtils.parseLegacy(Settings.PACK_SEND_PROMPT.toString());

    static OraxenPackServer initializeServer() {
        OraxenPlugin.get().packServer().stop();
        HandlerList.unregisterAll(packListener);
        Bukkit.getPluginManager().registerEvents(packListener, OraxenPlugin.get());

        PackServerType type = Settings.PACK_SERVER_TYPE.toEnumOrGet(PackServerType.class, () -> {
            Logs.logError("Invalid PackServer-type specified: " + Settings.PACK_SERVER_TYPE);
            Logs.logError("Valid types are: " + Arrays.stream(PackServerType.values()).map(Enum::name).collect(Collectors.joining(", ")));
            return PackServerType.NONE;
        });

        Logs.logInfo("PackServer set to " + type.name());

        return switch (type) {
            case SELFHOST -> new SelfHostServer();
            case POLYMATH -> new PolymathServer();
            case NONE -> new EmptyServer();
        };
    }

    default boolean isPackUploaded() {
        return uploadPack().isDone();
    }

    default CompletableFuture<Void> uploadPack() {
        return CompletableFuture.allOf(OraxenPlugin.get().packGenerator().packGenFuture);
    }

    void sendPack(Player player);

    default void start() {
    }

    default void stop() {
    }

    static byte[] hashArray(String hash) {
        int len = hash.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hash.charAt(i), 16) << 4)
                    + Character.digit(hash.charAt(i + 1), 16));
        }
        return data;
    }

    String packUrl();

    @Nullable
    default ResourcePackInfo packInfo() {
        String hash = Optional.ofNullable(OraxenPlugin.get().packGenerator().builtPack()).map(BuiltResourcePack::hash).orElse(null);
        if (hash == null) return null;
        return ResourcePackInfo.resourcePackInfo()
                .hash(hash)
                .id(UUID.nameUUIDFromBytes(OraxenPackServer.hashArray(hash)))
                .uri(URI.create(packUrl()))
                .build();
    }
}
