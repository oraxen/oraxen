package io.th0rgal.oraxen;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.PaintingVariantRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.event.RegistryEventProvider;
import io.papermc.paper.registry.event.WritableRegistry;
import io.papermc.paper.registry.tag.TagKey;
import io.th0rgal.oraxen.paintings.CustomPainting;
import io.th0rgal.oraxen.paintings.CustomPaintingRegistry;
import io.th0rgal.oraxen.sounds.CustomJukeboxSongRegistry;
import io.th0rgal.oraxen.sounds.CustomSound;
import io.th0rgal.oraxen.sounds.SoundManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Art;
import org.bukkit.JukeboxSong;
import org.bukkit.block.BlockType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OraxenRegistryBootstrap {

    private static final Key PLACEABLE = Key.key("minecraft", "placeable");
    private static final Key MINEABLE_AXE = Key.key("minecraft", "mineable/axe");
    private static final Key NOTE_BLOCK = Key.key("minecraft", "note_block");

    private OraxenRegistryBootstrap() {
    }

    static void registerPaintings(BootstrapContext context) {
        YamlConfiguration config = OraxenPluginBootstrap.loadConfig(context, "paintings.yml");
        Map<Key, CustomPainting> paintingsByKey = new LinkedHashMap<>();
        for (CustomPainting painting : CustomPainting.fromConfigSection(
                config.getConfigurationSection("paintings"), context.getLogger()::warn)) {
            paintingsByKey.put(painting.variantKey(), painting);
        }
        CustomPaintingRegistry.trackManagedVariantIds(
                paintingsByKey.keySet().stream().map(Key::asString).toList());
        if (paintingsByKey.isEmpty()) return;

        LifecycleEventType<BootstrapContext, ? extends LifecycleEvent, ?> phase = paintingPhase();
        context.getLifecycleManager().registerEventHandler(phase, event -> {
            WritableRegistry<Art, PaintingVariantRegistryEntry.Builder> registry = paintingRegistry(event);
            for (CustomPainting painting : paintingsByKey.values()) {
                TypedKey<Art> key = TypedKey.create(RegistryKey.PAINTING_VARIANT, painting.variantKey());
                try {
                    registry.register(key, builder -> {
                        builder.width(painting.width())
                                .height(painting.height())
                                .assetId(painting.assetId());
                        if (painting.title() != null)
                            builder.title(MiniMessage.miniMessage().deserialize(painting.title()));
                        if (painting.author() != null)
                            builder.author(MiniMessage.miniMessage().deserialize(painting.author()));
                    });
                } catch (RuntimeException exception) {
                    context.getLogger().warn("Could not register painting {}; retaining the existing registry entry",
                            painting.variantKey(), exception);
                }
            }
        });

        List<TypedKey<Art>> configured = paintingsByKey.keySet().stream()
                .map(key -> TypedKey.create(RegistryKey.PAINTING_VARIANT, key))
                .toList();
        List<TypedKey<Art>> placeable = paintingsByKey.values().stream()
                .filter(CustomPainting::includeInRandom)
                .map(painting -> TypedKey.create(RegistryKey.PAINTING_VARIANT, painting.variantKey()))
                .toList();
        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.PAINTING_VARIANT), event -> {
                    TagKey<Art> placeableTag = TagKey.create(RegistryKey.PAINTING_VARIANT, PLACEABLE);
                    List<TypedKey<Art>> values = event.registrar().hasTag(placeableTag)
                            ? new ArrayList<>(event.registrar().getTag(placeableTag))
                            : new ArrayList<>();
                    values.removeAll(configured);
                    values.addAll(placeable);
                    event.registrar().setTag(placeableTag, values);
                });
    }

    static void registerJukeboxSongs(BootstrapContext context) {
        SoundManager soundManager = new SoundManager(OraxenPluginBootstrap.loadSoundsConfig(context),
                context.getLogger()::warn);
        Map<String, CustomSound> soundsBySongId = new LinkedHashMap<>();
        for (CustomSound sound : soundManager.getJukeboxSounds())
            soundsBySongId.put(sound.getJukeboxSongId(), sound);
        CustomJukeboxSongRegistry.trackManagedSongIds(soundsBySongId.keySet());
        if (soundsBySongId.isEmpty()) return;

        context.getLifecycleManager().registerEventHandler(RegistryEvents.JUKEBOX_SONG.compose(), event -> {
            for (CustomSound sound : soundsBySongId.values()) {
                TypedKey<JukeboxSong> key = TypedKey.create(RegistryKey.JUKEBOX_SONG,
                        Key.key(sound.getJukeboxSongId()));
                try {
                    event.registry().register(key, builder -> builder
                            .soundEvent(factory -> factory.empty()
                                    .location(Key.key(sound.getSoundId()))
                                    .fixedRange(sound.getRange()))
                            .description(MiniMessage.miniMessage().deserialize(sound.getDescriptionText()))
                            .lengthInSeconds(sound.getLengthInSeconds())
                            .comparatorOutput(sound.getComparatorOutput()));
                } catch (RuntimeException exception) {
                    context.getLogger().warn("Could not register jukebox song {}; retaining the existing registry entry",
                            sound.getJukeboxSongId(), exception);
                }
            }
        });
    }

    static void registerBlockTagEdits(BootstrapContext context) {
        if (!OraxenPluginBootstrap.shouldRemoveNoteBlockMineableTag(
                OraxenPluginBootstrap.loadConfig(context, "mechanics.yml"))) return;

        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.BLOCK), event -> {
                    TagKey<BlockType> mineableAxe = TagKey.create(RegistryKey.BLOCK, MINEABLE_AXE);
                    if (!event.registrar().hasTag(mineableAxe)) return;

                    List<TypedKey<BlockType>> blocks = new ArrayList<>(event.registrar().getTag(mineableAxe));
                    blocks.remove(TypedKey.create(RegistryKey.BLOCK, NOTE_BLOCK));
                    event.registrar().setTag(mineableAxe, blocks);
                });
    }

    @SuppressWarnings("unchecked")
    private static LifecycleEventType<BootstrapContext, ? extends LifecycleEvent, ?> paintingPhase() {
        Method method;
        try {
            try {
                method = RegistryEventProvider.class.getMethod("compose");
            } catch (NoSuchMethodException ignored) {
                method = RegistryEventProvider.class.getMethod("freeze");
            }
            return (LifecycleEventType<BootstrapContext, ? extends LifecycleEvent, ?>)
                    method.invoke(RegistryEvents.PAINTING_VARIANT);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Paper does not expose a supported painting registry phase", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static WritableRegistry<Art, PaintingVariantRegistryEntry.Builder> paintingRegistry(LifecycleEvent event) {
        try {
            Method method = null;
            for (Class<?> eventInterface : event.getClass().getInterfaces()) {
                try {
                    method = eventInterface.getMethod("registry");
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (method == null) throw new NoSuchMethodException("registry");
            return (WritableRegistry<Art, PaintingVariantRegistryEntry.Builder>) method.invoke(event);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not access Paper's painting registry event", exception);
        }
    }
}
