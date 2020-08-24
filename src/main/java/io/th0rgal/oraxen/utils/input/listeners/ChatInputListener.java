package io.th0rgal.oraxen.utils.input.listeners;

import java.util.Optional;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import io.th0rgal.oraxen.utils.input.chat.ChatInputProvider;

public class ChatInputListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onChatAsync(AsyncPlayerChatEvent event) {
        Optional<ChatInputProvider> optional = ChatInputProvider.getByPlayer(event.getPlayer());
        if(!optional.isPresent())
            return;
        optional.get().response(event.getPlayer(), event.getMessage());
        event.setCancelled(true);
    }
    
}
