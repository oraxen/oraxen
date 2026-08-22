package io.th0rgal.oraxen.sounds;

import io.th0rgal.oraxen.pack.generation.LegacyDatapackCleaner;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;

/**
 * Runtime injector for Minecraft's dynamic jukebox_song registry.
 * <p>
 * Custom jukebox songs are injected into the live NMS registry so changes in
 * sounds.yml can be applied without a server restart or datapack reload.
 */
public final class CustomJukeboxSongRegistry {

    private static final ConcurrentMap<String, String> INJECTED_SIGNATURES = new ConcurrentHashMap<>();
    private static final Set<String> KNOWN_MANAGED_SONG_IDS = ConcurrentHashMap.newKeySet();
    private static final Object RELOAD_LOCK = new Object();

    private CustomJukeboxSongRegistry() {
        throw new IllegalStateException("Utility class");
    }

    public static void trackManagedSongIds(Collection<String> managedSongIds) {
        rememberManagedSongIds(managedSongIds);
    }

    public static void reload(Collection<CustomSound> sounds) {
        List<CustomSound> snapshot = List.copyOf(sounds);
        if (!SchedulerUtil.isGlobalThread()) {
            SchedulerUtil.runTask(() -> reloadNow(snapshot));
            return;
        }
        reloadNow(snapshot);
    }

    private static void reloadNow(Collection<CustomSound> sounds) {
        runWithReloadLock(() -> reloadLocked(sounds));
    }

    static void runWithReloadLock(Runnable action) {
        synchronized (RELOAD_LOCK) {
            action.run();
        }
    }

    private static void reloadLocked(Collection<CustomSound> sounds) {
        Collection<CustomSound> jukeboxSounds = sounds.stream()
                .filter(CustomSound::isJukeboxSound)
                .toList();
        if (!supportsCustomJukeboxSongs()) {
            new JukeboxDatapack(jukeboxSounds).generateAssets(List.of());
            if (!jukeboxSounds.isEmpty()) {
                Logs.logInfo("Generated legacy jukebox datapack for " + jukeboxSounds.size()
                        + " custom jukebox song(s).");
            }
            return;
        }

        clearLegacyDatapack();
        Set<String> managedSongIds = new LinkedHashSet<>(jukeboxSounds.stream()
                .map(CustomSound::getJukeboxSongId)
                .toList());

        try {
            Ref ref = Ref.get();
            Object registry = ref.jukeboxSongRegistry();
            if (!ref.mappedRegistryClass().isInstance(registry)) {
                Logs.logWarning("Could not hot reload custom jukebox songs: jukebox song registry is not mutable.");
                return;
            }

            int injected = 0;
            int updated = 0;
            int unchanged = 0;
            int removed = 0;

            ref.setFrozen(registry, false);
            try {
                removed = ref.removeUnconfiguredSongs(registry, managedSongIds);
                for (CustomSound sound : jukeboxSounds) {
                    String songId = sound.getJukeboxSongId();
                    String signature = signature(sound);
                    String previousSignature = INJECTED_SIGNATURES.get(songId);

                    if (signature.equals(previousSignature)) {
                        unchanged++;
                        continue;
                    }

                    Object location = ref.resourceLocation(songId);
                    Object resourceKey = ref.resourceKey(location);
                    Object song = ref.jukeboxSong(sound);

                    if (ref.containsKey(registry, location)) {
                        ref.replaceSong(registry, resourceKey, song, songId);
                        INJECTED_SIGNATURES.put(songId, signature);
                        updated++;
                        continue;
                    }

                    ref.register(registry, location, song);
                    ref.replaceSong(registry, resourceKey, song, songId);
                    INJECTED_SIGNATURES.put(songId, signature);
                    injected++;
                }
            } finally {
                ref.setFrozen(registry, true);
            }

            rememberManagedSongIds(managedSongIds);
            if (injected > 0 || updated > 0 || removed > 0) {
                Logs.logInfo("Hot reloaded " + injected + " new custom jukebox song(s), " + updated
                        + " updated, " + removed + " removed, " + unchanged + " unchanged.");
            } else {
                Logs.logInfo("Custom jukebox songs already hot reloaded (" + unchanged + " unchanged).");
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Logs.logWarning("Failed to hot reload custom jukebox songs: " + exception.getMessage());
            Logs.debug(exception);
        }
    }

    private static boolean supportsCustomJukeboxSongs() {
        return VersionUtil.atOrAbove("1.21.5");
    }

    private static void clearLegacyDatapack() {
        LegacyDatapackCleaner.clear("oraxen_jukebox");
    }

    private static void rememberManagedSongIds(Collection<String> managedSongIds) {
        KNOWN_MANAGED_SONG_IDS.clear();
        KNOWN_MANAGED_SONG_IDS.addAll(managedSongIds);
    }

    private static String signature(CustomSound sound) {
        return sound.getJukeboxSongId()
                + '|'
                + sound.getSoundId()
                + '|'
                + sound.getLengthInSeconds()
                + '|'
                + sound.getComparatorOutput()
                + '|'
                + sound.getRange()
                + '|'
                + AdventureUtils.GSON_SERIALIZER.serialize(sound.getDescription());
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
        private final Class<?> holderClass;
        private final Class<?> holderReferenceClass;
        private final Class<?> jukeboxSongClass;
        private final Class<?> soundEventClass;
        private final Class<?> componentClass;
        private final Object jukeboxSongRegistryKey;
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
        private final Method registryGetIdMethod;
        private final Method bindValueMethod;
        private final Method toIdRemoveIntMethod;
        private final Method toIdPutMethod;
        private final Method holderDirectMethod;
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
            holderClass = Class.forName("net.minecraft.core.Holder");
            holderReferenceClass = Class.forName("net.minecraft.core.Holder$Reference");
            jukeboxSongClass = Class.forName("net.minecraft.world.item.JukeboxSong");
            soundEventClass = Class.forName("net.minecraft.sounds.SoundEvent");
            componentClass = Class.forName("net.minecraft.network.chat.Component");

            Class<?> registriesClass = Class.forName("net.minecraft.core.registries.Registries");
            jukeboxSongRegistryKey = registriesClass.getField("JUKEBOX_SONG").get(null);

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
            registryGetIdMethod = registryMethod("getId", Object.class);
            bindValueMethod = holderReferenceClass.getDeclaredMethod("bindValue", Object.class);
            bindValueMethod.setAccessible(true);

            Class<?> toIdClass = toIdField.getType();
            toIdRemoveIntMethod = toIdClass.getMethod("removeInt", Object.class);
            toIdPutMethod = toIdClass.getMethod("put", Object.class, int.class);
            holderDirectMethod = holderClass.getMethod("direct", Object.class);
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

        private static Class<?> optionalClass(String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
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

            private void invoke(Object registry, Object location, Object resourceKey, Object value, Object registrationInfo) throws ReflectiveOperationException {
                if (!staticMethod && passesRegistry) {
                    method.invoke(registry, resourceKey, value, registrationInfo);
                    return;
                }

                if (passesRegistry) {
                    method.invoke(null, registry, location, value);
                    return;
                }

                if (staticMethod) {
                    method.invoke(null, location, value);
                } else {
                    method.invoke(registry, location, value);
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

        private Object jukeboxSongRegistry() throws ReflectiveOperationException {
            Object minecraftServer = getServerMethod.invoke(craftServerClass.cast(Bukkit.getServer()));
            Object registryAccess = registryAccessMethod.invoke(minecraftServer);
            return lookupOrThrowMethod.invoke(registryAccess, jukeboxSongRegistryKey);
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
            return resourceKeyCreateMethod.invoke(null, jukeboxSongRegistryKey, location);
        }

        private Object jukeboxSong(CustomSound sound) throws ReflectiveOperationException {
            Object soundEvent = soundEvent(sound);
            Object soundEventValue = soundEvent;
            Object soundEventHolder = holderDirectMethod.invoke(null, soundEvent);
            Object description = asVanillaComponent(sound.getDescription());
            float length = sound.getLengthInSeconds();
            int comparatorOutput = sound.getComparatorOutput();

            for (Constructor<?> constructor : jukeboxSongClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 4) continue;
                if (!parameterTypes[1].isAssignableFrom(componentClass)) continue;
                if (parameterTypes[2] == float.class && parameterTypes[3] == int.class) {
                    if (parameterTypes[0].isAssignableFrom(holderClass))
                        return constructor.newInstance(soundEventHolder, description, length, comparatorOutput);
                    if (parameterTypes[0].isAssignableFrom(soundEventClass))
                        return constructor.newInstance(soundEventValue, description, length, comparatorOutput);
                }
                if (parameterTypes[2] == int.class && parameterTypes[3] == int.class) {
                    if (parameterTypes[0].isAssignableFrom(holderClass))
                        return constructor.newInstance(soundEventHolder, description, sound.getLengthInSeconds(), comparatorOutput);
                    if (parameterTypes[0].isAssignableFrom(soundEventClass))
                        return constructor.newInstance(soundEventValue, description, sound.getLengthInSeconds(), comparatorOutput);
                }
            }

            throw new NoSuchMethodException("Unsupported JukeboxSong constructor");
        }

        private Object soundEvent(CustomSound sound) throws ReflectiveOperationException {
            Object location = resourceLocation(sound.getSoundId());
            Float range = sound.getRange();
            if (range != null) {
                try {
                    return soundEventClass.getMethod("createFixedRangeEvent", resourceLocationClass, float.class)
                            .invoke(null, location, range);
                } catch (NoSuchMethodException ignored) {
                    // Fall back to variable range below.
                }
            }

            try {
                return soundEventClass.getMethod("createVariableRangeEvent", resourceLocationClass)
                        .invoke(null, location);
            } catch (NoSuchMethodException ignored) {
                for (Constructor<?> constructor : soundEventClass.getConstructors()) {
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    if (parameterTypes.length == 2
                            && parameterTypes[0].isAssignableFrom(resourceLocationClass)) {
                        Object optionalRange = range != null ? Optional.of(range) : Optional.empty();
                        return constructor.newInstance(location, optionalRange);
                    }
                    if (parameterTypes.length == 1
                            && parameterTypes[0].isAssignableFrom(resourceLocationClass)) {
                        return constructor.newInstance(location);
                    }
                }
                throw new NoSuchMethodException("Unsupported SoundEvent constructor");
            }
        }

        private Object asVanillaComponent(net.kyori.adventure.text.Component component) throws ReflectiveOperationException {
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
            String legacyText = AdventureUtils.LEGACY_SERIALIZER.serialize(component);
            return componentClass.getMethod("literal", String.class).invoke(null, legacyText);
        }

        private boolean containsKey(Object registry, Object location) throws ReflectiveOperationException {
            return (boolean) containsKeyMethod.invoke(registry, location);
        }

        private void register(Object registry, Object location, Object song) throws ReflectiveOperationException {
            Object resourceKey = registryRegisterMethod.acceptsResourceKey() ? resourceKey(location) : null;
            registryRegisterMethod.invoke(registry, location, resourceKey, song, builtInRegistrationInfo);
        }

        @SuppressWarnings("unchecked")
        private int removeUnconfiguredSongs(Object registry, Collection<String> configuredSongIds) throws ReflectiveOperationException {
            Set<String> configuredIds = new HashSet<>(configuredSongIds);
            Set<String> staleIds = new LinkedHashSet<>(INJECTED_SIGNATURES.keySet());
            staleIds.addAll(KNOWN_MANAGED_SONG_IDS);
            staleIds.removeAll(configuredIds);

            int removed = 0;
            for (String songId : staleIds) {
                Object location = resourceLocation(songId);
                Object resourceKey = resourceKey(location);
                Optional<?> holderOptional = findHolder(registry, resourceKey);

                if (holderOptional.isPresent()) {
                    Object holder = holderOptional.get();
                    Object song = null;
                    int id = -1;

                    try {
                        song = value(holder);
                        id = (int) registryGetIdMethod.invoke(registry, song);
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // Missing IDs or values still need their key/location maps cleaned below.
                    }

                    if (byKeyField != null) ((Map<Object, Object>) byKeyField.get(registry)).remove(resourceKey);
                    if (byLocationField != null) ((Map<Object, Object>) byLocationField.get(registry)).remove(location);
                    if (registrationInfosField != null) ((Map<Object, Object>) registrationInfosField.get(registry)).remove(resourceKey);

                    if (song != null) {
                        ((Map<Object, Object>) byValueField.get(registry)).remove(song);
                        toIdRemoveIntMethod.invoke(toIdField.get(registry), song);
                    }

                    if (id >= 0 && byIdField != null) {
                        Object byId = byIdField.get(registry);
                        byId.getClass().getMethod("set", int.class, Object.class).invoke(byId, id, null);
                    }

                    removed++;
                }

                INJECTED_SIGNATURES.remove(songId);
            }

            return removed;
        }

        @SuppressWarnings("unchecked")
        private void replaceSong(Object registry, Object resourceKey, Object newSong, String songId) throws ReflectiveOperationException {
            Optional<?> holderOptional = findHolder(registry, resourceKey);
            if (holderOptional.isEmpty()) {
                throw new IllegalStateException("Missing holder for custom jukebox song '" + songId + "'");
            }

            Object holder = holderOptional.get();
            Object oldSong = null;
            int id = -1;

            try {
                oldSong = value(holder);
                id = (int) registryGetIdMethod.invoke(registry, oldSong);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Holder may exist without a bound value immediately after registration.
            }

            bindValueMethod.invoke(holder, newSong);

            Map<Object, Object> byValue = (Map<Object, Object>) byValueField.get(registry);
            if (oldSong != null) byValue.remove(oldSong);
            byValue.put(newSong, holder);

            Object toIdObject = toIdField.get(registry);
            if (id >= 0 && oldSong != null) {
                toIdRemoveIntMethod.invoke(toIdObject, oldSong);
                toIdPutMethod.invoke(toIdObject, newSong, id);
            }
        }

        private Optional<?> findHolder(Object registry, Object resourceKey) throws ReflectiveOperationException {
            return (Optional<?>) registryGetResourceKeyMethod.invoke(registry, resourceKey);
        }

        private Object value(Object holder) throws ReflectiveOperationException {
            return holderReferenceClass.getMethod("value").invoke(holder);
        }

        private void setFrozen(Object registry, boolean frozen) throws ReflectiveOperationException {
            frozenField.set(registry, frozen);
        }
    }
}
