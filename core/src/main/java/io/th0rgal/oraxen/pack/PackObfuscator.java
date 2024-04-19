package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.AtlasSource;
import team.unnamed.creative.atlas.SingleAtlasSource;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.blockstate.MultiVariant;
import team.unnamed.creative.blockstate.Selector;
import team.unnamed.creative.blockstate.Variant;
import team.unnamed.creative.font.BitMapFontProvider;
import team.unnamed.creative.model.ItemOverride;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;
import team.unnamed.creative.texture.Texture;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PackObfuscator {

    private ResourcePack resourcePack;
    private PackObfuscationType obfuscationType;

    public PackObfuscator(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
        this.obfuscationType = Settings.PACK_OBFUSCATION_TYPE.toEnumOrGet(PackObfuscationType.class, () -> {
            Logs.logError("Invalid PackObfuscation type: " + Settings.PACK_OBFUSCATION_TYPE + ", defaulting to " + PackObfuscationType.FULL, true);
            Logs.logError("Valid options are: " + StringUtils.join(Arrays.stream(PackObfuscationType.values()).map(Enum::name).toList(), ", "), true);
            return PackObfuscationType.FULL;
        });
    }

    public PackObfuscationType obfuscationType() {
        return obfuscationType;
    }

    public enum PackObfuscationType {
        FILENAME, NAMESPACE, FULL, NONE
    }

    private final File cachedPackZip = OraxenPlugin.get().packPath().resolve("cachedPack.zip").toFile();
    private final File tempPackFile = OraxenPlugin.get().packPath().resolve("obfuscationCache/tempPack").toFile();
    private final File cachedPackFile = OraxenPlugin.get().packPath().resolve("obfuscationCache/cachedPack").toFile();
    private final File packZip = OraxenPlugin.get().packPath().resolve("pack.zip").toFile();
    private final MinecraftResourcePackReader reader = MinecraftResourcePackReader.minecraft();
    private final MinecraftResourcePackWriter writer = MinecraftResourcePackWriter.minecraft();

    public ResourcePack obfuscatePack() {
        if (true) {
            Logs.logInfo("Obfuscating OraxenPack...");
            obfuscateModels();
            obfuscateFonts();
            obfuscateTextures();
        }

        return resourcePack;
    }

    private boolean checkCachedPack() {
        // Write current resourcePack to a directory
        writer.writeToDirectory(tempPackFile, resourcePack);

        // If the cachedPackZip exists, read it to a pack and write it to a normal directory
        if (cachedPackZip.exists()) writer.writeToDirectory(cachedPackFile, reader.readFromZipFile(cachedPackZip));
        // Get hash of cachedPack by reading from directory
        String cachedHash = cachedPackFile.exists() ? writer.build(reader.readFromDirectory(cachedPackFile)).hash() : "";
        // Get the hash of the current resourcePack by reading from the temp directory
        String tempHash = writer.build(reader.readFromDirectory(tempPackFile)).hash();
        // Compare the hashes
        if (!tempHash.equals(cachedHash)) writer.writeToZipFile(cachedPackZip, resourcePack);
            // If they are the same read from packZip
        else if (packZip.exists()) resourcePack = reader.readFromZipFile(packZip);
        else return false;
        // Delete the tempPackDir for next check
        try {
            FileUtils.deleteDirectory(tempPackFile.getParentFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tempHash.equals(cachedHash);
    }

    private record ObfuscatedModel(Model originalModel, Model obfuscatedModel) {
        public boolean containsKey(Key modelKey) {
            return originalModel.key().equals(modelKey) || obfuscatedModel.key().equals(modelKey);
        }
    }

    private record ObfuscatedTexture(Texture originalTexture, Texture obfuscatedTexture, boolean isItemTexture) {
        public boolean containsKey(Key textureKey) {
            Key key = isItemTexture ? keyPng(textureKey) : keyNoPng(textureKey);
            return originalTexture.key().equals(key) || obfuscatedTexture.key().equals(key);
        }
    }

    private final Set<ObfuscatedModel> obfuscatedModels = new HashSet<>();
    private final Set<ObfuscatedTexture> obfuscatedTextures = new HashSet<>();

    private void obfuscateModels() {
        resourcePack.models().forEach(this::obfuscateModel);

        obfuscatedModels.forEach(obf -> {
            resourcePack.removeModel(obf.originalModel.key());
            obf.obfuscatedModel.addTo(resourcePack);
        });

        obfuscateBlockStates();
    }

    private void obfuscateBlockStates() {
        resourcePack.blockStates().forEach(blockState -> {
            List<Selector> multiparts = blockState.multipart().stream().map(selector ->
                    Selector.of(selector.condition(), MultiVariant.of(selector.variant().variants().stream().map(this::obfuscateBlockstateVariant).toList()))
            ).toList();

            Map<String, MultiVariant> variants = new LinkedHashMap<>();
            blockState.variants().entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), MultiVariant.of(e.getValue().variants().stream().map(this::obfuscateBlockstateVariant).toList()))).toList()
                    .forEach(e -> variants.put(e.getKey(), e.getValue()));

            BlockState.of(blockState.key(), variants, multiparts).addTo(resourcePack);
        });
    }

    private void obfuscateFonts() {
        resourcePack.fonts().stream().map(font ->
                font.providers(font.providers().stream().map(provider -> {
                            if (provider instanceof BitMapFontProvider bitmapProvider) {
                                Texture obfTexture = obfuscateFontTexture(bitmapProvider);
                                return bitmapProvider.toBuilder().file(Optional.ofNullable(obfTexture).map(Texture::key).orElse(bitmapProvider.file())).build();
                            } else return provider;
                        }
                ).toList())
        ).forEach(resourcePack::font);
    }

    private void obfuscateTextures() {
        obfuscatedTextures.forEach(obf -> {
            resourcePack.removeTexture(obf.originalTexture.key());
            obf.obfuscatedTexture.addTo(resourcePack);
        });
        obfuscateAtlases();
    }

    private void obfuscateAtlases() {
        resourcePack.atlases().stream().map(atlas -> {
                    List<SingleAtlasSource> sources = obfuscatedTextures.stream()
                            .filter(ObfuscatedTexture::isItemTexture)
                            .map(obf -> keyNoPng(obf.obfuscatedTexture.key()))
                            .map(AtlasSource::single).toList();
                    return atlas.toBuilder().sources(sources.stream().map(a -> (AtlasSource) a).toList()).build();
                }
        ).forEach(resourcePack::atlas);
    }

    private Model obfuscateModel(Model model) {
        if (model.key().asString().contains("_ore")) Logs.logSuccess("Obfuscating " + model.key().asString() + "...");
        Optional<ObfuscatedModel> obfuscatedModel = obfuscatedModels.stream().filter(obf -> obf.containsKey(model.key())).findFirst();
        if (obfuscatedModel.isPresent()) return obfuscatedModel.get().obfuscatedModel();

        if (VanillaKeys.isVanilla(model)) {
            List<Key> textureKeys = allTexturesOfModel(model);
            if (obfuscatedTextures.stream().anyMatch(obf -> textureKeys.contains(obf.originalTexture.key())))
                obfuscate(obfuscateModelTextures(model));
            return obfuscateModelOverrides(model);
        } else return obfuscate(obfuscateModelTextures(model));
    }

    private Model obfuscate(Model model) {
        return obfuscatedModels.stream().filter(obf -> obf.containsKey(model.key())).findFirst().orElseGet(() -> {
            if (model.key().asString().contains("_ore"))
                Logs.logWarning("Obfuscating model " + model.key().asString() + "...");
            Model obfuscatedModel = obfuscateModelOverrides(model).toBuilder().key(obfuscatedKey(model.key())).build();
            ObfuscatedModel obf = new ObfuscatedModel(model, obfuscatedModel);
            if (model.key().asString().contains("_ore"))
                Logs.logWarning(obf.originalModel.key().asString() + ": " + obf.obfuscatedModel.key().asString());
            obfuscatedModels.add(obf);
            return obf;
        }).obfuscatedModel;
    }

    private Model obfuscateModelOverrides(Model model) {
        return model.toBuilder().overrides(model.overrides().stream().map(override -> {
            if (VanillaKeys.isVanilla(override.model())) return override;

            Key obfKey = obfuscatedModels.stream().filter(obf -> obf.containsKey(override.model())).map(obf -> obf.obfuscatedModel.key()).findFirst()
                    .orElse(Optional.ofNullable(resourcePack.model(override.model()))
                            .map(m -> {
                                Model obfuscatedModel = obfuscateModel(m);
                                obfuscatedModels.add(new ObfuscatedModel(model, obfuscatedModel));
                                return obfuscatedModel;
                            }).map(Model::key).stream().findFirst()
                            .orElse(override.model()));
            return ItemOverride.of(obfKey, override.predicate());
        }).toList()).build();
    }

    private Model obfuscateModelTextures(Model model) {
        if (model.key().asString().contains("_ore"))
            Logs.debug("Obfuscating ModelTextures for " + model.key().asString());
        Optional<ObfuscatedModel> obfModel = obfuscatedModels.stream().filter(obf -> obf.containsKey(model.key())).findFirst();
        if (obfModel.isPresent()) return obfModel.get().obfuscatedModel();

        List<ModelTexture> layers = model.textures().layers().stream().filter(l -> l.key() != null).map(modelTexture -> {
            Texture obfTexture = obfuscateItemTexture(modelTexture);
            if (obfTexture == null) return modelTexture;
            else return ModelTexture.ofKey(keyNoPng(obfTexture.key()));
        }).toList();

        Map<String, ModelTexture> variables = new HashMap<>();
        model.textures().variables().forEach((key, modelTexture) -> {
            Texture obfTexture = obfuscateItemTexture(modelTexture);
            variables.put(key, obfTexture != null ? ModelTexture.ofKey(keyNoPng(obfTexture.key())) : modelTexture);
        });

        ModelTextures.Builder builder = ModelTextures.builder().layers(layers).variables(variables);
        Optional.ofNullable(model.textures().particle()).ifPresentOrElse(
                p -> Optional.ofNullable(obfuscateItemTexture(p)).ifPresentOrElse(
                        o -> builder.particle(ModelTexture.ofKey(keyNoPng(o.key()))),
                        () -> builder.particle(model.textures().particle())
                ),
                () -> builder.particle(model.textures().particle())
        );

        return model.toBuilder().textures(builder.build()).build();
    }

    private List<Key> allTexturesOfModel(Model model) {
        List<Key> textureKeys = new ArrayList<>();
        for (ModelTexture layer : model.textures().layers())
            textureKeys.add(layer.key());
        for (ModelTexture variable : model.textures().variables().values())
            textureKeys.add(variable.key());
        Optional.ofNullable(model.textures().particle()).ifPresent(p -> textureKeys.add(p.key()));
        return textureKeys.stream().filter(Objects::nonNull).toList();
    }

    private Texture obfuscate(Texture texture, boolean isItemTexture) {
        return obfuscatedTextures.stream().filter(obf -> obf.containsKey(texture.key())).findFirst()
                .map(ObfuscatedTexture::obfuscatedTexture)
                .orElseGet(() -> {
                    Texture obf = texture.toBuilder().key(keyPng(obfuscatedKey(texture.key()))).build();
                    obfuscatedTextures.add(new ObfuscatedTexture(texture, obf, isItemTexture));
                    return obf;
                });
    }

    private Texture obfuscateItemTexture(ModelTexture modelTexture) {
        return obfuscatedTextures.stream().filter(obf -> obf.containsKey(modelTexture.key())).findFirst().map(ObfuscatedTexture::obfuscatedTexture)
                .orElse(resourcePack.textures().stream().filter(t -> t.key().equals(keyPng(modelTexture.key()))).findFirst().map(t -> obfuscate(t, true))
                        .orElse(null));
    }

    private Variant obfuscateBlockstateVariant(Variant variant) {
        Logs.logSuccess(StringUtils.join(obfuscatedModels.stream().filter(obf -> obf.originalModel.key().asString().contains("_ore")).map(obf -> obf.originalModel.key().asString()).collect(Collectors.toSet()), ", "));
        Key model = obfuscatedModels.stream().filter(obf -> obf.containsKey(variant.model())).findFirst().map(o -> o.obfuscatedModel.key()).orElse(variant.model());
        if (variant.model().asString().contains("_ore"))
            Logs.logError(variant.model().asString() + ": " + model.key().asString());
        return Variant.builder().model(model).uvLock(variant.uvLock()).weight(variant.weight()).x(variant.x()).y(variant.y()).build();
    }

    private Texture obfuscateFontTexture(BitMapFontProvider fontProvider) {
        Key keyPng = fontProvider.file() != null ? Key.key(StringUtils.appendIfMissing(fontProvider.file().asString(), ".png")) : null;
        return obfuscatedTextures.stream().filter(t -> t.originalTexture.key().equals(keyPng)).findFirst().map(ObfuscatedTexture::obfuscatedTexture)
                .orElse(resourcePack.textures().stream().filter(t -> t.key().equals(keyPng)).findFirst().map(t -> obfuscate(t, true))
                        .orElse(null));
    }

    private static class VanillaKeys {
        public static boolean isVanilla(Model model) {
            return defaultBlockKeys.contains(model.key()) || defaultItemKeys.contains(model.key());
        }

        public static boolean isVanilla(Texture texture) {
            return defaultBlockKeys.contains(texture.key()) || defaultItemKeys.contains(texture.key());
        }

        public static boolean isVanilla(Key key) {
            return defaultBlockKeys.contains(key) || defaultItemKeys.contains(key);
        }

        private final static List<Key> defaultItemKeys = Arrays.stream(Material.values()).filter(m -> !m.isLegacy() && m.isItem()).map(m -> Key.key("minecraft", "item/" + m.getKey().value())).toList();
        private final static List<Key> defaultBlockKeys = Arrays.stream(Material.values()).filter(m -> !m.isLegacy() && m.isBlock()).map(m -> Key.key("minecraft", "block/" + m.getKey().value())).toList();
    }

    private Key obfuscatedKey(Key key) {
        return switch (obfuscationType) {
            case NONE -> key;
            case FULL -> Key.key(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            case NAMESPACE -> Key.key(UUID.randomUUID().toString(), key.value());
            case FILENAME -> Key.key(key.namespace(), UUID.randomUUID().toString());
        };
    }

    private static Key keyNoPng(Key key) {
        return Key.key(StringUtils.removeEnd(key.asString(), ".png"));
    }

    private static Key keyPng(Key key) {
        return key != null ? Key.key(StringUtils.appendIfMissing(key.asString(), ".png")) : null;
    }
}
