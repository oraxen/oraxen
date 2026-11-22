package io.th0rgal.oraxen.studio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What this server last synced with Oraxen Studio, persisted in
 * {@code plugins/Oraxen/.studio/state.json} (dot-folders are never synced).
 * <p>
 * {@link #base} holds the SHA-256 of every tracked file as of the last successful
 * pull or push. Studio uses it to tell files that changed in Studio since then
 * (conflicts) from files that merely differ because this server changed them.
 */
public final class StudioSyncState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String workspaceId = "";
    private int rev = 0;
    @Nullable
    private Map<String, String> base = null;

    public String workspaceId() {
        return workspaceId;
    }

    public int rev() {
        return rev;
    }

    /** Hashes as of the last sync, or null if this server never synced with the workspace. */
    @Nullable
    public Map<String, String> base() {
        return base;
    }

    public void recordSync(String workspaceId, int rev, Map<String, String> base) {
        this.workspaceId = workspaceId;
        this.rev = rev;
        this.base = new LinkedHashMap<>(base);
    }

    public static Path file(Path dataFolder) {
        return dataFolder.resolve(".studio").resolve("state.json");
    }

    /** Loads the state for {@code workspaceId}; a state recorded for another workspace is ignored. */
    public static StudioSyncState load(Path dataFolder, String workspaceId) {
        Path file = file(dataFolder);
        if (Files.isRegularFile(file)) {
            try {
                StudioSyncState state = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), StudioSyncState.class);
                if (state != null && workspaceId.equals(state.workspaceId)) return state;
            } catch (IOException | JsonParseException ignored) {
                // Corrupt or unreadable state is treated as "never synced".
            }
        }
        StudioSyncState fresh = new StudioSyncState();
        fresh.workspaceId = workspaceId;
        return fresh;
    }

    @FunctionalInterface
    interface FileMover {
        void move(Path source, Path target, CopyOption... options) throws IOException;
    }

    public void save(Path dataFolder) throws IOException {
        save(dataFolder, Files::move);
    }

    void save(Path dataFolder, FileMover mover) throws IOException {
        Path file = file(dataFolder);
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling("state.json.tmp");
        Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
        try {
            mover.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            mover.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void clear(Path dataFolder) throws IOException {
        Files.deleteIfExists(file(dataFolder));
    }
}
