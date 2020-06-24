package io.th0rgal.oraxen.commands.brigadier;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.lucko.commodore.Commodore;
import org.bukkit.command.PluginCommand;

public class BrigadierManager {

    public static void registerCompletions(Commodore commodore, PluginCommand command) {
        commodore.register(command, LiteralArgumentBuilder.literal("oraxen")
                .then(LiteralArgumentBuilder.literal("give")
                        .then(RequiredArgumentBuilder.argument("player", StringArgumentType.string())
                                .then(RequiredArgumentBuilder.argument("itemID", StringArgumentType.string())
                                        .then(RequiredArgumentBuilder.argument("amount", IntegerArgumentType.integer())))))
                .then(LiteralArgumentBuilder.literal("inv"))
                .then(LiteralArgumentBuilder.literal("recipes")
                        .then(LiteralArgumentBuilder.literal("open")
                                .then(LiteralArgumentBuilder.literal("shaped"))
                                .then(LiteralArgumentBuilder.literal("shapeless"))
                                .then(LiteralArgumentBuilder.literal("furnace")
                                        .then(LiteralArgumentBuilder.literal("cookingtime"))
                                        .then(LiteralArgumentBuilder.literal("experience"))))
                        .then(LiteralArgumentBuilder.literal("save")))
                .then(LiteralArgumentBuilder.literal("reload")
                        .then(LiteralArgumentBuilder.literal("items"))
                        .then(LiteralArgumentBuilder.literal("pack"))
                        .then(LiteralArgumentBuilder.literal("recipes")))
        );
    }
}