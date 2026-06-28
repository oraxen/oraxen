package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.commands.arguments.ArgumentSuggestions;
import io.th0rgal.oraxen.commands.arguments.GreedyStringArgument;
import io.th0rgal.oraxen.commands.arguments.StringArgument;
import io.th0rgal.oraxen.glyphs.TextEffect;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


/**
 * Command for testing text effects.
 * <p>
 * Usage: /oraxen texteffect &lt;effect&gt; &lt;text&gt;
 * <p>
 * Examples:
 * - /oraxen texteffect rainbow Hello World!
 * - /oraxen texteffect wave Wavy Text
 * - /oraxen texteffect shake Shaky!
 */
public class TextEffectCommand {

    private static String[] getEffectNames() {
        return TextEffect.getEffects().stream()
                .map(TextEffect.Definition::getName)
                .toArray(String[]::new);
    }

    public OraxenCommand getTextEffectCommand() {
        return new OraxenCommand("texteffect")
                .withPermission("oraxen.command.texteffect")
                .withArguments(
                        new StringArgument("effect").replaceSuggestions(ArgumentSuggestions.strings(getEffectNames())),
                        new GreedyStringArgument("text")
                )
                .executesPlayer((player, args) -> {
                    String effectName = (String) args.get("effect");
                    String text = (String) args.get("text");
                    executeTextEffect(player, effectName, text);
                });
    }

    private void executeTextEffect(Player player, String effectName, String text) {
        if (text == null || text.isEmpty()) {
            AdventureUtils.sendMessage(player, Component.text("Please provide text to apply the effect to."));
            return;
        }

        TextEffect.Definition definition = TextEffect.getEffect(effectName);
        if (definition == null) {
            AdventureUtils.sendMessage(player, Component.text("Unknown effect: " + effectName + ". Available: " +
                    String.join(", ", getEffectNames())));
            return;
        }

        if (!TextEffect.isEnabled()) {
            AdventureUtils.sendMessage(player, Component.text("Text effects are disabled in settings.yml"));
            return;
        }

        if (!TextEffect.isEffectEnabled(definition)) {
            AdventureUtils.sendMessage(player, Component.text("The " + definition.getName() + " effect is disabled"));
            return;
        }

        Component effectComponent = TextEffect.apply(text, definition);

        // Send to chat
        AdventureUtils.sendMessage(player, Component.text("Chat: ").append(effectComponent));

        // Send as actionbar
        AdventureUtils.sendActionBar(player, effectComponent);

        // Info message
        AdventureUtils.sendMessage(player, Component.text("Sent " + definition.getName() + " effect"));
    }

    /**
     * Gets a command that lists all available text effects.
     */
    public OraxenCommand getTextEffectsListCommand() {
        return new OraxenCommand("texteffects")
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
                    sender.sendMessage("  /oraxen texteffect <effect> <text>");
                    sender.sendMessage("");
                    sender.sendMessage("In chat/config, use: <effect:NAME>text</effect>");

                    if (sender instanceof Player player) {
                        // Show demo of each effect
                        sender.sendMessage("");
                        sender.sendMessage("Demo:");
                        for (TextEffect.Definition definition : TextEffect.getEffects()) {
                            if (TextEffect.isEffectEnabled(definition)) {
                                Component demo = TextEffect.apply(definition.getName(), definition);
                                AdventureUtils.sendMessage(player, Component.text("  " + definition.getName() + ": ").append(demo));
                            }
                        }
                    }
                });
    }
}
