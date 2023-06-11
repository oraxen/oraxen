package io.th0rgal.oraxen.gestures;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class GestureListener implements Listener {

    private final GestureManager gestureManager;

    public GestureListener(GestureManager gestureManager) {
        this.gestureManager = gestureManager;
        if (VersionUtil.isPaperServer())
            Bukkit.getPluginManager().registerEvents(new GesturePaperListener(), OraxenPlugin.get());
    }

    public class GesturePaperListener implements Listener {

        @EventHandler
        public void onJump(PlayerJumpEvent event) {
            quit(event.getPlayer(), QuitMethod.JUMP);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        //PlayerAnimator.api.injectPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        //PlayerAnimator.api.removePlayer(event.getPlayer());
        quit(event.getPlayer(), null);
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        quit(event.getPlayer(), QuitMethod.SNEAK);
    }

    private void quit(Player player, QuitMethod quitMethod) {
        if(!gestureManager.isPlayerGesturing(player)) return;
        gestureManager.getPlayerModel(player).stopAnimation(quitMethod);
    }


}
