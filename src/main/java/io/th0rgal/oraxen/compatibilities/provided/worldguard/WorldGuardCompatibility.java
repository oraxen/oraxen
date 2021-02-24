package io.th0rgal.oraxen.compatibilities.provided.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WorldGuardCompatibility extends CompatibilityProvider<WorldGuardPlugin> {

    public WorldGuardCompatibility() {
    }

    public boolean cannotBreak(Player player, Block block) {
        LocalPlayer localPlayer = plugin.wrapPlayer(player);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        boolean canBypass = WorldGuard
                .getInstance()
                .getPlatform()
                .getSessionManager()
                .hasBypass(localPlayer, BukkitAdapter.adapt(player.getWorld()));
        return !canBypass && !query.testBuild(BukkitAdapter.adapt(block.getLocation()), localPlayer, Flags.BLOCK_BREAK)
                && !player.hasPermission("oraxen.worldguard.bypass") && !player.hasPermission("oraxen.worldguard.*");
    }

}
