package io.th0rgal.oraxen.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RemoveDefaultsCommand {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final String GLOBAL_LANG_NOTICE = "This file is for editing all languages at once. To, edit a specific language, use the corresponding file in the 'languages' folder.";

    CommandAPICommand getRemoveDefaultsCommand() {
        return new CommandAPICommand("remove-defaults")
                .withPermission("oraxen.command.remove-defaults")
                .executes((sender, args) -> {
                    removeDefaults(sender);
                });
    }

    private void removeDefaults(CommandSender sender) {
        Settings.GENERATE_DEFAULT_CONFIGS.setValue(false);
        Settings.GENERATE_DEFAULT_ASSETS.setValue(false);
        Settings.RECEIVE_LOADED_SOUND.setValue(false);

        AtomicInteger deletedFiles = new AtomicInteger();
        AtomicInteger failedFiles = new AtomicInteger();
        Path dataFolder = OraxenPlugin.get().getDataFolder().toPath();

        deletePath(dataFolder.resolve("pack/textures/default"), false, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("pack/models/default"), false, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("pack/textures/animations"), false, deletedFiles, failedFiles);

        Path globalLang = dataFolder.resolve("pack/lang/global.json");
        deletePath(dataFolder.resolve("pack/lang"), true, Set.of(globalLang), deletedFiles, failedFiles);
        clearGlobalLang(globalLang, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("pack/font"), true, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("pack/sounds"), true, deletedFiles, failedFiles);
        deleteKnownDefaultFiles(dataFolder, "recipes", deletedFiles, failedFiles);
        deleteKnownDefaultFiles(dataFolder, "items", deletedFiles, failedFiles);

        deletePath(dataFolder.resolve("glyphs/animations.yml"), false, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("glyphs/chat_tags.yml"), false, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("glyphs/emoji.yml"), false, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("glyphs/animations"), false, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("glyphs/chat_tags"), false, deletedFiles, failedFiles);
        deletePath(dataFolder.resolve("glyphs/emoji"), false, deletedFiles, failedFiles);

        if (failedFiles.get() > 0) {
            Message.REMOVE_DEFAULTS_FAILED.send(sender,
                    AdventureUtils.tagResolver("deleted", String.valueOf(deletedFiles.get())),
                    AdventureUtils.tagResolver("failed", String.valueOf(failedFiles.get())));
            return;
        }

        Message.REMOVE_DEFAULTS_SUCCESS.send(sender,
                AdventureUtils.tagResolver("files", String.valueOf(deletedFiles.get())));
    }

    private void deleteKnownDefaultFiles(Path dataFolder, String folder, AtomicInteger deletedFiles,
            AtomicInteger failedFiles) {
        Set<Path> defaultFiles = new HashSet<>();
        ResourcesManager.browseJar(entry -> {
            String entryName = entry.getName();
            if (!entry.isDirectory() && entryName.startsWith(folder + "/"))
                defaultFiles.add(dataFolder.resolve(entryName));
        });

        for (Path defaultFile : defaultFiles)
            if (Files.exists(defaultFile))
                deleteFile(defaultFile, deletedFiles, failedFiles);
    }

    private void deletePath(Path path, boolean keepRoot, AtomicInteger deletedFiles, AtomicInteger failedFiles) {
        deletePath(path, keepRoot, Set.of(), deletedFiles, failedFiles);
    }

    private void deletePath(Path path, boolean keepRoot, Set<Path> excludedPaths, AtomicInteger deletedFiles,
            AtomicInteger failedFiles) {
        if (!Files.exists(path))
            return;

        try {
            if (Files.isRegularFile(path)) {
                if (!excludedPaths.contains(path)) {
                    Files.delete(path);
                    deletedFiles.incrementAndGet();
                }
                return;
            }

            if (!Files.isDirectory(path))
                return;

            try (Stream<Path> files = Files.walk(path)) {
                files.filter(file -> !keepRoot || !file.equals(path))
                        .filter(file -> !excludedPaths.contains(file))
                        .sorted(Comparator.reverseOrder())
                        .forEach(file -> deleteFile(file, deletedFiles, failedFiles));
            }
        } catch (IOException e) {
            failedFiles.incrementAndGet();
            Logs.logWarning("Failed to delete default path: " + path.getFileName());
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
        }
    }

    private void clearGlobalLang(Path globalLang, AtomicInteger updatedFiles, AtomicInteger failedFiles) {
        try {
            Files.createDirectories(globalLang.getParent());
            JsonObject json = Files.isRegularFile(globalLang) ? readJsonObject(globalLang) : new JsonObject();
            JsonObject clearedJson = new JsonObject();
            JsonElement notice = json.get("DO_NOT_ALTER_THIS_LINE");
            clearedJson.addProperty("DO_NOT_ALTER_THIS_LINE",
                    notice != null && notice.isJsonPrimitive() ? notice.getAsString() : GLOBAL_LANG_NOTICE);
            Files.writeString(globalLang, GSON.toJson(clearedJson) + System.lineSeparator(), StandardCharsets.UTF_8);
            updatedFiles.incrementAndGet();
        } catch (IOException e) {
            failedFiles.incrementAndGet();
            Logs.logWarning("Failed to clear default language entries from global.json");
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
        }
    }

    private JsonObject readJsonObject(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (content.isBlank())
            return new JsonObject();

        try {
            JsonElement element = JsonParser.parseString(content);
            if (element.isJsonObject())
                return element.getAsJsonObject();
        } catch (JsonSyntaxException ignored) {
        }

        Logs.logWarning("Replacing invalid JSON while clearing default language entries from global.json");
        return new JsonObject();
    }

    private void deleteFile(Path path, AtomicInteger deletedFiles, AtomicInteger failedFiles) {
        try {
            Files.deleteIfExists(path);
            deletedFiles.incrementAndGet();
        } catch (IOException e) {
            failedFiles.incrementAndGet();
            Logs.logWarning("Failed to delete default file: " + path.getFileName());
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
        }
    }
}
