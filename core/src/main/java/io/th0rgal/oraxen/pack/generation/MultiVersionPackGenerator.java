package io.th0rgal.oraxen.pack.generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.ZipUtils;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Generates multiple versions of resource packs for different Minecraft client versions.
 * This allows servers to serve version-appropriate packs to players using ViaVersion/ProtocolSupport.
 */
public class MultiVersionPackGenerator {

    private final File packFolder;
    private final PackVersionManager versionManager;
    private final ResourcePack baseGenerator;

    public MultiVersionPackGenerator(File packFolder, ResourcePack baseGenerator) {
        this.packFolder = packFolder;
        this.versionManager = new PackVersionManager(packFolder);
        this.baseGenerator = baseGenerator;
    }

    /**
     * Generates multiple resource pack versions.
     * Each version will have a different pack.mcmeta with appropriate pack_format.
     *
     * @param output Virtual files to include in all pack versions
     */
    public void generateMultipleVersions(List<VirtualFile> output) {
        Logs.logInfo("Generating multi-version resource packs...");

        // Fire event to allow modifications before generation
        OraxenPackGeneratedEvent event = new OraxenPackGeneratedEvent(output);
        EventUtils.callEvent(event);
        output = event.getOutput();

        // Define which pack versions to generate
        versionManager.definePackVersions();

        // Set server pack version based on current server version
        MinecraftVersion currentVersion = MinecraftVersion.getCurrentVersion();
        versionManager.setServerPackVersion(currentVersion.getVersion());

        // Generate each pack version
        Collection<PackVersion> versions = versionManager.getAllVersions();
        for (PackVersion packVersion : versions) {
            try {
                generatePackVersion(packVersion, output);
            } catch (Exception e) {
                Logs.logError("Failed to generate pack for " + packVersion.getMinecraftVersion() + ": " + e.getMessage());
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
        }

        Logs.logSuccess("Generated " + versions.size() + " pack versions successfully");

        // Upload and send packs to players
        uploadAndSendPacks();
    }

    private void generatePackVersion(PackVersion packVersion, List<VirtualFile> output) throws IOException {
        Logs.logInfo("Generating pack for Minecraft " + packVersion.getMinecraftVersion() + " (format " + packVersion.getPackFormat() + ")");

        // Deep copy VirtualFiles to avoid shared InputStream consumption
        List<VirtualFile> versionOutput = new java.util.ArrayList<>();
        for (VirtualFile file : output) {
            // Skip any existing pack.mcmeta to avoid duplicates
            if (file.getPath().equals("pack.mcmeta") || file.getPath().endsWith("/pack.mcmeta")) {
                continue;
            }

            // Create deep copy with fresh InputStream
            try {
                byte[] content = org.apache.commons.io.IOUtils.toByteArray(file.getInputStream());
                java.io.ByteArrayInputStream freshStream = new java.io.ByteArrayInputStream(content);
                String path = file.getPath();
                String parentFolder = path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : "";
                String name = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
                versionOutput.add(new VirtualFile(parentFolder, name, freshStream));
            } catch (Exception e) {
                Logs.logWarning("Failed to copy VirtualFile " + file.getPath() + ": " + e.getMessage());
            }
        }

        // Create pack.mcmeta for this version
        File tempMcmeta = createPackMcmeta(packVersion);
        byte[] mcmetaContent = Files.readAllBytes(tempMcmeta.toPath());
        java.io.ByteArrayInputStream mcmetaStream = new java.io.ByteArrayInputStream(mcmetaContent);
        versionOutput.add(new VirtualFile("", "pack.mcmeta", mcmetaStream));

        // Generate the zip file
        File packFile = packVersion.getPackFile();
        ZipUtils.writeZipFile(packFile, versionOutput);

        Logs.logSuccess("  Created: " + packFile.getName() + " (" + formatFileSize(packFile.length()) + ")");

        // Clean up temp file
        tempMcmeta.delete();
    }

    private File createPackMcmeta(PackVersion packVersion) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();

        // Set description
        pack.addProperty("description", "§9§lOraxen §8| §7Extend the Game §7www§8.§7oraxen§8.§7com");

        // Set pack format
        pack.addProperty("pack_format", packVersion.getPackFormat());

        // Add supported_formats for broader compatibility (1.20.2+)
        // The supported_formats field was introduced in Minecraft 1.20.2 (pack format 18)
        if (packVersion.getPackFormat() >= 18) {
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", packVersion.getMinFormatInclusive());
            supportedFormats.addProperty("max_inclusive", packVersion.getMaxFormatInclusive());
            pack.add("supported_formats", supportedFormats);
        }

        root.add("pack", pack);

        // Write to temporary file
        File tempFile = new File(packFolder, "pack.mcmeta.tmp");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(tempFile.toPath(), gson.toJson(root), StandardCharsets.UTF_8);

        return tempFile;
    }

    private void uploadAndSendPacks() {
        SchedulerUtil.runTask(() -> {
            io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager uploadManager = OraxenPlugin.get().getMultiVersionUploadManager();
            if (uploadManager != null) {
                uploadManager.uploadAndSendToPlayers(versionManager, true, true);
            } else {
                uploadManager = new io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager(OraxenPlugin.get());
                OraxenPlugin.get().setMultiVersionUploadManager(uploadManager);
                uploadManager.uploadAndSendToPlayers(versionManager, false, false);
            }
        });
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public PackVersionManager getVersionManager() {
        return versionManager;
    }
}
