package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockBreaking {

    private final List<Rule> rules;

    public BlockBreaking(ConfigurationSection section, String sourceID) {
        List<?> breakingRules = section.getList("breaking");
        if ((breakingRules == null || breakingRules.isEmpty()) && (section.contains("hardness") || section.contains("drop"))) {
            Logs.logWarning("Block mechanic " + sourceID + " uses legacy 'hardness'/'drop' keys; please migrate them to a 'breaking' rule.");
            this.rules = parseLegacyRule(section, sourceID);
        } else {
            this.rules = parseRules(breakingRules, sourceID);
        }
    }

    public boolean hasHardness(ItemStack tool) {
        Rule rule = ruleFor(tool);
        return rule != null && rule.hardness() > 0.0D;
    }

    public double hardness(ItemStack tool) {
        Rule rule = ruleFor(tool);
        return rule != null ? rule.hardness() : 1.0D;
    }

    public Drop drop(ItemStack tool) {
        Rule rule = ruleFor(tool);
        return rule != null ? rule.drop() : Drop.emptyDrop();
    }

    @Nullable
    public DurabilityAction durabilityAction(ItemStack tool) {
        Rule rule = ruleFor(tool);
        return rule != null ? rule.durabilityAction() : null;
    }

    public double attributeSpeedMultiplier(ItemStack tool, Material blockType) {
        double nativeSpeed = nativeToolSpeed(tool, blockType);
        return configuredToolSpeed(tool, blockType) / nativeSpeed;
    }

    public double packetSpeedMultiplier(ItemStack tool, Material blockType) {
        return configuredToolSpeed(tool, blockType);
    }

    private double configuredToolSpeed(ItemStack tool, Material blockType) {
        Rule rule = ruleFor(tool);
        return rule == null || rule.fallback() ? 1.0D : tierToolSpeed(tool, blockType);
    }

    @Nullable
    private Rule ruleFor(ItemStack tool) {
        for (Rule rule : rules) {
            if (!rule.fallback() && rule.matches(tool)) return rule;
        }

        for (Rule rule : rules) {
            if (rule.fallback()) return rule;
        }

        return null;
    }

    private List<Rule> parseRules(List<?> ruleConfigs, String sourceID) {
        if (ruleConfigs == null || ruleConfigs.isEmpty())
            return List.of();

        List<Rule> parsedRules = new ArrayList<>();
        boolean seenFallback = false;
        for (Object entry : ruleConfigs) {
            if (!(entry instanceof Map<?, ?> map)) continue;

            boolean fallback = map.containsKey("else");
            boolean hasMatchers = map.containsKey("when");
            if (!fallback && !hasMatchers) {
                Logs.logWarning("Block mechanic " + sourceID + " has a breaking rule without 'when' or 'else'; the rule will never match.");
            }
            if (fallback && seenFallback) {
                Logs.logWarning("Block mechanic " + sourceID + " has multiple 'else' breaking rules; only the first 'else' rule will be used.");
            }
            if (fallback && map.containsKey("when")) {
                Logs.logWarning("Block mechanic " + sourceID + " has a breaking rule with both 'when' and 'else' keys; 'when' matchers will be ignored and the rule will act as a catch-all fallback.");
            }
            List<ToolMatcher> matchers = parseMatchers(map.get("when"), sourceID);
            if (hasMatchers && !fallback && matchers.isEmpty()) {
                Logs.logWarning("Block mechanic " + sourceID + " has a 'when' breaking rule with no valid tool entries; the rule will never match.");
            }
            if (!map.containsKey("hardness") && !map.containsKey("drops") && !map.containsKey("durability")) {
                Logs.logWarning("Block mechanic " + sourceID + " has a breaking rule without 'hardness', 'drops', or 'durability'; using default hardness 1.0 and no drops.");
            }
            double hardness = parseDouble(map.get("hardness"), 1.0D, sourceID);
            Drop drop = parseDrop(map.get("drops"), sourceID);
            DurabilityAction durabilityAction = parseDurabilityAction(map.get("durability"), sourceID);
            parsedRules.add(new Rule(matchers, fallback, hardness, drop, durabilityAction));
            if (fallback) seenFallback = true;
        }

        return List.copyOf(parsedRules);
    }

    private List<Rule> parseLegacyRule(ConfigurationSection section, String sourceID) {
        Drop drop = Drop.emptyDrop();
        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        BlockMechanicFactory factory = BlockMechanicFactory.getInstance();
        if (dropSection != null && factory != null) {
            drop = Drop.createDrop(factory.toolTypes, dropSection, sourceID);
        } else if (dropSection != null) {
            Logs.logWarning("Block mechanic " + sourceID + " has a legacy 'drop' section, but block tool types are unavailable; no drops will be configured.");
        }

        return List.of(new Rule(List.of(), true, parseDouble(section.get("hardness"), 1.0D, sourceID), drop, null));
    }

    @Nullable
    private DurabilityAction parseDurabilityAction(Object value, String sourceID) {
        if (value == null) return null;
        if (!(value instanceof Map<?, ?> map)) {
            Logs.logWarning("Invalid durability action in block mechanic " + sourceID + "; expected 'add'/'remove' map.");
            return null;
        }

        int add = parseNonNegativeInt(map.get("add"), 0, "durability.add", sourceID);
        int remove = parseNonNegativeInt(map.get("remove"), 0, "durability.remove", sourceID);
        return new DurabilityAction(add, remove);
    }

    private int parseNonNegativeInt(Object value, int fallback, String key, String sourceID) {
        if (value == null) return fallback;
        try {
            return Math.max(0, Integer.parseInt(value.toString()));
        } catch (NumberFormatException exception) {
            Logs.logWarning("Invalid " + key + " value '" + value + "' in block mechanic " + sourceID + "; using " + fallback + " instead.");
            Logs.debug(exception);
            return fallback;
        }
    }

    private List<ToolMatcher> parseMatchers(Object value, String sourceID) {
        if (value instanceof List<?> values) {
            List<ToolMatcher> matchers = new ArrayList<>();
            for (Object entry : values) {
                ToolMatcher matcher = parseMatcher(entry, sourceID);
                if (matcher != null) matchers.add(matcher);
            }
            if (matchers.isEmpty() && !values.isEmpty()) {
                Logs.logWarning("Block mechanic " + sourceID + " has a 'when' rule with no valid tool entries; the rule will never match.");
            }
            return matchers;
        }

        ToolMatcher matcher = parseMatcher(value, sourceID);
        return matcher == null ? List.of() : List.of(matcher);
    }

    @Nullable
    private ToolMatcher parseMatcher(Object value, String sourceID) {
        if (value == null) return null;

        String key = value.toString().trim();
        if (key.isEmpty()) return null;
        if (key.startsWith("#")) {
            NamespacedKey namespacedKey = namespacedKey(key.substring(1));
            if (namespacedKey == null) {
                logInvalidMatcher(key, sourceID);
                return null;
            }

            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, namespacedKey, Material.class);
            if (tag == null) {
                logInvalidItemTagMatcher(key, namespacedKey, sourceID);
                return null;
            }

            return tool -> tool != null && tag.isTagged(tool.getType());
        }

        Material material = Material.matchMaterial(stripMinecraftNamespace(key));
        if (material == null) {
            logInvalidMatcher(key, sourceID);
            return null;
        }

        return tool -> tool != null && tool.getType() == material;
    }

    private void logInvalidMatcher(String key, String sourceID) {
        Logs.logWarning("Invalid breaking.when entry '" + key + "' in block mechanic " + sourceID);
    }

    private void logInvalidItemTagMatcher(String key, NamespacedKey namespacedKey, String sourceID) {
        String path = namespacedKey.getKey();
        if (path.startsWith("mineable/") || path.startsWith("needs_")) {
            Logs.logWarning("Invalid breaking.when entry '" + key + "' in block mechanic " + sourceID + "; this is a block tag. Use item-registry tool tags instead (e.g. '#minecraft:axes' not '#minecraft:mineable/axe', '#minecraft:pickaxes' not '#minecraft:mineable/pickaxe').");
            return;
        }
        Logs.logWarning("Invalid breaking.when entry '" + key + "' in block mechanic " + sourceID + "; # tags must be item-registry tags (e.g. '#minecraft:axes' not '#minecraft:mineable/axe', '#minecraft:pickaxes' not '#minecraft:mineable/pickaxe'), not block-mining or tier tags (e.g. #minecraft:mineable/pickaxe, #minecraft:needs_stone_tool).");
    }

    private NamespacedKey namespacedKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) return NamespacedKey.fromString(normalized);
        return NamespacedKey.fromString("minecraft:" + normalized);
    }

    private String stripMinecraftNamespace(String key) {
        return key.toLowerCase(Locale.ROOT).startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }

    private Drop parseDrop(Object value, String sourceID) {
        if (!(value instanceof List<?> dropConfigs)) return Drop.emptyDrop();

        List<Loot> loots = new ArrayList<>();
        for (Object entry : dropConfigs) {
            if (!(entry instanceof Map<?, ?> map)) continue;
            LinkedHashMap<String, Object> safeMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> mapEntry : map.entrySet()) {
                safeMap.put(String.valueOf(mapEntry.getKey()), mapEntry.getValue());
            }
            loots.add(new Loot(safeMap, sourceID));
        }

        return Drop.emptyDrop(loots);
    }

    private double parseDouble(Object value, double fallback, String sourceID) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException exception) {
            Logs.logWarning("Invalid hardness value '" + value + "' in block mechanic " + sourceID + "; using " + fallback + " instead.");
            Logs.debug(exception);
            return fallback;
        }
    }

    public static double tierToolSpeed(final ItemStack tool, final Material blockType) {
        if (tool == null) return 1.0D;

        final Material toolType = tool.getType();
        final String toolName = toolType.name();

        double speed;
        if (toolType == Material.SHEARS) {
            if (blockType == Material.COBWEB || Tag.LEAVES.isTagged(blockType)) return 15.0D;
            if (Tag.WOOL.isTagged(blockType)) return 5.0D;
            speed = 2.0D;
        } else if (toolName.endsWith("_SWORD")) {
            if (blockType == Material.COBWEB || blockType.name().contains("BAMBOO")) return 15.0D;
            speed = 1.5D;
        } else if (toolName.startsWith("GOLDEN_")) speed = 12.0D;
        else if (toolName.startsWith("NETHERITE_")) speed = 9.0D;
        else if (toolName.startsWith("DIAMOND_")) speed = 8.0D;
        else if (toolName.startsWith("IRON_")) speed = 6.0D;
        else if (toolName.startsWith("STONE_")) speed = 4.0D;
        else if (toolName.startsWith("WOODEN_")) speed = 2.0D;
        else speed = 1.0D;

        return speed + efficiencySpeedBonus(tool);
    }

    private static double efficiencySpeedBonus(ItemStack tool) {
        int level = tool.getEnchantmentLevel(EnchantmentWrapper.EFFICIENCY);
        return level > 0 ? level * level + 1.0D : 0.0D;
    }

    private static double nativeToolSpeed(final ItemStack tool, final Material blockType) {
        if (tool == null) return 1.0D;

        final Material toolType = tool.getType();
        final String toolName = toolType.name();

        if (usesNativeSpeed(toolType, toolName, blockType)) return tierToolSpeed(tool, blockType);

        return 1.0D;
    }

    private static boolean usesNativeSpeed(Material toolType, String toolName, Material blockType) {
        if (toolType == Material.SHEARS) return true;
        if (toolName.endsWith("_SWORD")) return true;

        final String mineableTag = mineableTagName(toolName);
        return mineableTag != null && isMineable(blockType, mineableTag);
    }

    @Nullable
    private static String mineableTagName(String toolName) {
        if (toolName.endsWith("_PICKAXE")) return "mineable/pickaxe";
        if (toolName.endsWith("_AXE")) return "mineable/axe";
        if (toolName.endsWith("_SHOVEL")) return "mineable/shovel";
        if (toolName.endsWith("_HOE")) return "mineable/hoe";
        return null;
    }

    private static Tag<Material> mineableTag(String tagName) {
        return Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
    }

    private static boolean isMineable(Material blockType, String tagName) {
        Tag<Material> tag = mineableTag(tagName);
        return tag != null && tag.isTagged(blockType);
    }

    public record DurabilityAction(int add, int remove) {
    }

    private record Rule(List<ToolMatcher> matchers, boolean fallback, double hardness, Drop drop,
                        @Nullable DurabilityAction durabilityAction) {
        private boolean matches(ItemStack tool) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(tool));
        }
    }

    @FunctionalInterface
    private interface ToolMatcher {
        boolean matches(ItemStack tool);
    }
}
