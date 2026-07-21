package io.th0rgal.oraxen.mechanics;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.lang.reflect.Proxy;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

public class TestRegistryAccess implements RegistryAccess {

    @Override
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return new TestRegistry<>(type);
    }

    @Override
    @SuppressWarnings({"unchecked", "removal"})
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> key) {
        if (key == RegistryKey.SOUND_EVENT) {
            return new TestRegistry<>((Class<T>) Sound.class);
        }
        if (key == RegistryKey.MOB_EFFECT) {
            return new TestRegistry<>((Class<T>) PotionEffectType.class);
        }
        return new TestRegistry<>(null);
    }

    private static class TestRegistry<T extends Keyed> implements Registry<T> {
        private final Class<T> type;

        private TestRegistry(Class<T> type) {
            this.type = type;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(NamespacedKey key) {
            if (type != null && PotionEffectType.class.isAssignableFrom(type)) {
                return (T) new TestPotionEffectType(key);
            }
            if (type != null && type.isEnum()) {
                Object[] constants = type.getEnumConstants();
                return constants.length == 0 ? null : (T) constants[0];
            }
            Class<?> proxyType = type != null && type.isInterface() ? type : Keyed.class;
            return (T) Proxy.newProxyInstance(proxyType.getClassLoader(), new Class<?>[]{proxyType}, (proxy, method, args) -> switch (method.getName()) {
                case "getKey" -> key;
                case "key" -> key;
                case "name" -> key.getKey().toUpperCase().replace('.', '_');
                case "ordinal", "compareTo" -> 0;
                case "toString" -> key.toString();
                case "hashCode" -> key.hashCode();
                case "equals" -> proxy == args[0];
                default -> null;
            });
        }

        @Override
        public NamespacedKey getKey(T value) {
            return value.getKey();
        }

        @Override
        public boolean hasTag(TagKey<T> key) {
            return false;
        }

        @Override
        public Tag<T> getTag(TagKey<T> key) {
            return null;
        }

        @Override
        public Collection<Tag<T>> getTags() {
            return Collections.emptyList();
        }

        @Override
        public Stream<T> stream() {
            return Stream.empty();
        }

        @Override
        public Stream<NamespacedKey> keyStream() {
            return Stream.empty();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }
    }

    private static class TestPotionEffectType extends PotionEffectType {
        private final NamespacedKey key;

        private TestPotionEffectType(NamespacedKey key) {
            this.key = key;
        }

        @Override
        public PotionEffect createEffect(int duration, int amplifier) {
            return new PotionEffect(this, duration, amplifier);
        }

        @Override
        public boolean isInstant() {
            return false;
        }

        @Override
        public PotionEffectTypeCategory getCategory() {
            return PotionEffectTypeCategory.BENEFICIAL;
        }

        @Override
        public Color getColor() {
            return Color.WHITE;
        }

        @Override
        public double getDurationModifier() {
            return 1;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public String getName() {
            return key.getKey().toUpperCase();
        }

        @Override
        public String getTranslationKey() {
            return key.toString();
        }

        @Override
        public String translationKey() {
            return key.toString();
        }

        @Override
        public java.util.Map<Attribute, AttributeModifier> getEffectAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public double getAttributeModifierAmount(Attribute attribute, int effectAmplifier) {
            return 0;
        }

        @Override
        public Category getEffectCategory() {
            return Category.BENEFICIAL;
        }

        @Override
        public NamespacedKey getKey() {
            return key;
        }
    }
}
