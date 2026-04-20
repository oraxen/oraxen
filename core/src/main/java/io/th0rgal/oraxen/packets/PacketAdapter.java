package io.th0rgal.oraxen.packets;

import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanicFactory;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.SnapshotVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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
    void reregisterEfficencyMechanicListener(EfficiencyMechanicFactory efficiencyMechanicFactory);

    String getLatestMCVersion();
    boolean isNewer(SnapshotVersion snapshot);
    @Nullable Plugin getPlugin();
    public static class EmptyAdapter implements PacketAdapter {
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

        @Override
        public void reregisterEfficencyMechanicListener(EfficiencyMechanicFactory efficiencyMechanicFactory) {

        }

        @Override public String getLatestMCVersion() {
            return "1.21.8"; // TODO: update for the next mc update
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
