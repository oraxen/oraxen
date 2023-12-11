package io.th0rgal.oraxen.pack.dispatch;

import com.comphenix.protocol.ProtocolManager;
import com.viaversion.viaversion.api.Via;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BukkitPackSender extends PackSender implements Listener {

    private static final String prompt = Settings.SEND_PACK_PROMPT.toString();
    private static final boolean mandatory = Settings.SEND_PACK_MANDATORY.toBool();

    private final ProtocolManager protocolManager;
    private boolean useViaVersion;

    public BukkitPackSender(HostingProvider hostingProvider) {
        super(hostingProvider);
        protocolManager = OraxenPlugin.get().getProtocolManager();
    }

    @SuppressWarnings("unchecked")
    public int getPlayerProtocol(Player player) {
        if (useViaVersion) {
            return Via.getAPI().getPlayerVersion(player);
        } else {
            return protocolManager.getProtocolVersion(player);
        }
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
        if (Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            try {
                Class.forName("com.viaversion.viaversion.api.Via");
                useViaVersion = true;
            } catch (ClassNotFoundException ignored) { }
        }
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void sendPack(Player player) {
        int minProtocol = (int) Settings.SEND_PACK_MIN_PROTOCOL.getValue();
        int playerProtocol;
        if (minProtocol > 0 && (playerProtocol = getPlayerProtocol(player)) > 0 && playerProtocol < minProtocol) {
            return;
        }
        if (VersionUtil.isPaperServer()) player.setResourcePack(hostingProvider.getMinecraftPackURL(), hostingProvider.getSHA1(), AdventureUtils.MINI_MESSAGE.deserialize(prompt), mandatory);
        else player.setResourcePack(hostingProvider.getMinecraftPackURL(), hostingProvider.getSHA1(), AdventureUtils.parseLegacy(prompt), mandatory);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Settings.SEND_JOIN_MESSAGE.toBool()) sendWelcomeMessage(player, true);
        if (!Settings.SEND_PACK.toBool()) return;
        int delay = (int) Settings.SEND_PACK_DELAY.getValue();
        if (delay <= 0) sendPack(player);
        else Bukkit.getScheduler().runTaskLaterAsynchronously(OraxenPlugin.get(), () ->
                sendPack(player), delay * 20L);
    }
}
