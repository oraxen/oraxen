package io.th0rgal.oraxen.pack;

import com.ticxo.modelengine.api.ModelEngineAPI;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPack;
import io.th0rgal.oraxen.api.events.resourcepack.OraxenPostPackGenerateEvent;
import io.th0rgal.oraxen.api.events.resourcepack.OraxenPrePackGenerateEvent;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.font.Shift;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockFactory;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ParseUtils;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorDatapack;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.BuiltResourcePack;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.font.Font;
import team.unnamed.creative.font.FontProvider;
import team.unnamed.creative.lang.Language;
import team.unnamed.creative.model.ItemOverride;
import team.unnamed.creative.model.ItemPredicate;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;
import team.unnamed.creative.sound.SoundEvent;
import team.unnamed.creative.sound.SoundRegistry;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PackGenerator {

    public static Path externalPacks = OraxenPlugin.get().packPath().resolve("external_packs");
    public static final MinecraftResourcePackReader reader = MinecraftResourcePackReader.builder().lenient(true).build();
    public static final MinecraftResourcePackWriter writer = MinecraftResourcePackWriter.builder().prettyPrinting(true).build();

    private static final Path assetsFolder = OraxenPlugin.get().packPath().resolve("assets");
    @NotNull private ResourcePack resourcePack = ResourcePack.resourcePack();
    private BuiltResourcePack builtPack;
    private final CustomArmorDatapack customArmorDatapack;
    private final ModelGenerator modelGenerator;

    public PackGenerator() {
        generateDefaultPaths();
        if (Settings.PACK_IMPORT_DEFAULT.toBool()) PackDownloader.downloadDefaultPack();
        customArmorDatapack = Settings.CUSTOM_ARMOR_ENABLED.toBool() ? new CustomArmorDatapack() : null;
        this.modelGenerator = new ModelGenerator(resourcePack);
    }

    public void generatePack() {
        EventUtils.callEvent(new OraxenPrePackGenerateEvent(resourcePack));

        Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
            Logs.logInfo("Generating resourcepack...");
            if (Settings.PACK_IMPORT_DEFAULT.toBool()) importDefaultPack();
            if (Settings.PACK_IMPORT_EXTERNAL.toBool()) importExternalPacks();
            if (Settings.PACK_IMPORT_MODEL_ENGINE.toBool()) importModelEnginePack();

            try {
                OraxenPack.mergePack(resourcePack, reader.readFromDirectory(OraxenPlugin.get().packPath().toFile()));
            } catch (Exception e) {
                Logs.logError("Failed to read Oraxen/pack/assets-folder to a ResourcePack");
                if (!Settings.DEBUG.toBool()) Logs.logError(e.getMessage());
                else e.printStackTrace();
            }

            resourcePack.removeUnknownFile("pack.zip");
            for (Map.Entry<String, Writable> entry : new LinkedHashSet<>(resourcePack.unknownFiles().entrySet()))
                if (entry.getKey().startsWith("external_packs/")) resourcePack.removeUnknownFile(entry.getKey());
                else if (entry.getKey().startsWith(".deobfCachedPacks")) resourcePack.removeUnknownFile(entry.getKey());
                else if (entry.getKey().startsWith(".assetCache")) resourcePack.removeUnknownFile(entry.getKey());

            CustomBlockFactory.get().blockStates().forEach(resourcePack::blockState);
            addItemPackFiles();
            addGlyphFiles();
            addSoundFile();
            parseLanguageFiles();

            if (Settings.CUSTOM_ARMOR_ENABLED.toBool()) customArmorDatapack.generateTrimAssets();
            if (Settings.HIDE_SCOREBOARD_NUMBERS.toBool()) hideScoreboardNumbers();
            if (Settings.HIDE_SCOREBOARD_BACKGROUND.toBool()) hideScoreboardBackground();

            removeExcludedFileExtensions();
            sortModelOverrides();

            PackSlicer.processInputs(resourcePack);

            try {
                resourcePack = Bukkit.getScheduler().callSyncMethod(OraxenPlugin.get(), () -> {
                    OraxenPostPackGenerateEvent event = new OraxenPostPackGenerateEvent(resourcePack);
                    EventUtils.callEvent(event);
                    return event.resourcePack();
                }).get();
            } catch (Exception ignored) {}

            resourcePack = new PackObfuscator(resourcePack).obfuscatePack();

            File packZip = OraxenPlugin.get().packPath().resolve("pack.zip").toFile();
            if (Settings.PACK_ZIP.toBool()) writer.writeToZipFile(packZip, resourcePack);
            builtPack = writer.build(resourcePack);

            Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> {
                Logs.logSuccess("Finished generating resourcepack!", true);
                OraxenPlugin.get().packServer().uploadPack();
                if (Settings.PACK_SEND_RELOAD.toBool()) for (Player player : Bukkit.getOnlinePlayers())
                    OraxenPlugin.get().packServer().sendPack(player);
            });
        });
    }

    private void sortModelOverrides() {
        for (Model model : new ArrayList<>(resourcePack.models())) {
            List<ItemOverride> sortedOverrides = new LinkedHashSet<>(model.overrides()).stream().sorted(Comparator.comparingInt(o -> {
                Optional<ItemPredicate> cmd = o.predicate().stream().filter(p -> p.name().equals("custom_model_data")).findFirst();
                return cmd.isPresent() ? ParseUtils.parseInt(cmd.get().value().toString(), 0) : 0;
            })).toList();
            model.toBuilder().overrides(sortedOverrides).build().addTo(resourcePack);
        }
    }

    public ResourcePack resourcePack() {
        return resourcePack;
    }

    public void resourcePack(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    public BuiltResourcePack builtPack() {
        return builtPack;
    }

    private void addGlyphFiles() {
        Map<Key, List<FontProvider>> fontGlyphs = new LinkedHashMap<>();
        for (Glyph glyph : OraxenPlugin.get().fontManager().glyphs()) {
            if (!glyph.hasBitmap()) fontGlyphs.compute(glyph.font(), (key, providers) -> {
                if (providers == null) providers = new ArrayList<>();
                providers.add(glyph.fontProvider());
                return providers;
            });
        }

        for (FontManager.GlyphBitMap glyphBitMap : FontManager.glyphBitMaps.values()) {
            fontGlyphs.compute(glyphBitMap.font(), (key, providers) -> {
                if (providers == null) providers = new ArrayList<>();
                providers.add(glyphBitMap.fontProvider());
                return providers;
            });
        }

        for (Map.Entry<Key, List<FontProvider>> entry : fontGlyphs.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            resourcePack.font(Font.font(entry.getKey(), entry.getValue()));
        }

        Key shiftKey = Key.key(Settings.SHIFT_FONT.toString());
        Font shiftFont = resourcePack.font(shiftKey);
        FontProvider shiftProvider = FontProvider.space(Arrays.stream(Shift.values())
                .filter(s -> !s.equals(Shift.NULL))
                .map(s -> Map.entry(s.toString(), s.toNumber()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        if (shiftFont == null) resourcePack.font(shiftKey, shiftProvider);
        else shiftFont.providers().add(shiftProvider);
    }

    private void addSoundFile() {
        for (SoundRegistry customSoundRegistry : OraxenPlugin.get().soundManager().customSoundRegistries()) {
            SoundRegistry existingRegistry = resourcePack.soundRegistry(customSoundRegistry.namespace());
            if (existingRegistry == null) customSoundRegistry.addTo(resourcePack);
            else {
                Collection<SoundEvent> mergedEvents = new LinkedHashSet<>();
                mergedEvents.addAll(existingRegistry.sounds());
                mergedEvents.addAll(customSoundRegistry.sounds());
                SoundRegistry.soundRegistry().namespace(existingRegistry.namespace()).sounds(mergedEvents).build().addTo(resourcePack);
            }
        }
    }

    private void hideScoreboardNumbers() {
        resourcePack.unknownFile("assets/minecraft/shaders/core/rendertype_text.json", ShaderUtils.ScoreboardNumbers.json());
        resourcePack.unknownFile("assets/minecraft/shaders/post/deferred_text.vsh", ShaderUtils.ScoreboardNumbers.vsh());
    }

    private void hideScoreboardBackground() {
        resourcePack.unknownFile("assets/minecraft/shaders/core/" + "rendertype_gui.vsh", ShaderUtils.ScoreboardBackground.modernFile());
    }

    private void importDefaultPack() {
        Optional<File> defaultPack = Arrays.stream(Objects.requireNonNullElse(externalPacks.toFile().listFiles(), new File[]{}))
                .filter(f -> f.getName().startsWith("DefaultPack_")).findFirst();
        if (defaultPack.isEmpty()) return;
        Logs.logInfo("Importing DefaultPack...");

        try {
            OraxenPack.mergePack(resourcePack, defaultPack.get().isDirectory()
                    ? reader.readFromDirectory(defaultPack.get())
                    : reader.readFromZipFile(defaultPack.get())
            );
        } catch (Exception e) {
            Logs.logError("Failed to read Oraxen's DefaultPack...");
            if (!Settings.DEBUG.toBool()) Logs.logError(e.getMessage());
            else e.printStackTrace();
        }

    }

    private void importExternalPacks() {
        for (File file : Objects.requireNonNullElse(externalPacks.toFile().listFiles(), new File[]{})) {
            if (file == null || file.getName().startsWith("DefaultPack_")) continue;
            if (file.isDirectory()) {
                Logs.logInfo("Importing pack " + file.getName() + "...");
                try {
                    OraxenPack.mergePack(resourcePack, reader.readFromDirectory(file));
                } catch (Exception e) {
                    Logs.logError("Failed to read " + file.getPath() + " to a ResourcePack...");
                    if (!Settings.DEBUG.toBool()) Logs.logError(e.getMessage());
                    else e.printStackTrace();
                }
            } else if (file.getName().endsWith(".zip")) {
                Logs.logInfo("Importing zipped pack " + file.getName() + "...");
                try {
                    OraxenPack.mergePack(resourcePack, reader.readFromZipFile(file));
                } catch (Exception e) {
                    Logs.logError("Failed to read " + file.getPath() + " to a ResourcePack...");
                    if (!Settings.DEBUG.toBool()) Logs.logError(e.getMessage());
                    else e.printStackTrace();
                }
            } else {
                Logs.logError("Skipping unknown file " + file.getName() + " in imports folder");
                Logs.logError("File is neither a directory nor a zip file");
            }
        }
    }

    private void importModelEnginePack() {
        if (!PluginUtils.isEnabled("ModelEngine")) return;
        File megPack = ModelEngineAPI.getAPI().getDataFolder().toPath().resolve("resource pack.zip").toFile();
        if (!megPack.exists()) return;

       try {
           OraxenPack.mergePack(resourcePack, reader.readFromZipFile(megPack));
       } catch (Exception e) {
           Logs.logError("Failed to read ModelEngine-ResourcePack...");
           if (!Settings.DEBUG.toBool()) Logs.logError(e.getMessage());
           else e.printStackTrace();
       } finally {
           Logs.logSuccess("Imported ModelEngine pack successfully!");
       }
    }



    private static void generateDefaultPaths() {
        externalPacks.toFile().mkdirs();
        assetsFolder.resolve("minecraft/textures").toFile().mkdirs();
        assetsFolder.resolve("minecraft/models").toFile().mkdirs();
        assetsFolder.resolve("minecraft/sounds").toFile().mkdirs();
        assetsFolder.resolve("minecraft/font").toFile().mkdirs();
        assetsFolder.resolve("minecraft/lang").toFile().mkdirs();

        DefaultResourcePackExtractor.extractLatest(reader);
    }

    private void addItemPackFiles() {
        modelGenerator.generateBaseItemModels();
        modelGenerator.generateItemModels();
        AtlasGenerator.generateAtlasFile();
    }

    private void parseLanguageFiles() {
        parseGlobalLanguage();
        for (Language language : new ArrayList<>(resourcePack.languages())) {
            for (Map.Entry<String, String> entry : new LinkedHashSet<>(language.translations().entrySet())) {
                language.translations().put(entry.getKey(), AdventureUtils.parseLegacyThroughMiniMessage(entry.getValue()));
                language.translations().remove("DO_NOT_ALTER_THIS_LINE");
            }
            resourcePack.language(language);
        }
    }

    private void parseGlobalLanguage() {
        Language globalLanguage = resourcePack.language(Key.key("global"));
        if (globalLanguage == null) return;
        Logs.logInfo("Converting global lang file to individual language files...");

        for (Key langKey : availableLanguageCodes) {
            Language language = resourcePack.language(langKey);
            if (language == null) language = Language.language(langKey, Map.of());
            Map<String, String> newTranslations = new LinkedHashMap<>(language.translations());

            for (Map.Entry<String, String> globalEntry : new LinkedHashSet<>(globalLanguage.translations().entrySet())) {
                newTranslations.putIfAbsent(globalEntry.getKey(), globalEntry.getValue());
            }
            resourcePack.language(Language.language(langKey, newTranslations));
        }
        resourcePack.removeLanguage(Key.key("global"));
    }

    private static final Set<Key> availableLanguageCodes = new LinkedHashSet<>(Arrays.asList(
            "af_za", "ar_sa", "ast_es", "az_az", "ba_ru",
            "bar", "be_by", "bg_bg", "br_fr", "brb", "bs_ba", "ca_es", "cs_cz",
            "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca",
            "en_gb", "en_nz", "en_pt", "en_ud", "en_us", "enp", "enws", "eo_uy",
            "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve", "esan",
            "et_ee", "eu_es", "fa_ir", "fi_fi", "fil_ph", "fo_fo", "fr_ca", "fr_fr",
            "fra_de", "fur_it", "fy_nl", "ga_ie", "gd_gb", "gl_es", "haw_us", "he_il",
            "hi_in", "hr_hr", "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is",
            "isv", "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in", "ko_kr",
            "ksh", "kw_gb", "la_la", "lb_lu", "li_li", "lmo", "lol_us", "lt_lt",
            "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my", "mt_mt", "nah", "nds_de",
            "nl_be", "nl_nl", "nn_no", "no_no", "oc_fr", "ovd", "pl_pl", "pt_br",
            "pt_pt", "qya_aa", "ro_ro", "rpr", "ru_ru", "ry_ua", "se_no", "sk_sk",
            "sl_si", "so_so", "sq_al", "sr_sp", "sv_se", "sxu", "szl", "ta_in",
            "th_th", "tl_ph", "tlh_aa", "tok", "tr_tr", "tt_ru", "uk_ua", "val_es",
            "vec_it", "vi_vn", "yi_de", "yo_ng", "zh_cn", "zh_hk", "zh_tw", "zlm_arab")).stream().map(Key::key).collect(Collectors.toCollection(LinkedHashSet::new));

    private final static Set<String> ignoredExtensions = new LinkedHashSet<>(Arrays.asList(".json", ".png", ".mcmeta"));

    private void removeExcludedFileExtensions() {
        for (String extension : Settings.PACK_EXCLUDED_FILE_EXTENSIONS.toStringList()) {
            extension = extension.startsWith(".") ? extension : "." + extension;
            if (ignoredExtensions.contains(extension)) continue;
            for (Map.Entry<String, Writable> entry : new LinkedHashSet<>(resourcePack.unknownFiles().entrySet())) {
                if (entry.getKey().endsWith(extension)) resourcePack.removeUnknownFile(entry.getKey());
            }
        }
    }
}
