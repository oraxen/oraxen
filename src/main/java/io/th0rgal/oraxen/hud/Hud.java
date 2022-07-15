package io.th0rgal.oraxen.hud;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Font;

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
        Font font = OraxenPlugin.get().getFontManager().getFontFromFile(fontName);
        return font != null ? fontName : "minecraft:default";
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
