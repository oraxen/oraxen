package io.th0rgal.oraxen.sounds;

import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.pack.generation.OraxenDatapack;
import io.th0rgal.oraxen.utils.VirtualFile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/** Generates the datapack fallback used before Paper's mutable jukebox registry. */
final class JukeboxDatapack extends OraxenDatapack {

    private static final Key DATAPACK_KEY = Key.key("minecraft:file/oraxen_jukebox");
    private final Collection<CustomSound> jukeboxSounds;

    JukeboxDatapack(Collection<CustomSound> jukeboxSounds) {
        super("oraxen_jukebox", "Datapack for Oraxen's Custom Jukebox Songs", 18);
        this.jukeboxSounds = jukeboxSounds;
    }

    @Override
    protected Key getDatapackKey() {
        return DATAPACK_KEY;
    }

    @Override
    public void generateAssets(List<VirtualFile> output) {
        clearOldDataPack();
        if (jukeboxSounds.isEmpty()) return;

        writeMCMeta();
        for (CustomSound sound : jukeboxSounds) writeJukeboxSong(sound);

        if (isFirstInstall || !datapackEnabled) {
            Message.DATAPACK_GENERATED.send(Bukkit.getConsoleSender(),
                    TagResolver.resolver(Placeholder.parsed("datapack_name", "Jukebox")));
        }
        enableDatapack(true);
    }

    private void writeJukeboxSong(CustomSound sound) {
        File songFile = datapackFolder.toPath().resolve("data").resolve(sound.getJukeboxSongNamespace())
                .resolve("jukebox_song").resolve(sound.getJukeboxSongKey() + ".json").toFile();
        try {
            FileUtils.forceMkdirParent(songFile);
            FileUtils.writeStringToFile(songFile, sound.toJukeboxJson().toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write jukebox song " + sound.getJukeboxSongId(), exception);
        }
    }
}
