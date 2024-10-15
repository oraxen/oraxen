package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.FileUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import team.unnamed.creative.sound.Sound;
import team.unnamed.creative.sound.SoundEntry;
import team.unnamed.creative.sound.SoundEvent;
import team.unnamed.creative.sound.SoundRegistry;
import team.unnamed.creative.texture.Texture;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PackObfuscator {

    private ResourcePack resourcePack;
    private final PackObfuscationType obfuscationType;
    private File obfCachedPack;
    private final boolean cache = Settings.PACK_CACHE_OBFUSCATION.toBool();

    public PackObfuscator(ResourcePack resourcePack) {
        this.obfuscationType = Settings.PACK_OBFUSCATION_TYPE.toEnumOrGet(PackObfuscationType.class, (obfType) -> {
            Logs.logError("Invalid PackObfuscation type: " + obfType + ", defaulting to " + PackObfuscationType.FULL, true);
            Logs.logError("Valid options are: " + StringUtils.join(Arrays.stream(PackObfuscationType.values()).map(Enum::name).toList(), ", "), true);
            return PackObfuscationType.FULL;
        });
        this.resourcePack = resourcePack;
    }

    public PackObfuscationType obfuscationType() {
        return obfuscationType;
    }

    public enum PackObfuscationType {
        SIMPLE, FULL, NONE;

        public boolean isNone() {
            return this == NONE;
        }
    }

    public ResourcePack obfuscatePack() {
        if (obfuscationType.isNone()) return resourcePack;

        String hash = PackGenerator.writer.build(resourcePack).hash();
        this.obfCachedPack = OraxenPlugin.get().packPath().resolve(".deobfCachedPacks").resolve(hash).toFile();
        this.obfCachedPack.getParentFile().mkdirs();
        FileUtil.setHidden(obfCachedPack.toPath().getParent());

        if (shouldObfuscatePack()) {
            Logs.logInfo("Obfuscating OraxenPack...");
            obfuscateModels();
            obfuscateFonts();
            obfuscateTextures();
            obfuscateSounds();

            if (cache) {
                obfCachedPack.mkdirs();
                PackGenerator.writer.writeToDirectory(obfCachedPack, resourcePack);
                Logs.logInfo("Caching obfuscated ResourcePack...");
            }
        }

        List<File> files = FileUtil.listFiles(this.obfCachedPack.getParentFile());
        for (final File file : files) {
            if (file.isDirectory() && !file.getName().equals(obfCachedPack.getName())) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (Exception ignored) {
                }
            }
        }

        return resourcePack;
    }

    public boolean shouldObfuscatePack() {
        // We do not want to cache it or use a cached version
        if (!cache) return true;
        // We want to use cached version, but it does not exist
        if (!obfCachedPack.exists()) return true;

        // We want to use cached version, and it exists, so read it to pack
        resourcePack = PackGenerator.reader.readFromDirectory(obfCachedPack);
        return false;
    }

    private final Set<ObfuscatedModel> obfuscatedModels = new HashSet<>();
    private final Set<ObfuscatedTexture> obfuscatedTextures = new HashSet<>();
    private final Set<ObfuscatedSound> obfuscatedSounds = new HashSet<>();
    private final Map<String, String> obfuscatedNamespaces = new HashMap<>();

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

    private record ObfuscatedSound(Sound originalSound, Sound obfuscatedSound) {
        public boolean containsKey(Key soundKey) {
            return originalSound.key().equals(soundKey) || obfuscatedSound.key().equals(soundKey);
        }
    }

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
                    .map(e -> Map.entry(e.getKey(), MultiVariant.of(e.getValue().variants().stream().map(this::obfuscateBlockstateVariant).toList())))
                    .toList().forEach(e -> variants.put(e.getKey(), e.getValue()));

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
        ).toList().forEach(resourcePack::font);
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
            List<AtlasSource> nonSingleSources = new ArrayList<>(atlas.sources().stream().filter(Predicate.not(s -> s instanceof SingleAtlasSource)).toList());
            Set<SingleAtlasSource> obfSources = atlas.sources().stream().filter(SingleAtlasSource.class::isInstance)
                    .map(SingleAtlasSource.class::cast)
                    .map(singleSource -> obfuscatedTextures.stream()
                            .filter(obf -> obf.containsKey(singleSource.resource()))
                            .map(obf -> AtlasSource.single(keyNoPng(obf.obfuscatedTexture.key())))
                            .findFirst().orElse(singleSource)
                    ).collect(Collectors.toSet());

            nonSingleSources.addAll(obfSources);
            return atlas.toBuilder().sources(nonSingleSources).build();
        }).forEach(resourcePack::atlas);
    }

    private void obfuscateSounds() {
        resourcePack.sounds().stream().map(sound -> {
            if (DefaultResourcePackExtractor.vanillaSounds.contains(sound.key())) return sound;
            Sound obfSound = Sound.sound(obfuscatedKey(sound.key()), sound.data());
            obfuscatedSounds.add(new ObfuscatedSound(sound, obfSound));
            return obfSound;
        }).toList().forEach(resourcePack::sound);

        resourcePack.soundRegistries().stream().map(soundRegistry -> {
            List<SoundEvent> obfSoundEvents = soundRegistry.sounds().stream().map(soundEvent -> {
                List<SoundEntry> obfEntries = soundEvent.sounds().stream().map(soundEntry ->
                        obfuscatedSounds.stream().filter(obf -> obf.containsKey(soundEntry.key())).findFirst()
                                .map(obf -> soundEntry.toBuilder().key(obf.obfuscatedSound.key()).build())
                                .orElse(soundEntry)).toList();

                return soundEvent.toBuilder().sounds(obfEntries).build();
            }).toList();
            return SoundRegistry.soundRegistry(soundRegistry.namespace(), obfSoundEvents);
        }).toList().forEach(resourcePack::soundRegistry);

        obfuscatedSounds.forEach(obf -> {
            resourcePack.removeSound(obf.originalSound.key());
            obf.obfuscatedSound.addTo(resourcePack);
        });
    }

    private Model obfuscateModel(Model model) {
        Optional<ObfuscatedModel> obfuscatedModel = obfuscatedModels.stream().filter(obf -> obf.containsKey(model.key())).findFirst();
        if (obfuscatedModel.isPresent()) return obfuscatedModel.get().obfuscatedModel();

        if (VanillaKeys.isVanillaModel(model.key())) {
            List<Key> textureKeys = allTexturesOfModel(model);
            if (obfuscatedTextures.stream().anyMatch(obf -> textureKeys.contains(obf.originalTexture.key())))
                obfuscate(obfuscateParentModel(obfuscateModelTextures(model)));
            Model obfVanillaModel = obfuscateModelOverrides(model);
            obfuscatedModels.add(new ObfuscatedModel(model, obfVanillaModel));
            return obfVanillaModel;
        } else return obfuscate(obfuscateModelTextures(model));
    }

    private Model obfuscateParentModel(Model model) {
        Key parentKey = model.parent();
        if (parentKey == null) return model;
        Optional<ObfuscatedModel> obfuscatedModel = obfuscatedModels.stream().filter(obf -> obf.containsKey(model.key())).findFirst();
        if (obfuscatedModel.isPresent()) return obfuscatedModel.get().obfuscatedModel();

        Optional<Key> parent = obfuscatedModels.stream().filter(obf -> obf.containsKey(parentKey)).findFirst().map(obf -> obf.obfuscatedModel.key());
        if (parent.isEmpty()) parent = Optional.ofNullable(DefaultResourcePackExtractor.vanillaResourcePack.model(parentKey)).map(Model::key);
        if (parent.isEmpty() && !parentKey.equals(model.key())) {
            Optional<Model> parentModel = Optional.ofNullable(resourcePack.model(parentKey));
            parentModel.ifPresent(this::obfuscateModel);
            parent = parentModel.map(Model::key);
        }

        return model.toBuilder().parent(parent.orElse(parentKey)).build();
    }

    private Model obfuscate(Model model) {
        return obfuscatedModels.stream().filter(obf -> obf.containsKey(model.key())).findFirst().orElseGet(() -> {
            Model obfuscatedModel = obfuscateModelOverrides(model).toBuilder().key(obfuscatedKey(model.key())).build();
            ObfuscatedModel obf = new ObfuscatedModel(model, obfuscatedModel);
            obfuscatedModels.add(obf);
            return obf;
        }).obfuscatedModel;
    }

    private Model obfuscateModelOverrides(Model model) {
        return model.toBuilder().overrides(model.overrides().stream().map(override -> {
            if (VanillaKeys.isVanillaModel(override.model())) return override;

            Key obfKey = obfuscatedModels.stream().filter(obf -> obf.containsKey(override.model()))
                    .map(obf -> obf.obfuscatedModel.key()).findFirst()
                    .orElse(Optional.ofNullable(resourcePack.model(override.model()))
                            .map(overrideModel -> {
                                Model obfuscatedModel = obfuscateModel(overrideModel);
                                obfuscatedModels.add(new ObfuscatedModel(overrideModel, obfuscatedModel));
                                return obfuscatedModel.key();
                            }).stream().findFirst()
                            .orElse(override.model()));
            return ItemOverride.of(obfKey, override.predicate());
        }).toList()).build();
    }

    private Model obfuscateModelTextures(Model model) {
        Optional<ObfuscatedModel> obfModel = obfuscatedModels.stream().filter(obf -> obf.containsKey(model.key())).findFirst();
        if (obfModel.isPresent()) return obfModel.get().obfuscatedModel();
        if (VanillaKeys.isVanillaModel(model.key())) return model;

        List<ModelTexture> layers = model.textures().layers().stream().filter(l -> l.key() != null).map(modelTexture -> {
            Texture obfTexture = obfuscateModelTexture(modelTexture);
            if (obfTexture == null) return modelTexture;
            else return ModelTexture.ofKey(keyNoPng(obfTexture.key()));
        }).toList();

        Map<String, ModelTexture> variables = new HashMap<>();
        model.textures().variables().forEach((key, modelTexture) -> {
            Texture obfTexture = obfuscateModelTexture(modelTexture);
            variables.put(key, obfTexture != null ? ModelTexture.ofKey(keyNoPng(obfTexture.key())) : modelTexture);
        });

        ModelTextures.Builder builder = ModelTextures.builder().layers(layers).variables(variables);
        Optional.ofNullable(model.textures().particle()).ifPresentOrElse(
                p -> Optional.ofNullable(obfuscateModelTexture(p)).ifPresentOrElse(
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

    private Texture obfuscate(Texture texture) {
        return obfuscatedTextures.stream().filter(obf -> obf.containsKey(texture.key())).findFirst()
                .map(ObfuscatedTexture::obfuscatedTexture)
                .orElseGet(() -> {
                    Texture obf = texture.toBuilder().key(keyPng(obfuscatedKey(texture.key()))).build();
                    obfuscatedTextures.add(new ObfuscatedTexture(texture, obf, true));
                    return obf;
                });
    }

    private Variant obfuscateBlockstateVariant(Variant variant) {
        Key model = obfuscatedModels.stream().filter(obf -> obf.containsKey(variant.model())).findFirst().map(o -> o.obfuscatedModel.key()).orElse(variant.model());
        return Variant.builder().model(model).uvLock(variant.uvLock()).weight(variant.weight()).x(variant.x()).y(variant.y()).build();
    }

    @Nullable
    private Texture obfuscateModelTexture(ModelTexture modelTexture) {
        Optional<Key> keyNoPng = Optional.ofNullable(modelTexture.key()).map(PackObfuscator::keyNoPng);
        if (keyNoPng.isEmpty()) return null;
        Key key = keyNoPng.get();

        Optional<Texture> texture = obfuscatedTextures.stream().filter(obf -> obf.containsKey(key)).findFirst().map(ObfuscatedTexture::obfuscatedTexture);
        if (texture.isEmpty()) texture = VanillaKeys.getVanillaTexture(key);
        if (texture.isEmpty()) texture = resourcePack.textures().stream().filter(t -> t.key().equals(key)).findFirst();

        return texture.map(this::obfuscate).orElse(null);
    }

    private Texture obfuscateFontTexture(BitMapFontProvider fontProvider) {
        return obfuscatedTextures.stream().filter(obf -> obf.containsKey(keyPng(fontProvider.file()))).findFirst().map(ObfuscatedTexture::obfuscatedTexture)
                .orElse(VanillaKeys.getVanillaTexture(fontProvider.file())
                        .orElse(resourcePack.textures().stream().filter(t -> t.key().equals(keyPng(fontProvider.file()))).findFirst()
                                .map(this::obfuscate).orElse(null)));
    }

    private static class VanillaKeys {

        public static boolean isVanillaModel(Key key) {
            return DefaultResourcePackExtractor.vanillaResourcePack.model(key) != null;
        }

        public static boolean isVanillaTexture(Key key) {
            return DefaultResourcePackExtractor.vanillaResourcePack.texture(key) != null;
        }

        public static Optional<Texture> getVanillaTexture(@NotNull Key key) {
            return Optional.ofNullable(DefaultResourcePackExtractor.vanillaResourcePack.texture(key));
        }
    }

    private Key obfuscatedKey(Key key) {
        return switch (obfuscationType) {
            case NONE -> key;
            case FULL -> Key.key(obfuscatedNamespaces.computeIfAbsent(key.namespace(), (namespace) -> UUID.randomUUID().toString()), UUID.randomUUID().toString());
            case SIMPLE -> Key.key(key.namespace(), UUID.randomUUID().toString());
        };
    }

    private static Key keyNoPng(Key key) {
        return Key.key(StringUtils.removeEnd(key.asString(), ".png"));
    }

    private static Key keyPng(Key key) {
        return key != null ? Key.key(StringUtils.appendIfMissing(key.asString(), ".png")) : null;
    }
}
