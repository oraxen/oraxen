package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.utils.NMS;
import net.minecraft.server.v1_14_R1.EnumHand;
import net.minecraft.server.v1_14_R1.PacketPlayOutOpenBook;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import net.md_5.bungee.chat.ComponentSerializer;

public class PackDispatcher {

    private static String url;

    public static void setPackURL(String packURL) {
        url = packURL;
    }

    public static void sendPack(Player player) {
        player.setResourcePack(url);
    }

    public static void sendMenu(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.spigot().setPages(ComponentSerializer.parse(Pack.MENU_JSON.toString()));

        ItemStack held = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(book);
        NMS.sendPacket(player, new PacketPlayOutOpenBook(EnumHand.MAIN_HAND));
        player.getInventory().setItemInMainHand(held);
    }

}
