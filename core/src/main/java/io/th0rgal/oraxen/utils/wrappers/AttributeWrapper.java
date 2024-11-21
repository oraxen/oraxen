package io.th0rgal.oraxen.utils.wrappers;

import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class AttributeWrapper {

    public static final Attribute MAX_HEALTH = VersionUtil.atOrAbove("1.21.2") ? Attribute.MAX_HEALTH : Registry.ATTRIBUTE.get(NamespacedKey.fromString("generic.max_health"));

    @Nullable
    public static Attribute fromString(@NotNull String attribute) {
        return Registry.ATTRIBUTE.get(NamespacedKey.fromString(attribute.toLowerCase(Locale.ENGLISH)));
    }
}
