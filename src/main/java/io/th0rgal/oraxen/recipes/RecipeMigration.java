package io.th0rgal.oraxen.recipes;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Renames recipe syntax without deserializing or traversing item stack data. */
public final class RecipeMigration {
    private static final Map<String, String> RECIPE_KEYS = Map.of(
            "cookingTime", "cooking-time",
            "experience_cost", "experience-cost",
            "level_change", "level-change");
    private static final Map<String, String> ITEM_KEYS = Map.of(
            "oraxen_item", "oraxen-item",
            "crucible_item", "crucible-item",
            "mmoitems_type", "mmoitems-type",
            "mmoitems_id", "mmoitems-id",
            "ecoitem_id", "ecoitem-id",
            "minecraft_type", "minecraft-type",
            "minecraft_item", "minecraft-item");
    private static final List<String> ITEM_SECTIONS = List.of("result", "input", "base", "addition", "template");

    private RecipeMigration() {
    }

    /** Keeps a sibling .bak file and replaces the source only when an atomic move is supported. */
    public static boolean migrate(File file) throws IOException {
        Path source = file.toPath();
        String original = Files.readString(source, StandardCharsets.UTF_8);
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);
        loaderOptions.setAllowDuplicateKeys(false);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);
        String migrated;
        try {
            Node root = yaml.compose(new StringReader(original));
            if (!(root instanceof MappingNode recipes)) return false;
            assertUniqueKeys(recipes);
            boolean changed = false;
            for (int i = 0; i < recipes.getValue().size(); i++) {
                NodeTuple entry = recipes.getValue().get(i);
                Node migratedRecipe = migrateRecipe(entry.getValueNode());
                if (migratedRecipe != entry.getValueNode()) {
                    recipes.getValue().set(i, new NodeTuple(entry.getKeyNode(), migratedRecipe));
                    changed = true;
                }
            }
            if (!changed) return false;
            StringWriter writer = new StringWriter();
            yaml.serialize(root, writer);
            migrated = writer.toString();
        } catch (RuntimeException exception) {
            throw new IOException("Could not migrate recipe file " + file.getName(), exception);
        }

        Path temporary = Files.createTempFile(source.toAbsolutePath().getParent(), ".recipe-migration-", ".tmp");
        try {
            Files.writeString(temporary, migrated, StandardCharsets.UTF_8);
            Path backup = source.resolveSibling(source.getFileName() + ".bak");
            for (int suffix = 1; Files.exists(backup); suffix++)
                backup = source.resolveSibling(source.getFileName() + ".bak." + suffix);
            Files.copy(source, backup);
            Files.move(temporary, source, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return true;
    }

    private static Node migrateRecipe(Node node) {
        if (!(node instanceof MappingNode original)) return node;
        MappingNode recipe = copy(original);
        boolean changed = rename(recipe, RECIPE_KEYS);
        for (int i = 0; i < recipe.getValue().size(); i++) {
            NodeTuple entry = recipe.getValue().get(i);
            if (!(entry.getKeyNode() instanceof ScalarNode key)) continue;
            Node migrated = ITEM_SECTIONS.contains(key.getValue()) ? migrateItem(entry.getValueNode())
                    : key.getValue().equals("ingredients") ? migrateIngredients(entry.getValueNode()) : entry.getValueNode();
            if (migrated != entry.getValueNode()) {
                recipe.getValue().set(i, new NodeTuple(entry.getKeyNode(), migrated));
                changed = true;
            }
        }
        return changed ? recipe : node;
    }

    private static Node migrateItem(Node node) {
        if (!(node instanceof MappingNode original)) return node;
        MappingNode item = copy(original);
        return rename(item, ITEM_KEYS) ? item : node;
    }

    private static Node migrateIngredients(Node node) {
        boolean changed = false;
        if (node instanceof MappingNode original) {
            MappingNode mapping = copy(original);
            assertUniqueKeys(mapping);
            for (int i = 0; i < mapping.getValue().size(); i++) {
                NodeTuple entry = mapping.getValue().get(i);
                Node migrated = migrateItem(entry.getValueNode());
                if (migrated != entry.getValueNode()) {
                    mapping.getValue().set(i, new NodeTuple(entry.getKeyNode(), migrated));
                    changed = true;
                }
            }
            return changed ? mapping : node;
        }
        if (node instanceof SequenceNode original) {
            List<Node> items = new ArrayList<>();
            for (Node item : original.getValue()) {
                Node migrated = migrateItem(item);
                items.add(migrated);
                changed |= migrated != item;
            }
            if (!changed) return node;
            SequenceNode sequence = new SequenceNode(original.getTag(), items, original.getFlowStyle());
            copyComments(original, sequence);
            return sequence;
        }
        return node;
    }

    // Copy changed containers so aliases in arbitrary item data keep their original keys.
    private static MappingNode copy(MappingNode original) {
        MappingNode copy = new MappingNode(original.getTag(), new ArrayList<>(original.getValue()), original.getFlowStyle());
        copyComments(original, copy);
        return copy;
    }

    private static void copyComments(Node original, Node copy) {
        copy.setBlockComments(original.getBlockComments());
        copy.setInLineComments(original.getInLineComments());
        copy.setEndComments(original.getEndComments());
    }

    private static void assertUniqueKeys(MappingNode mapping) {
        var keys = new HashSet<String>();
        for (NodeTuple entry : mapping.getValue()) {
            if (entry.getKeyNode() instanceof ScalarNode key && !keys.add(key.getValue()))
                throw new IllegalArgumentException("Duplicate recipe key " + key.getValue());
        }
    }

    private static Node value(MappingNode mapping, String name) {
        for (NodeTuple tuple : mapping.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode key && key.getValue().equals(name))
                return tuple.getValueNode();
        }
        return null;
    }

    private static boolean rename(MappingNode mapping, Map<String, String> names) {
        assertUniqueKeys(mapping);
        boolean changed = false;
        List<NodeTuple> entries = mapping.getValue();
        for (int i = 0; i < entries.size(); i++) {
            NodeTuple tuple = entries.get(i);
            if (!(tuple.getKeyNode() instanceof ScalarNode key)) continue;
            String replacement = names.get(key.getValue());
            // An explicit modern key wins; retain conflicting legacy data rather than discard it.
            if (replacement == null || value(mapping, replacement) != null) continue;
            ScalarNode renamed = new ScalarNode(key.getTag(), replacement, key.getStartMark(), key.getEndMark(), key.getScalarStyle());
            copyComments(key, renamed);
            entries.set(i, new NodeTuple(renamed, tuple.getValueNode()));
            changed = true;
        }
        return changed;
    }
}
