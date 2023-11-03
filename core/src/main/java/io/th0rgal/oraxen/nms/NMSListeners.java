package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NMSListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (NMSHandlers.getHandler() == null) return;

        if (Settings.NMS_GLYPHS.toBool()) NMSHandlers.getHandler().inject(player);
        if (NoteBlockMechanicFactory.isEnabled() && NoteBlockMechanicFactory.getInstance().removeMineableTag())
            NMSHandlers.getHandler().customBlockDefaultTools(player);
    }
}
