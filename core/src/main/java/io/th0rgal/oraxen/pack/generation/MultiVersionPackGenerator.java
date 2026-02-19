package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.ZipUtils;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

/**
 * Generates multiple versions of resource packs for different Minecraft client versions.
 * This allows servers to serve version-appropriate packs to players using ViaVersion/ProtocolSupport.
 *
 * <h2>Architecture Overview</h2>
 *
 * <p>The multi-version pack system consists of several components:
 *
 * <ul>
 *   <li><b>MultiVersionPackGenerator</b> (this class) - Generates multiple pack ZIP files, each with
 *       a different pack_format in pack.mcmeta to target specific Minecraft versions.</li>
 *   <li><b>PackVersionManager</b> - Manages the mapping of Minecraft versions to pack formats
 *       and provides version lookup functionality.</li>
 *   <li><b>PackVersion</b> - Represents a single pack version with its format number and
 *       supported format range.</li>
 *   <li><b>ProtocolVersion</b> - Maps network protocol versions to pack formats and version strings.</li>
 *   <li><b>PlayerVersionDetector</b> - Detects player client versions using ViaVersion or ProtocolSupport APIs.</li>
 *   <li><b>MultiVersionPackSender</b> - Sends the appropriate pack version to each player based on their client.</li>
 *   <li><b>MultiVersionUploadManager</b> - Handles uploading all pack versions to the hosting provider.</li>
 * </ul>
 *
 * <h2>Flow</h2>
 *
 * <ol>
 *   <li>ResourcePack.generate() checks if multi-version mode is enabled</li>
 *   <li>If enabled, MultiVersionPackGenerator.generateMultipleVersions() is called</li>
 *   <li>VirtualFiles are materialized to byte arrays to allow reuse across multiple packs</li>
 *   <li>For each target Minecraft version, a pack ZIP is created with appropriate pack.mcmeta</li>
 *   <li>MultiVersionUploadManager uploads all pack versions to the hosting provider</li>
 *   <li>MultiVersionPackSender registers to listen for player joins</li>
 *   <li>When a player joins, their client version is detected and the appropriate pack is sent</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 *
 * <p>Enable multi-version packs in settings.yml:
 * <pre>
 * Pack:
 *   generation:
 *     multi_version_packs: true
 *   upload:
 *     type: polymath  # self-host is not supported for multi-version
 * </pre>
 *
 * @see PackVersionManager
 * @see PackVersion
 * @see ProtocolVersion
 * @see PlayerVersionDetector
 * @see io.th0rgal.oraxen.pack.dispatch.MultiVersionPackSender
 * @see io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager
 */
public class MultiVersionPackGenerator {

    private final File packFolder;
    private final PackVersionManager versionManager;

    public MultiVersionPackGenerator(File packFolder) {
        this.packFolder = packFolder;
        this.versionManager = new PackVersionManager(packFolder);
    }

    /**
     * Generates multiple resource pack versions.
     * Each version will have a different pack.mcmeta with appropriate pack_format.
     *
     * @param output Virtual files to include in all pack versions
     * @param switchingFromSinglePack true if switching from single-pack mode (treat as reload)
     */
    public void generateMultipleVersions(List<VirtualFile> output, boolean switchingFromSinglePack) {
        Logs.logInfo("Generating multi-version resource packs...");

        // Fire event to allow modifications before generation
        OraxenPackGeneratedEvent event = new OraxenPackGeneratedEvent(output);
        EventUtils.callEvent(event);
        output = event.getOutput();

        // Define which pack versions to generate
        versionManager.definePackVersions();

        // Set server pack version based on current server version
        // MinecraftVersion.getVersion() returns "1.21.0" format directly
        String versionString = MinecraftVersion.getCurrentVersion().getVersion();
        versionManager.setServerPackVersion(versionString);

        // Materialize all VirtualFile InputStreams to byte arrays ONCE
        // This prevents stream consumption issues when reusing files across multiple pack versions
        List<MaterializedFile> materializedFiles = new java.util.ArrayList<>();
        JsonObject originalMcmeta = null;
        for (VirtualFile file : output) {
            // Skip pack.mcmeta as each version needs its own, but preserve user customizations
            if (file.getPath().equals("pack.mcmeta") || file.getPath().endsWith("/pack.mcmeta")) {
                try {
                    byte[] mcmetaBytes = org.apache.commons.io.IOUtils.toByteArray(file.getInputStream());
                    String mcmetaContent = new String(mcmetaBytes, StandardCharsets.UTF_8);
                    originalMcmeta = JsonParser.parseString(mcmetaContent).getAsJsonObject();
                } catch (Exception e) {
                    Logs.logWarning("Failed to read pack.mcmeta: " + e.getMessage());
                }
                continue;
            }

            try {
                byte[] content = org.apache.commons.io.IOUtils.toByteArray(file.getInputStream());
                materializedFiles.add(new MaterializedFile(file.getPath(), content));
            } catch (Exception e) {
                Logs.logWarning("Failed to read VirtualFile " + file.getPath() + ": " + e.getMessage());
            }
        }

        // Generate each pack version
        Collection<PackVersion> versions = versionManager.getAllVersions();
        int successCount = 0;
        for (PackVersion packVersion : versions) {
            try {
                generatePackVersion(packVersion, materializedFiles, originalMcmeta);
                successCount++;
            } catch (Exception e) {
                Logs.logError("Failed to generate pack for " + packVersion.getMinecraftVersion() + ": " + e.getMessage());
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
        }

        if (successCount == versions.size()) {
            Logs.logSuccess("Generated " + successCount + " pack versions successfully");
        } else {
            Logs.logWarning("Generated " + successCount + "/" + versions.size() + " pack versions (" + (versions.size() - successCount) + " failed)");
        }

        // Upload and send packs to players
        uploadAndSendPacks(switchingFromSinglePack);
    }

    private void generatePackVersion(PackVersion packVersion, List<MaterializedFile> materializedFiles, JsonObject originalMcmeta) throws IOException {
        Logs.logInfo("Generating pack for Minecraft " + packVersion.getMinecraftVersion() + " (format " + packVersion.getPackFormat() + ")");

        // Create fresh VirtualFiles for this pack version
        List<VirtualFile> versionOutput = new java.util.ArrayList<>();
        for (MaterializedFile mFile : materializedFiles) {
            // Create fresh InputStream for this pack version
            java.io.ByteArrayInputStream freshStream = new java.io.ByteArrayInputStream(mFile.content);
            String path = mFile.path;
            String parentFolder = path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : "";
            String name = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
            versionOutput.add(new VirtualFile(parentFolder, name, freshStream));
        }

        // Create pack.mcmeta for this version (preserving user customizations from the original)
        File tempMcmeta = createPackMcmeta(packVersion, originalMcmeta);
        try {
            byte[] mcmetaContent = Files.readAllBytes(tempMcmeta.toPath());
            java.io.ByteArrayInputStream mcmetaStream = new java.io.ByteArrayInputStream(mcmetaContent);
            versionOutput.add(new VirtualFile("", "pack.mcmeta", mcmetaStream));

            // Generate the zip file
            File packFile = packVersion.getPackFile();
            ZipUtils.writeZipFile(packFile, versionOutput);

            Logs.logSuccess("  Created: " + packFile.getName() + " (" + formatFileSize(packFile.length()) + ")");
        } finally {
            // Always clean up temp file, even if exception occurs
            tempMcmeta.delete();
        }
    }

    private File createPackMcmeta(PackVersion packVersion, JsonObject originalMcmeta) throws IOException {
        JsonObject mcmeta = PackMcmetaUtils.createPackMcmeta(
            packVersion.getPackFormat(),
            packVersion.getMinFormatInclusive(),
            packVersion.getMaxFormatInclusive(),
            originalMcmeta
        );

        File tempFile = new File(packFolder, "pack.mcmeta." + packVersion.getFileIdentifier() + ".tmp");
        PackMcmetaUtils.writePackMcmeta(tempFile.toPath(), mcmeta);

        return tempFile;
    }

    private void uploadAndSendPacks(boolean switchingFromSinglePack) {
        SchedulerUtil.runTask(() -> {
            io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager uploadManager = OraxenPlugin.get().getMultiVersionUploadManager();
            // Detect reload: either the multi-version manager already existed (re-generation),
            // or we're switching from single-pack mode (which cleared the old manager before we got here).
            boolean isReload = uploadManager != null || switchingFromSinglePack;
            if (uploadManager == null) {
                uploadManager = new io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager(OraxenPlugin.get());
                OraxenPlugin.get().setMultiVersionUploadManager(uploadManager);
            }
            uploadManager.uploadAndSendToPlayers(versionManager, isReload, isReload);
        });
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Helper class to hold materialized file data (path and content bytes).
     * Used to prevent InputStream consumption issues when generating multiple pack versions.
     */
    private static class MaterializedFile {
        final String path;
        final byte[] content;

        MaterializedFile(String path, byte[] content) {
            this.path = path;
            this.content = content;
        }
    }
}
