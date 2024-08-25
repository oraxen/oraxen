package io.th0rgal.oraxen.pack.creative;

import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.overlay.Overlay;
import team.unnamed.creative.overlay.ResourceContainer;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;
import team.unnamed.creative.serialize.minecraft.ResourceCategories;
import team.unnamed.creative.serialize.minecraft.ResourceCategory;
import team.unnamed.creative.serialize.minecraft.fs.FileTreeWriter;
import team.unnamed.creative.serialize.minecraft.io.JsonResourceSerializer;
import team.unnamed.creative.serialize.minecraft.io.ResourceSerializer;
import team.unnamed.creative.serialize.minecraft.metadata.MetadataSerializer;
import team.unnamed.creative.serialize.minecraft.sound.SoundRegistrySerializer;
import team.unnamed.creative.sound.SoundRegistry;
import team.unnamed.creative.texture.Texture;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Map;

import static io.th0rgal.oraxen.pack.creative.MinecraftResourcePackStructure.*;


public class OraxenPackWriter implements MinecraftResourcePackWriter {

    public <T extends Keyed> void writeFullCategory(
            final @NotNull String basePath,
            final @NotNull ResourceContainer resourceContainer,
            final @NotNull FileTreeWriter target,
            final @NotNull ResourceCategory<T> category
    ) {
        for (T resource : category.lister().apply(resourceContainer)) {
            String path = basePath + category.pathOf(resource);
            final ResourceSerializer<T> serializer = category.serializer();

            if (serializer instanceof JsonResourceSerializer) {
                // if it's a JSON serializer, we can use our own method, that will
                // do some extra configuration
                writeToJson(target, (JsonResourceSerializer<T>) serializer, resource, path);
            } else {
                try (OutputStream output = target.openStream(path)) {
                    category.serializer().serialize(resource, output);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private void writeWithBasePath(FileTreeWriter target, ResourceContainer container, String basePath) {
        // write resources from most categories
        for (ResourceCategory<?> category : ResourceCategories.categories()) {
            writeFullCategory(basePath, container, target, category);
        }

        // write sound registries
        for (SoundRegistry soundRegistry : container.soundRegistries()) {
            writeToJson(target, SoundRegistrySerializer.INSTANCE, soundRegistry, basePath + MinecraftResourcePackStructure.pathOf(soundRegistry));
        }

        // write textures
        for (Texture texture : container.textures()) {
            target.write(
                    basePath + MinecraftResourcePackStructure.pathOf(texture),
                    texture.data()
            );

            Metadata metadata = texture.meta();
            if (!metadata.parts().isEmpty()) {
                writeToJson(target, MetadataSerializer.INSTANCE, metadata, basePath + MinecraftResourcePackStructure.pathOfMeta(texture));
            }
        }

        // write unknown files
        for (Map.Entry<String, Writable> entry : container.unknownFiles().entrySet()) {
            target.write(basePath + entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void write(final @NotNull FileTreeWriter target, final @NotNull ResourcePack resourcePack) {
        // write icon
        {
            Writable icon = resourcePack.icon();
            if (icon != null) {
                target.write(PACK_ICON_FILE, icon);
            }
        }

        // write metadata
        {
            Metadata metadata = resourcePack.metadata();
            // TODO: Should we check for pack meta?
            writeToJson(target, MetadataSerializer.INSTANCE, metadata, PACK_METADATA_FILE);
        }

        writeWithBasePath(target, resourcePack, "");

        // write from overlays
        for (Overlay overlay : resourcePack.overlays()) {
            writeWithBasePath(target, overlay, OVERLAYS_FOLDER + '/' + overlay.directory() + '/');
        }
    }

    private <T> void writeToJson(FileTreeWriter writer, JsonResourceSerializer<T> serializer, T object, String path) {
        try (JsonWriter jsonWriter = new JsonWriter(writer.openWriter(path))) {
            jsonWriter.setIndent("  ");
            serializer.serializeToJson(object, jsonWriter);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write to " + path, e);
        }
    }
}
