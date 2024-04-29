package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.nms.NMSHandlers;
import org.bukkit.entity.Player;

public class PacketHelpers {

    // Serialize initial string from json to component, then parse to handle tags and serialize again to json string
    public static String readJson(String text) {
        return AdventureUtils.parseMiniMessageThroughLegacy(AdventureUtils.GSON_SERIALIZER.deserialize(text));
    }

    public static String toJson(String text) {
        return AdventureUtils.GSON_SERIALIZER.serialize(AdventureUtils.MINI_MESSAGE.deserialize(text)).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
    }

    public static void applyMiningFatigue(Player player) {
        NMSHandlers.handler().applyMiningFatigue(player);
    }

    public static void removeMiningFatigue(Player player) {
        NMSHandlers.handler().removeMiningFatigue(player);
    }
}
