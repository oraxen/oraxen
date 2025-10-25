package io.th0rgal.oraxen.utils.platform;

import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.key.Key;
import org.bukkit.World;

public abstract class BukkitWrapper {

    private static BukkitWrapper INSTANCE;

    public static BukkitWrapper get() {
        if (INSTANCE == null) {
            INSTANCE = VersionUtil.isPaperServer() ? new PaperBukkitWrapper() : new SpigotBukkitWrapper();
        }
        return INSTANCE;
    }

    public abstract boolean isFirstInstall(Key datapackKey);

    public abstract boolean isDatapackEnabled(Key datapackKey, World world);

    public abstract void setDatapackEnabled(String datapackName, boolean enabled);
}
