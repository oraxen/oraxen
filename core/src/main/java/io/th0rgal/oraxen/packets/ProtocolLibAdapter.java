package io.th0rgal.oraxen.packets;

import com.comphenix.protocol.ProtocolLibrary;
import io.th0rgal.oraxen.packets.protocollib.InventoryPacketListener;
import io.th0rgal.oraxen.packets.protocollib.ScoreboardPacketListener;
import io.th0rgal.oraxen.packets.protocollib.TitlePacketListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanicFactory;
import io.th0rgal.oraxen.packets.protocollib.mechanics.provided.gameplay.efficiency.EfficiencyMechanicListener;
import io.th0rgal.oraxen.utils.SnapshotVersion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProtocolLibAdapter implements PacketAdapter {
    private InventoryPacketListener inventoryPacketListener;
    private TitlePacketListener titlePacketListener;

    private EfficiencyMechanicListener efficiencyMechanicListener;

    @Override
    public boolean isEnabled() {
        return PacketAdapter.isProtocolLibEnabled();
    }

    @Override
    public void registerInventoryListener() {
        inventoryPacketListener = new InventoryPacketListener();
        ProtocolLibrary.getProtocolManager().addPacketListener(inventoryPacketListener);
    }

    @Override
    public void registerScoreboardListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new ScoreboardPacketListener());
    }

    @Override
    public void registerTitleListener() {
        titlePacketListener = new TitlePacketListener();
        ProtocolLibrary.getProtocolManager().addPacketListener(titlePacketListener);
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
}
