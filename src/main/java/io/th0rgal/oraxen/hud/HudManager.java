package io.th0rgal.oraxen.hud;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import it.unimi.dsi.fastutil.Pair;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO Multi hud support
// Perhaps reading the previous huddisplaytext, scanning for any shift glyph
// Then get length of string excluding sait shift and getShift(shift + string.length())
// Would prob center it and then new shifts would adjust it from centerpoint
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

    public void setActiveHudForPlayer(Player player, Hud hud) {
        player.getPersistentDataContainer().set(hudDisplayKey, PersistentDataType.STRING, getHudID(hud));
    }

    public boolean getHudStateForPlayer(Player player) {
        return !Boolean.FALSE.equals(player.getPersistentDataContainer().get(hudToggleKey, DataType.BOOLEAN));
    }

    public void setHudStateForPlayer(Player player, boolean state) {
        player.getPersistentDataContainer().set(hudToggleKey, DataType.BOOLEAN, state);
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
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, player.getUniqueId(), changeFont(hudDisplay));
    }

    public void registerTask() {
        if (hudTaskEnabled) return;
        if (hudTask != null) hudTask.cancel();
        if (hudUpdateTime == 0) return;

        hudTask = new HudTask();
        hudTask.runTaskTimer(OraxenPlugin.get(), 0, hudUpdateTime);
        hudTaskEnabled = true;
    }

    private void loadHuds(final ConfigurationSection section) {
        for (final String hudName : section.getKeys(false)) {
            final ConfigurationSection hudSection = section.getConfigurationSection(hudName);
            if (hudSection == null) continue;
            huds.put(hudName, (new Hud(
                    hudSection.getString("display_text"),
                    hudSection.getString("text_font", "minecraft:default"),
                    hudSection.getString("permission", ""),
                    hudSection.getBoolean("disabled_whilst_in_water", false),
                    hudSection.getBoolean("enabled_by_default", false),
                    hudSection.getBoolean("enable_for_spectator_mode", false)
            )));
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
        if (message == null) return null;
        message = translateGlyphsForHudDisplay(message);
        message = translateShiftForHudDisplay(message);
        return message;
    }

    private String translatePlaceholdersForHudDisplay(Player player, String message) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }

    private BaseComponent[] changeFont(String message) {
        String regex = "<font:(.*?).+?(?=>)";
        Matcher matcher = Pattern.compile(regex).matcher(message);
        List<Pair<String, String>> fontStringMap = new ArrayList<>();
        String currFont;

        while (matcher.find()) {
            currFont = "minecraft:" + matcher.group().replaceFirst("<font:", "");
            String replaceMsg = message.replaceFirst(matcher.group(), "").replaceFirst(">", "");
            String temp = replaceMsg.contains("<font:") ? replaceMsg.substring(0, (replaceMsg.indexOf("<font:"))) : replaceMsg;
            message = replaceMsg.replaceFirst(temp, "");
            fontStringMap.add(Pair.of(temp, currFont));
        }

        if (fontStringMap.isEmpty()) return new ComponentBuilder(message).create();

        List<BaseComponent> componentList = new ArrayList<>();
        for (Pair<String, String> s : fontStringMap) {
            BaseComponent tempComp = new ComponentBuilder(s.first()).font(s.second()).getCurrentComponent();
            componentList.add(tempComp);
        }
        return componentList.toArray(new BaseComponent[0]);
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
