package io.th0rgal.oraxen.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PacketHelpers {

    // Serialize initial string from json to component, then parse to handle tags and serialize again to json string
    public static String readJson(String text) {
        return AdventureUtils.parseMiniMessageThroughLegacy(AdventureUtils.GSON_SERIALIZER.deserialize(text));
    }

    public static String toJson(String text) {
        return AdventureUtils.GSON_SERIALIZER.serialize(AdventureUtils.MINI_MESSAGE.deserialize(text)).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
    }

    public static void applyMiningFatigue(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EFFECT);
            packet.getIntegers().write(0, player.getEntityId()).write(1, -1);
            packet.getEffectTypes().write(0, PotionEffectType.SLOW_DIGGING);
            packet.getBytes().write(0, (byte) 0).write(1, (byte) 0);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } else player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, -1, Integer.MAX_VALUE, false, false, false));
    }

    public static void removeMiningFatigue(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
            packet.getIntegers().write(0, player.getEntityId());
            packet.getEffectTypes().write(0, PotionEffectType.SLOW_DIGGING);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } else player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
    }
}
