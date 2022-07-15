package io.th0rgal.oraxen.hud;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HudManager {

    public final int hudUpdateTime;
    public final NamespacedKey hudToggleKey;
    public final NamespacedKey hudDisplayKey;
    private final HudEvents hudEvents;
    private static HudTask hudTask;
    private static boolean hudTaskEnabled;
    private final Map<String, Hud> huds;

    public HudManager(final ConfigsManager hudManager) {
        final ConfigurationSection hudSection = hudManager.getHud().getConfigurationSection("huds");
        hudUpdateTime = hudManager.getHud().getInt("update_time_in_ticks", 40);
        hudEvents = new HudEvents();
        hudTaskEnabled = false;
        hudToggleKey = new NamespacedKey(OraxenPlugin.get(), "hud_toggle");
        hudDisplayKey = new NamespacedKey(OraxenPlugin.get(), "hud_display");
        huds = new HashMap<>();
        if (hudSection != null)
            loadHuds(hudSection);
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(hudEvents, OraxenPlugin.get());
    }

    public void unregisterEvents() {
        HandlerList.unregisterAll(hudEvents);
    }

    public final Map<String, Hud> getHuds() { return huds; }
    public Hud getHudFromID(final String id) {return huds.get(id);}

    public String getHudID(Hud hud) {
        for (Map.Entry<String, Hud> entry : huds.entrySet()) {
            if (entry.getValue().equals(hud)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Hud getActiveHudForPlayer(Player player) {
        return huds.get(player.getPersistentDataContainer().get(hudDisplayKey, DataType.STRING));
    }

    public boolean getHudStateForPlayer(Player player) {
        return !Boolean.FALSE.equals(player.getPersistentDataContainer().get(hudToggleKey, DataType.BOOLEAN));
    }

    public void setHudStateForPlayer(Player player, boolean state) {
        player.getPersistentDataContainer().set(hudToggleKey, DataType.BOOLEAN, state);
    }

    public void toggleHudForPlayer(Player player, boolean toggle) {
        player.getPersistentDataContainer().set(hudToggleKey, DataType.BOOLEAN, toggle);
    }

    public Collection<Hud> getDefaultEnabledHuds() {
        return huds.values().stream().filter(Hud::isEnabledByDefault).toList();
    }

    public void updateHud(final Player player) {
        enableHud(player, getActiveHudForPlayer(player));
    }

    public void disableHud(final Player player) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, player.getUniqueId());
    }

    public void enableHud(final Player player, Hud hud) {
        if (hud == null || hud.getDisplayText() == null || !getHudStateForPlayer(player)) return;

        String hudDisplay = parsedHudDisplays.get(hud);
        hudDisplay = translatePlaceholdersForHudDisplay(player, hudDisplay);
        BaseComponent[] component = new ComponentBuilder(hudDisplay).font(hud.getFont()).create();
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, player.getUniqueId(), component);
    }

    public void registerTask() {
        if (hudTaskEnabled) return;
        if (hudTask != null) hudTask.cancel();

        hudTask = new HudTask();
        hudTask.runTaskTimer(OraxenPlugin.get(), 0, hudUpdateTime);
        hudTaskEnabled = true;
    }

    private void loadHuds(final ConfigurationSection section) {
        for (final String hudName : section.getKeys(false)) {
            final ConfigurationSection hudSection = section.getConfigurationSection(hudName);
            if (hudSection == null) continue;
            huds.put(hudName, (new Hud(hudSection.getString("display"),
                    hudSection.getString("text_font", "minecraft:default"),
                    hudSection.getString("permission", ""),
                    hudSection.getBoolean("disabled_whilst_in_water", false),
                    hudSection.getBoolean("enabled_by_default", false),
                    hudSection.getObject("enabled_in_gamemodes", GameMode[].class, new GameMode[]{GameMode.SURVIVAL, GameMode.CREATIVE, GameMode.ADVENTURE}))
            ));
        }
    }

    public Map<Hud, String> parsedHudDisplays;

    public Map<Hud, String> generateHudDisplays() {
        Map<Hud, String> hudDisplays = new HashMap<>();
        for (Map.Entry<String, Hud> entry : huds.entrySet()) {
            hudDisplays.put(entry.getValue(), translateHudDisplay(entry.getValue()));
        }
        return hudDisplays;
    }

    private String translateHudDisplay(Hud hud) {
        String message = hud.getDisplayText();
        message = translateGlyphsForHudDisplay(message);
        message = translateShiftForHudDisplay(message);
        return message;
    }

    private String translatePlaceholdersForHudDisplay(Player player, String message) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }

    private String translateGlyphsForHudDisplay(String message) {
        for (Map.Entry<String, Glyph> entry : OraxenPlugin.get().getFontManager().getGlyphByPlaceholderMap().entrySet()) {
            message = message.replace("%oraxen_" + entry.getValue().getName() + "%", String.valueOf(entry.getValue().getCharacter()))
                    .replace("<glyph:" + entry.getValue().getName() + ">", String.valueOf(entry.getValue().getCharacter()));
        }
        return message;
    }

    private String translateShiftForHudDisplay(String message) {
        FontManager fontManager = OraxenPlugin.get().getFontManager();
        String regex = "<shift:(.*?).+?(?=>)";
        Matcher matcher = Pattern.compile(regex).matcher(message);
        String regex2 = "%oraxen_shift_(.*?).+?(?=%)";
        Matcher matcher2 = Pattern.compile(regex2).matcher(message);
        List<String> shifts = new ArrayList<>();
        while (matcher.find()) {
            shifts.add(matcher.group().replace("<shift:", ""));
        }
        while (matcher2.find()) {
            shifts.add(matcher2.group().replace("%oraxen_shift_", ""));
        }

        for (String shift : shifts) {
            message = message.replace("<shift:" + shift + ">", fontManager.getShift(Integer.parseInt(shift)));
            message = message.replace("%oraxen_shift_" + shift + "%", fontManager.getShift(Integer.parseInt(shift)));
        }
        return message;
    }
}
