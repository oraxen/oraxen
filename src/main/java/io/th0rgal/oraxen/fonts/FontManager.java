package io.th0rgal.oraxen.fonts;

import io.th0rgal.oraxen.glyphs.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class FontManager {

    public final boolean autoGenerate;
    public final String permsChatcolor;
    public static volatile Map<String, GlyphBitMap> glyphBitMaps = Map.of();
    private final Map<String, Glyph> glyphMap;
    private final List<Glyph> glyphsByCharacterLength;
    private final Map<String, Glyph> glyphByPlaceholder;
    private final Map<String, String> reverse;
    private final FontEvents fontEvents;
    private final Set<Font> fonts;

    // New glyph types
    private final ShiftProvider shiftProvider;
    private final Map<String, ReferenceGlyph> referenceGlyphMap;
    private final Map<String, AnimatedGlyph> animatedGlyphMap;
    private final Map<String, AnimatedGlyph> animatedByPlaceholder;

    public FontManager(final ConfigsManager configsManager) {
        final Configuration fontConfiguration = configsManager.getFont();
        final ConfigurationSection bitmapSection = fontConfiguration.getConfigurationSection("bitmaps");
        autoGenerate = fontConfiguration.getBoolean("settings.automatically_generate");
        permsChatcolor = fontConfiguration.getString("settings.perms_chatcolor");
        final Map<String, GlyphBitMap> loadedGlyphBitMaps = new HashMap<>();
        if (bitmapSection != null) {
            bitmapSection.getKeys(false).forEach(key -> {
                final ConfigurationSection section = bitmapSection.getConfigurationSection(key);
                if (section != null) {
                    loadedGlyphBitMaps.put(key, new GlyphBitMap(
                            section.getString("texture"), section.getInt("rows"), section.getInt("columns"),
                            section.getInt("ascent", 8), section.getInt("height", 8)));
                }
            });
        }
        glyphBitMaps = Map.copyOf(loadedGlyphBitMaps);
        glyphMap = new LinkedHashMap<>();
        glyphByPlaceholder = new LinkedHashMap<>();
        reverse = new LinkedHashMap<>();
        fontEvents = new FontEvents(this);
        fonts = new HashSet<>();

        // Initialize new glyph type maps
        shiftProvider = new ShiftProvider();
        referenceGlyphMap = new LinkedHashMap<>();
        animatedGlyphMap = new LinkedHashMap<>();
        animatedByPlaceholder = new LinkedHashMap<>();

        // Parse all glyph types using new method
        ConfigsManager.GlyphParseOutput glyphOutput = configsManager.parseAllGlyphConfigs();
        loadGlyphs(glyphOutput.glyphs());
        loadReferenceGlyphs(glyphOutput.referenceGlyphs());
        loadAnimatedGlyphs(glyphOutput.animatedGlyphs());
        glyphsByCharacterLength = glyphMap.values().stream()
                .sorted(Comparator.comparingInt((Glyph glyph) -> glyph.getCharacters().length()).reversed())
                .toList();

        if (fontConfiguration.isConfigurationSection("fonts"))
            loadFonts(fontConfiguration.getConfigurationSection("fonts"));

        Logs.logSuccess("Loaded " + (glyphMap.size() - referenceGlyphMap.size()) + " glyphs, " +
                referenceGlyphMap.size() + " reference glyphs, " +
                animatedGlyphMap.size() + " animated glyphs");
    }

    public static GlyphBitMap getGlyphBitMap(String id) {
        return id != null ? glyphBitMaps.getOrDefault(id, null) : null;
    }

    public void verifyRequired() {
        OraxenPlugin.get().saveResource("glyphs/required.yml", true);
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(fontEvents, OraxenPlugin.get());
        fontEvents.registerChatHandlers();
    }

    public void unregisterEvents() {
        HandlerList.unregisterAll(fontEvents);
        fontEvents.unregisterChatHandlers();
    }

    public FontEvents getFontEvents() {
        return fontEvents;
    }

    private void loadGlyphs(Collection<Glyph> glyphs) {
        verifyRequiredGlyphs();
        for (Glyph glyph : glyphs)
            registerGlyph(glyph, true);
    }

    private void registerGlyph(Glyph glyph, boolean registerUnicode) {
        if (glyph.getCharacter().isBlank())
            return;
        glyphMap.put(glyph.getName(), glyph);
        if (registerUnicode) {
            glyph.getCharacters().codePoints()
                    .mapToObj(Character::toString)
                    .forEach(character -> {
                        String existing = reverse.put(character, glyph.getName());
                        if (existing != null && !existing.equals(glyph.getName()))
                            Logs.logWarning("Character '" + character + "' claimed by both '" + existing + "' and '" + glyph.getName() + "'");
                    });
        }
        for (final String placeholder : glyph.getPlaceholders())
            glyphByPlaceholder.put(placeholder, glyph);
    }

    /**
     * Loads reference glyphs and resolves their source references.
     */
    private void loadReferenceGlyphs(List<ReferenceGlyph> referenceGlyphs) {
        for (ReferenceGlyph refGlyph : referenceGlyphs) {
            if (refGlyph.resolve(this)) {
                referenceGlyphMap.put(refGlyph.getName(), refGlyph);
                registerGlyph(refGlyph, false);
            }
        }
    }

    /**
     * Loads animated glyphs.
     * Note: GIF processing happens later during resource pack generation.
     */
    private void loadAnimatedGlyphs(List<AnimatedGlyph> animatedGlyphs) {
        for (AnimatedGlyph animGlyph : animatedGlyphs) {
            animatedGlyphMap.put(animGlyph.getName(), animGlyph);
            for (String placeholder : animGlyph.getPlaceholders()) {
                animatedByPlaceholder.put(placeholder, animGlyph);
            }
        }
    }

    private void loadFonts(final ConfigurationSection section) {
        for (final String fontName : section.getKeys(false)) {
            final ConfigurationSection fontSection = section.getConfigurationSection(fontName);
            fonts.add(new Font(fontSection.getString("type"),
                    fontSection.getString("file"),
                    (float) fontSection.getDouble("shift_x"),
                    (float) fontSection.getDouble("shift_y"),
                    (float) fontSection.getDouble("size"),
                    (float) fontSection.getDouble("oversample")));
        }
    }

    private void verifyRequiredGlyphs() {
        // Ensure required.yml exists (shifts.yml is deprecated - ShiftProvider handles
        // shifts dynamically)
        checkYamlKeys(new File(OraxenPlugin.get().getDataFolder() + "/glyphs/required.yml"));
    }

    private void checkYamlKeys(File file) {
        File tempFile = new File(OraxenPlugin.get().getDataFolder() + "/glyphs/temp.yml");
        try {
            Files.copy(Objects.requireNonNull(OraxenPlugin.get().getResource("glyphs/" + file.getName())),
                    tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (!file.exists()) {
                OraxenPlugin.get().saveResource("glyphs/" + file.getName(), false);
            }
            // Check if file is equal to the resource
            else if (!Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) {
                List<String> tempKeys = OraxenYaml.loadConfiguration(tempFile).getKeys(false).stream().toList();
                List<String> requiredKeys = OraxenYaml.loadConfiguration(file).getKeys(false).stream().toList();
                if (!new HashSet<>(requiredKeys).containsAll(tempKeys)) {
                    file.renameTo(new File(OraxenPlugin.get().getDataFolder() + "/glyphs/" + file.getName() + ".old"));
                    OraxenPlugin.get().saveResource("glyphs/" + file.getName(), true);
                    Logs.logWarning("glyphs/" + file.getName()
                            + " was incorrect, renamed to .old and regenerated the default one");
                }
            }
        } catch (IOException e) {
            file.renameTo(new File(OraxenPlugin.get().getDataFolder() + "/glyphs/" + file.getName() + ".old"));
            OraxenPlugin.get().saveResource("glyphs/" + file.getName(), true);
        }
        tempFile.delete();
    }

    /**
     * Gets all registered glyphs, including resolved {@link ReferenceGlyph}
     * instances.
     * <p>
     * Note: {@link ReferenceGlyph#toJson()} returns {@code null} since reference
     * glyphs reuse their source glyph's font provider; callers generating
     * resource-pack output must null-check {@code toJson()} or use
     * {@link #getBaseGlyphs()} instead.
     */
    public final Collection<Glyph> getGlyphs() {
        return glyphMap.values();
    }

    /**
     * Gets all registered glyphs sorted from longest to shortest character sequence.
     */
    public final List<Glyph> getGlyphsByCharacterLength() {
        return glyphsByCharacterLength;
    }

    /**
     * Gets all registered glyphs excluding {@link ReferenceGlyph} instances.
     */
    public final Collection<Glyph> getBaseGlyphs() {
        return glyphMap.values().stream().filter(glyph -> !(glyph instanceof ReferenceGlyph)).toList();
    }

    public final Collection<Glyph> getEmojis() {
        return glyphMap.values().stream().filter(Glyph::isEmoji).toList();
    }

    public final Collection<Font> getFonts() {
        return fonts;
    }

    public Font getFontFromFile(String file) {
        return getFonts().stream().filter(font -> font.file().equals(file)).findFirst().orElse(null);
    }

    // Reference glyph accessors

    /**
     * Gets all reference glyphs.
     */
    public Collection<ReferenceGlyph> getReferenceGlyphs() {
        return referenceGlyphMap.values();
    }

    /**
     * Gets a reference glyph by ID.
     */
    @Nullable
    public ReferenceGlyph getReferenceGlyphFromID(String id) {
        return referenceGlyphMap.get(id);
    }

    /**
     * Gets a reference glyph by placeholder.
     */
    @Nullable
    public ReferenceGlyph getReferenceGlyphFromPlaceholder(String placeholder) {
        Glyph glyph = glyphByPlaceholder.get(placeholder);
        return glyph instanceof ReferenceGlyph referenceGlyph ? referenceGlyph : null;
    }

    // Animated glyph accessors

    /**
     * Gets all animated glyphs.
     */
    public Collection<AnimatedGlyph> getAnimatedGlyphs() {
        return animatedGlyphMap.values();
    }

    /**
     * Gets an animated glyph by ID.
     */
    @Nullable
    public AnimatedGlyph getAnimatedGlyphFromID(String id) {
        return animatedGlyphMap.get(id);
    }

    /**
     * Gets an animated glyph by placeholder.
     */
    @Nullable
    public AnimatedGlyph getAnimatedGlyphFromPlaceholder(String placeholder) {
        return animatedByPlaceholder.get(placeholder);
    }

    /**
     * Get a Glyph from a given Glyph-ID
     * 
     * @param id The Glyph-ID
     * @return Returns the Glyph if it exists, otherwise the required Glyph
     */
    @NotNull
    public Glyph getGlyphFromName(final String id) {
        return glyphMap.get(id) != null ? glyphMap.get(id) : glyphMap.get("required");
    }

    /**
     * Get a Glyph from a given Glyph-ID
     * 
     * @param id The Glyph-ID
     * @return Returns the Glyph if it exists, otherwise null
     */
    @Nullable
    public Glyph getGlyphFromID(final String id) {
        return glyphMap.get(id);
    }

    public Glyph getGlyphFromPlaceholder(final String word) {
        return glyphByPlaceholder.get(word);
    }

    public Map<String, Glyph> getGlyphByPlaceholderMap() {
        return glyphByPlaceholder;
    }

    public Map<String, String> getReverseMap() {
        return reverse;
    }

    /**
     * Gets the ShiftProvider instance.
     *
     * @return The ShiftProvider for generating shift strings
     */
    public ShiftProvider getShiftProvider() {
        return shiftProvider;
    }

    /**
     * Gets a shift string for the specified pixel offset.
     * Uses the modern space font provider instead of legacy bitmap glyphs.
     *
     * @param length The pixel offset (positive = right, negative = left)
     * @return A string of shift characters from the shift font
     */
    public String getShift(int length) {
        return shiftProvider.getShiftString(length);
    }

    private final Map<UUID, List<String>> currentGlyphCompletions = new ConcurrentHashMap<>();

    public void sendGlyphTabCompletion(Player player) {
        List<String> completions = getGlyphTabCompletions(getGlyphByPlaceholderMap().values(),
                Settings.UNICODE_COMPLETIONS.toBool());

        player.removeCustomChatCompletions(
                currentGlyphCompletions.getOrDefault(player.getUniqueId(), new ArrayList<>()));
        player.addCustomChatCompletions(completions);
        currentGlyphCompletions.put(player.getUniqueId(), completions);
    }

    public void clearGlyphTabCompletions(Player player) {
        this.currentGlyphCompletions.remove(player.getUniqueId());
    }

    public static List<String> getGlyphTabCompletions(Collection<Glyph> glyphs, boolean includeUnicodeCompletions) {
        return glyphs.stream()
                .filter(Glyph::hasTabCompletion)
                .flatMap(glyph -> includeUnicodeCompletions
                        ? Stream.concat(Arrays.stream(glyph.getPlaceholders()), Stream.of(glyph.getCharacters()))
                        : Arrays.stream(glyph.getPlaceholders()))
                .filter(completion -> !completion.isEmpty())
                .distinct()
                .toList();
    }

    public record GlyphBitMap(String texture, int rows, int columns, int ascent, int height) {

        public JsonObject toJson(FontManager fontManager) {
            JsonObject json = new JsonObject();
            JsonArray chars = new JsonArray();

            List<Glyph> bitmapGlyphs = fontManager.getGlyphs().stream().filter(Glyph::hasBitmap)
                    .filter(g -> g.getBitMap() != null && g.getBitMap().equals(this)).toList();

            for (int i = 1; i <= rows(); i++) {
                int currentRow = i;
                List<Glyph> glyphsInRow = bitmapGlyphs.stream().filter(g -> g.getBitmapEntry().row() == currentRow)
                        .toList();
                StringBuilder charRow = new StringBuilder();
                for (int j = 1; j <= columns(); j++) {
                    int currentColumn = j;
                    Glyph glyph = glyphsInRow.stream().filter(g -> g.getBitmapEntry().column() == currentColumn)
                            .findFirst().orElse(null);
                    charRow.append(glyph != null ? glyph.getCharacter() : Glyph.WHITESPACE_GLYPH);
                }
                chars.add(""); // Add row
                chars.set(i - 1, new JsonPrimitive(charRow.toString()));
            }
            json.add("chars", chars);

            json.addProperty("type", "bitmap");
            json.addProperty("ascent", ascent);
            json.addProperty("height", height);
            json.addProperty("file", texture.endsWith(".png") ? texture : texture + ".png");

            return json;
        }
    }
}
