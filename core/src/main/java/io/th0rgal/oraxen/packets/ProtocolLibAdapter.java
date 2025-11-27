package io.th0rgal.oraxen.packets;

import com.comphenix.protocol.ProtocolLibrary;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.packets.protocollib.InventoryPacketListener;
import io.th0rgal.oraxen.packets.protocollib.ScoreboardPacketListener;
import io.th0rgal.oraxen.packets.protocollib.TitlePacketListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanicFactory;
import io.th0rgal.oraxen.packets.protocollib.mechanics.provided.gameplay.efficiency.EfficiencyMechanicListener;
import io.th0rgal.oraxen.utils.SnapshotVersion;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProtocolLibAdapter implements PacketAdapter {
    private ScoreboardPacketListener scoreboardPacketListener;

    private InventoryPacketListener inventoryPacketListener;
    private TitlePacketListener titlePacketListener;

    private EfficiencyMechanicListener efficiencyMechanicListener;

    @Override
    public boolean isEnabled() {
        return PacketAdapter.isProtocolLibEnabled();
    }

    @Override
    public void registerInventoryListener() {
        if(inventoryPacketListener != null) {
            OraxenPlugin.get().getLogger().severe("[ProtocolLibAdapter]: Inventory Listener is already registered!");
            return;
        }
        inventoryPacketListener = new InventoryPacketListener();
        ProtocolLibrary.getProtocolManager().addPacketListener(inventoryPacketListener);
    }

    @Override
    public void registerScoreboardListener() {
        if(scoreboardPacketListener != null) {
            OraxenPlugin.get().getLogger().severe("[ProtocolLibAdapter]: Scoreboard Listener is already registered!");
            return;
        }
        scoreboardPacketListener = new ScoreboardPacketListener();
        ProtocolLibrary.getProtocolManager().addPacketListener(scoreboardPacketListener);
    }

    @Override
    public void registerTitleListener() {
        Logs.logInfo("[ProtocolLibAdapter] registerTitleListener called");
        if(titlePacketListener != null) {
            OraxenPlugin.get().getLogger().severe("[ProtocolLibAdapter]: Title Listener is already registered!");
            return;
        }
        titlePacketListener = new TitlePacketListener();
        ProtocolLibrary.getProtocolManager().addPacketListener(titlePacketListener);
        Logs.logInfo("[ProtocolLibAdapter] Title Listener registered successfully");
    }

    @Override public void removeInventoryListener() {
        if (inventoryPacketListener != null)
            ProtocolLibrary.getProtocolManager().removePacketListener(inventoryPacketListener);
        inventoryPacketListener = null;
    }

    @Override public void removeTitleListener() {
        if (titlePacketListener != null)
            ProtocolLibrary.getProtocolManager().removePacketListener(titlePacketListener);
        titlePacketListener = null;
    }

    @Override
    public void reregisterEfficencyMechanicListener(EfficiencyMechanicFactory efficiencyMechanicFactory) {
        if (efficiencyMechanicListener != null)
            ProtocolLibrary.getProtocolManager().removePacketListener(efficiencyMechanicListener);
        efficiencyMechanicListener = new EfficiencyMechanicListener(efficiencyMechanicFactory);
        ProtocolLibrary.getProtocolManager().addPacketListener(efficiencyMechanicListener);
    }

    @Override public String getLatestMCVersion() {
        return ProtocolLibrary.MAXIMUM_MINECRAFT_VERSION;
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    public boolean isNewer(SnapshotVersion snapshot) {
        try {
            return snapshot.getSnapshotDate().after(DATE_FORMAT.parse(ProtocolLibrary.MINECRAFT_LAST_RELEASE_DATE));
        } catch (ParseException ignored) {
            // will never happen we know what format the date has.
            return true;
        }
    }

    @Nullable
    @Override
    public Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib");
    }
}
