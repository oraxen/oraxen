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
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class RemoveBrandingCommand {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    CommandAPICommand getRemoveBrandingCommand() {
        return new CommandAPICommand("remove-branding")
                .withPermission("oraxen.command.remove-branding")
                .executes((sender, args) -> {
                    removeBranding(sender);
                });
    }

    private void removeBranding(CommandSender sender) {
        Settings.GENERATE_DEFAULT_ASSETS.setValue(false);
        Settings.RECEIVE_LOADED_SOUND.setValue(false);

        Path langFolder = OraxenPlugin.get().getDataFolder().toPath().resolve("pack/lang");
        int updatedFiles;
        try {
            Files.createDirectories(langFolder);
            updatedFiles = updateExistingLangFiles(langFolder);
            if (updatedFiles == 0)
                updatedFiles = updateLangFile(langFolder.resolve("global.json"));
        } catch (IOException e) {
            Logs.logError("Failed to remove Oraxen branding from pack language files");
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
            Message.REMOVE_BRANDING_FAILED.send(sender);
            return;
        }

        Message.REMOVE_BRANDING_SUCCESS.send(sender, AdventureUtils.tagResolver("files", String.valueOf(updatedFiles)));
    }

    private int updateExistingLangFiles(Path langFolder) throws IOException {
        try (Stream<Path> files = Files.list(langFolder)) {
            return files
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .mapToInt(path -> {
                        try {
                            return updateLangFile(path);
                        } catch (IOException e) {
                            Logs.logWarning("Failed to update branding entries in " + path.getFileName());
                            if (Settings.DEBUG.toBool())
                                e.printStackTrace();
                            return 0;
                        }
                    })
                    .sum();
        }
    }

    private int updateLangFile(Path path) throws IOException {
        JsonObject lang = readLangFile(path);
        lang.addProperty("menu.game", "");
        lang.addProperty("menu.disconnect", "<gray>See you soon!");
        lang.addProperty("menu.returnToGame", "Back to the server");
        lang.addProperty("resourcePack.server.name", "Resource Pack");

        Files.writeString(path, GSON.toJson(lang) + System.lineSeparator(), StandardCharsets.UTF_8);
        return 1;
    }

    private JsonObject readLangFile(Path path) throws IOException {
        if (!Files.exists(path))
            return new JsonObject();

        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (content.isBlank())
            return new JsonObject();

        try {
            JsonElement element = JsonParser.parseString(content);
            if (element.isJsonObject())
                return element.getAsJsonObject();
        } catch (JsonSyntaxException ignored) {
        }

        Logs.logWarning("Replacing invalid JSON while removing branding from " + path.getFileName());
        return new JsonObject();
    }
}
