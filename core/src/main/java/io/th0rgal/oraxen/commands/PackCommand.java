package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ResourcesManager;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PackCommand {

    @SuppressWarnings("unchecked")
    CommandAPICommand getPackCommand() {
        return new CommandAPICommand("pack")
                .withPermission("oraxen.command.pack")
                .withSubcommand(sendPackCommand())
                .withSubcommand(extractDefaultPackContent());

    }

    private CommandAPICommand sendPackCommand() {
        return new CommandAPICommand("send")
                .withPermission("oraxen.command.pack.send")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.getOptional("targets").orElse(sender instanceof Player ? sender : null);
                    if (targets != null) for (final Player target : targets)
                        OraxenPlugin.get().getPackServer().sendPack(target);
                });
    }

    private CommandAPICommand extractDefaultPackContent() {
        return new CommandAPICommand("extract_default")
                .withOptionalArguments(new TextArgument("folder").replaceSuggestions(ArgumentSuggestions.strings("all", "textures", "models", "sounds")))
                .withOptionalArguments(new BooleanArgument("override"))
                .executes((sender, args) -> {
                    final String type = (String) args.getOptional("folder").orElse("all");
                    final ZipInputStream zip = ResourcesManager.browse();
                    try {
                        ZipEntry entry = zip.getNextEntry();
                        final ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());
                        while (entry != null) {
                            extract(entry, type, resourcesManager, (Boolean) args.getOptional("override").orElse(false));
                            entry = zip.getNextEntry();
                        }
                        zip.closeEntry();
                        zip.close();
                    } catch (final IOException ex) {
                        ex.printStackTrace();
                    }

                });
    }

    private void extract(ZipEntry entry, String type, ResourcesManager resourcesManager, boolean override) {
        if (!entry.getName().startsWith("pack/" + (type.equals("all") ? "" : type))) return;
        resourcesManager.extractFileIfTrue(entry, !OraxenPlugin.get().getDataFolder().toPath().resolve(entry.getName()).toFile().exists() || override);
    }
}
