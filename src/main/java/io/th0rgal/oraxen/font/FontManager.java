package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;

import java.util.*;

public class FontManager {

    public final boolean autoGenerate;
    public final String permsChatcolor;
    private final Map<String, Glyph> glyphMap;
    private final Map<String, Glyph> glyphByPlaceholder;
    private final Map<Character, String> reverse;
    private final FontEvents fontEvents;
    private final Set<Font> fonts;
    private final int lastCode = -1;

    public FontManager(final ConfigsManager configsManager) {
        final Configuration fontConfiguration = configsManager.getFont();
        autoGenerate = fontConfiguration.getBoolean("settings.automatically_generate");
        permsChatcolor = fontConfiguration.getString("settings.perms_chatcolor");
        glyphMap = new HashMap<>();
        glyphByPlaceholder = new HashMap<>();
        reverse = new HashMap<>();
        fontEvents = new FontEvents(this);
        fonts = new HashSet<>();
        loadGlyphs(configsManager.parseGlyphConfigs());
        if (fontConfiguration.isConfigurationSection("fonts"))
            loadFonts(fontConfiguration.getConfigurationSection("fonts"));
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(fontEvents, OraxenPlugin.get());
    }

    public void unregisterEvents() {
        HandlerList.unregisterAll(fontEvents);
    }

    private void loadGlyphs(Collection<Glyph> glyphs) {
        for (Glyph glyph : glyphs) {
            glyphMap.put(glyph.getName(), glyph);
            reverse.put(glyph.getCharacter(), glyph.getName());
            for (final String placeholder : glyph.getPlaceholders())
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

    public Map<String, Glyph> getGlyphByPlaceholderMap() {
        return glyphByPlaceholder;
    }

    public Map<Character, String> getReverseMap() {
        return reverse;
    }


    public String getShift(int length) {
        StringBuilder output = new StringBuilder();
        while (length > 0) {
            int biggestPower = Integer.highestOneBit(length);
            output.append(getGlyphFromName("shift_" + biggestPower).getCharacter());
            length -= biggestPower;
        }
        return output.toString();
    }

}
