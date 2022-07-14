package io.th0rgal.oraxen.font;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;


import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

public class FontManager {

    public final boolean autoGenerate;
    public final String permsChatcolor;
    private final Map<String, Glyph> glyphMap;
    private final Map<String, Glyph> glyphByPlaceholder;
    private final Map<Character, String> reverse;
    private final FontEvents fontEvents;
    private final Set<Font> fonts;
    private static FontManager instance = null;

    public FontManager(final ConfigsManager configsManager) {
        final Configuration fontConfiguration = configsManager.getFont();
        instance = this;
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

    public final Collection<Glyph> getEmojis() {
        return glyphMap.values().stream().filter(Glyph::isEmoji).toList();
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

    public static FontManager getInstance() {
        return instance;
    }


    public String getShift(int length) {
        // Ensure shifts.yml exists as it is required
        if (!Path.of(OraxenPlugin.get().getDataFolder() + "/glyphs/shifts.yml").toFile().exists())
            OraxenPlugin.get().saveResource("glyphs/shifts.yml", false);
        StringBuilder output = new StringBuilder();
        while (length > 0) {
            int biggestPower = Integer.highestOneBit(length);
            output.append(getGlyphFromName("shift_" + biggestPower).getCharacter());
            length -= biggestPower;
        }
        return output.toString();
    }

    public void sendGlyphTabCompletion(Player player, Boolean addPlayers) {
        for (Map.Entry<String, Glyph> entry : getGlyphByPlaceholderMap().entrySet()) {
            if (entry.getValue().hasTabCompletion()) {
                ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

                if (addPlayers) packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                else {
                    packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                }

                final WrappedGameProfile profile = new WrappedGameProfile(
                        UUID.randomUUID(), " " + entry.getValue().getCharacter());

                if (entry.getValue().getTabIconTexture() != null && entry.getValue().getTabIconSignature() != null)
                    profile.getProperties().put("textures",
                            new WrappedSignedProperty(
                                    "textures",
                                    entry.getValue().getTabIconTexture(),
                                    entry.getValue().getTabIconSignature()));

                PlayerInfoData data = new PlayerInfoData(profile
                        , 0, EnumWrappers.NativeGameMode.SPECTATOR,
                        WrappedChatComponent.fromText(""));

                List<PlayerInfoData> dataList = new ArrayList<>();
                dataList.add(data);
                packet.getPlayerInfoDataLists().write(0, dataList);

                protocolManager.sendServerPacket(player, packet);
            }
        }
    }
}
