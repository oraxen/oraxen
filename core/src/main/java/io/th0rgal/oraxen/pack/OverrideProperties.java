package io.th0rgal.oraxen.pack;

import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import team.unnamed.creative.model.ItemOverride;
import team.unnamed.creative.model.ItemPredicate;

import java.util.ArrayList;
import java.util.List;

public class OverrideProperties {

    public static List<ItemOverride> FISHING_ROD;
    public static List<ItemOverride> SHIELD;
    public static List<ItemOverride> BOW;
    public static List<ItemOverride> CROSSBOW;
    public static List<ItemOverride> LIGHT;
    public static List<ItemOverride> COMPASS;
    public static List<ItemOverride> CLOCK;

    public static List<ItemOverride> fromMaterial(Material material) {
        return switch (material) {
            case FISHING_ROD -> OverrideProperties.FISHING_ROD;
            case SHIELD -> OverrideProperties.SHIELD;
            case BOW -> OverrideProperties.BOW;
            case CROSSBOW -> OverrideProperties.CROSSBOW;
            case LIGHT -> OverrideProperties.LIGHT;
            case COMPASS, RECOVERY_COMPASS -> COMPASS;
            case CLOCK -> CLOCK;
            default -> new ArrayList<>();
        };
    }

    static {
        FISHING_ROD = new ArrayList<>();
        FISHING_ROD.add(ItemOverride.of(Key.key("item/fishing_rod_cast"), ItemPredicate.cast()));

        SHIELD = new ArrayList<>();
        SHIELD.add(ItemOverride.of(Key.key("item/shield_blocking"), ItemPredicate.blocking()));

        BOW = new ArrayList<>();
        BOW.add(ItemOverride.of(Key.key("item/bow_pulling_0"), ItemPredicate.pulling()));
        BOW.add(ItemOverride.of(Key.key("item/bow_pulling_1"), ItemPredicate.pulling(), ItemPredicate.pull(0.65f)));
        BOW.add(ItemOverride.of(Key.key("item/bow_pulling_2"), ItemPredicate.pulling(), ItemPredicate.pull(0.9f)));

        CROSSBOW = new ArrayList<>();
        CROSSBOW.add(ItemOverride.of(Key.key("item/crossbow_pulling_0"), ItemPredicate.pulling()));
        CROSSBOW.add(ItemOverride.of(Key.key("item/crossbow_pulling_1"), ItemPredicate.pulling(), ItemPredicate.pull(0.65f)));
        CROSSBOW.add(ItemOverride.of(Key.key("item/crossbow_pulling_2"), ItemPredicate.pulling(), ItemPredicate.pull(0.9f)));
        CROSSBOW.add(ItemOverride.of(Key.key("item/crossbow_arrow"), ItemPredicate.charged()));
        CROSSBOW.add(ItemOverride.of(Key.key("item/crossbow_firework"), ItemPredicate.firework()));

        LIGHT = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            String lightKey = "item/light_" + (i >= 10 ? i : "0" + i);
            LIGHT.add(ItemOverride.of(Key.key(lightKey), ItemPredicate.custom("level", (float) i / 16)));
        }

        COMPASS = new ArrayList<>();
        COMPASS.add(ItemOverride.of(Key.key("item/compass"), ItemPredicate.angle(0.0f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_17"), ItemPredicate.angle(0.015625f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_18"), ItemPredicate.angle(0.046875f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_19"), ItemPredicate.angle(0.078125f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_20"), ItemPredicate.angle(0.109375f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_21"), ItemPredicate.angle(0.140625f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_22"), ItemPredicate.angle(0.171875f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_23"), ItemPredicate.angle(0.203125f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_24"), ItemPredicate.angle(0.234375f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_25"), ItemPredicate.angle(0.265625f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_26"), ItemPredicate.angle(0.296875f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_27"), ItemPredicate.angle(0.328125f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_28"), ItemPredicate.angle(0.359375f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_29"), ItemPredicate.angle(0.390625f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_30"), ItemPredicate.angle(0.421875f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_31"), ItemPredicate.angle(0.453125f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_00"), ItemPredicate.angle(0.484375f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_01"), ItemPredicate.angle(0.515625f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_02"), ItemPredicate.angle(0.546875f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_03"), ItemPredicate.angle(0.578125f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_04"), ItemPredicate.angle(0.609375f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_05"), ItemPredicate.angle(0.640625f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_06"), ItemPredicate.angle(0.671875f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_07"), ItemPredicate.angle(0.703125f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_08"), ItemPredicate.angle(0.734375f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_09"), ItemPredicate.angle(0.765625f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_10"), ItemPredicate.angle(0.796875f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_11"), ItemPredicate.angle(0.828125f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_12"), ItemPredicate.angle(0.859375f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_13"), ItemPredicate.angle(0.890625f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_14"), ItemPredicate.angle(0.921875f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass_15"), ItemPredicate.angle(0.953125f)));
        COMPASS.add(ItemOverride.of(Key.key("item/compass"), ItemPredicate.angle(0.984375f)));


        CLOCK = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            String clockKey = "item/clock" + (i == 0 ? "" : "_" + (i < 10 ? "0" + i : i));
            float time = i == 0 ? 0f : i == 1 ? 0.0078125f : (float) i / 64;
            CLOCK.add(ItemOverride.of(Key.key(clockKey), ItemPredicate.time(time)));
        }

    }
}
