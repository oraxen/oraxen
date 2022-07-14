package io.th0rgal.oraxen.hud;

import org.bukkit.GameMode;

import java.util.Arrays;
import java.util.List;

public record Hud(String hudDisplay, String hudPerm, boolean disableWhilstInWater, boolean enabledByDefault, GameMode[] enabledInGameMode) {

    public String getHudDisplay() {
        return hudDisplay;
    }

    public String getHudPerm() {
        return hudPerm;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public boolean isDisabledWhilstInWater() {
        return disableWhilstInWater;
    }

    public List<GameMode> getGameModes() {
        return Arrays.stream(enabledInGameMode).toList();
    }
}
