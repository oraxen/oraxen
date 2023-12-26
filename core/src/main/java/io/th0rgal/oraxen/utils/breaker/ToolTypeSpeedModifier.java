package io.th0rgal.oraxen.utils.breaker;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ToolTypeSpeedModifier {
    public static final Set<ToolTypeSpeedModifier> VANILLA = new HashSet<>();

    static {
        VANILLA.add(new ToolTypeSpeedModifier(Set.of(Material.AIR), 1f));
        VANILLA.add(new ToolTypeSpeedModifier(Tag.ITEMS_TOOLS.getValues().stream().filter(m -> m.toString().startsWith("WOODEN_")).collect(Collectors.toSet()), 2));
        VANILLA.add(new ToolTypeSpeedModifier(Tag.ITEMS_TOOLS.getValues().stream().filter(m -> m.toString().startsWith("STONE_")).collect(Collectors.toSet()), 4));
        VANILLA.add(new ToolTypeSpeedModifier(Tag.ITEMS_TOOLS.getValues().stream().filter(m -> m.toString().startsWith("IRON_")).collect(Collectors.toSet()), 6));
        VANILLA.add(new ToolTypeSpeedModifier(Tag.ITEMS_TOOLS.getValues().stream().filter(m -> m.toString().startsWith("DIAMOND_")).collect(Collectors.toSet()), 8));
        VANILLA.add(new ToolTypeSpeedModifier(Tag.ITEMS_TOOLS.getValues().stream().filter(m -> m.toString().startsWith("NETHERITE_")).collect(Collectors.toSet()), 9));
        VANILLA.add(new ToolTypeSpeedModifier(Tag.ITEMS_TOOLS.getValues().stream().filter(m -> m.toString().startsWith("GOLDEN_")).collect(Collectors.toSet()), 12));

        VANILLA.add(new ToolTypeSpeedModifier(Set.of(Material.SHEARS), 15, Tag.LEAVES.getValues()));
        VANILLA.add(new ToolTypeSpeedModifier(Set.of(Material.SHEARS), 15, Set.of(Material.COBWEB)));
        VANILLA.add(new ToolTypeSpeedModifier(Set.of(Material.SHEARS), 5, Tag.WOOL.getValues()));
        VANILLA.add(new ToolTypeSpeedModifier(Set.of(Material.SHEARS), 2));

        VANILLA.add(new ToolTypeSpeedModifier(Tag.ITEMS_SWORDS.getValues(), 15, Tag.SWORD_EFFICIENT.getValues()));
        VANILLA.add(new ToolTypeSpeedModifier(Tag.ITEMS_SWORDS.getValues(), 1.5F));
    }

    private final Set<Material> toolType;
    private final float speedModifier;
    private final Set<Material> materials;

    public ToolTypeSpeedModifier(Set<Material> toolType, float speedModifier) {
        this.toolType = toolType;
        this.speedModifier = speedModifier;
        this.materials = new HashSet<>();
    }

    public ToolTypeSpeedModifier(Set<Material> toolType, float speedModifier, Set<Material> materials) {
        this.toolType = toolType;
        this.speedModifier = speedModifier;
        this.materials = materials;
    }

    public Set<Material> getToolType() {
        return toolType;
    }

    public float getSpeedModifier() {
        return speedModifier;
    }

    public Set<Material> getMaterials() {
        return materials;
    }
}
