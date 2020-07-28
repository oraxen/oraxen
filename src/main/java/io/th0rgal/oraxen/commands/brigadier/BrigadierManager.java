package io.th0rgal.oraxen.commands.brigadier;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.th0rgal.oraxen.items.OraxenItems;
import me.lucko.commodore.Commodore;
import org.bukkit.command.PluginCommand;

public class BrigadierManager {

    public static void registerCompletions(Commodore commodore, PluginCommand command) {

        RequiredArgumentBuilder<Object, String> items = RequiredArgumentBuilder.argument("player", StringArgumentType.string());

        OraxenItems.getAllItems().keySet().forEach(itemID -> items.then(LiteralArgumentBuilder.literal(itemID)));

        LiteralCommandNode<?> completions = LiteralArgumentBuilder.literal("oraxen")
                .then(LiteralArgumentBuilder.literal("give")
                        .then(items
                                .then(RequiredArgumentBuilder.argument("amount", IntegerArgumentType.integer()))))
                .then(LiteralArgumentBuilder.literal("inv")
                        .then(LiteralArgumentBuilder.literal("all")))
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
                .then(LiteralArgumentBuilder.literal("repair")
                        .then(LiteralArgumentBuilder.literal("all")))
                .then(LiteralArgumentBuilder.literal("debug"))
                .then(LiteralArgumentBuilder.literal("pack")
                        .then(LiteralArgumentBuilder.literal("getpack"))
                        .then(LiteralArgumentBuilder.literal("getmenu"))
                        .then(LiteralArgumentBuilder.literal("sendpack")
                                .then(RequiredArgumentBuilder.argument("player", StringArgumentType.string())))
                        .then(LiteralArgumentBuilder.literal("sendmenu")
                                .then(RequiredArgumentBuilder.argument("player", StringArgumentType.string()))))
                .build();

        commodore.register(command, completions);

    }
}