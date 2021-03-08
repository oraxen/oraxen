package io.th0rgal.oraxen.utils.input.sign;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.input.InputProvider;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SignMenuFactory {

    // https://www.spigotmc.org/threads/signmenu-1-15-2-get-player-sign-input.249381/

    private static final int ACTION_INDEX = 9;
    private static final int SIGN_LINES = 4;

    private static final String NBT_FORMAT = "{\"text\":\"%s\"}";
    private static final String NBT_BLOCK_ID = "minecraft:sign";

    private final Plugin plugin;

    private final Map<Player, Menu> inputReceivers;

    public SignMenuFactory(Plugin plugin) {
        this.plugin = plugin;
        this.inputReceivers = new HashMap<>();
        this.listen();
    }

    private void listen() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.plugin, PacketType.Play.Client.UPDATE_SIGN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();

                Menu menu = inputReceivers.remove(player);

                if (menu == null) {
                    return;
                }
                event.setCancelled(true);

                menu.text = Arrays.asList(event.getPacket().getStringArrays().read(0));

                boolean success = true;

                if (menu.response != null)
                    success = menu.response.test(player, (menu.setInput(event.getPacket().getStringArrays().read(0))));

                if (!success && menu.reopenOnFail()) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> menu.open(player), 2L);
                }
                player.sendBlockChange(menu.position.toLocation(player.getWorld()), Material.AIR.createBlockData());
            }
        });
    }

    public Menu newProvider() {
        return new Menu();
    }

    public class Menu implements InputProvider {

        public List<String> text;

        public String[] input = new String[0];

        private BiPredicate<Player, InputProvider> response;
        private boolean reopenOnFail;

        private BlockPosition position;

        private Menu() {
        }

        @Override
        public boolean hasMultipleLines() {
            return true;
        }

        @Override
        public String[] getInput() {
            return input;
        }

        private Menu setInput(String[] input) {
            this.input = input;
            return this;
        }

        @Override
        public Menu setMessage(String message) {
            text = Arrays.asList(message.split("\\u00B6line"));
            return this;
        }

        protected BlockPosition getPosition() {
            return this.position;
        }

        @Override
        public Menu reopenOnFail(boolean state) {
            this.reopenOnFail = state;
            return this;
        }

        @Override
        public boolean reopenOnFail() {
            return reopenOnFail;
        }

        @Override
        public Menu onRespond(BiPredicate<Player, InputProvider> response) {
            this.response = response;
            return this;
        }

        @Override
        public void open(Player player) {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(text, "message");
            Location location = player.getLocation();
            this.position = new BlockPosition(location.getBlockX(), location.getBlockY() - 5, location.getBlockZ());

            player.sendBlockChange(this.position.toLocation(location.getWorld()), Material.OAK_SIGN.createBlockData());

            PacketContainer openSign = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
            PacketContainer signData = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

            openSign.getBlockPositionModifier().write(0, this.position);

            NbtCompound signNBT = (NbtCompound) signData.getNbtModifier().read(0);

            IntStream
                .range(0, SIGN_LINES)
                .forEach(line -> signNBT.put("Text" + (line + 1), text.size() > line ? String.format(NBT_FORMAT, color(text.get(line))) : " "));

            signNBT.put("x", this.position.getX());
            signNBT.put("y", this.position.getY());
            signNBT.put("z", this.position.getZ());
            signNBT.put("id", NBT_BLOCK_ID);

            signData.getBlockPositionModifier().write(0, this.position);
            signData.getIntegers().write(0, ACTION_INDEX);
            signData.getNbtModifier().write(0, signNBT);

            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, signData);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, openSign);
            } catch (InvocationTargetException exception) {
                exception.printStackTrace();
            }
            inputReceivers.put(player, this);
        }

        @Override
        public void close() {
            List<Player> players = inputReceivers
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(this))
                .map(Entry::getKey)
                .collect(Collectors.toList());
            for (Player player : players)
                inputReceivers.remove(player);
            this.response = null;
            this.reopenOnFail = false;
            this.text = null;
            this.position = null;
        }

        private String color(String input) {
            return Utils.handleColors(input);
        }
    }
}