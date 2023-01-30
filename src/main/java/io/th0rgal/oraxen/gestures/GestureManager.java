package io.th0rgal.oraxen.gestures;

import com.ticxo.playeranimator.api.PlayerAnimator;
import com.ticxo.playeranimator.api.animation.pack.AnimationPack;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GestureManager {
    private final Map<Player, OraxenPlayerModel> gesturingPlayers;
    private final ConfigsManager configsManager;

    public GestureManager(ConfigsManager configsManager) {
        this.configsManager = configsManager;
        loadGestures();
        gesturingPlayers = new HashMap<>();

        Bukkit.getPluginManager().registerEvents(new GestureListener(this), OraxenPlugin.get());
    }

    public void playGesture(Player player, String gesture) {
        if (isPlayerGesturing(player)) return;
        OraxenPlayerModel model = new OraxenPlayerModel(player, QuitMethod.SNEAK, false, false);
        addPlayerToGesturing(player, model);
        model.playAnimation(gesture);

    }

    public void stopGesture(Player player) {
        OraxenPlayerModel model = getPlayerModel(player);
        if (model == null) return;
        Logs.debug("Removed");
        removePlayerFromGesturing(player);
        model.despawn();
    }

    public boolean isPlayerGesturing(Player player) {
        return gesturingPlayers.containsKey(player);
    }

    public OraxenPlayerModel getPlayerModel(Player player) {
        return gesturingPlayers.getOrDefault(player, null);
    }

    public void addPlayerToGesturing(Player player, OraxenPlayerModel gesture) {
        gesturingPlayers.put(player, gesture);
    }

    public void removePlayerFromGesturing(Player player) {
        gesturingPlayers.remove(player);
    }

    public void loadGestures() {
        PlayerAnimator.api.getAnimationManager().clearRegistry();
        gestures.clear();

        File gestureDir = new File(OraxenPlugin.get().getDataFolder().getPath() + "/gestures/");
        if (!gestureDir.exists()) return;
        File[] gestureFiles = gestureDir.listFiles();
        if (gestureFiles == null || gestureFiles.length == 0) return;
        gestureFiles = Arrays.stream(gestureFiles).filter(f -> f.getPath().endsWith(".bbmodel")).distinct().toArray(File[]::new);
        if (gestureFiles.length == 0) return;
        for (File animationFile : gestureFiles) {
            String animationKey = Utils.removeExtension(animationFile.getName());
            PlayerAnimator.api.getAnimationManager().importAnimations(animationKey, animationFile);
        }
        for (Map.Entry<String, AnimationPack> packEntry : PlayerAnimator.api.getAnimationManager().getRegistry().entrySet()) {
            Set<String> animationNames = packEntry.getValue().getAnimations().keySet().stream().map(animation -> packEntry.getKey().replace(":", ".") + "." + animation).collect(Collectors.toSet());
            gestures.addAll(animationNames);
        }
    }

    public static Set<String> gestures = new HashSet<>();

    public Set<String> getGestures() {
        return gestures;
    }

    public void reload() {
        for (Map.Entry<Player, OraxenPlayerModel> entry : gesturingPlayers.entrySet()) {
            stopGesture(entry.getKey());
            gesturingPlayers.remove(entry.getKey());
        }
        loadGestures();
    }
}
