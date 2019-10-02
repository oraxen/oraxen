package io.th0rgal.oraxen.recipes;

import org.bukkit.configuration.ConfigurationSection;

public abstract class RecipeLoader {

    private ConfigurationSection section;

    public RecipeLoader(ConfigurationSection section) {
        this.section = section;
    }

    protected ConfigurationSection getSection() {
        return section;
    }

    protected String getRecipeName() {
        return section.getName();
    }

    public abstract void registerRecipe();

}
