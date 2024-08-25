package io.th0rgal.oraxen.pack.creative;

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.KeyUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.overlay.Overlay;
import team.unnamed.creative.overlay.ResourceContainer;
import team.unnamed.creative.serialize.minecraft.GsonUtil;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;
import team.unnamed.creative.serialize.minecraft.ResourceCategories;
import team.unnamed.creative.serialize.minecraft.ResourceCategory;
import team.unnamed.creative.serialize.minecraft.fs.FileTreeReader;
import team.unnamed.creative.serialize.minecraft.io.BinaryResourceDeserializer;
import team.unnamed.creative.serialize.minecraft.io.JsonResourceDeserializer;
import team.unnamed.creative.serialize.minecraft.io.ResourceDeserializer;
import team.unnamed.creative.serialize.minecraft.metadata.MetadataSerializer;
import team.unnamed.creative.serialize.minecraft.sound.SoundRegistrySerializer;
import team.unnamed.creative.texture.Texture;
import team.unnamed.creative.util.Keys;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

import static io.th0rgal.oraxen.pack.creative.MinecraftResourcePackStructure.*;
import static java.util.Objects.requireNonNull;

public class OraxenPackReader implements MinecraftResourcePackReader {

    @Override
    @SuppressWarnings("PatternValidation")
    public @NotNull ResourcePack read(final @NotNull FileTreeReader reader) {
        ResourcePack resourcePack = ResourcePack.resourcePack();

        // textures that are waiting for metadata, or metadata
        // waiting for textures (because we can't know the order
        // they come in)
        // (null key means it is root resource pack)
        Map<String, Map<Key, Texture>> incompleteTextures = new LinkedHashMap<>();

        while (reader.hasNext()) {
            String path = reader.next();

            // tokenize path in sections, e.g.: [ assets, minecraft, textures, ... ]
            Queue<String> tokens = tokenize(path);

            if (tokens.isEmpty()) {
                // this should never happen
                throw new IllegalStateException("Token collection is empty!");
            }

            // single token means the file is on the
            // root level (top level files) so it may be:
            // - pack.mcmeta
            // - pack.png
            if (tokens.size() == 1) {
                switch (tokens.poll()) {
                    case PACK_METADATA_FILE: {
                        // found pack.mcmeta file, deserialize and add
                        Metadata metadata = MetadataSerializer.INSTANCE.readFromTree(parseJson(reader.stream()));
                        resourcePack.metadata(metadata);
                        continue;
                    }
                    case PACK_ICON_FILE: {
                        // found pack.png file, add
                        resourcePack.icon(reader.content().asWritable());
                        continue;
                    }
                    default: {
                        // unknown top level file
                        resourcePack.unknownFile(path, reader.content().asWritable());
                        continue;
                    }
                }
            }

            // the container to use, it is initially the default resource-pack,
            // but it may change if the file is inside an overlay folder
            @Subst("dir")
            @Nullable String overlayDir = null;

            // the file path, relative to the container
            String containerPath = path;
            ResourceContainer container = resourcePack;

            // if there are two or more tokens, it means the
            // file is inside a folder, in a Minecraft resource
            // pack, the first folder is always "assets"
            String folder = tokens.poll();

            if (folder.equals(OVERLAYS_FOLDER)) {
                // gets the overlay name, set after the
                // "overlays" folder, e.g. "overlays/foo",
                // or "overlays/bar"
                overlayDir = tokens.poll();
                if (tokens.isEmpty()) {
                    // this means that there is a file directly
                    // inside the "overlays" folder, this is illegal
                    resourcePack.unknownFile(containerPath, reader.content().asWritable());
                    continue;
                }

                Overlay overlay = resourcePack.overlay(overlayDir);
                if (overlay == null) {
                    // first occurrence, register overlay
                    overlay = Overlay.overlay(overlayDir);
                    resourcePack.overlay(overlay);
                }

                container = overlay;
                folder = tokens.poll();
                containerPath = path.substring((OVERLAYS_FOLDER + '/' + overlayDir + '/').length());
            }

            // null check to make ide happy
            if (folder == null || !folder.equals(ASSETS_FOLDER) || tokens.isEmpty()) {
                // not assets! this is an unknown file
                container.unknownFile(containerPath, reader.content().asWritable());
                continue;
            }

            // inside "assets", we should always have a folder
            // with any name, which is a namespace, e.g. "minecraft"
            String namespace = tokens.poll();

            if (!Keys.isValidNamespace(namespace)) {
                // invalid namespace found
                container.unknownFile(containerPath, reader.content().asWritable());
                continue;
            }

            if (tokens.isEmpty()) {
                // found a file directly inside "assets", like
                // assets/<file>, it is not allowed
                container.unknownFile(containerPath, reader.content().asWritable());
                continue;
            }

            // so we already have "assets/<namespace>/", most files inside
            // the namespace folder always have a "category", e.g. textures,
            // lang, font, etc. But not always! There is sounds.json file and
            // gpu_warnlist.json file
            String categoryName = tokens.poll();

            if (tokens.isEmpty()) {
                // this means "category" is a file
                // (remember: last tokens are always files)
                if (categoryName.equals(SOUNDS_FILE)) {
                    // found a sound registry!
                    container.soundRegistry(SoundRegistrySerializer.INSTANCE.readFromTree(
                            parseJson(reader.stream()),
                            namespace
                    ));
                    continue;
                } else {
                    // TODO: gpu_warnlist.json?
                    container.unknownFile(containerPath, reader.content().asWritable());
                    continue;
                }
            }

            // so "category" is actually a category like "textures",
            // "lang", "font", etc. next we can compute the relative
            // path inside the category
            String categoryPath = path(tokens);

            if (categoryName.equals(TEXTURES_FOLDER)) {
                String keyOfMetadata = withoutExtension(categoryPath, METADATA_EXTENSION);
                if (keyOfMetadata != null) {
                    KeyUtils.parseKey(namespace, categoryPath, "texture-meta");
                    // found metadata for texture
                    Key key = Key.key(namespace, keyOfMetadata);
                    Metadata metadata = MetadataSerializer.INSTANCE.readFromTree(parseJson(reader.stream()));

                    Map<Key, Texture> incompleteTexturesThisContainer = incompleteTextures.computeIfAbsent(overlayDir, k -> new LinkedHashMap<>());
                    Texture texture = incompleteTexturesThisContainer.remove(key);
                    if (texture == null) {
                        // metadata was found first, put
                        incompleteTexturesThisContainer.put(key, Texture.texture(key, Writable.EMPTY, metadata));
                    } else {
                        // texture was found before the metadata, nice!
                        container.texture(texture.meta(metadata));
                    }
                } else {
                    KeyUtils.parseKey(namespace, categoryPath, "texture");

                    Key key = Key.key(namespace, categoryPath);
                    Writable data = reader.content().asWritable();
                    Map<Key, Texture> incompleteTexturesThisContainer = incompleteTextures.computeIfAbsent(overlayDir, k -> new LinkedHashMap<>());
                    Texture waiting = incompleteTexturesThisContainer.remove(key);

                    if (waiting == null) {
                        // found texture before metadata
                        incompleteTexturesThisContainer.put(key, Texture.texture(key, data));
                    } else {
                        // metadata was found first
                        container.texture(Texture.texture(
                                key,
                                data,
                                waiting.meta()
                        ));
                    }
                }
            } else {
                @SuppressWarnings("rawtypes")
                ResourceCategory category = ResourceCategories.getByFolder(categoryName);
                if (category == null) {
                    // unknown category
                    container.unknownFile(containerPath, reader.content().asWritable());
                    continue;
                }
                String keyValue = withoutExtension(categoryPath, category.extension());
                if (keyValue == null) {
                    // wrong extension
                    container.unknownFile(containerPath, reader.content().asWritable());
                    continue;
                }

                KeyUtils.parseKey(namespace, keyValue, "model");
                Key key = Key.key(namespace, keyValue);
                try {
                    ResourceDeserializer<?> deserializer = category.deserializer();
                    Object resource;
                    if (deserializer instanceof BinaryResourceDeserializer) {
                        resource = ((BinaryResourceDeserializer<?>) deserializer)
                                .deserializeBinary(reader.content().asWritable(), key);
                    } else if (deserializer instanceof JsonResourceDeserializer) {
                        resource = ((JsonResourceDeserializer<?>) deserializer)
                                .deserializeFromJson(parseJson(reader.stream()), key);
                    } else {
                        resource = deserializer.deserialize(reader.stream(), key);
                    }
                    //noinspection unchecked
                    category.setter().accept(container, resource);
                } catch (Exception e) {
                    Logs.logWarning("Failed to deserialize resource at: <gold>" + path);
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                    else Logs.logWarning(e.getMessage());
                }
            }
        }

        for (Map.Entry<String, Map<Key, Texture>> entry : incompleteTextures.entrySet()) {
            @Subst("dir")
            @Nullable String overlayDir = entry.getKey();
            Map<Key, Texture> incompleteTexturesThisContainer = entry.getValue();
            ResourceContainer container;

            if (overlayDir == null) {
                // root
                container = resourcePack;
            } else {
                // from an overlay
                container = resourcePack.overlay(overlayDir);
                requireNonNull(container, "container"); // should never happen, but make ide happy
            }

            for (Texture texture : incompleteTexturesThisContainer.values()) {
                if (texture.data() != Writable.EMPTY) {
                    container.texture(texture);
                }
            }
        }
        return resourcePack;
    }

    private static @Nullable String withoutExtension(String string, String extension) {
        if (string.endsWith(extension)) {
            return string.substring(0, string.length() - extension.length());
        } else {
            // string doesn't end with extension
            return null;
        }
    }

    private @NotNull JsonElement parseJson(final @NotNull InputStream input) {
        try (final JsonReader jsonReader = new JsonReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            jsonReader.setLenient(true);
            return GsonUtil.parseReader(jsonReader);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to close JSON reader", e);
        }
    }
}
