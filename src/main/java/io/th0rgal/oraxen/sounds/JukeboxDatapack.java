package io.th0rgal.oraxen.sounds;

import io.th0rgal.oraxen.pack.generation.OraxenDatapack;
import io.th0rgal.oraxen.utils.VirtualFile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import io.th0rgal.oraxen.configs.Message;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class JukeboxDatapack extends OraxenDatapack {
    public static final Key DATAPACK_KEY = Key.key("minecraft:file/oraxen_jukebox");
    private final Collection<CustomSound> jukeboxSounds;

    public JukeboxDatapack(Collection<CustomSound> jukeboxSounds) {
        super("oraxen_jukebox",
                "Datapack for Oraxen's Custom Jukebox Songs",
                18); // Pack format for 1.20.4
        this.jukeboxSounds = jukeboxSounds;
    }

    @Override
    protected Key getDatapackKey() {
        return DATAPACK_KEY;
    }

    @Override
    public void generateAssets(List<VirtualFile> output) {
        if (jukeboxSounds.isEmpty()) {
            return;
        }

        writeMCMeta();
        writeJukeboxSongs();

        if (isFirstInstall || !datapackEnabled) {
            Message.DATAPACK_GENERATED.send(Bukkit.getConsoleSender(),
                    TagResolver.resolver(Placeholder.parsed("datapack_name", "Jukebox")));
        }

        enableDatapack(true);
    }

    private void writeJukeboxSongs() {
        for (CustomSound sound : jukeboxSounds) {
            File songFile = datapackFolder.toPath()
                    .resolve("data")
                    .resolve(sound.getJukeboxSongNamespace())
                    .resolve("jukebox_song")
                    .resolve(sound.getJukeboxSongKey() + ".json")
                    .toFile();

            try {
                FileUtils.forceMkdirParent(songFile);
                songFile.createNewFile();
                FileUtils.writeStringToFile(songFile, sound.toJukeboxJson().toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}