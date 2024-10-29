package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.utils.ParseUtils;
import org.apache.commons.lang3.StringUtils;
import team.unnamed.creative.font.FontProvider;
import team.unnamed.creative.font.SpaceFontProvider;

import java.util.List;

public enum Shift {

    NULL(""),
    MINUS_1("\uE101"),
    MINUS_2("\uE102"),
    MINUS_4("\uE103"),
    MINUS_8("\uE104"),
    MINUS_16("\uE105"),
    MINUS_32("\uE106"),
    MINUS_64("\uE107"),
    MINUS_128("\uE108"),
    MINUS_256("\uE109"),
    MINUS_512("\uE110"),
    MINUS_1024("\uE111"),

    PLUS_1("\uE112"),
    PLUS_2("\uE113"),
    PLUS_4("\uE114"),
    PLUS_8("\uE115"),
    PLUS_16("\uE116"),
    PLUS_32("\uE117"),
    PLUS_64("\uE118"),
    PLUS_128("\uE119"),
    PLUS_256("\uE120"),
    PLUS_512("\uE121"),
    PLUS_1024("\uE122");

    private final String unicode;

    Shift(String unicode) {
        this.unicode = unicode;
    }

    private static final List<Shift> powers_plus = List.of(
            NULL, PLUS_1, PLUS_2, PLUS_4,
            PLUS_8, PLUS_16, PLUS_32, PLUS_64,
            PLUS_128, PLUS_256, PLUS_512, PLUS_1024
    );

    private static final List<Shift> powers_minus = List.of(
            NULL, MINUS_1, MINUS_2, MINUS_4,
            MINUS_8, MINUS_16, MINUS_32, MINUS_64,
            MINUS_128, MINUS_256, MINUS_512, MINUS_1024
    );

    public int toNumber() {
        return (name().startsWith("PLUS") ? 1 : -1) * ParseUtils.parseInt(StringUtils.substringAfter(name(), "_"), 0);
    }

    public static String of(int shift) {
        StringBuilder builder = new StringBuilder();
        List<Shift> powers = (shift > 0) ? powers_plus : powers_minus;
        for (int i = 0; i < powers.size(); i++) {
            int pow = i + 1;
            int bit = 1 << i;
            if ((Math.abs(shift) & bit) != 0)
                builder.append(powers.get(pow)); // Assuming unicode is directly the string representation
        }
        return builder.reverse().toString();
    }

    public static SpaceFontProvider fontProvider() {
        SpaceFontProvider.Builder spaceBuilder = FontProvider.space();
        for (Shift shift : powers_minus) spaceBuilder.advance(shift.unicode, shift.toNumber());
        for (Shift shift : powers_plus) spaceBuilder.advance(shift.unicode, shift.toNumber());
        return spaceBuilder.build();
    }

    @Override
    public String toString() {
        return this.unicode;
    }
}
