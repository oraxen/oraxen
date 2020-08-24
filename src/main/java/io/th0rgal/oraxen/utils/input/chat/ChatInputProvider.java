package io.th0rgal.oraxen.utils.input.chat;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.input.InputProvider;
import io.th0rgal.oraxen.utils.input.listeners.ChatInputListener;
import io.th0rgal.oraxen.utils.logs.Logs;

public class ChatInputProvider implements InputProvider {

    public static Listener LISTENER;
    private static HashSet<ChatInputProvider> PROVIDERS;

    public static void load(OraxenPlugin plugin) {
        if (PROVIDERS != null)
            return;
        PROVIDERS = new HashSet<>();
        Bukkit.getPluginManager().registerEvents((LISTENER = new ChatInputListener()), plugin);
    }

    public static ChatInputProvider getFree() {
        return PROVIDERS.stream().filter(ChatInputProvider::isFree).findFirst().orElseGet(new CIPSupplier());
    }

    public static Optional<ChatInputProvider> getByPlayer(Player player) {
        return getByUniqueId(player.getUniqueId());
    }

    public static Optional<ChatInputProvider> getByUniqueId(UUID userId) {
        return PROVIDERS.stream().filter(provider -> provider.getUserId().equals(userId)).findFirst();
    }

    private ChatInputProvider() {
    }

    private UUID userId;

    private String input;
    private String message;

    private BiPredicate<Player, InputProvider> response;
    private boolean reopenOnFail;
    
    @Override
    public boolean hasMultipleLines() {
        return false;
    }

    public UUID getUserId() {
        return userId;
    }

    public ChatInputProvider setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String[] getInput() {
        return input == null ? new String[0] : new String[] { input };
    }

    public boolean isFree() {
        return userId == null;
    }

    public ChatInputProvider reopenOnFail(boolean state) {
        this.reopenOnFail = state;
        return this;
    }

    public boolean reopenOnFail() {
        return reopenOnFail;
    }

    @Override
    public void open(Player player) {
        if (this.userId != null)
            return;
        if (message == null || player == null) {
            Logs
                .log(ChatColor.RED, "No message set for ChatInputProvider [player: "
                    + (player == null ? null : player.getUniqueId().toString()) + "]");
            return;
        }
        player.sendMessage(message);
        this.userId = player.getUniqueId();
    }

    public ChatInputProvider response(Player player, String input) {
        this.input = input;
        if (response != null && response.test(player, this) && reopenOnFail) {
            player.sendMessage(message);
            return this;
        }
        close();
        return this;
    }

    @Override
    public ChatInputProvider onRespond(BiPredicate<Player, InputProvider> response) {
        this.response = response;
        return this;
    }
    
    @Override
    public void close() {
        this.userId = null;
        this.message = null;
        this.response = null;
        this.reopenOnFail = false;
    }

    private static class CIPSupplier implements Supplier<ChatInputProvider> {

        private ChatInputProvider provider;

        @Override
        public ChatInputProvider get() {
            if (provider != null)
                return provider;
            provider = new ChatInputProvider();
            PROVIDERS.add(provider);
            return provider;
        }

    }

}
