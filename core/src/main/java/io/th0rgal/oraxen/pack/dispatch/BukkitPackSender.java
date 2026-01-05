package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.reflect.Method;
import java.util.UUID;

public class BukkitPackSender extends PackSender implements Listener {

    private static final String prompt = Settings.SEND_PACK_PROMPT.toString();
    private static final boolean mandatory = Settings.SEND_PACK_MANDATORY.toBool();
    private static Method setResourcePackWithLayerMethod;
    private static boolean layerMethodChecked = false;

    public BukkitPackSender(HostingProvider hostingProvider) {
        super(hostingProvider);
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    private static boolean hasLayerMethod() {
        if (!layerMethodChecked) {
            try {
                setResourcePackWithLayerMethod = Player.class.getMethod("setResourcePack", 
                    UUID.class, String.class, String.class, Component.class, boolean.class, String.class);
                layerMethodChecked = true;
            } catch (NoSuchMethodException e) {
                layerMethodChecked = true;
                return false;
            }
        }
        return setResourcePackWithLayerMethod != null;
    }

    @Override
    public void sendPack(Player player) {
        String layer = Settings.SEND_PACK_LAYER.toString();
        boolean useLayer = VersionUtil.atOrAbove("1.21") && layer != null && !layer.isEmpty() && hasLayerMethod();

        if (VersionUtil.atOrAbove("1.20.3")) {
            if (VersionUtil.isPaperServer()) {
                if (useLayer) {
                    try {
                        // Paper 1.21+ with layer support
                        setResourcePackWithLayerMethod.invoke(player, hostingProvider.getPackUUID(), 
                            hostingProvider.getPackURL(), hostingProvider.getSHA1(), 
                            AdventureUtils.MINI_MESSAGE.deserialize(prompt), mandatory, layer);
                    } catch (Exception e) {
                        // Fallback for older Paper versions that don't support layer parameter
                        player.setResourcePack(hostingProvider.getPackUUID(), hostingProvider.getPackURL(), 
                            hostingProvider.getSHA1(), AdventureUtils.MINI_MESSAGE.deserialize(prompt), mandatory);
                    }
                } else {
                    // Paper 1.20.3-1.20.6 (no layer parameter)
                    player.setResourcePack(hostingProvider.getPackUUID(), hostingProvider.getPackURL(), 
                        hostingProvider.getSHA1(), AdventureUtils.MINI_MESSAGE.deserialize(prompt), mandatory);
                }
            } else {
                // Spigot - try with layer first if supported
                if (useLayer) {
                    try {
                        // Convert legacy string to Component for layer method
                        Component promptComponent = AdventureUtils.LEGACY_SERIALIZER.deserialize(AdventureUtils.parseLegacy(prompt));
                        setResourcePackWithLayerMethod.invoke(player, hostingProvider.getPackUUID(), 
                            hostingProvider.getPackURL(), hostingProvider.getSHA1(), 
                            promptComponent, mandatory, layer);
                    } catch (Exception e) {
                        // Fallback if layer not supported
                        player.setResourcePack(hostingProvider.getPackUUID(), hostingProvider.getPackURL(), 
                            hostingProvider.getSHA1(), AdventureUtils.parseLegacy(prompt), mandatory);
                    }
                } else {
                    player.setResourcePack(hostingProvider.getPackUUID(), hostingProvider.getPackURL(), 
                        hostingProvider.getSHA1(), AdventureUtils.parseLegacy(prompt), mandatory);
                }
            }
        } else {
            // Pre-1.20.3 versions (no UUID, no layer)
            if (VersionUtil.isPaperServer()) {
                player.setResourcePack(hostingProvider.getPackURL(), hostingProvider.getSHA1(), 
                    AdventureUtils.MINI_MESSAGE.deserialize(prompt), mandatory);
            } else {
                player.setResourcePack(hostingProvider.getPackURL(), hostingProvider.getSHA1(), 
                    AdventureUtils.parseLegacy(prompt), mandatory);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Settings.SEND_JOIN_MESSAGE.toBool()) sendWelcomeMessage(player, true);
        if (!Settings.SEND_PACK.toBool()) return;
        int delay = (int) Settings.SEND_PACK_DELAY.getValue();
        if (delay <= 0) sendPack(player);
        else SchedulerUtil.runTaskLaterAsync(delay * 20L, () ->
                sendPack(player));
    }
}
