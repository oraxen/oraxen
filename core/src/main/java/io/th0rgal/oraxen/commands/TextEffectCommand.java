package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.TextEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


/**
 * Command for testing text effects.
 * <p>
 * Usage:
 * - /oraxen texteffect <effect> <text>
 * - /oraxen texteffect <effect> <speed> <text>
 * - /oraxen texteffect <effect> <speed> <param> <text>
 * <p>
 * Examples:
 * - /oraxen texteffect rainbow Hello World!
 * - /oraxen texteffect wave 5 Wavy Text
 * - /oraxen texteffect shake 8 2 Shaky!
 */
public class TextEffectCommand {

    private static String[] getEffectNames() {
        return TextEffect.getEffects().stream()
                .map(TextEffect.Definition::getName)
                .toArray(String[]::new);
    }

    public CommandAPICommand getTextEffectCommand() {
        // Create three variants to handle different argument counts
        // CommandAPI doesn't handle optional integers before greedy strings well
        return new CommandAPICommand("texteffect")
                .withPermission("oraxen.command.texteffect")
                .withSubcommands(
                        // Variant with speed and param: /texteffect <effect> <speed> <param> <text>
                        getFullCommand(),
                        // Variant with just speed: /texteffect <effect> <speed> <text>
                        getSpeedCommand(),
                        // Basic variant: /texteffect <effect> <text>
                        getBasicCommand()
                )
                .executes((sender, args) -> {
                    sender.sendMessage("Usage: /oraxen texteffect <effect> [speed] [param] <text>");
                    sender.sendMessage("Use /oraxen texteffects for list of effects");
                });
    }

    private CommandAPICommand getBasicCommand() {
        return new CommandAPICommand("basic")
                .withPermission("oraxen.command.texteffect")
                .withArguments(
                        new StringArgument("effect").replaceSuggestions(ArgumentSuggestions.strings(getEffectNames())),
                        new GreedyStringArgument("text")
                )
                .executesPlayer((player, args) -> {
                    String effectName = (String) args.get("effect");
                    String text = (String) args.get("text");
                    executeTextEffect(player, effectName, TextEffect.DEFAULT_SPEED, TextEffect.DEFAULT_PARAM, text);
                });
    }

    private CommandAPICommand getSpeedCommand() {
        return new CommandAPICommand("speed")
                .withPermission("oraxen.command.texteffect")
                .withArguments(
                        new StringArgument("effect").replaceSuggestions(ArgumentSuggestions.strings(getEffectNames())),
                        new IntegerArgument("speed", 1, 7),
                        new GreedyStringArgument("text")
                )
                .executesPlayer((player, args) -> {
                    String effectName = (String) args.get("effect");
                    int speed = (Integer) args.get("speed");
                    String text = (String) args.get("text");
                    executeTextEffect(player, effectName, speed, TextEffect.DEFAULT_PARAM, text);
                });
    }

    private CommandAPICommand getFullCommand() {
        return new CommandAPICommand("full")
                .withPermission("oraxen.command.texteffect")
                .withArguments(
                        new StringArgument("effect").replaceSuggestions(ArgumentSuggestions.strings(getEffectNames())),
                        new IntegerArgument("speed", 1, 7),
                        new IntegerArgument("param", 0, 7),
                        new GreedyStringArgument("text")
                )
                .executesPlayer((player, args) -> {
                    String effectName = (String) args.get("effect");
                    int speed = (Integer) args.get("speed");
                    int param = (Integer) args.get("param");
                    String text = (String) args.get("text");
                    executeTextEffect(player, effectName, speed, param, text);
                });
    }

    private void executeTextEffect(Player player, String effectName, int speed, int param, String text) {
        if (text == null || text.isEmpty()) {
            player.sendMessage(Component.text("Please provide text to apply the effect to."));
            return;
        }

        TextEffect.Definition definition = TextEffect.getEffect(effectName);
        if (definition == null) {
            player.sendMessage(Component.text("Unknown effect: " + effectName + ". Available: " +
                    String.join(", ", getEffectNames())));
            return;
        }

        if (!TextEffect.isEnabled()) {
            player.sendMessage(Component.text("Text effects are disabled in settings.yml"));
            return;
        }

        if (!TextEffect.isEffectEnabled(definition)) {
            player.sendMessage(Component.text("The " + definition.getName() + " effect is disabled"));
            return;
        }

        Component effectComponent = TextEffect.apply(text, definition, speed, param);

        // Send to chat
        player.sendMessage(Component.text("Chat: ").append(effectComponent));

        // Send as actionbar
        OraxenPlugin.get().getAudience().player(player).sendActionBar(effectComponent);

        // Info message
        player.sendMessage(Component.text("Sent " + definition.getName() + " effect (speed=" + speed + ", param=" + param + ")"));
    }

    /**
     * Gets a command that lists all available text effects.
     */
    public CommandAPICommand getTextEffectsListCommand() {
        return new CommandAPICommand("texteffects")
                .withPermission("oraxen.command.texteffect")
                .executes((sender, args) -> {
                    sender.sendMessage("Available text effects:");
                    for (TextEffect.Definition definition : TextEffect.getEffects()) {
                        String description = definition.getDescription();
                        String suffix = TextEffect.isEffectEnabled(definition) ? "" : " (disabled)";
                        if (description == null || description.isEmpty()) {
                            sender.sendMessage("- " + definition.getName() + suffix);
                        } else {
                            sender.sendMessage("- " + definition.getName() + ": " + description + suffix);
                        }
                    }
                    sender.sendMessage("");
                    sender.sendMessage("Usage:");
                    sender.sendMessage("  /oraxen texteffect basic <effect> <text>");
                    sender.sendMessage("  /oraxen texteffect speed <effect> <speed> <text>");
                    sender.sendMessage("  /oraxen texteffect full <effect> <speed> <param> <text>");
                    sender.sendMessage("");
                    sender.sendMessage("  speed: 1-7 (default: 3)");
                    sender.sendMessage("  param: 0-7 (amplitude/intensity, default: 3)");

                    if (sender instanceof Player player) {
                        // Show demo of each effect
                        sender.sendMessage("");
                        sender.sendMessage("Demo:");
                        for (TextEffect.Definition definition : TextEffect.getEffects()) {
                            if (TextEffect.isEffectEnabled(definition)) {
                                Component demo = TextEffect.apply(definition.getName(), definition);
                                player.sendMessage(Component.text("  " + definition.getName() + ": ").append(demo));
                            }
                        }
                    }
                });
    }
}
