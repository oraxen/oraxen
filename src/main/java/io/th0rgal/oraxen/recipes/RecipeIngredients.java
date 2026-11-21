package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import net.Indyuce.mmoitems.MMOItems;
import io.th0rgal.oraxen.utils.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

/** Shared ingredient parsing and matching for crafting and workstation recipes. */
public final class RecipeIngredients {
    private static volatile Map<String, RecipeChoice> tags = Map.of();

    private RecipeIngredients() {
    }

    /** Validates a complete tag set without changing the active set. */
    public static Map<String, RecipeChoice> prepare(ConfigurationSection section) {
        Map<String, Object> definitions = section == null ? Map.of() : section.getValues(false);
        Map<String, RecipeChoice> resolved = new LinkedHashMap<>();
        Map<String, RecipeChoice> workbench = new LinkedHashMap<>();
        for (String name : definitions.keySet()) {
            resolveTag(name, definitions, resolved, new HashSet<>(), false);
            resolveTag(name, definitions, workbench, new HashSet<>(), true);
        }
        resolved.replaceAll((name, choice) -> new TagChoice(choice, workbench.get(name)));
        return Collections.unmodifiableMap(resolved);
    }

    public static void publish(Map<String, RecipeChoice> prepared) {
        Map<String, RecipeChoice> copy = new LinkedHashMap<>();
        prepared.forEach((key, value) -> copy.put(key, value.clone()));
        tags = Collections.unmodifiableMap(copy);
    }

    public static Map<String, RecipeChoice> tags() {
        Map<String, RecipeChoice> snapshot = new LinkedHashMap<>();
        tags.forEach((key, choice) -> snapshot.put(key, choice.clone()));
        return Collections.unmodifiableMap(snapshot);
    }

    public static Map<String, RecipeChoice> prepareTags(ConfigurationSection section) {
        return prepare(section);
    }

    public static void publishTags(Map<String, RecipeChoice> prepared) {
        publish(prepared);
    }

    public static RecipeChoice parseChoice(Object input, boolean workbench) {
        return choice(input, workbench);
    }

    public static void load(ConfigurationSection section) {
        publish(prepare(section));
    }

    public static RecipeChoice choice(Object input) {
        return choice(input, false);
    }

    public static RecipeChoice choice(Object input, boolean workbench) {
        return parse(input, workbench, name -> {
            RecipeChoice custom = tags.get(name);
            return custom == null ? vanillaTag(name) : custom instanceof TagChoice tag
                    ? tag.choice(workbench).clone() : custom.clone();
        });
    }

    public static int amount(Object input) {
        Map<String, Object> values = values(input);
        Object amount = values.getOrDefault("amount", 1);
        if (!(amount instanceof Number number) || number.doubleValue() != number.intValue() || number.intValue() < 1)
            throw new IllegalArgumentException("Ingredient amount must be a positive integer");
        return number.intValue();
    }

    public static ItemStack result(Object input) {
        if (input == null) return null;
        if (input instanceof List<?>) throw new IllegalArgumentException("Recipe results cannot be alternatives");
        Map<String, Object> values = values(input);
        if (values.containsKey("predicates")) throw new IllegalArgumentException("Recipe results cannot have predicates");
        Object item = values.containsKey("item") ? values.get("item") : input;
        ItemStack result;
        if (item instanceof String text) {
            if (text.startsWith("#")) throw new IllegalArgumentException("Recipe results cannot be tags");
            result = shorthand(text);
        } else result = explicit(values(item));
        if (result != null) {
            result = result.clone();
            result.setAmount(amount(input));
        }
        return result;
    }

    private static RecipeChoice parse(Object input, boolean workbench, java.util.function.Function<String, RecipeChoice> tagResolver) {
        if (input == null) return null;
        if (input instanceof List<?> alternatives) {
            if (alternatives.isEmpty()) throw new IllegalArgumentException("Ingredient alternatives cannot be empty");
            List<RecipeChoice> choices = new ArrayList<>();
            for (Object alternative : alternatives) {
                RecipeChoice choice = parse(alternative, workbench, tagResolver);
                if (choice == null) throw new IllegalArgumentException("Invalid ingredient alternative");
                choices.add(choice);
            }
            return combine(choices);
        }
        if (input instanceof String text) {
            if (text.startsWith("#")) return tagResolver.apply(text.substring(1));
            ItemStack item = shorthand(text);
            if (text.startsWith("oraxen:")) {
                String id = text.substring(7);
                return new MatchingChoice(List.of(item), candidate -> id.equals(OraxenItems.getIdByItem(candidate)));
            }
            return new MatchingChoice(List.of(item), candidate -> candidate.getType() == item.getType()
                    && OraxenItems.getIdByItem(candidate) == null);
        }
        Map<String, Object> values = values(input);
        amount(input);
        RecipeChoice base;
        if (values.containsKey("item")) base = parse(values.get("item"), workbench, tagResolver);
        else if (selector(values, "minecraft-type") != null) {
            Material material = material(selector(values, "minecraft-type").toString());
            base = new RecipeChoice.MaterialChoice(material);
        } else if (selector(values, "minecraft-tag") != null || values.containsKey("tag")) {
            Object tag = values.containsKey("tag") ? values.get("tag") : selector(values, "minecraft-tag");
            base = tagResolver.apply(tag.toString().replaceFirst("^#", ""));
        } else {
            ItemStack item = explicit(values);
            if (item == null || item.getType().isAir()) return null;
            Object oraxen = selector(values, "oraxen-item");
            if (workbench && oraxen != null && hasBackpackMechanic(oraxen.toString()))
                base = new MatchingChoice(List.of(item), stack -> stack != null
                        && oraxen.toString().equals(OraxenItems.getIdByItem(stack)));
            else base = new RecipeChoice.ExactChoice(oraxen == null ? item : ItemUpdater.updateItem(item));
        }
        if (base == null) return null;
        if (!values.containsKey("predicates")) return base;
        Predicate<ItemStack> predicate = predicates(values.get("predicates"), representatives(base));
        RecipeChoice choice = base;
        return new MatchingChoice(representatives(base), item -> choice.test(item) && predicate.test(item));
    }

    private static ItemStack shorthand(String text) {
        if (text.startsWith("oraxen:")) return oraxen(text.substring(7));
        if (text.startsWith("minecraft:")) return new ItemStack(material(text.substring(10)));
        throw new IllegalArgumentException("Item shorthand requires an oraxen: or minecraft: namespace: " + text);
    }

    private static ItemStack explicit(Map<String, Object> values) {
        Object id;
        if ((id = selector(values, "oraxen-item")) != null) return oraxen(id.toString());
        if ((id = selector(values, "crucible-item")) != null) return new WrappedCrucibleItem(id.toString()).build();
        if ((id = selector(values, "mmoitems-id")) != null) {
            if (!PluginUtils.isEnabled("MMOItems")) return null;
            return MMOItems.plugin.getItem(Objects.toString(selector(values, "mmoitems-type"), ""), id.toString());
        }
        if ((id = selector(values, "ecoitem-id")) != null) return new WrappedEcoItem(id.toString()).build();
        if ((id = selector(values, "minecraft-type")) != null) return new ItemStack(material(id.toString()));
        if ((id = selector(values, "minecraft-item")) instanceof ItemStack item) return item.clone();
        if (id != null) {
            MemoryConfiguration section = new MemoryConfiguration();
            values.forEach(section::set);
            return section.getItemStack(values.containsKey("minecraft-item") ? "minecraft-item" : "minecraft_item");
        }
        if (values.isEmpty()) return null;
        throw new IllegalArgumentException("Missing or unsupported item selector: " + values.keySet());
    }

    private static boolean hasBackpackMechanic(String id) {
        MechanicFactory factory = MechanicsManager.getMechanicFactory("backpack");
        return factory != null && factory.getMechanic(id) != null;
    }

    private static Object selector(Map<String, Object> values, String key) {
        return values.containsKey(key) ? values.get(key) : values.get(key.replace('-', '_'));
    }

    private static ItemStack oraxen(String id) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null) throw new IllegalArgumentException("Unknown Oraxen item: " + id);
        return builder.build();
    }

    private static Material material(String id) {
        Material material = Material.matchMaterial(id);
        if (material == null || material.isAir() || !material.isItem()) throw new IllegalArgumentException("Invalid item material: " + id);
        return material;
    }

    private static Map<String, Object> values(Object input) {
        if (input instanceof ConfigurationSection section) return section.getValues(false);
        if (input instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(key.toString(), value));
            return result;
        }
        return Map.of();
    }

    private static RecipeChoice resolveTag(String name, Map<String, Object> definitions, Map<String, RecipeChoice> resolved, Set<String> visiting, boolean workbench) {
        if (resolved.containsKey(name)) return resolved.get(name);
        if (!definitions.containsKey(name)) return vanillaTag(name);
        if (!visiting.add(name)) throw new IllegalArgumentException("Cyclic recipe tag: " + name);
        RecipeChoice choice = parse(definitions.get(name), workbench, child -> resolveTag(child, definitions, resolved, visiting, workbench));
        if (choice == null) throw new IllegalArgumentException("Empty recipe tag: " + name);
        visiting.remove(name);
        resolved.put(name, choice);
        return choice;
    }

    private record TagChoice(RecipeChoice exact, RecipeChoice workbench) implements RecipeChoice {
        private RecipeChoice choice(boolean crafting) {
            return crafting ? workbench : exact;
        }

        @Override
        public ItemStack getItemStack() {
            return exact.getItemStack();
        }

        @Override
        public boolean test(ItemStack item) {
            return exact.test(item);
        }

        @Override
        public TagChoice clone() {
            return new TagChoice(exact.clone(), workbench.clone());
        }
    }

    private static RecipeChoice vanillaTag(String name) {
        NamespacedKey key = NamespacedKey.fromString(name);
        if (key == null) throw new IllegalArgumentException("Invalid recipe tag: " + name);
        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
        if (tag == null) tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
        if (tag == null || tag.getValues().isEmpty()) throw new IllegalArgumentException("Unknown or empty recipe tag: " + name);
        return new RecipeChoice.MaterialChoice(tag);
    }

    private static RecipeChoice combine(List<RecipeChoice> choices) {
        List<ItemStack> items = new ArrayList<>();
        choices.forEach(choice -> items.addAll(representatives(choice)));
        return new MatchingChoice(items, item -> choices.stream().anyMatch(choice -> choice.test(item)));
    }

    public static List<ItemStack> representatives(RecipeChoice choice) {
        if (choice instanceof MatchingChoice matching) return matching.getChoices();
        if (choice instanceof RecipeChoice.ExactChoice exact) return exact.getChoices();
        if (choice instanceof RecipeChoice.MaterialChoice materials)
            return materials.getChoices().stream().map(ItemStack::new).toList();
        throw new IllegalArgumentException("Unsupported ingredient choice: " + choice.getClass().getName());
    }

    /** Display slots for choices whose representatives are lost during Bukkit registration. */
    public static Map<Integer, List<ItemStack>> displayOverrides(Recipe recipe) {
        Map<Integer, List<ItemStack>> slots = new LinkedHashMap<>();
        if (recipe instanceof ShapedRecipe shaped) {
            Map<Character, RecipeChoice> choices = shaped.getChoiceMap();
            String[] shape = shaped.getShape();
            int top = shape.length, bottom = -1, left = shape[0].length(), right = -1;
            // Minecraft trims empty rows and columns before building the recipe display.
            for (int row = 0; row < shape.length; row++) {
                for (int column = 0; column < shape[row].length(); column++) {
                    if (choices.get(shape[row].charAt(column)) == null) continue;
                    top = Math.min(top, row);
                    bottom = row;
                    left = Math.min(left, column);
                    right = Math.max(right, column);
                }
            }
            int slot = 0;
            for (int row = top; row <= bottom; row++)
                for (int column = left; column <= right; column++)
                    addDisplay(slots, slot++, choices.get(shape[row].charAt(column)));
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            List<RecipeChoice> choices = shapeless.getChoiceList();
            for (int slot = 0; slot < choices.size(); slot++) addDisplay(slots, slot, choices.get(slot));
        } else if (recipe instanceof SmithingTransformRecipe smithing) {
            addDisplay(slots, 0, smithing.getTemplate());
            addDisplay(slots, 1, smithing.getBase());
            addDisplay(slots, 2, smithing.getAddition());
        }
        return Map.copyOf(slots);
    }

    private static void addDisplay(Map<Integer, List<ItemStack>> slots, int slot, RecipeChoice choice) {
        if (!(choice instanceof MatchingChoice matching)) return;
        List<ItemStack> items = matching.getChoices().stream().distinct().toList();
        if (items.stream().anyMatch(ItemStack::hasItemMeta)) slots.put(slot, items);
    }

    /** Bukkit accepts only its built-in choices. Events must check the original choice. */
    public static RecipeChoice serverChoice(RecipeChoice choice) {
        if (!(choice instanceof MatchingChoice)) return choice.clone();
        List<Material> materials = representatives(choice).stream().map(ItemStack::getType).distinct().toList();
        return new RecipeChoice.MaterialChoice(materials);
    }

    public static RecipeChoice cookingChoice(RecipeChoice choice) {
        if (!(choice instanceof MatchingChoice)) return choice.clone();
        RecipeChoice snapshot = choice.clone();
        try {
            // Native matching runs before the furnace checks its output slot.
            return (RecipeChoice) RecipeChoice.class.getMethod("predicateChoice", Predicate.class, ItemStack.class)
                    .invoke(null, (Predicate<ItemStack>) snapshot::test, snapshot.getItemStack());
        } catch (NoSuchMethodException exception) {
            throw new IllegalArgumentException("Predicate cooking ingredients require Paper's native predicate recipe support", exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot create a native predicate cooking ingredient", exception);
        }
    }

    public static void loadTags(ConfigurationSection section) {
        load(section);
    }

    private static Predicate<ItemStack> predicates(Object input, List<ItemStack> samples) {
        if (input instanceof List<?> entries) {
            if (entries.isEmpty()) throw new IllegalArgumentException("Predicates cannot be empty");
            List<Predicate<ItemStack>> tests = new ArrayList<>();
            for (Object entry : entries) {
                Map<String, Object> definition = values(entry);
                Object type = definition.get("type");
                if ("enchantment".equals(type)) {
                    if (!definition.keySet().equals(Set.of("type", "enchantments")))
                        throw new IllegalArgumentException("Invalid enchantment predicate fields");
                    tests.add(predicates(Map.of("enchantments", definition.get("enchantments")), samples));
                } else if ("exact".equals(type)) {
                    if (!definition.keySet().equals(Set.of("type", "component", "value"))
                            || !(definition.get("component") instanceof String name))
                        throw new IllegalArgumentException("Exact predicates require component and value");
                    tests.add(componentPredicate(name, definition.get("value"), samples.getFirst()));
                } else throw new IllegalArgumentException("Unsupported ingredient predicate type: " + type);
            }
            return item -> tests.stream().allMatch(test -> test.test(item));
        }
        Map<String, Object> predicates = values(input);
        if (predicates.isEmpty()) throw new IllegalArgumentException("Predicates must be a nonempty mapping");
        List<Predicate<ItemStack>> tests = new ArrayList<>();
        for (Map.Entry<String, Object> entry : predicates.entrySet()) {
            switch (entry.getKey()) {
                case "enchantments" -> {
                    Map<String, Object> enchantments = values(entry.getValue());
                    if (enchantments.isEmpty()) throw new IllegalArgumentException("Enchantments must be a nonempty mapping");
                    enchantments.forEach((name, level) -> {
                        NamespacedKey key = NamespacedKey.fromString(name);
                        Enchantment enchantment = key == null ? null : Enchantment.getByKey(key);
                        if (enchantment == null || !(level instanceof Number number) || number.intValue() < 1
                                || number.doubleValue() != number.intValue())
                            throw new IllegalArgumentException("Invalid enchantment predicate: " + name);
                        int minimum = ((Number) level).intValue();
                        tests.add(item -> item.getEnchantmentLevel(enchantment) >= minimum);
                    });
                }
                case "exact", "components" -> {
                    Map<String, Object> components = values(entry.getValue());
                    if (components.isEmpty()) throw new IllegalArgumentException("Exact components must be a nonempty mapping");
                    components.forEach((name, value) -> tests.add(componentPredicate(name, value, samples.getFirst())));
                }
                default -> throw new IllegalArgumentException("Unsupported ingredient predicate: " + entry.getKey());
            }
        }
        return item -> tests.stream().allMatch(test -> test.test(item));
    }

    private static Predicate<ItemStack> componentPredicate(String name, Object value, ItemStack sample) {
        NamespacedKey componentKey = NamespacedKey.fromString(name);
        if (componentKey == null || !componentKey.getNamespace().equals("minecraft"))
            throw new IllegalArgumentException("Invalid component: " + name);
        if (value instanceof String text && text.startsWith("snbt:")) {
            String snbt = text.substring(5).trim();
            if (snbt.matches("[+-]?[0-9]+")) {
                try {
                    value = Integer.valueOf(snbt);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Component integer is out of range: " + name, exception);
                }
            } else if (snbt.equals("true") || snbt.equals("1b")) value = true;
            else if (snbt.equals("false") || snbt.equals("0b")) value = false;
            else throw new IllegalArgumentException("Unsupported SNBT component value for " + name);
        }
        String keyName = componentKey.getKey();
        if (keyName.equals("damage") || keyName.equals("repair_cost")) {
            if (!(value instanceof Number number) || number.intValue() < 0 || number.doubleValue() != number.intValue())
                throw new IllegalArgumentException("Component requires a nonnegative integer: " + name);
            int expected = ((Number) value).intValue();
            return item -> {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                return keyName.equals("damage")
                        ? meta instanceof org.bukkit.inventory.meta.Damageable damageable && damageable.getDamage() == expected
                        : meta instanceof org.bukkit.inventory.meta.Repairable repairable && repairable.getRepairCost() == expected;
            };
        }
        if (keyName.equals("unbreakable") && value instanceof Boolean expected)
            return item -> item.hasItemMeta() && item.getItemMeta().isUnbreakable() == expected;
        try {
            NamespacedKey key = NamespacedKey.fromString(name);
            if (key == null || !key.getNamespace().equals("minecraft")) throw new IllegalArgumentException("Invalid component: " + name);
            Class<?> types = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            Object type = types.getField(key.getKey().toUpperCase(Locale.ROOT)).get(null);
            Class<?> valued = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            Method getData = ItemStack.class.getMethod("getData", valued);
            Object component = value;
            if (value instanceof ConfigurationSection || value instanceof Map<?, ?>) {
                MemoryConfiguration configuration = new MemoryConfiguration();
                values(value).forEach(configuration::set);
                component = configuration;
            } else if (Set.of("damage", "max_damage", "max_stack_size", "repair_cost").contains(key.getKey())) {
                if (!(value instanceof Number number) || number.intValue() < 0 || number.doubleValue() != number.intValue())
                    throw new IllegalArgumentException("Component requires a nonnegative integer: " + name);
                component = ((Number) value).intValue();
            } else if (!key.getKey().equals("enchantment_glint_override") || !(value instanceof Boolean)) {
                throw new IllegalArgumentException("Unsupported scalar component predicate: " + name);
            }
            if (NMSHandlers.getHandler() == null)
                throw new IllegalArgumentException("Exact component predicates require an available NMS handler");
            ItemBuilder builder = new ItemBuilder(sample.clone());
            if (!NMSHandlers.getHandler().setComponent(builder, key.getKey(), component))
                throw new IllegalArgumentException("Unsupported component predicate: " + name);
            Object parsed = builder.getComponent(key.getKey());
            ItemStack expected = NMSHandlers.getHandler().applyGenericComponents(sample.clone(), Map.of(key.getKey(), parsed));
            Object expectedValue = getData.invoke(expected, type);
            if (expectedValue == null) throw new IllegalArgumentException("Cannot read component predicate: " + name);
            return item -> {
                try {
                    return expectedValue.equals(getData.invoke(item, type));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    return false;
                }
            };
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalArgumentException("Exact component predicates are unavailable for " + name, exception);
        }
    }

    /** Full matcher retained separately from Bukkit's material-based registration choice. */
    public static final class MatchingChoice implements RecipeChoice {
        private final List<ItemStack> items;
        private final Predicate<ItemStack> predicate;

        private MatchingChoice(List<ItemStack> items, Predicate<ItemStack> predicate) {
            if (items.isEmpty()) throw new IllegalArgumentException("Ingredient choices cannot be empty");
            this.items = items.stream().map(ItemStack::clone).toList();
            this.predicate = predicate;
        }

        public List<ItemStack> getChoices() {
            return items.stream().map(ItemStack::clone).toList();
        }

        @Override
        public ItemStack getItemStack() {
            return items.getFirst().clone();
        }

        @Override
        public boolean test(ItemStack item) {
            return item != null && !item.getType().isAir() && predicate.test(item);
        }

        @Override
        public MatchingChoice clone() {
            return new MatchingChoice(items, predicate);
        }
    }
}
