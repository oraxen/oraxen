package io.th0rgal.oraxen.utils;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class PacketHelpers {

    public static GsonComponentSerializer gson = GsonComponentSerializer.gson();
    public static String readJson(String text) {
        // Serialize initial string from json to component, then parse to handle tags and serialize again to json string
        return AdventureUtils.MINI_MESSAGE.serialize(gson.deserialize(text)).replace("\\", "");
    }
    public static String toJson(String text) {
        return gson.serialize(AdventureUtils.MINI_MESSAGE.deserialize(text)).replace("\\", "");
    }
}
