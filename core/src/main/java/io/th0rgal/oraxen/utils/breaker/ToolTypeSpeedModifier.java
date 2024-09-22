package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.nms.NMSHandlers;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ToolTypeSpeedModifier {
    public static final ToolTypeSpeedModifier EMPTY = new ToolTypeSpeedModifier(Set.of(Material.AIR), 1f);
    public static final Set<ToolTypeSpeedModifier> VANILLA = new HashSet<>();

    static {
        VANILLA.add(EMPTY);

        NMSHandlers.getHandler();
        Set<Material> itemTools = new HashSet<>();
        itemTools.addAll(Tag.ITEMS_SHOVELS.getValues());
        itemTools.addAll(Tag.ITEMS_SWORDS.getValues());
        itemTools.addAll(Tag.ITEMS_AXES.getValues());
        itemTools.addAll(Tag.ITEMS_PICKAXES.getValues());
        itemTools.addAll(Tag.ITEMS_HOES.getValues());

        VANILLA.add(new ToolTypeSpeedModifier(itemTools.stream().filter(m -> m.toString().startsWith("WOODEN_")).collect(Collectors.toSet()), 2));
        VANILLA.add(new ToolTypeSpeedModifier(itemTools.stream().filter(m -> m.toString().startsWith("STONE_")).collect(Collectors.toSet()), 4));
        VANILLA.add(new ToolTypeSpeedModifier(itemTools.stream().filter(m -> m.toString().startsWith("IRON_")).collect(Collectors.toSet()), 6));
        VANILLA.add(new ToolTypeSpeedModifier(itemTools.stream().filter(m -> m.toString().startsWith("DIAMOND_")).collect(Collectors.toSet()), 8));
        VANILLA.add(new ToolTypeSpeedModifier(itemTools.stream().filter(m -> m.toString().startsWith("NETHERITE_")).collect(Collectors.toSet()), 9));
        VANILLA.add(new ToolTypeSpeedModifier(itemTools.stream().filter(m -> m.toString().startsWith("GOLDEN_")).collect(Collectors.toSet()), 12));

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
    private final Set<CustomBlockMechanic> customBlocks;

    public ToolTypeSpeedModifier(Set<Material> toolType, float speedModifier) {
        this.toolType = toolType;
        this.speedModifier = speedModifier;
        this.materials = new HashSet<>();
        this.customBlocks = new HashSet<>();
    }

    //TODO allow for specifying a ToolTypeSpeedModifier in CustomBlockMechanic Drops bestTools
    /*public ToolTypeSpeedModifier(String bestTool) {
        String tool = StringUtils.substringBefore(bestTool, " ");
        double modifier = ParseUtils.parseDouble(StringUtils.substringAfter(bestTool, " "), 1.0);
        VANILLA.stream().filter(t -> t.toolTypes())
    }*/

    public ToolTypeSpeedModifier(Set<Material> toolType, float speedModifier, Set<Material> materials) {
        this.toolType = toolType;
        this.speedModifier = speedModifier;
        this.materials = materials;
        this.customBlocks = new HashSet<>();
    }

    public ToolTypeSpeedModifier(Set<Material> toolType, float speedModifier, Collection<CustomBlockMechanic> customBlocks) {
        this.toolType = toolType;
        this.speedModifier = speedModifier;
        this.materials = new HashSet<>();
        this.customBlocks = new HashSet<>(customBlocks);
    }

    public Set<Material> toolTypes() {
        return toolType;
    }

    public float speedModifier() {
        return speedModifier;
    }

    public Set<Material> materials() {
        return materials;
    }

    public Set<CustomBlockMechanic> customBlocks() {
        return customBlocks;
    }
}
