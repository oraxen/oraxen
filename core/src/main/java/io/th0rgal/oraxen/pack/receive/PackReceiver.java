package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class PackReceiver implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        TagResolver playerResolver = AdventureUtils.tagResolver("player", event.getPlayer().getName());
        PackAction packAction = switch (status) {
            case ACCEPTED -> new PackAction(Settings.RECEIVE_ALLOWED_ACTIONS.toConfigSection(), playerResolver);
            case DECLINED -> new PackAction(Settings.RECEIVE_DENIED_ACTIONS.toConfigSection(), playerResolver);
            case FAILED_DOWNLOAD -> new PackAction(Settings.RECEIVE_FAILED_ACTIONS.toConfigSection(), playerResolver);
            case SUCCESSFULLY_LOADED -> new PackAction(Settings.RECEIVE_LOADED_ACTIONS.toConfigSection(), playerResolver);

            // 1.20.3+ only. switch statement should never reach this however on lower versions
            case DOWNLOADED -> new PackAction(Settings.RECEIVE_DOWNLOADED_ACTIONS.toConfigSection(), playerResolver);
            case INVALID_URL -> new PackAction(Settings.RECEIVE_INVALID_URL_ACTIONS.toConfigSection(), playerResolver);
            case FAILED_RELOAD -> new PackAction(Settings.RECEIVE_FAILED_RELOAD_ACTIONS.toConfigSection(), playerResolver);
            case DISCARDED -> new PackAction(Settings.RECEIVE_DISCARDED_ACTIONS.toConfigSection(), playerResolver);
        };
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            if (packAction.hasMessage())
                sendMessage(event.getPlayer(), packAction.getMessageType(), packAction.getMessageContent());
            if (packAction.hasSound())
                packAction.playSound(event.getPlayer(), event.getPlayer().getLocation());

            packAction.getCommandsParser().perform(event.getPlayer());
        }, packAction.getDelay());
    }

    private void sendMessage(Player receiver, String action, Component message) {
        @NotNull Audience audience = OraxenPlugin.get().getAudience().sender(receiver);
        switch (action) {
            case "KICK" -> receiver.kickPlayer(AdventureUtils.LEGACY_SERIALIZER.serialize(message));
            case "CHAT" -> audience.sendMessage(message);
            case "ACTION_BAR" -> audience.sendActionBar(message);
            case "TITLE" ->
                    audience.showTitle(Title.title(Component.empty(), message, Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(3500), Duration.ofMillis(250))));
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }
    }

}
