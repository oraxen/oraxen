package io.th0rgal.oraxen.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public enum NMS {

    PACKET(Type.NMS, "Packet"),
    ENTITY_PLAYER(Type.NMS, "EntityPlayer"),
    ITEM_STACK(Type.NMS, "ItemStack"),
    NBT_TAG_COMPOUND(Type.NMS, "NBTTagCompound"),
    NBT_BASE(Type.NMS, "NBTBase"),
    NBT_TAG_STRING(Type.NMS, "NBTTagString"),
    NBT_TAG_INT(Type.NMS, "NBTTagInt"),
    PACKET_PLAY_OUT_RESOURCE_PACK_SEND(Type.NMS, "PacketPlayOutResourcePackSend"),

    CRAFT_PLAYER(Type.CB, "entity.CraftPlayer"),
    CRAFT_ITEM_STACK(Type.CB, "inventory.CraftItemStack");

    private Type type;
    private String className;

    NMS(Type type, String className) {
        this.type = type;
        this.className = className;
    }

    public Class<?> toClass() {
        switch (this.type) {

            case NMS:
                return NMS.getNMSClass(className);

            case CB:
                return NMS.getCBClass(className);

            default:
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }

        }
    }

    public static String nmsProtocol;

    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = getNMSPlayer(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object getNMSPlayer(Player player) {
        try {
            return player.getClass().getMethod("getHandle").invoke(player);
        } catch (Exception e) {
            return null;
        }
    }

    public static Class<?> getNMSClass(String name) {
        if (nmsProtocol == null)
            nmsProtocol = NMS.getVersion();

        try {
            return Class.forName("net.minecraft.server." + nmsProtocol + "." + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> getCBClass(String name) {
        if (nmsProtocol == null)
            nmsProtocol = NMS.getVersion();

        try {
            return Class.forName("org.bukkit.craftbukkit." + nmsProtocol + "." + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }

    public static short getSubVersion() {
        return Short.parseShort(getVersion().substring(2));
    }

}

enum Type {

    NMS,
    CB,
    OTHER

}