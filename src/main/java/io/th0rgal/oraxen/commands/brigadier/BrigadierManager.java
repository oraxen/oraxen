package io.th0rgal.oraxen.commands.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.commodore.Commodore;
import org.bukkit.command.PluginCommand;

public class BrigadierManager {

    public static void registerCompletions(Commodore commodore, PluginCommand command) {
        commodore.register(command, LiteralArgumentBuilder.literal("oraxen")
                .then(LiteralArgumentBuilder.literal("give")
                    .then(LiteralArgumentBuilder.literal("")))
                .then(LiteralArgumentBuilder.literal("inv"))
                .then(LiteralArgumentBuilder.literal("recipes")
                        .then(LiteralArgumentBuilder.literal("open"))
                        .then(LiteralArgumentBuilder.literal("save")))
        );
    }
}