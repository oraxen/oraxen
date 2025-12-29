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

import java.util.Arrays;

/**
 * Command for testing text effects.
 * <p>
 * Usage: /oraxen texteffect <effect> [speed] [param] <text>
 * <p>
 * Examples:
 * - /oraxen texteffect rainbow Hello World!
 * - /oraxen texteffect wave 5 3 Wavy Text
 * - /oraxen texteffect shake 8 2 Shaky!
 */
public class TextEffectCommand {

    public CommandAPICommand getTextEffectCommand() {
        return new CommandAPICommand("texteffect")
                .withPermission("oraxen.command.texteffect")
                .withArguments(
                        new StringArgument("effect")
                                .replaceSuggestions(ArgumentSuggestions.strings(
                                        Arrays.stream(TextEffect.Type.values())
                                                .map(TextEffect.Type::getName)
                                                .toArray(String[]::new)
                                )),
                        new IntegerArgument("speed", 1, 15).setOptional(true),
                        new IntegerArgument("param", 0, 15).setOptional(true),
                        new GreedyStringArgument("text")
                )
                .executesPlayer((player, args) -> {
                    String effectName = (String) args.get("effect");
                    Integer speed = (Integer) args.getOptional("speed").orElse(TextEffect.DEFAULT_SPEED);
                    Integer param = (Integer) args.getOptional("param").orElse(TextEffect.DEFAULT_PARAM);
                    String text = (String) args.get("text");

                    if (text == null || text.isEmpty()) {
                        player.sendMessage(Component.text("Please provide text to apply the effect to."));
                        return;
                    }

                    TextEffect.Type type = TextEffect.Type.fromName(effectName);
                    if (type == null) {
                        player.sendMessage(Component.text("Unknown effect: " + effectName + ". Available: " +
                                String.join(", ", Arrays.stream(TextEffect.Type.values())
                                        .map(TextEffect.Type::getName)
                                        .toArray(String[]::new))));
                        return;
                    }

                    if (!TextEffect.isEnabled()) {
                        player.sendMessage(Component.text("Text effects are disabled in settings.yml"));
                        return;
                    }

                    if (!TextEffect.isEffectEnabled(type)) {
                        player.sendMessage(Component.text("The " + type.getName() + " effect is disabled in settings.yml"));
                        return;
                    }

                    Component effectComponent = TextEffect.apply(text, type, speed, param);

                    // Send to chat
                    player.sendMessage(Component.text("Chat: ").append(effectComponent));

                    // Send as actionbar
                    OraxenPlugin.get().getAudience().player(player).sendActionBar(effectComponent);

                    // Info message
                    player.sendMessage(Component.text("Sent " + type.getName() + " effect (speed=" + speed + ", param=" + param + ")"));
                });
    }

    /**
     * Gets a command that lists all available text effects.
     */
    public CommandAPICommand getTextEffectsListCommand() {
        return new CommandAPICommand("texteffects")
                .withPermission("oraxen.command.texteffect")
                .executes((sender, args) -> {
                    sender.sendMessage("Available text effects:");
                    sender.sendMessage("- rainbow: Cycles through rainbow colors");
                    sender.sendMessage("- wave: Vertical sine wave motion");
                    sender.sendMessage("- shake: Random jitter");
                    sender.sendMessage("- pulse: Opacity fades in/out");
                    sender.sendMessage("- gradient: Static color gradient");
                    sender.sendMessage("- typewriter: Characters appear sequentially");
                    sender.sendMessage("- wobble: Rotation oscillation");
                    sender.sendMessage("- obfuscate: Rapidly cycling random colors");
                    sender.sendMessage("");
                    sender.sendMessage("Usage: /oraxen texteffect <effect> [speed] [param] <text>");
                    sender.sendMessage("  speed: 1-15 (default: 3)");
                    sender.sendMessage("  param: 0-15 (amplitude/intensity, default: 3)");

                    if (sender instanceof Player player) {
                        // Show demo of each effect
                        sender.sendMessage("");
                        sender.sendMessage("Demo:");
                        for (TextEffect.Type type : TextEffect.Type.values()) {
                            if (TextEffect.isEffectEnabled(type)) {
                                Component demo = TextEffect.apply(type.getName(), type);
                                player.sendMessage(Component.text("  " + type.getName() + ": ").append(demo));
                            }
                        }
                    }
                });
    }
}
