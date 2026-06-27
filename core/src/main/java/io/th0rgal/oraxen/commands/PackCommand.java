package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.commands.arguments.ArgumentSuggestions;
import io.th0rgal.oraxen.commands.arguments.BooleanArgument;
import io.th0rgal.oraxen.commands.arguments.EntitySelectorArgument;
import io.th0rgal.oraxen.commands.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ResourcesManager;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;

public class PackCommand {

    @SuppressWarnings("unchecked")
    OraxenCommand getPackCommand() {
        return new OraxenCommand("pack")
                .withPermission("oraxen.command.pack")
                .withSubcommand(sendPackCommand())
                .withSubcommand(extractDefaultPackContent());

    }

    private OraxenCommand sendPackCommand() {
        return new OraxenCommand("send")
                .withPermission("oraxen.command.pack.send")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.getOptional("targets")
                            .orElse(sender instanceof Player player ? List.of(player) : null);
                    if (targets == null) return;
                    
                    var multiVersionManager = OraxenPlugin.get().getMultiVersionUploadManager();
                    var mvPackSender = multiVersionManager != null ? multiVersionManager.getPackSender() : null;
                    if (mvPackSender != null) {
                        // Multi-version mode: send version-appropriate packs
                        for (final Player target : targets) {
                            mvPackSender.sendPack(target);
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

    private OraxenCommand extractDefaultPackContent() {
        return new OraxenCommand("extract_default")
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
