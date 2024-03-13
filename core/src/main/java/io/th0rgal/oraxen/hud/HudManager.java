package io.th0rgal.oraxen.hud;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HudManager {

    public final int hudUpdateTime;
    public final NamespacedKey hudToggleKey;
    public final NamespacedKey hudDisplayKey;
    private final HudEvents hudEvents;
    private static HudTask hudTask;
    private static boolean hudTaskEnabled;
    private final Map<String, Hud> huds;

    public HudManager(final ConfigsManager hudManager) {
        final ConfigurationSection hudSection = getHudConfigSection();
        hudUpdateTime = hudManager.getHud().getInt("update_time_in_ticks", 40);
        hudEvents = new HudEvents();
        hudTaskEnabled = false;
        hudToggleKey = new NamespacedKey(OraxenPlugin.get(), "hud_toggle");
        hudDisplayKey = new NamespacedKey(OraxenPlugin.get(), "hud_display");
        huds = new HashMap<>();
        if (hudSection != null)
            loadHuds(hudSection);
    }

    public ConfigurationSection getHudConfigSection() {
        return OraxenPlugin.get().getConfigsManager().getHud().getConfigurationSection("huds");
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(hudEvents, OraxenPlugin.get());
    }

    public void unregisterEvents() {
        HandlerList.unregisterAll(hudEvents);
    }

    public void reregisterEvents() {
        unregisterEvents();
        registerEvents();
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

    public boolean hasActiveHud(Player player) {
        return !player.getPersistentDataContainer().getOrDefault(hudDisplayKey, DataType.STRING, "").isEmpty();
    }

    public Hud getActiveHud(Player player) {
        return huds.get(player.getPersistentDataContainer().get(hudDisplayKey, DataType.STRING));
    }

    public void setActiveHud(Player player, Hud hud) {
        player.getPersistentDataContainer().set(hudDisplayKey, PersistentDataType.STRING, getHudID(hud));
    }

    public boolean getHudState(Player player) {
        return player.getPersistentDataContainer().getOrDefault(hudToggleKey, DataType.BOOLEAN, true);
    }

    public void setHudState(Player player, boolean state) {
        player.getPersistentDataContainer().set(hudToggleKey, DataType.BOOLEAN, state);
    }

    public Collection<Hud> getDefaultEnabledHuds() {
        return huds.values().stream().filter(Hud::isEnabledByDefault).toList();
    }

    public void updateHud(final Player player) {
        enableHud(player, getActiveHud(player));
    }

    public void disableHud(final Player player) {
        OraxenPlugin.get().getAudience().player(player).sendActionBar(Component.empty());
    }

    public void enableHud(final Player player, Hud hud) {
        if (hud == null || hud.getDisplayText() == null || !getHudState(player)) return;

        String hudDisplay = parsedHudDisplays.get(hud);
        hudDisplay = translatePlaceholdersForHudDisplay(player, hudDisplay);
        hudDisplay = AdventureUtils.parseLegacy(hudDisplay);
        OraxenPlugin.get().getAudience().player(player).sendActionBar(AdventureUtils.MINI_MESSAGE.deserialize(hudDisplay));
    }

    public void registerTask() {
        if (hudTaskEnabled) return;
        if (hudTask != null) hudTask.cancel();
        if (hudUpdateTime == 0) return;
        if (huds.isEmpty()) return;

        hudTask = new HudTask();
        hudTask.runTaskTimer(OraxenPlugin.get(), 0, hudUpdateTime);
        hudTaskEnabled = true;
    }

    public void unregisterTask() {
        if (!hudTaskEnabled) return;
        if (hudTask != null) hudTask.cancel();
        hudTaskEnabled = false;
    }

    public void restartTask() {
        unregisterTask();
        registerTask();
    }

    public void loadHuds(final ConfigurationSection section) {
        if (section == null) return;
        for (final String hudName : section.getKeys(false)) {
            final ConfigurationSection hudSection = section.getConfigurationSection(hudName);
            if (hudSection == null || hudSection.getKeys(false).isEmpty()) continue;
            huds.clear();
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
            hudDisplays.put(entry.getValue(), translateMiniMessageTagsForHud(entry.getValue()));
        }
        return hudDisplays;
    }

    private String translateMiniMessageTagsForHud(Hud hud) {
        return AdventureUtils.parseMiniMessage(hud.getDisplayText());
    }

    private String translatePlaceholdersForHudDisplay(Player player, String message) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }
}
