package io.th0rgal.oraxen.utils.platform;

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackManager;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class PaperBukkitWrapper extends BukkitWrapper {

    @Override
    public boolean isFirstInstall(Key datapackKey) {
        DatapackManager manager = Bukkit.getDatapackManager();
        for (Datapack pack : manager.getPacks()) {
            if (datapackKey.value().equals(pack.getName()))
                return false;
        }
        return true;
    }

    @Override
    public boolean isDatapackEnabled(Key datapackKey, World world) {
        DatapackManager manager = Bukkit.getDatapackManager();
        for (Datapack pack : manager.getPacks()) {
            if (datapackKey.value().equals(pack.getName()))
                return pack.isEnabled();
        }
        return false;
    }

    @Override
    public void setDatapackEnabled(String datapackName, boolean enabled) {
        DatapackManager manager = Bukkit.getDatapackManager();
        for (Datapack pack : manager.getPacks()) {
            // Paper exposes directory datapacks by their repository ID, including "file/".
            if (("file/" + datapackName).equals(pack.getName())) {
                pack.setEnabled(enabled);
                break;
            }
        }
    }
}
