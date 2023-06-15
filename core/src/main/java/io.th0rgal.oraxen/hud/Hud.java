package io.th0rgal.oraxen.hud;

public record Hud(String displayText,
                  String fontName,
                  String perm,
                  boolean disableWhilstInWater,
                  boolean enabledByDefault,
                  boolean enableInSpectatorMode)
{

    public String getDisplayText() {
        return displayText;
    }

    public String getFont() {
        return fontName;
    }

    public String getPerm() {
        return perm;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public boolean isDisabledWhilstInWater() {
        return disableWhilstInWater;
    }

    public boolean enableInSpectatorMode() {
        return true;
    }
}
