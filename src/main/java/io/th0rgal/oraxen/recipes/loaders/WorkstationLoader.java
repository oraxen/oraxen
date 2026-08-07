package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.CustomWorkstationRecipe;
import io.th0rgal.oraxen.recipes.CustomWorkstationRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;

public class WorkstationLoader extends RecipeLoader {
    private final CustomWorkstationRecipe.Type type;

    public WorkstationLoader(ConfigurationSection section, CustomWorkstationRecipe.Type type) {
        super(section);
        this.type = type;
    }

    @Override
    public void registerRecipe() {
        String baseKey = type == CustomWorkstationRecipe.Type.CAULDRON ? "input" : "base";
        String fluid = type == CustomWorkstationRecipe.Type.CAULDRON ? getSection().getString("fluid", "water").toLowerCase() : null;
        if (fluid != null && !java.util.Set.of("water", "lava", "powder_snow", "empty").contains(fluid))
            throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has invalid cauldron fluid: " + fluid);
        RecipeChoice addition = null;
        if (getSection().isConfigurationSection("addition")) {
            if (type == CustomWorkstationRecipe.Type.CAULDRON)
                throw new IllegalArgumentException("Recipe '" + getRecipeName()
                        + "' cannot define an addition ingredient: cauldron recipes only consume the held input");
            addition = getRequiredChoice("addition");
        }
        int value = switch (type) {
            case ANVIL -> Math.max(0, getSection().getInt("experience_cost", 0));
            case GRINDSTONE -> Math.max(0, getSection().getInt("experience", 0));
            case CAULDRON -> getSection().getInt("level_change", fluid.equals("empty") ? 0 : -1);
        };
        int level = getSection().getInt("level", 1);
        if (type == CustomWorkstationRecipe.Type.CAULDRON) {
            if ((fluid.equals("lava") || fluid.equals("empty")) && level != 1)
                throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' requires cauldron level 1 for " + fluid);
            if (fluid.equals("empty") && value != 0)
                throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' cannot change an empty cauldron level");
            if (fluid.equals("lava") && value != -1 && value != 0)
                throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has unsupported lava level_change: " + value);
            if ((fluid.equals("water") || fluid.equals("powder_snow")) && (level < 1 || level > 3 || level + value > 3))
                throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has an invalid cauldron level transition");
        }
        CustomWorkstationRegistry.register(new CustomWorkstationRecipe(getRecipeName(), type,
                new CustomWorkstationRecipe.Ingredient(getRequiredChoice(baseKey), getIngredientAmount(baseKey)),
                addition == null ? null : new CustomWorkstationRecipe.Ingredient(addition, getIngredientAmount("addition")),
                getValidResult(), value, getPermission(),
                fluid,
                level));
    }
}
