package io.th0rgal.oraxen.packets;

import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import io.th0rgal.oraxen.utils.SnapshotVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface PacketAdapter {
    static boolean isPacketEventsEnabled() {
        return getPacketEventsPlugin() != null;
    }
    @Nullable
    static Plugin getPacketEventsPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PacketEvents");
        if (plugin != null && plugin.isEnabled()) return plugin;
        plugin = Bukkit.getPluginManager().getPlugin("packetevents");
        return plugin != null && plugin.isEnabled() ? plugin : null;
    }
    static boolean isProtocolLibEnabled() {
        return PluginUtils.isEnabled("ProtocolLib");
    }
    boolean isEnabled();
    default boolean whenEnabled(Consumer<PacketAdapter> whenEnabled) {
        boolean enabled = isEnabled();
        if (enabled && whenEnabled != null) whenEnabled.accept(this);
        return enabled;
    }
    void registerInventoryListener();
    void registerScoreboardListener();
    void registerTitleListener();
    void removeInventoryListener();
    void removeTitleListener();

    String getLatestMCVersion();
    boolean isNewer(SnapshotVersion snapshot);
    @Nullable Plugin getPlugin();
    public static class EmptyAdapter implements PacketAdapter {
        /**
         * Leading {@code major[.minor[.patch]]} of a release version, ignoring any {@code -pre1}/{@code -rc1}
         * suffix. The trailing boundary keeps snapshot ids such as {@code 25w03a} from matching as {@code 25}.
         */
        private static final Pattern RELEASE_VERSION = Pattern.compile("^\\d+(?:\\.\\d+){0,2}(?=$|[-+])");

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void registerInventoryListener() {
        }

        @Override
        public void registerScoreboardListener() {
        }

        @Override
        public void registerTitleListener() {
        }

        @Override
        public void removeInventoryListener() {

        }

        @Override
        public void removeTitleListener() {

        }

        @Override public String getLatestMCVersion() {
            return latestMCVersion(Bukkit.getMinecraftVersion());
        }

        /**
         * Without a protocol library there is no catalogue of Minecraft releases to query, so the
         * newest release we can be sure exists is the one this server runs. Deriving it from the
         * server version keeps it correct across Minecraft releases with no manual bump.
         *
         * @param serverVersion the server's Minecraft version, e.g. {@code "26.2"} or {@code "1.21.8-pre1"}.
         * @return the release version to treat as latest, falling back to the newest version known to
         * the pack-format table when the server reports a snapshot id such as {@code "25w03a"}.
         */
        static String latestMCVersion(@Nullable String serverVersion) {
            if (serverVersion != null) {
                Matcher matcher = RELEASE_VERSION.matcher(serverVersion.trim());
                if (matcher.find()) return matcher.group();
            }
            return ResourcePackFormatUtil.getLatestKnownVersion().getVersion();
        }

        @Override public boolean isNewer(SnapshotVersion snapshot) {
            return true; // no way to know
        }

        @Nullable
        @Override
        public Plugin getPlugin() {
            return null;
        }
    }
}
