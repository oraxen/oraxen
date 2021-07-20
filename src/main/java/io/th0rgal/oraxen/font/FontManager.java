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

    public FontManager(YamlConfiguration fontConfiguration) {
        autoGenerate = fontConfiguration.getBoolean("settings.automatically_generate");
        glyphMap = new HashMap<>();
        glyphByPlaceholder = new HashMap<>();
        reverse = new HashMap<>();
        fontEvents = new FontEvents(this);
        loadGlyphs(fontConfiguration.getConfigurationSection("glyphs"));
        miniMessagePlaceholders = createMiniPlaceholders();
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(fontEvents, OraxenPlugin.get());
    }

    public void unregisterEvents() {
        HandlerList.unregisterAll(fontEvents);
    }

    private void loadGlyphs(ConfigurationSection section) {
        for (String glyphName : section.getKeys(false)) {
            ConfigurationSection glyphSection = section.getConfigurationSection(glyphName);
            String[] placeholders = new String[0];
            String permission = null;
            if (glyphSection.isConfigurationSection("chat")) {
                ConfigurationSection chatSection = glyphSection.getConfigurationSection("chat");
                placeholders = chatSection.getStringList("placeholders").toArray(new String[0]);
                if (chatSection.isString("permission"))
                    permission = chatSection.getString("permission");
            }
            String texture = glyphSection.getString("texture");
            if (!texture.endsWith(".png"))
                texture += ".png";
            Glyph glyph = new Glyph(glyphName, (char) glyphSection.getInt("code"), texture,
                    glyphSection.getInt("ascent"), glyphSection.getInt("height"), permission, placeholders);
            glyphMap.put(glyphName, glyph);
            reverse.put(glyph.character(), glyphName);
            for (String placeholder : placeholders)
                glyphByPlaceholder.put(placeholder, glyph);
        }
    }

    public final Collection<Glyph> getGlyphs() {
        return glyphMap.values();
    }

    public Glyph getGlyphFromName(String name) {
        return glyphMap.get(name);
    }

    public Glyph getGlyphFromPlaceholder(String word) {
        return glyphByPlaceholder.get(word);
    }

    public Map<Character, String> getReverseMap() {
        return reverse;
    }

    private String[] createMiniPlaceholders() {
        List<String> placeholders = new ArrayList<>();
        for (Map.Entry<String, Glyph> entry : glyphMap.entrySet()) {
            placeholders.add("glyph:" + entry.getKey());
            placeholders.add(String.valueOf(entry.getValue().character()));
        }
        return placeholders.toArray(new String[0]);
    }

    public String[] getMiniMessagePlaceholders() {
        return miniMessagePlaceholders;
    }

}
