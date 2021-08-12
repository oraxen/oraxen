package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;

import java.util.*;

public class FontManager {

    public final boolean autoGenerate;
    private final Map<String, Glyph> glyphMap;
    private final Map<String, Glyph> glyphByPlaceholder;
    private final Map<Character, String> reverse;
    private final FontEvents fontEvents;
    private final String[] miniMessagePlaceholders;
    private final String[] zipPlaceholders;
    private final Set<Font> fonts;

    public FontManager(final YamlConfiguration fontConfiguration) {
        autoGenerate = fontConfiguration.getBoolean("settings.automatically_generate");
        glyphMap = new HashMap<>();
        glyphByPlaceholder = new HashMap<>();
        reverse = new HashMap<>();
        fontEvents = new FontEvents(this);
        fonts = new HashSet<>();
        if (fontConfiguration.isConfigurationSection("glyphs"))
            loadGlyphs(fontConfiguration.getConfigurationSection("glyphs"));
        if (fontConfiguration.isConfigurationSection("fonts"))
            loadFonts(fontConfiguration.getConfigurationSection("fonts"));
        miniMessagePlaceholders = createMiniPlaceholders();
        zipPlaceholders = createZipPlaceholders();
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(fontEvents, OraxenPlugin.get());
    }

    public void unregisterEvents() {
        HandlerList.unregisterAll(fontEvents);
    }

    private void loadGlyphs(final ConfigurationSection section) {
        for (final String glyphName : section.getKeys(false)) {
            final ConfigurationSection glyphSection = section.getConfigurationSection(glyphName);
            String[] placeholders = new String[0];
            String permission = null;
            if (glyphSection.isConfigurationSection("chat")) {
                final ConfigurationSection chatSection = glyphSection.getConfigurationSection("chat");
                placeholders = chatSection.getStringList("placeholders").toArray(new String[0]);
                if (chatSection.isString("permission"))
                    permission = chatSection.getString("permission");
            }
            String texture = glyphSection.getString("texture");
            if (!texture.endsWith(".png"))
                texture += ".png";
            final Glyph glyph = new Glyph(glyphName, (char) glyphSection.getInt("code"), texture,
                    glyphSection.getInt("ascent"), glyphSection.getInt("height"), permission, placeholders);
            glyphMap.put(glyphName, glyph);
            reverse.put(glyph.character(), glyphName);
            for (final String placeholder : placeholders)
                glyphByPlaceholder.put(placeholder, glyph);
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
                    (float) fontSection.getDouble("oversample")
            ));
        }
    }

    public final Collection<Glyph> getGlyphs() {
        return glyphMap.values();
    }

    public final Collection<Font> getFonts() {
        return fonts;
    }

    public Glyph getGlyphFromName(final String name) {
        return glyphMap.get(name);
    }

    public Glyph getGlyphFromPlaceholder(final String word) {
        return glyphByPlaceholder.get(word);
    }

    public Map<Character, String> getReverseMap() {
        return reverse;
    }

    private String[] createMiniPlaceholders() {
        final List<String> placeholders = new ArrayList<>();
        for (final Map.Entry<String, Glyph> entry : glyphMap.entrySet()) {
            placeholders.add("glyph:" + entry.getKey());
            placeholders.add(String.valueOf(entry.getValue().character()));
        }
        return placeholders.toArray(new String[0]);
    }

    public String[] getMiniMessagePlaceholders() {
        return miniMessagePlaceholders;
    }

    private String[] createZipPlaceholders() {
        final List<String> placeholders = new ArrayList<>();
        for (final Map.Entry<String, Glyph> entry : glyphMap.entrySet()) {
            placeholders.add("<glyph:" + entry.getKey() + ">");
            placeholders.add(entry.getValue().getHexcode());
        }
        return placeholders.toArray(new String[0]);
    }


    public String[] getZipPlaceholders() {
        return zipPlaceholders;
    }

}
