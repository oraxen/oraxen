package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.zip.ZipEntry;

public class PackCommand {

    @SuppressWarnings("unchecked")
    CommandAPICommand getPackCommand() {
        return new CommandAPICommand("pack")
                .withPermission("oraxen.command.pack")
                .withSubcommand(sendPackCommand())
                .withSubcommand(sendPackMessage())
                .withSubcommand(extractDefaultPackContent());

    }

    private CommandAPICommand sendPackCommand() {
        return new CommandAPICommand("send")
                .withPermission("oraxen.command.pack.send")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.getOptional("targets").orElse(sender instanceof Player ? sender : null);
                    if (targets == null) return;
                    
                    var multiVersionManager = OraxenPlugin.get().getMultiVersionUploadManager();
                    if (multiVersionManager != null && multiVersionManager.getPackSender() != null) {
                        // Multi-version mode: send version-appropriate packs
                        for (final Player target : targets) {
                            multiVersionManager.getPackSender().sendPack(target);
                        }
                    } else {
                        // Single-pack mode: use regular UploadManager
                        var uploadManager = OraxenPlugin.get().getUploadManager();
                        var packSender = uploadManager != null ? uploadManager.getSender() : null;
                        if (packSender == null) return;
                        for (final Player target : targets) {
                            packSender.sendPack(target);
                        }
                    }
                });
    }

    private CommandAPICommand sendPackMessage() {
        return new CommandAPICommand("msg")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.getOptional("targets").orElse(sender instanceof Player ? sender : null);
                    if (targets != null) for (final Player target : targets)
                        Message.COMMAND_JOIN_MESSAGE.send(target, AdventureUtils.tagResolver("pack_url",
                                OraxenPlugin.get().getPackURL() != null ? OraxenPlugin.get().getPackURL() : ""));
                });
    }

    private CommandAPICommand extractDefaultPackContent() {
        return new CommandAPICommand("extract_default")
                .withOptionalArguments(new TextArgument("folder").replaceSuggestions(ArgumentSuggestions.strings("all", "textures", "models", "sounds")))
                .withOptionalArguments(new BooleanArgument("override"))
                .executes((sender, args) -> {
                    final String type = (String) args.getOptional("folder").orElse("all");
                    final boolean override = (Boolean) args.getOptional("override").orElse(false);
                    ResourcesManager.browseJar(entry ->
                        extract(entry, type, OraxenPlugin.get().getResourceManager(), override)
                    );
                });
    }

    private void extract(ZipEntry entry, String type, ResourcesManager resourcesManager, boolean override) {
        if (!entry.getName().startsWith("pack/" + (type.equals("all") ? "" : type))) return;
        resourcesManager.extractFileIfTrue(entry, !OraxenPlugin.get().getDataFolder().toPath().resolve(entry.getName()).toFile().exists() || override);
    }
}
