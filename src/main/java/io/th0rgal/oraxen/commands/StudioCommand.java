package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.studio.StudioSyncService;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.command.CommandSender;

public class StudioCommand {

    OraxenCommand getStudioCommand() {
        return new OraxenCommand("studio")
                .withPermission("oraxen.command.studio")
                .withSubcommand(connectCommand())
                .withSubcommand(statusCommand())
                .withSubcommand(pullCommand())
                .withSubcommand(pushCommand())
                .withSubcommand(disconnectCommand())
                .executes((sender, args) -> sendStatus(sender));
    }

    private OraxenCommand connectCommand() {
        return new OraxenCommand("connect")
                .withPermission("oraxen.command.studio.connect")
                .executes((sender, args) -> {
                    StudioSyncService studio = OraxenPlugin.get().getStudioSyncService();
                    if (studio != null) {
                        studio.initiatePairing(sender);
                    }
                });
    }

    private OraxenCommand statusCommand() {
        return new OraxenCommand("status")
                .withPermission("oraxen.command.studio.status")
                .executes((sender, args) -> sendStatus(sender));
    }

    private OraxenCommand pullCommand() {
        return new OraxenCommand("pull")
                .withPermission("oraxen.command.studio.pull")
                .executes((sender, args) -> {
                    StudioSyncService studio = OraxenPlugin.get().getStudioSyncService();
                    if (studio != null) {
                        studio.pullFromStudio(sender);
                    }
                });
    }

    private OraxenCommand pushCommand() {
        return new OraxenCommand("push")
                .withPermission("oraxen.command.studio.push")
                .withSubcommand(new OraxenCommand("force")
                        .withPermission("oraxen.command.studio.push")
                        .executes((sender, args) -> push(sender, true)))
                .executes((sender, args) -> push(sender, false));
    }

    private void push(CommandSender sender, boolean force) {
        StudioSyncService studio = OraxenPlugin.get().getStudioSyncService();
        if (studio != null) {
            studio.pushToStudio(sender, force);
        }
    }

    private OraxenCommand disconnectCommand() {
        return new OraxenCommand("disconnect")
                .withPermission("oraxen.command.studio.disconnect")
                .executes((sender, args) -> {
                    StudioSyncService studio = OraxenPlugin.get().getStudioSyncService();
                    if (studio != null) {
                        studio.disconnect(sender);
                    }
                });
    }

    private void sendStatus(CommandSender sender) {
        StudioSyncService studio = OraxenPlugin.get().getStudioSyncService();
        if (studio == null) return;
        boolean hasSecret = !studio.getSecret().isEmpty();
        boolean wsConnected = studio.isWebSocketConnected();
        String wsState = wsConnected ? "<#55ffa4>connected" : (hasSecret ? "<#facc43>configured (connecting)" : "<#fa4943>not connected");
        String workspaceInfo = studio.getConnectedWorkspaceId().isEmpty()
                ? ""
                : " <gray>(workspace: <#43c9fa>" + studio.getConnectedWorkspaceName() + "</#43c9fa>)";
        String msg = "<prefix><#529ced>Oraxen Studio: " + wsState + workspaceInfo
                + " <gray>| URL: <#43c9fa>" + studio.getStudioBaseUrl()
                + "</#43c9fa> | Last rev: <#43fa8c>#" + studio.getLastSyncedRevision();
        AdventureUtils.sendMessage(sender, AdventureUtils.MINI_MESSAGE.deserialize(msg));
    }
}
