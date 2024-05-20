package io.th0rgal.oraxen.items.helpers;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public enum ItemRarityWrapper {

    COMMON(NamedTextColor.WHITE),
    UNCOMMON(NamedTextColor.YELLOW),
    RARE(NamedTextColor.AQUA),
    EPIC(NamedTextColor.LIGHT_PURPLE);

    TextColor color;

    ItemRarityWrapper(TextColor color) {
        this.color = color;
    }

    /**
     * Gets the color formatting associated with the rarity.
     * @return
     */
    @NotNull
    public TextColor getColor() {
        return color;
    }
}
