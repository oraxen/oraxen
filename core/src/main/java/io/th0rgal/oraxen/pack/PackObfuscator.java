package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.AtlasSource;
import team.unnamed.creative.atlas.SingleAtlasSource;
import team.unnamed.creative.blockstate.MultiVariant;
import team.unnamed.creative.blockstate.Selector;
import team.unnamed.creative.blockstate.Variant;
import team.unnamed.creative.font.BitMapFontProvider;
import team.unnamed.creative.model.ItemOverride;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.texture.Texture;

import java.util.*;

public class PackObfuscator {

    private final ResourcePack resourcePack;

    public PackObfuscator(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    public void obfuscatePack() {
        Logs.logInfo("Obfuscating OraxenPack...");
        obfuscateModels();
        obfuscateFonts();
        obfuscateTextures();
    }

    private record ObfuscatedModel(Model originalModel, Model obfuscatedModel) {
    }

    private record ObfuscatedTexture(Texture originalTexture, Texture obfuscatedTexture, boolean isItemTexture) {
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

            blockState.multipart().clear();
            blockState.multipart().addAll(multiparts);

            Map<String, MultiVariant> variants = new HashMap<>();

            blockState.variants().entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), MultiVariant.of(e.getValue().variants().stream().map(this::obfuscateBlockstateVariant).toList()))).toList()
                    .forEach(e -> variants.put(e.getKey(), e.getValue()));
            blockState.variants().clear();
            blockState.variants().putAll(variants);

            blockState.addTo(resourcePack);
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
                            .map(obf -> keyNoPng(obf.obfuscatedTexture))
                            .map(AtlasSource::single).toList();
                    return atlas.toBuilder().sources(sources.stream().map(a -> (AtlasSource) a).toList()).build();
                }
        ).forEach(resourcePack::atlas);
    }

    private Model obfuscateModel(Model model) {
        if (VanillaKeys.isVanilla(model)) {
            List<Key> textureKeys = allTexturesOfModel(model);
            if (obfuscatedTextures.stream().anyMatch(obf -> textureKeys.contains(obf.originalTexture.key())))
                obfuscate(obfuscateModelTextures(model));
            return obfuscateModelOverrides(model);
        } else return obfuscate(obfuscateModelTextures(model));
    }
    private Model obfuscate(Model model) {
        Model obfuscatedModel = obfuscateModelOverrides(model).toBuilder().key(Key.key(UUID.randomUUID().toString())).build();
        obfuscatedModels.add(new ObfuscatedModel(model, obfuscatedModel));
        return obfuscatedModel;
    }
    private Model obfuscateModelOverrides(Model model) {
        Model obfOverride = model.toBuilder().overrides(model.overrides().stream().map(override -> {
            if (VanillaKeys.isVanilla(override.model())) return override;

            Key obfKey = resourcePack.models().stream().filter(m -> m.key().equals(override.model())).findFirst().map(m -> obfuscateModel(m).key())
                    .orElse(obfuscatedModels.stream().filter(o -> o.originalModel.key() == override.model() || o.obfuscatedModel.key().equals(override.model().key())).map(o -> o.obfuscatedModel.key()).findFirst()
                            .orElse(override.model())
                    );
            return ItemOverride.of(obfKey, override.predicate());
        }).toList()).build();

        obfuscatedModels.add(new ObfuscatedModel(model, obfOverride));
        return obfOverride;
    }
    private Model obfuscateModelTextures(Model model) {
        List<ModelTexture> layers = model.textures().layers().stream().filter(l -> l.key() != null).map(modelTexture -> {
            Texture obfTexture = obfuscateItemTexture(modelTexture);
            if (obfTexture == null) return modelTexture;
            else return ModelTexture.ofKey(keyNoPng(obfTexture));
        }).toList();

        Map<String, ModelTexture> variables = new HashMap<>();
        for (Map.Entry<String, ModelTexture> entry : model.textures().variables().entrySet()) {
            Texture obfTexture = obfuscateItemTexture(entry.getValue());
            variables.put(entry.getKey(), obfTexture != null ? ModelTexture.ofKey(keyNoPng(obfTexture)) : entry.getValue());
        }

        ModelTextures.Builder builder = ModelTextures.builder().layers(layers).variables(variables);
        Optional.ofNullable(model.textures().particle()).ifPresentOrElse(
                p -> Optional.ofNullable(obfuscateItemTexture(p)).ifPresentOrElse(
                        o -> builder.particle(ModelTexture.ofKey(keyNoPng(o))),
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
        Texture obfTexture = texture.toBuilder().key(Key.key(UUID.randomUUID() + ".png")).build();
        obfuscatedTextures.add(new ObfuscatedTexture(texture, obfTexture, isItemTexture));
        return obfTexture;
    }
    private Texture obfuscateItemTexture(ModelTexture modelTexture) {
        Key keyPng = modelTexture.key() != null ? Key.key(StringUtils.appendIfMissing(modelTexture.key().asString(), ".png")) : null;
        return obfuscatedTextures.stream().filter(t -> t.originalTexture.key().equals(keyPng)).findFirst().map(ObfuscatedTexture::obfuscatedTexture)
                .orElse(resourcePack.textures().stream().filter(t -> t.key().equals(keyPng)).findFirst().map(t -> obfuscate(t, true))
                        .orElse(null));
    }

    private Variant obfuscateBlockstateVariant(Variant variant) {
        Key model = obfuscatedModels.stream().filter(o -> o.originalModel.key().equals(variant.model())).findFirst().map(o -> o.obfuscatedModel.key()).orElse(variant.model());
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

    private Key keyNoPng(Texture texture) {
        return Key.key(StringUtils.removeEnd(texture.key().asString(), ".png"));
    }
}
