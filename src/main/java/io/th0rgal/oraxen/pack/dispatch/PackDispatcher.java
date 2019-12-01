package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.utils.NMS;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class PackDispatcher {

    private static String url;

    public static void setPackURL(String packURL) {
        url = packURL;
    }

    public static void sendPack(Player player) {
        player.setResourcePack(url);
    }

    @SuppressWarnings("unchecked")
    public static void sendMenu(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        BaseComponent page = new TextComponent("§8§lRESOURCE PACK\nTo see the new items you need to use a resourcepack\n\nTo try to load it directly from the game, ");
        BaseComponent clic = new TextComponent("§a§l§nCLICK HERE");
        clic.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here to load directly the resourcepack from the game").create()));
        clic.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/oraxen pack getpack"));
        page.addExtra(clic);
        page.addExtra("\nTo download it from the internet, ");
        BaseComponent cmd = new TextComponent("§r§c§l§nCLICK HERE");
        cmd.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Clique here to download the resourcepack from the website").create()));
        cmd.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        page.addExtra(cmd);

        meta.spigot().addPage(new BaseComponent[]{page});
        book.setItemMeta(meta);

        ItemStack held = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(book);
        Class<?> PacketClass = NMS.PACKET_PLAY_OUT_OPEN_BOOK.toClass();
        Class<?> EnumHandClass = NMS.ENUM_HAND.toClass();

        try {
            Constructor<?> packetConstructor = PacketClass.getConstructor(EnumHandClass);
            Object packet = packetConstructor.newInstance(Enum.valueOf((Class<Enum>)EnumHandClass, "MAIN_HAND"));
            NMS.sendPacket(player, packet);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        player.getInventory().setItemInMainHand(held);
    }

}
