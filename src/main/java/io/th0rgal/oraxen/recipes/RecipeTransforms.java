package io.th0rgal.oraxen.recipes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.lang.reflect.Method;
import java.util.*;

public final class RecipeTransforms {
    private static final Set<String> LEGACY_COMPONENTS = Set.of("enchantments", "stored_enchantments", "damage", "repair_cost", "custom_name", "lore", "unbreakable");
    private static final Set<String> PROTECTED = Set.of("custom_data", "custom_model_data", "item_model");

    private RecipeTransforms() {}

    public static Transform parse(ConfigurationSection recipe) {
        ConfigurationSection section = recipe.getConfigurationSection("transform");
        if (section == null) {
            if (recipe.contains("transform")) throw new IllegalArgumentException("transform must be a section");
            return null;
        }
        if (!section.isList("keep-components"))
            throw new IllegalArgumentException("transform.keep-components must be a list");
        return keeping(section.getStringList("keep-components"));
    }

    public static Transform keeping(List<String> names) {
        List<Component> components = new ArrayList<>();
        for (String configured : names) {
            String name = configured.startsWith("minecraft:") ? configured.substring(10) : configured;
            name = name.replace('-', '_');
            if (!name.matches("[a-z_]+") || PROTECTED.contains(name))
                throw new IllegalArgumentException("Unsupported or protected transform component " + configured);
            components.add(resolve(name));
        }
        return new Transform(List.copyOf(components));
    }

    public static Transform parseSmithing(ConfigurationSection recipe) {
        if (recipe.contains("transform") && !"base".equals(recipe.getString("transform.source", "base")))
            throw new IllegalArgumentException("Smithing transform source must be base");
        return parse(recipe);
    }

    private static Component resolve(String name) {
        if (LEGACY_COMPONENTS.contains(name)) return new Component(name, null, null, null);
        try {
            Class<?> types = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            Object type = types.getField(name.toUpperCase(Locale.ROOT)).get(null);
            Class<?> valued = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            if (!valued.isInstance(type)) {
                Class<?> nonValued = Class.forName("io.papermc.paper.datacomponent.DataComponentType$NonValued");
                Class<?> component = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
                return new Component(name, type, ItemStack.class.getMethod("hasData", component),
                        ItemStack.class.getMethod("setData", nonValued));
            }
            Method get = ItemStack.class.getMethod("getData", valued);
            Method set = Arrays.stream(ItemStack.class.getMethods())
                    .filter(method -> method.getName().equals("setData") && method.getParameterCount() == 2
                            && method.getParameterTypes()[0] == valued && method.getParameterTypes()[1] == Object.class)
                    .findFirst().orElseThrow(NoSuchMethodException::new);
            return new Component(name, type, get, set);
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalArgumentException("Transform component " + name + " is unsupported on this server", e);
        }
    }

    public static final class Transform {
        private final List<Component> components;

        private Transform(List<Component> components) {
            this.components = components;
        }

        public ItemStack apply(ItemStack source, ItemStack result) {
            if (source == null || result == null) throw new IllegalArgumentException("Missing transform source or result");
            ItemStack output = result.clone();
            for (Component component : components) {
                try {
                    if (component.type == null) {
                        copyLegacy(component.name, source, output);
                        continue;
                    }
                    Object value = component.get.invoke(source, component.type);
                    if (component.set.getParameterCount() == 1) {
                        if (Boolean.TRUE.equals(value)) component.set.invoke(output, component.type);
                    } else if (value != null) component.set.invoke(output, component.type, value);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot copy transform component " + component.name, e);
                }
            }
            return output;
        }
    }

    private static void copyLegacy(String name, ItemStack source, ItemStack output) {
        ItemMeta from = source.getItemMeta(), to = output.getItemMeta();
        if (from == null || to == null) throw new IllegalArgumentException("Transform requires item metadata");
        switch (name) {
            case "enchantments" -> {
                to.getEnchants().keySet().forEach(to::removeEnchant);
                from.getEnchants().forEach((enchantment, level) -> to.addEnchant(enchantment, level, true));
            }
            case "stored_enchantments" -> {
                if (!(from instanceof EnchantmentStorageMeta storedFrom) || !(to instanceof EnchantmentStorageMeta storedTo))
                    throw new IllegalArgumentException("Stored enchantments require enchanted books");
                storedTo.getStoredEnchants().keySet().forEach(storedTo::removeStoredEnchant);
                storedFrom.getStoredEnchants().forEach((enchantment, level) -> storedTo.addStoredEnchant(enchantment, level, true));
            }
            case "damage" -> {
                if (!(from instanceof Damageable damagedFrom) || !(to instanceof Damageable damagedTo))
                    throw new IllegalArgumentException("Damage requires damageable items");
                damagedTo.setDamage(damagedFrom.getDamage());
            }
            case "repair_cost" -> {
                if (!(from instanceof Repairable repairedFrom) || !(to instanceof Repairable repairedTo))
                    throw new IllegalArgumentException("Repair cost requires repairable items");
                repairedTo.setRepairCost(repairedFrom.getRepairCost());
            }
            case "custom_name" -> to.displayName(from.displayName());
            case "lore" -> to.lore(from.lore());
            case "unbreakable" -> to.setUnbreakable(from.isUnbreakable());
            default -> throw new IllegalArgumentException("Unsupported transform component " + name);
        }
        output.setItemMeta(to);
    }

    private record Component(String name, Object type, Method get, Method set) {}
}
