package io.th0rgal.oraxen.utils.wrappers;

import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class AttributeWrapper {

    public static final Attribute ARMOR = VersionUtil.atOrAbove("1.21.2") ? Attribute.ARMOR : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.armor"));
    public static final Attribute ARMOR_TOUGHNESS = VersionUtil.atOrAbove("1.21.2") ? Attribute.ARMOR_TOUGHNESS : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.armor_toughness"));
    public static final Attribute ATTACK_DAMAGE = VersionUtil.atOrAbove("1.21.2") ? Attribute.ATTACK_DAMAGE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.attack_damage"));
    public static final Attribute ATTACK_KNOCKBACK = VersionUtil.atOrAbove("1.21.2") ? Attribute.ATTACK_KNOCKBACK : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.attack_knockback"));
    public static final Attribute ATTACK_SPEED = VersionUtil.atOrAbove("1.21.2") ? Attribute.ATTACK_SPEED : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.attack_speed"));
    public static final Attribute BLOCK_BREAK_SPEED = VersionUtil.atOrAbove("1.21.2") ? Attribute.BLOCK_BREAK_SPEED : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.block_break_speed"));
    public static final Attribute BLOCK_INTERACTION_RANGE = VersionUtil.atOrAbove("1.21.2") ? Attribute.BLOCK_INTERACTION_RANGE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.block_interaction_range"));
    public static final Attribute BURNING_TIME = VersionUtil.atOrAbove("1.21.2") ? Attribute.BURNING_TIME : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.burning_time"));
    public static final Attribute ENTITY_INTERACTION_RANGE = VersionUtil.atOrAbove("1.21.2") ? Attribute.ENTITY_INTERACTION_RANGE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.entity_interaction_range"));
    public static final Attribute EXPLOSION_KNOCKBACK_RESISTANCE = VersionUtil.atOrAbove("1.21.2") ? Attribute.EXPLOSION_KNOCKBACK_RESISTANCE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.explosion_knockback_resistance"));
    public static final Attribute FALL_DAMAGE_MULTIPLIER = VersionUtil.atOrAbove("1.21.2") ? Attribute.FALL_DAMAGE_MULTIPLIER : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.fall_damage_multiplier"));
    public static final Attribute FLYING_SPEED = VersionUtil.atOrAbove("1.21.2") ? Attribute.FLYING_SPEED : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.flying_speed"));
    public static final Attribute FOLLOW_RANGE = VersionUtil.atOrAbove("1.21.2") ? Attribute.FOLLOW_RANGE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.follow_range"));
    public static final Attribute GRAVITY = VersionUtil.atOrAbove("1.21.2") ? Attribute.GRAVITY : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.gravity"));
    public static final Attribute JUMP_STRENGTH = VersionUtil.atOrAbove("1.21.2") ? Attribute.JUMP_STRENGTH : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.jump_strength"));
    public static final Attribute KNOCKBACK_RESISTANCE = VersionUtil.atOrAbove("1.21.2") ? Attribute.KNOCKBACK_RESISTANCE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.knockback_resistance"));
    public static final Attribute LUCK = VersionUtil.atOrAbove("1.21.2") ? Attribute.LUCK : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.luck"));
    public static final Attribute MAX_ABSORPTION = VersionUtil.atOrAbove("1.21.2") ? Attribute.MAX_ABSORPTION : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.max_absorption"));
    public static final Attribute MAX_HEALTH = VersionUtil.atOrAbove("1.21.2") ? Attribute.MAX_HEALTH : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.max_health"));
    public static final Attribute MINING_EFFICIENCY = VersionUtil.atOrAbove("1.21.2") ? Attribute.MINING_EFFICIENCY : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.mining_efficiency"));
    public static final Attribute MOVEMENT_EFFICIENCY = VersionUtil.atOrAbove("1.21.2") ? Attribute.MOVEMENT_EFFICIENCY : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.movement_efficiency"));
    public static final Attribute MOVEMENT_SPEED = VersionUtil.atOrAbove("1.21.2") ? Attribute.MOVEMENT_SPEED : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.movement_speed"));
    public static final Attribute OXYGEN_BONUS = VersionUtil.atOrAbove("1.21.2") ? Attribute.OXYGEN_BONUS : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.oxygen_bonus"));
    public static final Attribute SAFE_FALL_DISTANCE = VersionUtil.atOrAbove("1.21.2") ? Attribute.SAFE_FALL_DISTANCE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.safe_fall_distance"));
    public static final Attribute SCALE = VersionUtil.atOrAbove("1.21.2") ? Attribute.SCALE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.scale"));
    public static final Attribute SNEAKING_SPEED = VersionUtil.atOrAbove("1.21.2") ? Attribute.SNEAKING_SPEED : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.sneaking_speed"));
    public static final Attribute SPAWN_REINFORCEMENTS = VersionUtil.atOrAbove("1.21.2") ? Attribute.SPAWN_REINFORCEMENTS : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.spawn_reinforcements"));
    public static final Attribute STEP_HEIGHT = VersionUtil.atOrAbove("1.21.2") ? Attribute.STEP_HEIGHT : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.step_height"));
    public static final Attribute SUBMERGED_MINING_SPEED = VersionUtil.atOrAbove("1.21.2") ? Attribute.SUBMERGED_MINING_SPEED : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.submerged_mining_speed"));
    public static final Attribute SWEEPING_DAMAGE_RATIO = VersionUtil.atOrAbove("1.21.2") ? Attribute.SWEEPING_DAMAGE_RATIO : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.sweeping_damage_ratio"));
    public static final Attribute TEMPT_RANGE = VersionUtil.atOrAbove("1.21.2") ? Attribute.TEMPT_RANGE : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.tempt_range"));

    @Nullable
    public static Attribute fromString(@NotNull String attribute) {
        String attributeName = attribute.replace("GENERIC_", "").toLowerCase(Locale.ENGLISH);

        return VersionUtil.atOrAbove("1.21.2") ? Registry.ATTRIBUTE.get(NamespacedKey.fromString(attributeName)) : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic." + attributeName));
    }
}
