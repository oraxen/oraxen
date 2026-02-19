package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.sound.CustomSound;
import io.th0rgal.oraxen.sound.JukeboxDatapack;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Generates sounds.json from SoundManager config and merges
 * with any existing sounds.json from imported packs.
 * Extracted from ResourcePack to reduce class size.
 */
class SoundGenerator {

    void generateSound(List<VirtualFile> output) {
        SoundManager soundManager = OraxenPlugin.get().getSoundManager();
        if (!soundManager.isAutoGenerate())
            return;

        List<VirtualFile> soundFiles = output.stream()
                .filter(file -> file.getPath().equals("assets/minecraft/sounds.json")).toList();
        JsonObject outputJson = new JsonObject();

        // If file was imported by other means, we attempt to merge in sound.yml entries
        for (VirtualFile soundFile : soundFiles) {
            if (soundFile != null) {
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
            }
            output.remove(soundFile);
        }

        Collection<CustomSound> customSounds = handleCustomSoundEntries(soundManager.getCustomSounds());

        // Add all sounds to the sounds.json
        for (CustomSound sound : customSounds) {
            outputJson.add(sound.getName(), sound.toJson());
        }

        InputStream soundInput = new ByteArrayInputStream(outputJson.toString().getBytes(StandardCharsets.UTF_8));
        output.add(new VirtualFile("assets/minecraft", "sounds.json", soundInput));
        try {
            soundInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize JukeboxDatapack with jukebox sounds after processing all sounds
        Collection<CustomSound> jukeboxSounds = customSounds.stream()
                .filter(CustomSound::isJukeboxSound)
                .toList();
        if (!jukeboxSounds.isEmpty()) {
            JukeboxDatapack jukeboxDatapack = new JukeboxDatapack(jukeboxSounds);
            jukeboxDatapack.clearOldDataPack();
            jukeboxDatapack.generateAssets(output);
        }
    }

    private Collection<CustomSound> handleCustomSoundEntries(Collection<CustomSound> sounds) {
        ConfigurationSection mechanic = OraxenPlugin.get().getConfigsManager().getMechanics();
        ConfigurationSection customSounds = mechanic.getConfigurationSection("custom_block_sounds");
        ConfigurationSection noteblock = mechanic.getConfigurationSection("noteblock");
        ConfigurationSection stringblock = mechanic.getConfigurationSection("stringblock");
        ConfigurationSection furniture = mechanic.getConfigurationSection("furniture");
        ConfigurationSection block = mechanic.getConfigurationSection("block");

        handleWoodSoundEntries(sounds, customSounds, noteblock, block);
        handleStoneSoundEntries(sounds, customSounds, stringblock, furniture);

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
     * @param configKey The config key to check in customSounds (e.g., "noteblock_and_block")
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
                s -> s.getName().startsWith("required." + soundPrefix) || s.getName().startsWith("block." + soundPrefix);

        if (customSounds == null) {
            sounds.removeIf(soundFilter);
            return;
        }

        if (!customSounds.getBoolean(configKey, true)) {
            sounds.removeIf(soundFilter);
        }

        if (section1 != null && !section1.getBoolean("enabled", section1EnabledDefault) &&
                section2 != null && !section2.getBoolean("enabled", section2EnabledDefault)) {
            sounds.removeIf(soundFilter);
        }
    }

    private void handleWoodSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            ConfigurationSection noteblock,
            ConfigurationSection block) {
        handleSoundEntries(sounds, customSounds, "wood", "noteblock_and_block", noteblock, true, block, false);
    }

    private void handleStoneSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            ConfigurationSection stringblock,
            ConfigurationSection furniture) {
        handleSoundEntries(sounds, customSounds, "stone", "stringblock_and_furniture", stringblock, true, furniture, true);
    }

    private void removeUnwantedSoundEntries(Collection<CustomSound> sounds) {
        sounds.removeIf(s -> s.getName().equals("required") ||
                s.getName().equals("block") ||
                s.getName().equals("block.wood") ||
                s.getName().equals("block.stone") ||
                s.getName().equals("required.wood") ||
                s.getName().equals("required.stone"));
    }
}
