package io.th0rgal.oraxen.utils.platform;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.packs.DataPack;

public final class SpigotBukkitWrapper extends BukkitWrapper {

    @Override
    public boolean isFirstInstall(Key datapackKey) {
        try {
            return Bukkit.getDataPackManager().getDataPacks().stream()
                    .filter(d -> d.getKey() != null)
                    .noneMatch(d -> datapackKey.equals(Key.key(d.getKey().toString())));
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean isDatapackEnabled(Key datapackKey, World world) {
        try {
            for (DataPack dataPack : Bukkit.getDataPackManager().getEnabledDataPacks(world)) {
                if (dataPack.getKey() == null)
                    continue;
                if (dataPack.getKey().equals(datapackKey))
                    return true;
            }
            for (DataPack dataPack : Bukkit.getDataPackManager().getDisabledDataPacks(world)) {
                if (dataPack.getKey() == null)
                    continue;
                if (dataPack.getKey().equals(datapackKey))
                    return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public void setDatapackEnabled(String datapackName, boolean enabled) {
        // not available on spigot
    }
}
