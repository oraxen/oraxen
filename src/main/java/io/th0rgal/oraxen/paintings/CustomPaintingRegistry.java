package io.th0rgal.oraxen.paintings;

import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.bukkit.Bukkit;

/**
 * Runtime injector for Minecraft's dynamic painting_variant registry.
 * <p>
 * Custom paintings are NMS-only and injected into the live registry so changes
 * in paintings.yml can be applied without restarting.
 */
public final class CustomPaintingRegistry {

    private static final String TAG = "CustomPaintings";
    private static final ConcurrentMap<String, String> INJECTED_SIGNATURES = new ConcurrentHashMap<>();
    private static final Set<String> KNOWN_MANAGED_VARIANT_IDS = ConcurrentHashMap.newKeySet();

    private CustomPaintingRegistry() {
        throw new IllegalStateException("Utility class");
    }

    public static void reload(Collection<CustomPainting> paintings) {
        clearLegacyDatapack();
        if (!supportsCustomPaintings()) {
            if (!paintings.isEmpty()) {
                Logs.logWarning("Custom paintings require Paper 1.21.5 or newer. Skipping " + paintings.size() + " custom painting(s).");
            }
            return;
        }
        List<String> managedVariantIds = paintings.stream().map(painting -> painting.variantKey().asString()).toList();

        try {
            Ref ref = Ref.get();
            Object registry = ref.paintingRegistry();
            if (!ref.mappedRegistryClass().isInstance(registry)) {
                Logs.logWarning("Could not hot reload custom paintings: painting registry is not mutable.");
                return;
            }

            int injected = 0;
            int updated = 0;
            int unchanged = 0;
            int removed = 0;

            ref.setFrozen(registry, false);
            try {
                removed = ref.removeUnconfiguredVariants(registry, managedVariantIds);
                for (CustomPainting painting : paintings) {
                    String variantId = painting.variantKey().asString();
                    String signature = signature(painting);
                    String previousSignature = INJECTED_SIGNATURES.get(variantId);

                    if (signature.equals(previousSignature)) {
                        unchanged++;
                        continue;
                    }

                    Object location = ref.resourceLocation(variantId);
                    Object resourceKey = ref.resourceKey(location);
                    Object variant = ref.paintingVariant(painting);

                    if (ref.containsKey(registry, location)) {
                        ref.replaceVariant(registry, resourceKey, variant, variantId);
                        INJECTED_SIGNATURES.put(variantId, signature);
                        updated++;
                        continue;
                    }

                    ref.register(registry, location, variant);
                    ref.replaceVariant(registry, resourceKey, variant, variantId);
                    INJECTED_SIGNATURES.put(variantId, signature);
                    injected++;
                }
            } finally {
                ref.setFrozen(registry, true);
            }

            int tagChanges = updatePlaceableRegistryTag(
                    managedVariantIds,
                    paintings.stream().filter(CustomPainting::includeInRandom)
                            .map(painting -> painting.variantKey().asString()).toList()
            );

            if (injected > 0 || updated > 0 || removed > 0 || tagChanges > 0) {
                Logs.logInfo("Hot reloaded " + injected + " new custom painting(s), " + updated
                        + " updated, " + removed + " removed, " + unchanged + " unchanged, "
                        + tagChanges + " random-placement change(s).");
            } else {
                Logs.logInfo("Custom paintings already hot reloaded (" + unchanged + " unchanged).");
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Logs.logWarning("Failed to hot reload custom paintings: " + exception.getMessage());
            Logs.debug(exception);
        }
    }

    private static boolean supportsCustomPaintings() {
        return VersionUtil.atOrAbove("1.21.5") && VersionUtil.isPaperServer();
    }

    private static void clearLegacyDatapack() {
        try {
            PaintingDatapack paintingDatapack = new PaintingDatapack(List.of());
            paintingDatapack.clearOldDataPack();
            paintingDatapack.generateAssets(List.of());
        } catch (RuntimeException exception) {
            Logs.debug(exception);
        }
    }

    private static int updatePlaceableRegistryTag(Collection<String> managedVariantIds, Collection<String> randomVariantIds) {
        try {
            Ref ref = Ref.get();
            Object registry = ref.paintingRegistry();
            if (!ref.mappedRegistryClass().isInstance(registry)) return 0;

            Set<String> managedIds = new HashSet<>(managedVariantIds);
            managedIds.addAll(KNOWN_MANAGED_VARIANT_IDS);
            Set<String> randomIds = new HashSet<>(randomVariantIds);
            Set<String> before = ref.currentManagedPlaceableIds(registry, managedIds);

            LinkedHashMap<String, Object> placeableById = new LinkedHashMap<>();
            ref.placeableHolders(registry).forEach(holder -> {
                String id = ref.holderId(holder);
                if (id == null) return;
                if (managedIds.contains(id) && !randomIds.contains(id)) return;

                placeableById.putIfAbsent(id, holder);
            });

            for (String variantId : randomIds) {
                ref.findHolder(registry, variantId).ifPresent(holder -> placeableById.putIfAbsent(variantId, holder));
            }

            Set<String> after = new LinkedHashSet<>();
            for (String variantId : placeableById.keySet()) {
                if (managedIds.contains(variantId)) after.add(variantId);
            }

            if (before.equals(after)) {
                rememberManagedVariantIds(managedVariantIds);
                return 0;
            }

            ref.setFrozen(registry, false);
            try {
                ref.bindPlaceableTag(registry, new ArrayList<>(placeableById.values()));
            } finally {
                ref.setFrozen(registry, true);
            }

            rememberManagedVariantIds(managedVariantIds);
            return changedCount(before, after);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Logs.logWarning("Failed to update placeable custom painting tag: " + exception.getMessage());
            Logs.debug(exception);
            return 0;
        }
    }

    private static void rememberManagedVariantIds(Collection<String> managedVariantIds) {
        KNOWN_MANAGED_VARIANT_IDS.clear();
        KNOWN_MANAGED_VARIANT_IDS.addAll(managedVariantIds);
    }

    private static int changedCount(Set<String> before, Set<String> after) {
        int changed = 0;
        for (String id : before) {
            if (!after.contains(id)) changed++;
        }
        for (String id : after) {
            if (!before.contains(id)) changed++;
        }
        return changed;
    }

    private static String signature(CustomPainting painting) {
        return painting.variantKey().asString()
                + '|'
                + painting.width()
                + '|'
                + painting.height()
                + '|'
                + painting.assetId().asString()
                + '|'
                + nullToEmpty(painting.title())
                + '|'
                + nullToEmpty(painting.author());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class Ref {
        private static volatile Ref instance;

        private final Class<?> craftServerClass;
        private final Class<?> idMapClass;
        private final Class<?> registryClass;
        private final Class<?> mappedRegistryClass;
        private final Class<?> registryAccessClass;
        private final Class<?> resourceLocationClass;
        private final Class<?> resourceKeyClass;
        private final Class<?> registrationInfoClass;
        private final Class<?> holderReferenceClass;
        private final Class<?> tagKeyClass;
        private final Class<?> paintingVariantClass;
        private final Object paintingVariantRegistryKey;
        private final Object placeableTagKey;
        private final Field frozenField;
        private final Field byKeyField;
        private final Field byLocationField;
        private final Field byValueField;
        private final Field byIdField;
        private final Field registrationInfosField;
        private final Field toIdField;
        private final Method getServerMethod;
        private final Method registryAccessMethod;
        private final Method lookupOrThrowMethod;
        private final Method resourceKeyCreateMethod;
        private final RegistryRegisterMethod registryRegisterMethod;
        private final Method containsKeyMethod;
        private final Method registryGetResourceKeyMethod;
        private final Method registryGetTagMethod;
        private final Method registryGetIdMethod;
        private final Method bindValueMethod;
        private final Method bindTagMethod;
        private final Method bindTagsMethod;
        private final Method registryGetTagsMethod;
        private final Method unwrapKeyMethod;
        private final Method resourceKeyLocationMethod;
        private final Method toIdRemoveIntMethod;
        private final Method toIdPutMethod;
        private final Object builtInRegistrationInfo;

        private Ref() throws ReflectiveOperationException {
            craftServerClass = Class.forName("org.bukkit.craftbukkit.CraftServer");
            idMapClass = Class.forName("net.minecraft.core.IdMap");
            registryClass = Class.forName("net.minecraft.core.Registry");
            mappedRegistryClass = Class.forName("net.minecraft.core.MappedRegistry");
            registryAccessClass = Class.forName("net.minecraft.core.RegistryAccess");
            resourceLocationClass = resourceLocationClass();
            resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
            registrationInfoClass = optionalClass("net.minecraft.core.RegistrationInfo");
            holderReferenceClass = Class.forName("net.minecraft.core.Holder$Reference");
            tagKeyClass = Class.forName("net.minecraft.tags.TagKey");
            paintingVariantClass = paintingVariantClass();

            Class<?> registriesClass = Class.forName("net.minecraft.core.registries.Registries");
            paintingVariantRegistryKey = registriesClass.getField("PAINTING_VARIANT").get(null);
            placeableTagKey = Class.forName("net.minecraft.tags.PaintingVariantTags").getField("PLACEABLE").get(null);

            frozenField = mappedRegistryClass.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            byKeyField = mappedRegistryField("byKey");
            byLocationField = mappedRegistryField("byLocation");
            byValueField = mappedRegistryClass.getDeclaredField("byValue");
            byValueField.setAccessible(true);
            byIdField = mappedRegistryField("byId");
            registrationInfosField = mappedRegistryField("registrationInfos");
            toIdField = mappedRegistryClass.getDeclaredField("toId");
            toIdField.setAccessible(true);

            getServerMethod = craftServerClass.getMethod("getServer");
            registryAccessMethod = getServerMethod.getReturnType().getMethod("registryAccess");
            lookupOrThrowMethod = registryAccessClass.getMethod("lookupOrThrow", resourceKeyClass);
            resourceKeyCreateMethod = resourceKeyClass.getMethod("create", resourceKeyClass, resourceLocationClass);
            registryRegisterMethod = registryRegisterMethod();
            containsKeyMethod = registryMethod("containsKey", resourceLocationClass);
            registryGetResourceKeyMethod = registryMethod("get", resourceKeyClass);
            registryGetTagMethod = registryMethod("get", tagKeyClass);
            registryGetIdMethod = registryMethod("getId", Object.class);
            bindValueMethod = holderReferenceClass.getDeclaredMethod("bindValue", Object.class);
            bindValueMethod.setAccessible(true);
            bindTagMethod = optionalMethod(mappedRegistryClass, "bindTag", tagKeyClass, List.class);
            bindTagsMethod = optionalMethod(mappedRegistryClass, "bindTags", Map.class);
            Method getTagsMethod = optionalMethod(mappedRegistryClass, "getTags");
            registryGetTagsMethod = getTagsMethod != null ? getTagsMethod : optionalMethod(registryClass, "getTags");
            unwrapKeyMethod = Class.forName("net.minecraft.core.Holder").getMethod("unwrapKey");
            resourceKeyLocationMethod = resourceKeyLocationMethod();

            Class<?> toIdClass = toIdField.getType();
            toIdRemoveIntMethod = toIdClass.getMethod("removeInt", Object.class);
            toIdPutMethod = toIdClass.getMethod("put", Object.class, int.class);
            builtInRegistrationInfo = registrationInfoClass != null
                    ? registrationInfoClass.getField("BUILT_IN").get(null)
                    : null;
        }

        private static Ref get() throws ReflectiveOperationException {
            Ref local = instance;
            if (local != null) return local;

            synchronized (Ref.class) {
                local = instance;
                if (local == null) {
                    local = new Ref();
                    instance = local;
                }
                return local;
            }
        }

        private static Class<?> resourceLocationClass() throws ClassNotFoundException {
            try {
                return Class.forName("net.minecraft.resources.ResourceLocation");
            } catch (ClassNotFoundException ignored) {
                return Class.forName("net.minecraft.resources.Identifier");
            }
        }

        private static Class<?> paintingVariantClass() throws ClassNotFoundException {
            try {
                return Class.forName("net.minecraft.world.entity.decoration.painting.PaintingVariant");
            } catch (ClassNotFoundException ignored) {
                return Class.forName("net.minecraft.world.entity.decoration.PaintingVariant");
            }
        }

        private static Class<?> optionalClass(String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
                return null;
            }
        }

        private static Method optionalMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
            try {
                return owner.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }

        private Method registryMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
            try {
                return mappedRegistryClass.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                return registryClass.getMethod(name, parameterTypes);
            }
        }

        private record RegistryRegisterMethod(Method method, boolean staticMethod, boolean passesRegistry) {

            private static RegistryRegisterMethod of(Method method) {
                boolean staticMethod = Modifier.isStatic(method.getModifiers());
                boolean passesRegistry = method.getParameterCount() == 3;
                return new RegistryRegisterMethod(method, staticMethod, passesRegistry);
            }

            private boolean acceptsResourceKey() {
                return method.getParameterCount() > 0
                        && method.getParameterTypes()[staticMethod && passesRegistry ? 1 : 0].getName()
                        .equals("net.minecraft.resources.ResourceKey");
            }

            private void invoke(Object registry, Object location, Object resourceKey, Object variant, Object registrationInfo) throws ReflectiveOperationException {
                if (!staticMethod && passesRegistry) {
                    method.invoke(registry, resourceKey, variant, registrationInfo);
                    return;
                }

                if (passesRegistry) {
                    method.invoke(null, registry, location, variant);
                    return;
                }

                if (staticMethod) {
                    method.invoke(null, location, variant);
                } else {
                    method.invoke(registry, location, variant);
                }
            }
        }

        private RegistryRegisterMethod registryRegisterMethod() throws NoSuchMethodException {
            if (registrationInfoClass != null) {
                try {
                    return RegistryRegisterMethod.of(mappedRegistryClass.getMethod("register", resourceKeyClass, Object.class, registrationInfoClass));
                } catch (NoSuchMethodException ignored) {
                    // Try static Registry register fallbacks below.
                }
            }

            try {
                return RegistryRegisterMethod.of(registryClass.getMethod("register", registryClass, resourceLocationClass, Object.class));
            } catch (NoSuchMethodException ignored) {
                try {
                    return RegistryRegisterMethod.of(registryClass.getMethod("register", idMapClass, resourceLocationClass, Object.class));
                } catch (NoSuchMethodException ignoredAgain) {
                    return RegistryRegisterMethod.of(idMapClass.getMethod("register", resourceLocationClass, Object.class));
                }
            }
        }

        private Method resourceKeyLocationMethod() throws NoSuchMethodException {
            try {
                return resourceKeyClass.getMethod("location");
            } catch (NoSuchMethodException ignored) {
                return resourceKeyClass.getMethod("identifier");
            }
        }

        private Field mappedRegistryField(String name) {
            try {
                Field field = mappedRegistryClass.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                return null;
            }
        }

        private Class<?> mappedRegistryClass() {
            return mappedRegistryClass;
        }

        private Object paintingRegistry() throws ReflectiveOperationException {
            Object minecraftServer = getServerMethod.invoke(craftServerClass.cast(Bukkit.getServer()));
            Object registryAccess = registryAccessMethod.invoke(minecraftServer);
            return lookupOrThrowMethod.invoke(registryAccess, paintingVariantRegistryKey);
        }

        private Object resourceLocation(String id) throws ReflectiveOperationException {
            try {
                Method tryParse = resourceLocationClass.getMethod("tryParse", String.class);
                Object parsed = tryParse.invoke(null, id);
                if (parsed != null) return parsed;
            } catch (NoSuchMethodException ignored) {
                // Try newer factory names below.
            }

            try {
                return resourceLocationClass.getMethod("parse", String.class).invoke(null, id);
            } catch (NoSuchMethodException ignored) {
                int sep = id.indexOf(':');
                if (sep <= 0 || sep == id.length() - 1)
                    throw new ReflectiveOperationException("Invalid resource location: " + id);
                return resourceLocationClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                        .invoke(null, id.substring(0, sep), id.substring(sep + 1));
            }
        }

        private Object resourceKey(Object location) throws ReflectiveOperationException {
            return resourceKeyCreateMethod.invoke(null, paintingVariantRegistryKey, location);
        }

        private Object paintingVariant(CustomPainting painting) throws ReflectiveOperationException {
            Object assetId = resourceLocation(painting.assetId().asString());
            Optional<?> title = optionalComponent(painting.title());
            Optional<?> author = optionalComponent(painting.author());

            for (Constructor<?> constructor : paintingVariantClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 5
                        && parameterTypes[0] == int.class
                        && parameterTypes[1] == int.class
                        && parameterTypes[2].isAssignableFrom(resourceLocationClass)) {
                    return constructor.newInstance(painting.width(), painting.height(), assetId, title, author);
                }
            }

            for (Constructor<?> constructor : paintingVariantClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 3
                        && parameterTypes[0] == int.class
                        && parameterTypes[1] == int.class
                        && parameterTypes[2].isAssignableFrom(resourceLocationClass)) {
                    return constructor.newInstance(painting.width(), painting.height(), assetId);
                }
            }

            Constructor<?> constructor = paintingVariantClass.getConstructor(int.class, int.class);
            return constructor.newInstance(painting.width(), painting.height());
        }

        private Optional<?> optionalComponent(String value) throws ReflectiveOperationException {
            if (value == null || value.isBlank()) return Optional.empty();

            net.kyori.adventure.text.Component adventureComponent = AdventureUtils.MINI_MESSAGE.deserialize(value);
            Object vanillaComponent = asVanillaComponent(adventureComponent);
            if (vanillaComponent != null) return Optional.of(vanillaComponent);

            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            String legacyText = AdventureUtils.LEGACY_SERIALIZER.serialize(adventureComponent);
            return Optional.of(componentClass.getMethod("literal", String.class).invoke(null, legacyText));
        }

        private Object asVanillaComponent(net.kyori.adventure.text.Component component) {
            try {
                Class<?> paperAdventureClass = Class.forName("io.papermc.paper.adventure.PaperAdventure");
                for (Method method : paperAdventureClass.getDeclaredMethods()) {
                    if (!method.getName().equals("asVanilla") || method.getParameterCount() != 1) continue;
                    if (!Modifier.isStatic(method.getModifiers())) continue;
                    if (!method.getParameterTypes()[0].isAssignableFrom(net.kyori.adventure.text.Component.class)) continue;

                    method.setAccessible(true);
                    return method.invoke(null, component);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fall back to a legacy-formatted literal vanilla component.
            }
            return null;
        }

        private boolean containsKey(Object registry, Object location) throws ReflectiveOperationException {
            return (boolean) containsKeyMethod.invoke(registry, location);
        }

        private void register(Object registry, Object location, Object variant) throws ReflectiveOperationException {
            Object resourceKey = registryRegisterMethod.acceptsResourceKey() ? resourceKey(location) : null;
            registryRegisterMethod.invoke(registry, location, resourceKey, variant, builtInRegistrationInfo);
        }

        @SuppressWarnings("unchecked")
        private int removeUnconfiguredVariants(Object registry, Collection<String> configuredVariantIds) throws ReflectiveOperationException {
            Set<String> configuredIds = new HashSet<>(configuredVariantIds);
            Set<String> staleIds = new LinkedHashSet<>(INJECTED_SIGNATURES.keySet());
            staleIds.addAll(KNOWN_MANAGED_VARIANT_IDS);
            staleIds.removeAll(configuredIds);

            int removed = 0;
            for (String variantId : staleIds) {
                Object location = resourceLocation(variantId);
                Object resourceKey = resourceKey(location);
                Optional<?> holderOptional = findHolder(registry, resourceKey);

                if (holderOptional.isPresent()) {
                    Object holder = holderOptional.get();
                    Object variant = null;
                    int id = -1;

                    try {
                        variant = value(holder);
                        id = (int) registryGetIdMethod.invoke(registry, variant);
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // Missing IDs or values still need their key/location maps cleaned below.
                    }

                    if (byKeyField != null) ((Map<Object, Object>) byKeyField.get(registry)).remove(resourceKey);
                    if (byLocationField != null) ((Map<Object, Object>) byLocationField.get(registry)).remove(location);
                    if (registrationInfosField != null) ((Map<Object, Object>) registrationInfosField.get(registry)).remove(resourceKey);

                    if (variant != null) {
                        ((Map<Object, Object>) byValueField.get(registry)).remove(variant);
                        toIdRemoveIntMethod.invoke(toIdField.get(registry), variant);
                    }

                    if (id >= 0 && byIdField != null) {
                        Object byId = byIdField.get(registry);
                        byId.getClass().getMethod("set", int.class, Object.class).invoke(byId, id, null);
                    }

                    removed++;
                }

                INJECTED_SIGNATURES.remove(variantId);
            }

            return removed;
        }

        @SuppressWarnings("unchecked")
        private void replaceVariant(Object registry, Object resourceKey, Object newVariant, String variantId) throws ReflectiveOperationException {
            Optional<?> holderOptional = findHolder(registry, resourceKey);
            if (holderOptional.isEmpty()) {
                throw new IllegalStateException("Missing holder for custom painting variant '" + variantId + "'");
            }

            Object holder = holderOptional.get();
            Object oldVariant = null;
            int id = -1;

            try {
                oldVariant = value(holder);
                id = (int) registryGetIdMethod.invoke(registry, oldVariant);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Holder may exist without a bound value immediately after registration.
            }

            bindValueMethod.invoke(holder, newVariant);

            Map<Object, Object> byValue = (Map<Object, Object>) byValueField.get(registry);
            if (oldVariant != null) byValue.remove(oldVariant);
            byValue.put(newVariant, holder);

            Object toIdObject = toIdField.get(registry);
            if (id >= 0 && oldVariant != null) {
                toIdRemoveIntMethod.invoke(toIdObject, oldVariant);
                toIdPutMethod.invoke(toIdObject, newVariant, id);
            }
        }

        private Optional<?> findHolder(Object registry, String variantId) throws ReflectiveOperationException {
            return findHolder(registry, resourceKey(resourceLocation(variantId)));
        }

        private Optional<?> findHolder(Object registry, Object resourceKey) throws ReflectiveOperationException {
            return (Optional<?>) registryGetResourceKeyMethod.invoke(registry, resourceKey);
        }

        private Object value(Object holder) throws ReflectiveOperationException {
            return holderReferenceClass.getMethod("value").invoke(holder);
        }

        private Set<String> currentManagedPlaceableIds(Object registry, Set<String> managedIds) throws ReflectiveOperationException {
            Set<String> current = new LinkedHashSet<>();
            for (Object holder : placeableHolders(registry).toList()) {
                String id = holderId(holder);
                if (id != null && managedIds.contains(id)) current.add(id);
            }
            return current;
        }

        @SuppressWarnings("unchecked")
        private Stream<Object> placeableHolders(Object registry) throws ReflectiveOperationException {
            Optional<?> tag = (Optional<?>) registryGetTagMethod.invoke(registry, placeableTagKey);
            if (tag.isEmpty()) return Stream.empty();
            Object namedHolderSet = tag.get();
            return (Stream<Object>) namedHolderSet.getClass().getMethod("stream").invoke(namedHolderSet);
        }

        private String holderId(Object holder) {
            try {
                Optional<?> keyOptional = (Optional<?>) unwrapKeyMethod.invoke(holder);
                if (keyOptional.isEmpty()) return null;

                Object location = resourceKeyLocationMethod.invoke(keyOptional.get());
                return location.toString();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }

        private void bindPlaceableTag(Object registry, List<Object> holders) throws ReflectiveOperationException {
            if (bindTagMethod != null) {
                bindTagMethod.invoke(registry, placeableTagKey, holders);
                return;
            }

            if (bindTagsMethod == null || registryGetTagsMethod == null) {
                throw new NoSuchMethodException("MappedRegistry#bindTag or #bindTags");
            }

            Map<Object, List<Object>> tags = new LinkedHashMap<>();
            Stream<?> namedTags = (Stream<?>) registryGetTagsMethod.invoke(registry);
            namedTags.forEach(named -> {
                try {
                    Object tagKey = named.getClass().getMethod("key").invoke(named);
                    List<Object> tagHolders = new ArrayList<>();
                    ((Stream<?>) named.getClass().getMethod("stream").invoke(named)).forEach(tagHolders::add);
                    tags.put(tagKey, tagHolders);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            tags.put(placeableTagKey, holders);
            bindTagsMethod.invoke(registry, tags);
        }

        private void setFrozen(Object registry, boolean frozen) throws ReflectiveOperationException {
            frozenField.set(registry, frozen);
        }
    }
}
