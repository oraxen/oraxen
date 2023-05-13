package io.th0rgal.oraxen.gestures;

public class Gesture {

    private final String key;
    private final boolean canLook;
    private final boolean canMove;
    private final QuitMethod quitMethod;

    public Gesture(String key, boolean canLook, boolean canMove, QuitMethod quitMethod) {
        this.key = key;
        this.canLook = canLook;
        this.canMove = canMove;
        this.quitMethod = quitMethod;
    }
}
