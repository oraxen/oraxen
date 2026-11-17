package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.commands.arguments.ArgumentSuggestions;
import io.th0rgal.oraxen.commands.arguments.StringArgument;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockType;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockLimitsCommand {
    private static final int BAR_WIDTH = 30;
    private static final List<String> TYPES = List.of("note", "chorus", "string", "stair", "slab", "door",
            "trapdoor", "grate", "bulb");

    OraxenCommand getBlockLimitsCommand() {
        return new OraxenCommand("block-limits")
                .withPermission("oraxen.command.block-limits")
                .withArguments(new StringArgument("type").setOptional(true)
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                supportedTypes().toArray(String[]::new))))
                .executes((sender, args) -> {
                    String type = normalizeType(args.getOrDefault("type", "").toString());
                    List<Usage> usages = usages();
                    if (!type.isEmpty() && usages.stream().noneMatch(usage -> usage.type().equals(type))) {
                        AdventureUtils.sendMessage(sender, Component.text("Unknown block type. Choose from "
                                + String.join(", ", usages.stream().map(Usage::type).toList()) + ".", NamedTextColor.RED));
                        return;
                    }
                    AdventureUtils.sendMessage(sender, report(usages.stream()
                            .filter(usage -> type.isEmpty() || usage.type().equals(type)).toList()));
                });
    }

    static List<String> supportedTypes() {
        return VersionUtil.atOrAbove("1.20.4") ? TYPES : TYPES.subList(0, 3);
    }

    static String normalizeType(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "full", "noteblock", "note_block" -> "note";
            case "chorusblock", "chorus_block" -> "chorus";
            case "stringblock", "string_block", "tripwire" -> "string";
            default -> type.toLowerCase(Locale.ROOT);
        };
    }

    private static List<Usage> usages() {
        boolean enabled = MechanicsManager.isMechanicEnabled("block");
        List<Usage> usages = new ArrayList<>();
        // Note blocks accept variation IDs 0 through 775, inclusive.
        usages.add(new Usage("note", "Note blocks", 776, enabled ? countSlots(
                NoteBlockMechanicFactory.BLOCK_PER_VARIATION, NoteBlockMechanicFactory.getInstance(), 0, 775) : 0));
        usages.add(new Usage("chorus", "Chorus blocks", ChorusBlockMechanicFactory.MAX_BLOCK_VARIATION,
                enabled ? countSlots(ChorusBlockMechanicFactory.BLOCK_PER_VARIATION,
                        ChorusBlockMechanicFactory.getInstance(), 1, ChorusBlockMechanicFactory.MAX_BLOCK_VARIATION) : 0));
        usages.add(new Usage("string", "String blocks", 127, enabled ? countSlots(
                StringBlockMechanicFactory.BLOCK_PER_VARIATION, StringBlockMechanicFactory.getInstance(), 1, 127) : 0));
        if (!VersionUtil.atOrAbove("1.20.4")) return usages;
        ShapedBlockMechanicFactory shaped = enabled ? ShapedBlockMechanicFactory.getInstance() : null;
        var mechanics = shaped == null ? Map.of() : shaped.getAllMechanics();
        for (ShapedBlockType type : ShapedBlockType.values()) {
            String id = type.name().toLowerCase(Locale.ROOT);
            int used = (int) Arrays.stream(type.getMaterials()).filter(mechanics::containsKey).count();
            usages.add(new Usage(id, "Copper " + id + " blocks", type.getMaterials().length, used));
        }
        return usages;
    }

    static int countSlots(Map<Integer, ? extends Mechanic> slots, MechanicFactory factory, int min, int max) {
        if (factory == null) return 0;
        // Static variation maps can retain entries belonging to a factory replaced by reload.
        return (int) slots.entrySet().stream()
                .filter(entry -> entry.getKey() >= min && entry.getKey() <= max)
                .filter(entry -> entry.getValue().getFactory() == factory).count();
    }

    static Component report(List<Usage> usages) {
        Component report = Component.empty();
        for (Usage usage : usages) {
            if (!report.equals(Component.empty())) report = report.append(Component.newline()).append(Component.newline());
            int filled = (int) Math.round((double) usage.used() / usage.max() * BAR_WIDTH);
            String percent = String.format(Locale.ROOT, "%.1f", 100.0 * usage.used() / usage.max());
            report = report.append(Component.text(usage.title(), NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text("|".repeat(filled), NamedTextColor.GREEN))
                    .append(Component.text("|".repeat(BAR_WIDTH - filled), NamedTextColor.DARK_GRAY))
                    .append(Component.newline())
                    .append(Component.text(usage.used() + " / " + usage.max() + " used (" + percent + "%) • "
                            + (usage.max() - usage.used()) + " free", NamedTextColor.WHITE));
        }
        return report;
    }

    record Usage(String type, String title, int max, int used) {}
}
