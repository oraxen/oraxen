package io.th0rgal.oraxen.gestures;

import com.ticxo.playeranimator.api.model.player.PlayerModel;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.entity.Player;

public class OraxenPlayerModel extends PlayerModel {
    private final QuitMethod quitMethod;
    private final boolean canLook;
    private final boolean canMove;
    private final float lockedYaw;
    private boolean isPlaying;

    public OraxenPlayerModel(Player player, QuitMethod quitMethod, boolean canLook, boolean canMove) {
        super(player);
        this.quitMethod = quitMethod;
        this.canLook = canLook;
        this.canMove = canMove;
        this.lockedYaw = !canLook ? player.getLocation().getYaw() : super.getBaseYaw();
    }

    public void stopAnimation(QuitMethod quitMethod) {
        isPlaying = quitMethod == null || this.quitMethod != quitMethod;
        OraxenPlugin.get().getGesturesManager().removePlayerFromGesturing(getPlayer());
        getPlayer().setInvisible(false);
    }

    @Override
    public void playAnimation(String name) {
        super.playAnimation(name);
        getPlayer().setInvisible(true);
        isPlaying = true;
    }

    @Override
    public boolean update() {
        boolean update = (super.update() && isPlaying);
        if (!update) {
            stopAnimation(null);
        }
        return update;
    }

    @Override
    public float getBaseYaw() {
        return canLook ? super.getBaseYaw() : lockedYaw;
    }

    public Player getPlayer() {
        return (Player) this.getBase();
    }
}
