package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.VersionUtil;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.packs.DataPack;

import com.google.gson.JsonObject;

import net.kyori.adventure.key.Key;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class OraxenDatapack {
    protected static final World defaultWorld = Bukkit.getWorlds().get(0);
    protected final File datapackFolder;
    protected final JsonObject datapackMeta = new JsonObject();
    protected final boolean isFirstInstall;
    protected final boolean datapackEnabled;
    protected final String name;

    protected OraxenDatapack(String name, String description, int packFormat) {
        this.datapackFolder = defaultWorld.getWorldFolder().toPath()
                .resolve("datapacks/" + name).toFile();

        JsonObject data = new JsonObject();
        data.addProperty("description", description);
        data.addProperty("pack_format", packFormat);
        datapackMeta.add("pack", data);

        this.name = name;
        this.isFirstInstall = isFirstInstall();
        this.datapackEnabled = isDatapackEnabled();
    }

    protected void writeMCMeta() {
        try {
            File packMeta = datapackFolder.toPath().resolve("pack.mcmeta").toFile();
            packMeta.createNewFile();
            FileUtils.writeStringToFile(packMeta, datapackMeta.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearOldDataPack() {
        try {
            FileUtils.deleteDirectory(datapackFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract Key getDatapackKey();

    public abstract void generateAssets(List<VirtualFile> output);

    protected boolean isFirstInstall() {
        return Bukkit.getDataPackManager().getDataPacks().stream()
                .filter(d -> d.getKey() != null)
                .noneMatch(d -> getDatapackKey().equals(Key.key(d.getKey().toString())));
    }

    protected boolean isDatapackEnabled() {
        for (DataPack dataPack : Bukkit.getDataPackManager().getEnabledDataPacks(defaultWorld)) {
            if (dataPack.getKey() == null)
                continue;
            if (dataPack.getKey().equals(getDatapackKey()))
                return true;
        }
        for (DataPack dataPack : Bukkit.getDataPackManager().getDisabledDataPacks(defaultWorld)) {
            if (dataPack.getKey() == null)
                continue;
            if (dataPack.getKey().equals(getDatapackKey()))
                return true;
        }
        return false;
    }

    protected void enableDatapack(boolean enabled) {
        if (VersionUtil.isPaperServer()) {
            Bukkit.getDatapackManager().getPacks().stream()
                    .filter(d -> d.getName() == this.name)
                    .findFirst()
                    .ifPresent(d -> d.setEnabled(enabled));
        }
    }

    public boolean isEnabled() {
        return datapackEnabled;
    }

    public boolean isFirstTime() {
        return isFirstInstall;
    }
}
