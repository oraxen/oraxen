package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.sounds.CustomSound;
import io.th0rgal.oraxen.sounds.SoundManager;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Generates namespaced sounds.json files from SoundManager config and merges
 * with any existing sounds.json from imported packs.
 * Extracted from ResourcePack to reduce class size.
 */
class SoundGenerator {

    void generateSound(List<VirtualFile> output) {
        SoundManager soundManager = OraxenPlugin.get().getSoundManager();
        if (!soundManager.isAutoGenerate())
            return;

        List<VirtualFile> soundFiles = output.stream()
                .filter(file -> isSoundsJson(file.getPath())).toList();
        Map<String, JsonObject> outputJsons = new LinkedHashMap<>();

        // If sounds.json files were imported by other means, merge sounds.yml entries into them.
        for (VirtualFile soundFile : soundFiles) {
            String namespace = namespaceFromSoundsJsonPath(soundFile.getPath());
            JsonObject outputJson = outputJsons.computeIfAbsent(namespace, ignored -> new JsonObject());
            try {
                JsonElement soundElement = JsonParser
                        .parseString(IOUtils.toString(soundFile.getInputStream(), StandardCharsets.UTF_8));
                if (soundElement != null && soundElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : soundElement.getAsJsonObject().entrySet())
                        outputJson.add(entry.getKey(), entry.getValue());
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            output.remove(soundFile);
        }

        Collection<CustomSound> customSounds = handleCustomSoundEntries(soundManager.getCustomSounds());

        // Add all configured sounds to the sounds.json of their namespace.
        for (CustomSound sound : customSounds) {
            JsonObject outputJson = outputJsons.computeIfAbsent(sound.getNamespace(), ignored -> new JsonObject());
            outputJson.add(sound.getKey(), sound.toJson());
        }

        if (outputJsons.isEmpty())
            outputJsons.put("minecraft", new JsonObject());

        for (Map.Entry<String, JsonObject> entry : outputJsons.entrySet()) {
            InputStream soundInput = new ByteArrayInputStream(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
            output.add(new VirtualFile("assets/" + entry.getKey(), "sounds.json", soundInput));
            try {
                soundInput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Collection<CustomSound> handleCustomSoundEntries(Collection<CustomSound> sounds) {
        ConfigurationSection mechanic = OraxenPlugin.get().getConfigsManager().getMechanics();
        ConfigurationSection customSounds = mechanic.getConfigurationSection("custom_block_sounds");
        ConfigurationSection furniture = mechanic.getConfigurationSection("furniture");
        ConfigurationSection block = mechanic.getConfigurationSection("block");

        handleWoodSoundEntries(sounds, customSounds, block);
        handleStoneSoundEntries(sounds, customSounds, block, furniture);

        // Clear the sounds.json file of yaml configuration entries that should not be
        // there
        removeUnwantedSoundEntries(sounds);

        return sounds;
    }

    /**
     * Generic handler for sound entries with a specific material type.
     *
     * @param sounds The sound collection to filter
     * @param customSounds The custom block sounds config section
     * @param soundPrefix The sound prefix to filter (e.g., "wood" or "stone")
     * @param configKey The config key to check in customSounds
     * @param section1 First mechanic section to check
     * @param section1EnabledDefault Default enabled value for section1
     * @param section2 Second mechanic section to check
     * @param section2EnabledDefault Default enabled value for section2
     */
    private void handleSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            String soundPrefix,
            String configKey,
            ConfigurationSection section1,
            boolean section1EnabledDefault,
            ConfigurationSection section2,
            boolean section2EnabledDefault) {
        Predicate<CustomSound> soundFilter =
                s -> s.getNamespace().equals("minecraft")
                        && (s.getName().startsWith("required." + soundPrefix)
                        || s.getName().startsWith("block." + soundPrefix));

        if (customSounds == null) {
            sounds.removeIf(soundFilter);
            return;
        }

        if (!isCustomSoundEnabled(customSounds, configKey)) {
            sounds.removeIf(soundFilter);
        }

        boolean section1NeedsSounds = section1 == null
                ? section1EnabledDefault
                : section1.getBoolean("enabled", section1EnabledDefault);
        boolean section2NeedsSounds = section2 == null
                ? section2EnabledDefault
                : section2.getBoolean("enabled", section2EnabledDefault);
        if (!section1NeedsSounds && !section2NeedsSounds) {
            sounds.removeIf(soundFilter);
        }
    }

    private void handleWoodSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            ConfigurationSection block) {
        handleSoundEntries(sounds, customSounds, "wood", "block", block, true, null, false);
    }

    private void handleStoneSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            ConfigurationSection block,
            ConfigurationSection furniture) {
        Predicate<CustomSound> soundFilter =
                s -> s.getNamespace().equals("minecraft")
                        && (s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));

        if (customSounds == null) {
            sounds.removeIf(soundFilter);
            return;
        }

        boolean blockNeedsSounds = BlockSounds.isBlockSoundEnabled(customSounds)
                && (block == null || block.getBoolean("enabled", true));
        boolean furnitureNeedsSounds = BlockSounds.isFurnitureSoundEnabled(customSounds)
                && (furniture == null || furniture.getBoolean("enabled", true));

        if (!blockNeedsSounds && !furnitureNeedsSounds)
            sounds.removeIf(soundFilter);
    }

    private boolean isCustomSoundEnabled(ConfigurationSection customSounds, String configKey) {
        if ("block".equals(configKey)) return BlockSounds.isBlockSoundEnabled(customSounds);
        if ("furniture".equals(configKey)) return BlockSounds.isFurnitureSoundEnabled(customSounds);
        return customSounds.getBoolean(configKey, true);
    }

    private void removeUnwantedSoundEntries(Collection<CustomSound> sounds) {
        sounds.removeIf(s -> s.getNamespace().equals("minecraft") && (s.getName().equals("required") ||
                s.getName().equals("block") ||
                s.getName().equals("block.wood") ||
                s.getName().equals("block.stone") ||
                s.getName().equals("required.wood") ||
                s.getName().equals("required.stone")));
    }

    private boolean isSoundsJson(String path) {
        return path.startsWith("assets/") && path.endsWith("/sounds.json");
    }

    private String namespaceFromSoundsJsonPath(String path) {
        String normalized = path.replace('\\', '/');
        int namespaceStart = "assets/".length();
        int namespaceEnd = normalized.indexOf('/', namespaceStart);
        return namespaceEnd == -1 ? "minecraft" : normalized.substring(namespaceStart, namespaceEnd);
    }
}
