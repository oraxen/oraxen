package io.th0rgal.oraxen.recipes.builders;

import io.th0rgal.oraxen.utils.ItemUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class WorkstationBuilder extends RecipeBuilder {

    private final String valueKey;
    private int value;

    protected WorkstationBuilder(Player player, String builderName, String valueKey) {
        super(player, builderName);
        this.valueKey = valueKey;
    }

    @Override
    public void saveRecipe(String name) {
        saveRecipe(name, null);
    }

    @Override
    public List<String> getMissingIngredients() {
        ItemStack[] content = getInventory().getContents();
        // The addition slot is optional for anvil and grindstone recipes
        List<String> missing = new ArrayList<>();
        if (ItemUtils.isEmpty(content[0])) missing.add("base");
        if (ItemUtils.isEmpty(content[2])) missing.add("result");
        return missing;
    }

    @Override
    public void saveRecipe(String name, String permission) {
        List<String> missing = getMissingIngredients();
        if (!missing.isEmpty())
            throw new IllegalStateException("Cannot save recipe '" + name + "', missing: " + String.join(", ", missing));
        ItemStack[] content = getInventory().getContents();
        ConfigurationSection newCraftSection = getConfig().createSection(name);

        setSerializedItem(newCraftSection.createSection("base"), content[0]);
        if (!ItemUtils.isEmpty(content[1]))
            setSerializedItem(newCraftSection.createSection("addition"), content[1]);
        setSerializedItem(newCraftSection.createSection("result"), content[2]);
        newCraftSection.set(valueKey, value);

        if (permission != null && !permission.isEmpty())
            newCraftSection.set("permission", permission);

        saveConfig();
        close();
    }

    protected void setValue(int value) {
        this.value = Math.max(0, value);
    }
}
