package io.th0rgal.oraxen.paintings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.pack.generation.OraxenDatapack;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class PaintingDatapack extends OraxenDatapack {

    public static final Key DATAPACK_KEY = Key.key("minecraft:file/oraxen_paintings");
    private final Collection<CustomPainting> paintings;

    public PaintingDatapack(Collection<CustomPainting> paintings) {
        super("oraxen_paintings",
                "Datapack for Oraxen's Custom Paintings",
                ResourcePackFormatUtil.getCurrentDataPackFormat());
        this.paintings = paintings;
    }

    @Override
    protected Key getDatapackKey() {
        return DATAPACK_KEY;
    }

    @Override
    public void generateAssets(List<VirtualFile> output) {
        if (paintings.isEmpty()) {
            enableDatapack(false);
            return;
        }

        if (!writeMCMeta() || !writePaintingVariants() || !writePlaceableTag()) {
            enableDatapack(false);
            return;
        }

        if (isFirstInstall || !datapackEnabled) {
            Message.DATAPACK_GENERATED.send(Bukkit.getConsoleSender(),
                    TagResolver.resolver(Placeholder.parsed("datapack_name", "Paintings")));
        }

        enableDatapack(true);
    }

    private boolean writePaintingVariants() {
        for (CustomPainting painting : paintings) {
            Path paintingFolder = datapackFolder.toPath()
                    .resolve("data")
                    .resolve(painting.variantKey().namespace())
                    .resolve("painting_variant")
                    .normalize();
            Path paintingPath = paintingFolder.resolve(painting.variantKey().value() + ".json").normalize();
            if (!paintingPath.startsWith(paintingFolder)) {
                Logs.logError("Invalid painting variant path " + painting.variantKey().asString());
                return false;
            }

            File paintingFile = paintingPath.toFile();

            try {
                paintingFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(paintingFile, painting.toJson().toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Logs.logError("Failed to write painting variant file " + paintingFile.getPath() + ": " + e.getMessage());
                Logs.debug(e);
                return false;
            }
        }
        return true;
    }

    private boolean writePlaceableTag() {
        JsonArray values = new JsonArray();
        paintings.stream()
                .filter(CustomPainting::includeInRandom)
                .map(painting -> painting.variantKey().asString())
                .forEach(values::add);

        if (values.isEmpty()) {
            return true;
        }

        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);

        File tagFile = datapackFolder.toPath()
                .resolve("data/minecraft/tags/painting_variant/placeable.json")
                .toFile();

        try {
            tagFile.getParentFile().mkdirs();
            FileUtils.writeStringToFile(tagFile, tag.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logs.logError("Failed to write placeable painting tag " + tagFile.getPath() + ": " + e.getMessage());
            Logs.debug(e);
            return false;
        }
        return true;
    }
}
