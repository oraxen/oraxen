package io.th0rgal.oraxen.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public enum NMS {

    PACKET(NMS.getNMSClass("Packet")),
    ENTITY_PLAYER(NMS.getNMSClass("EntityPlayer")),
    ITEM_STACK(NMS.getNMSClass("ItemStack")),
    NBT_TAG_COMPOUND(NMS.getNMSClass("NBTTagCompound")),
    NBT_BASE(NMS.getNMSClass("NBTBase")),
    NBT_TAG_STRING(NMS.getNMSClass("NBTTagString")),
    NBT_TAG_INT(NMS.getNMSClass("NBTTagInt")),

    PACKET_PLAY_OUT_OPEN_BOOK(NMS.getNMSClass("PacketPlayOutOpenBook")),
    ENUM_HAND(NMS.getNMSClass("EnumHand")),
    CRAFT_META_BOOK(NMS.getNMSClass("CraftMetaBook")),

    CRAFT_PLAYER(NMS.getCBClass("entity.CraftPlayer")),
    CRAFT_ITEM_STACK(NMS.getCBClass("inventory.CraftItemStack"));

    private final Class<?> clazz;

    NMS(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> toClass() {
        return this.clazz;
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

    private static Class<?> getNMSClass(String name) {
        if (nmsProtocol == null)
            nmsProtocol = NMS.getVersion();

        try {
            return Class.forName("net.minecraft.server." + nmsProtocol + "." + name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class<?> getCBClass(String name) {
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