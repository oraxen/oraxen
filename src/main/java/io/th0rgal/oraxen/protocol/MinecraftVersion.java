package io.th0rgal.oraxen.protocol;

import java.util.HashMap;
import java.util.Map;

public enum MinecraftVersion {
    UNDEFINED(0),
    MINECRAFT_1_7_2(4),
    MINECRAFT_1_7_6(5),
    MINECRAFT_1_8(47),
    MINECRAFT_1_9(107),
    MINECRAFT_1_9_1(108),
    MINECRAFT_1_9_2(109),
    MINECRAFT_1_9_4(110),
    MINECRAFT_1_10(210),
    MINECRAFT_1_11(315),
    MINECRAFT_1_11_1(316),
    MINECRAFT_1_12(335),
    MINECRAFT_1_12_1(338),
    MINECRAFT_1_12_2(340),
    MINECRAFT_1_13(393),
    MINECRAFT_1_13_1(401),
    MINECRAFT_1_13_2(404),
    MINECRAFT_1_14(477),
    MINECRAFT_1_14_1(480),
    MINECRAFT_1_14_2(485),
    MINECRAFT_1_14_3(490),
    MINECRAFT_1_14_4(498),
    MINECRAFT_1_15(573),
    MINECRAFT_1_15_1(575),
    MINECRAFT_1_15_2(578),
    MINECRAFT_1_16(735),
    MINECRAFT_1_16_1(736),
    MINECRAFT_1_16_2(751),
    MINECRAFT_1_16_3(753),
    MINECRAFT_1_16_4(754),
    MINECRAFT_1_17(755),
    MINECRAFT_1_17_1(756),
    MINECRAFT_1_18(757),
    MINECRAFT_1_18_2(758),
    MINECRAFT_1_19(759),
    MINECRAFT_1_19_1(760),
    MINECRAFT_1_19_3(761),
    MINECRAFT_1_19_4(762),
    MINECRAFT_1_20(763);

    public static final MinecraftVersion MINIMUM_VERSION = MINECRAFT_1_7_2;
    public static final MinecraftVersion MAXIMUM_VERSION = values()[values().length - 1];

    private static final Map<Integer, MinecraftVersion> PVN_MAP = new HashMap<>();

    static {
        int pvn = MINIMUM_VERSION.getProtocolVersion();
        MinecraftVersion currentVersion = UNDEFINED;

        while (currentVersion != MAXIMUM_VERSION) {
            currentVersion = currentVersion.next();
            for (; pvn <= currentVersion.getProtocolVersion(); pvn++) {
                PVN_MAP.put(pvn, currentVersion);
            }
        }
    }

    public static MinecraftVersion fromVersionNumber(int pvn) {
        return PVN_MAP.getOrDefault(pvn, UNDEFINED);
    }

    final int protocolVersion;

    MinecraftVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public MinecraftVersion next() {
        if (this == MAXIMUM_VERSION) {
            return MAXIMUM_VERSION;
        } else {
            return values()[ordinal() + 1];
        }
    }

    public MinecraftVersion previous() {
        if (this == MINIMUM_VERSION) {
            return MINIMUM_VERSION;
        } else {
            return values()[ordinal() - 1];
        }
    }

    public String getVersionName() {
        return toString().substring(10).replace('_', '.');
    }
}