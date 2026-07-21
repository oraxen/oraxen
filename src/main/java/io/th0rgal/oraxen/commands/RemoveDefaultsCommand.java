package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.ResourcesManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RemoveDefaultsCommand {

    OraxenCommand getRemoveDefaultsCommand() {
        OraxenCommand confirmed = new OraxenCommand("confirm")
                .withPermission("oraxen.command.remove-defaults")
                .executes((sender, args) -> {
                    removeDefaults(sender);
                });
        return new OraxenCommand("remove-defaults")
                .withPermission("oraxen.command.remove-defaults")
                .withSubcommand(confirmed)
                .executes((sender, args) -> {
                    Message.REMOVE_DEFAULTS_CONFIRM.send(sender);
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

        Path languageFolder = dataFolder.resolve("pack/lang");
        deletePath(languageFolder, true, Set.of(languageFolder.resolve("global.json")), deletedFiles, failedFiles);
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

    void deletePath(Path path, boolean keepRoot, Set<Path> excludedPaths, AtomicInteger deletedFiles,
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
