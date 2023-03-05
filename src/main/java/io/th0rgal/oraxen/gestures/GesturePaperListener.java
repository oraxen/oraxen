package io.th0rgal.oraxen.gestures;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GesturePaperListener implements Listener {

    private final GestureManager gestureManager;

    public GesturePaperListener(GestureManager gestureManager) {
        this.gestureManager = gestureManager;
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        quit(event.getPlayer(), QuitMethod.JUMP);
    }

    private void quit(Player player, QuitMethod quitMethod) {
        if(!gestureManager.isPlayerGesturing(player)) return;
        gestureManager.getPlayerModel(player).stopAnimation(quitMethod);
    }
}
