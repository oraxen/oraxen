package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class PackSender {

    protected final HostingProvider hostingProvider;


    protected PackSender(HostingProvider hostingProvider) {
        this.hostingProvider = hostingProvider;
    }

    public abstract void register();

    public abstract void unregister();

    public abstract void sendPack(Player player);

    protected void sendWelcomeMessage(Player player, boolean delayed) {
        long delay = (int) Settings.JOIN_MESSAGE_DELAY.getValue();
        if (delay == -1 || !delayed)
            Message.COMMAND_JOIN_MESSAGE.send(player,
                    AdventureUtils.tagResolver("pack_url", hostingProvider.getPackURL()),
                    AdventureUtils.tagResolver("player", player.getName()));
        else
            SchedulerUtil.runTaskLaterAsync(delay * 20L,
                    () -> Message.COMMAND_JOIN_MESSAGE.send(player,
                            AdventureUtils.tagResolver("pack_url", hostingProvider.getPackURL()),
                            AdventureUtils.tagResolver("player", player.getName())));
    }

}
