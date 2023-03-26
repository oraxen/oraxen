package io.th0rgal.oraxen.font;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

public class FontManager {

    public final boolean autoGenerate;
    public final String permsChatcolor;
    private final Map<String, Glyph> glyphMap;
    private final Map<String, Glyph> glyphByPlaceholder;
    private final Map<Character, String> reverse;
    private final FontEvents fontEvents;
    private final Set<Font> fonts;
    private final ProtocolManager protocolManager = OraxenPlugin.get().getProtocolManager();

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

    public void verifyRequired() {
        OraxenPlugin.get().saveResource("glyphs/required.yml", true);
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(fontEvents, OraxenPlugin.get());
    }

    public void unregisterEvents() {
        HandlerList.unregisterAll(fontEvents);
    }

    private void loadGlyphs(Collection<Glyph> glyphs) {
        verifyRequiredGlyphs();
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
            else if (Settings.AUTOMATICALLY_SET_GLYPH_CODE.toBool()) {
                List<String> tempKeys = YamlConfiguration.loadConfiguration(tempFile).getKeys(false).stream().toList();
                List<String> requiredKeys = YamlConfiguration.loadConfiguration(file).getKeys(false).stream().toList();
                if (!new HashSet<>(requiredKeys).containsAll(tempKeys)) {
                    file.renameTo(new File(OraxenPlugin.get().getDataFolder() + "/glyphs/" + file.getName() + ".old"));
                    OraxenPlugin.get().saveResource("glyphs/" + file.getName(), true);
                    Logs.logWarning("glyphs/" + file.getName() +  " was incorrect, renamed to .old and regenerated the default one");
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

    public Glyph getGlyphFromName(final String name) {
        return glyphMap.get(name) != null ? glyphMap.get(name) : glyphMap.get("required");
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

    public void sendGlyphTabCompletion(Player player, boolean add) {
        boolean useUnicodeCompletions = Settings.UNICODE_COMPLETIONS.toBool();
        if (PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS.isSupported()) {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS);

            Object[] constants = PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS.getPacketClass().getDeclaredClasses()[0].getEnumConstants();
            packet.getModifier().write(0, constants[(add) ? 0 : 1]);
            packet.getModifier().write(1, getGlyphByPlaceholderMap().values().stream()
                    .filter(Glyph::hasTabCompletion)
                    .flatMap(glyph -> useUnicodeCompletions
                            ? Stream.of(String.valueOf(glyph.getCharacter()))
                            : Arrays.stream(glyph.getPlaceholders()))
                    .toList());

            protocolManager.sendServerPacket(player, packet);
        }
        else for (Glyph glyph : getGlyphByPlaceholderMap().values()) {
            if (glyph.hasTabCompletion()) {
                PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);

                packet.getPlayerInfoAction().write(0, (add) ? EnumWrappers.PlayerInfoAction.ADD_PLAYER : EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

                List<WrappedGameProfile> profiles = useUnicodeCompletions
                        ? Collections.singletonList(getGameProfileForCompletion(String.valueOf(glyph.getCharacter())))
                        : Arrays.stream(glyph.getPlaceholders())
                        .map(this::getGameProfileForCompletion)
                        .toList();

                if (glyph.getTabIconTexture() != null && glyph.getTabIconSignature() != null) {
                    for (WrappedGameProfile profile : profiles) {
                        profile.getProperties().put("textures",
                                new WrappedSignedProperty(
                                        "textures",
                                        glyph.getTabIconTexture(),
                                        glyph.getTabIconSignature()));
                    }
                }

                packet.getPlayerInfoDataLists().write(0, profiles.stream()
                        .map(profile -> new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SPECTATOR, WrappedChatComponent.fromText("")))
                        .toList());

                protocolManager.sendServerPacket(player, packet);
            }
        }
    }

    private WrappedGameProfile getGameProfileForCompletion(String completion) {
        return new WrappedGameProfile(UUID.randomUUID(), completion);
    }
}
