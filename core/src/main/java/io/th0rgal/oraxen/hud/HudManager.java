package io.th0rgal.oraxen.hud;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
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
        parsedHudDisplays = new HashMap<>();
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
        player.sendActionBar(Component.empty());
    }

    public void enableHud(final Player player, Hud hud) {
        if (hud == null) {
            Logs.logWarning("[HUD] HUD is null for player " + player.getName());
            return;
        }
        if (hud.getDisplayText() == null) {
            Logs.logWarning("[HUD] HUD display text is null for player " + player.getName());
            return;
        }
        if (!getHudState(player)) {
            Logs.logWarning("[HUD] HUD is disabled for player " + player.getName());
            return;
        }

        String hudDisplay = parsedHudDisplays.get(hud);
        if (hudDisplay == null) {
            Logs.logWarning("[HUD] No parsed HUD display found for HUD with text: " + hud.getDisplayText());
            return;
        }
        hudDisplay = translatePlaceholdersForHudDisplay(player, hudDisplay);
        hudDisplay = AdventureUtils.parseLegacy(hudDisplay);
        
        try {
            var component = AdventureUtils.MINI_MESSAGE.deserialize(hudDisplay);
            player.sendActionBar(component);
        } catch (Exception e) {
            Logs.logWarning("[HUD] Failed to send actionbar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void registerTask() {
        if (hudTaskEnabled) {
            Logs.logWarning("[HUD] Task already enabled");
            return;
        }
        if (hudTask != null) hudTask.cancel();
        if (hudUpdateTime == 0) {
            Logs.logWarning("[HUD] hudUpdateTime is 0, skipping task registration");
            return;
        }
        if (huds.isEmpty()) {
            Logs.logInfo("No HUDs loaded, skipping task registration");
            return;
        }

        hudTask = new HudTask();
        hudTask.start(0, hudUpdateTime);
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
        if (section == null) {
            Logs.logWarning("[HUD] HUD section is null");
            return;
        }
        huds.clear();
        for (final String hudName : section.getKeys(false)) {
            final ConfigurationSection hudSection = section.getConfigurationSection(hudName);
            if (hudSection == null || hudSection.getKeys(false).isEmpty()) {
                Logs.logWarning("[HUD] Skipping empty HUD: " + hudName);
                continue;
            }
            String displayText = hudSection.getString("display_text");
            huds.put(hudName, (new Hud(
                    displayText,
                    hudSection.getString("text_font", "minecraft:default"),
                    hudSection.getString("permission", ""),
                    hudSection.getBoolean("disabled_whilst_in_water", false),
                    hudSection.getBoolean("enabled_by_default", false),
                    hudSection.getBoolean("enable_for_spectator_mode", false)
            )));
        }
        parsedHudDisplays = generateHudDisplays();
    }

    public Map<Hud, String> parsedHudDisplays;

    public Map<Hud, String> generateHudDisplays() {
        Map<Hud, String> hudDisplays = new HashMap<>();
        for (Map.Entry<String, Hud> entry : huds.entrySet()) {
            String parsed = translateMiniMessageTagsForHud(entry.getValue());
            hudDisplays.put(entry.getValue(), parsed);
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
