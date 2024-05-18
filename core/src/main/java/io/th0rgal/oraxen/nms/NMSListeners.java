package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanicFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class NMSListeners implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (NMSHandlers.getHandler().isEmpty()) return;

        if (GlyphHandlers.isNms()) NMSHandlers.getHandler().glyphHandler().inject(player);
        if (NoteBlockMechanicFactory.isEnabled() && NoteBlockMechanicFactory.get().removeMineableTag())
            NMSHandlers.getHandler().customBlockDefaultTools(player);
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (GlyphHandlers.isNms()) NMSHandlers.getHandler().glyphHandler().uninject(event.getPlayer());
    }
}
