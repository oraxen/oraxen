package io.th0rgal.oraxen.font;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.packets.InventoryPacketListener;
import io.th0rgal.oraxen.font.packets.TitlePacketListener;
import io.th0rgal.oraxen.nms.GlyphHandlers;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
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
import java.util.stream.Stream;

public class FontManager {

    public final boolean autoGenerate;
    public final String permsChatcolor;
    public static Map<String, GlyphBitMap> glyphBitMaps = new HashMap<>();
    private final Map<String, Glyph> glyphMap;
    private final Map<String, Glyph> glyphByPlaceholder;
    private final Map<Character, String> reverse;
    private final FontEvents fontEvents;
    private final Set<Font> fonts;
    private boolean useNmsGlyphs;

    public FontManager(final ConfigsManager configsManager) {
        final Configuration fontConfiguration = configsManager.getFont();
        final ConfigurationSection bitmapSection = fontConfiguration.getConfigurationSection("bitmaps");
        autoGenerate = fontConfiguration.getBoolean("settings.automatically_generate");
        permsChatcolor = fontConfiguration.getString("settings.perms_chatcolor");
        if (bitmapSection != null) {
            glyphBitMaps = bitmapSection.getKeys(false).stream().collect(HashMap::new, (map, key) -> {
                final ConfigurationSection section = bitmapSection.getConfigurationSection(key);
                if (section != null) {
                    map.put(key, new GlyphBitMap(
                            section.getString("texture"), section.getInt("rows"), section.getInt("columns"),
                            section.getInt("ascent", 8), section.getInt("height", 8)));
                }
            }, HashMap::putAll);
        }
        glyphMap = new HashMap<>();
        glyphByPlaceholder = new HashMap<>();
        reverse = new HashMap<>();
        fontEvents = new FontEvents(this);
        fonts = new HashSet<>();
        loadGlyphs(configsManager.parseGlyphConfigs());
        if (fontConfiguration.isConfigurationSection("fonts"))
            loadFonts(fontConfiguration.getConfigurationSection("fonts"));

        useNmsGlyphs = GlyphHandlers.isNms() && NMSHandlers.getHandler() != null;
        if (VersionUtil.atOrAbove("1.20.5") && useNmsGlyphs) {
            Logs.logError("Oraxens NMS Glyph system is not working for 1.20.5...");
            useNmsGlyphs = false;
        } else if (useNmsGlyphs) {
            NMSHandlers.getHandler().glyphHandler().setupNmsGlyphs();
            Logs.logSuccess("Oraxens NMS Glyph system has been enabled!");
            Logs.logInfo("Disabling packet-based glyph systems", true);
            if (PluginUtils.isEnabled("ProtocolLib")){
                ProtocolLibrary.getProtocolManager().removePacketListener(new InventoryPacketListener());
                ProtocolLibrary.getProtocolManager().removePacketListener(new TitlePacketListener());
            }
        }
    }

    public boolean useNmsGlyphs() {
        return useNmsGlyphs;
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
        for (Glyph glyph : glyphs) {
            if (glyph.getCharacter().isBlank()) continue;
            glyphMap.put(glyph.getName(), glyph);
            reverse.put(glyph.getCharacter().charAt(0), glyph.getName());
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

    private void verifyRequiredGlyphs() {
        // Ensure shifts.yml exists as it is required
        checkYamlKeys(new File(OraxenPlugin.get().getDataFolder() + "/glyphs/shifts.yml"));
        checkYamlKeys(new File(OraxenPlugin.get().getDataFolder() + "/glyphs/required.yml"));
    }

    private void checkYamlKeys(File file) {
        File tempFile = new File(OraxenPlugin.get().getDataFolder() + "/glyphs/temp.yml");
        try {
            Files.copy(Objects.requireNonNull(OraxenPlugin.get().getResource("glyphs/" + file.getName())), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
                    Logs.logWarning("glyphs/" + file.getName() + " was incorrect, renamed to .old and regenerated the default one");
                }
            }
        } catch (IOException e) {
            file.renameTo(new File(OraxenPlugin.get().getDataFolder() + "/glyphs/" + file.getName() + ".old"));
            OraxenPlugin.get().saveResource("glyphs/" + file.getName(), true);
        }
        tempFile.delete();
    }

    public final Collection<Glyph> getGlyphs() {
        return glyphMap.values();
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

    /**
     * Get a Glyph from a given Glyph-ID
     * @param id The Glyph-ID
     * @return Returns the Glyph if it exists, otherwise the required Glyph
     */
    @NotNull
    public Glyph getGlyphFromName(final String id) {
        return glyphMap.get(id) != null ? glyphMap.get(id) : glyphMap.get("required");
    }

    /**
     * Get a Glyph from a given Glyph-ID
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

    public Map<Character, String> getReverseMap() {
        return reverse;
    }

    public String getShift(int length) {
        StringBuilder output = new StringBuilder();
        String prefix = "shift_";
        if (length < 0) {
            prefix = "neg_shift_";
            length = -length;
        }
        while (length > 0) {
            int biggestPower = Integer.highestOneBit(length);
            output.append(getGlyphFromName(prefix + biggestPower).getCharacter());
            length -= biggestPower;
        }
        return output.toString();
    }

    private final Map<UUID, List<String>> currentGlyphCompletions = new HashMap<>();
    public void sendGlyphTabCompletion(Player player) {
        List<String> completions = getGlyphByPlaceholderMap().values().stream()
                .filter(Glyph::hasTabCompletion)
                .flatMap(glyph -> Settings.UNICODE_COMPLETIONS.toBool()
                        ? Stream.of(glyph.getCharacter())
                        : Arrays.stream(glyph.getPlaceholders()))
                .toList();

        if (VersionUtil.atOrAbove("1.19.4")) {
            player.removeCustomChatCompletions(currentGlyphCompletions.getOrDefault(player.getUniqueId(), new ArrayList<>()));
            player.addCustomChatCompletions(completions);
            currentGlyphCompletions.put(player.getUniqueId(), completions);
        }
    }

    public void clearGlyphTabCompletions(Player player) {
        this.currentGlyphCompletions.remove(player.getUniqueId());
    }

    public record GlyphBitMap(String texture, int rows, int columns, int ascent, int height) {

        public JsonObject toJson(FontManager fontManager) {
            JsonObject json = new JsonObject();
            JsonArray chars = new JsonArray();

            List<Glyph> bitmapGlyphs = fontManager.getGlyphs().stream().filter(Glyph::hasBitmap).filter(g -> g.getBitMap() != null && g.getBitMap().equals(this)).toList();

            for (int i = 1; i <= rows(); i++) {
                int currentRow = i;
                List<Glyph> glyphsInRow = bitmapGlyphs.stream().filter(g -> g.getBitmapEntry().row() == currentRow).toList();
                StringBuilder charRow = new StringBuilder();
                for (int j = 1; j <= columns(); j++) {
                    int currentColumn = j;
                    Glyph glyph = glyphsInRow.stream().filter(g -> g.getBitmapEntry().column() == currentColumn).findFirst().orElse(null);
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
