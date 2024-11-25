package io.th0rgal.oraxen.sound;

import io.th0rgal.oraxen.pack.generation.OraxenDatapack;
import io.th0rgal.oraxen.utils.VirtualFile;
import net.kyori.adventure.key.Key;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
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
        if (jukeboxSounds.isEmpty())
            return;

        datapackFolder.toPath().resolve("data/oraxen/jukebox_song").toFile().mkdirs();
        writeMCMeta();
        writeJukeboxSongs();
    }

    private void writeJukeboxSongs() {
        for (CustomSound sound : jukeboxSounds) {
            File songFile = datapackFolder.toPath()
                    .resolve("data/oraxen/jukebox_song/" + sound.getName() + ".json")
                    .toFile();

            try {
                songFile.createNewFile();
                FileUtils.writeStringToFile(songFile, sound.toJukeboxJson().toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}