package io.th0rgal.oraxen.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.util.Entities;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.momirealms.antigrieflib.AbstractAntiGriefCompatibility;
import net.momirealms.antigrieflib.Flag;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class OraxenWorldGuardCompatibility extends AbstractAntiGriefCompatibility {

    private RegionContainer container;

    OraxenWorldGuardCompatibility(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        registerFlagTester(Flag.PLACE, (player, location) -> canBuild(player, location, Flags.BLOCK_PLACE));
        registerFlagTester(Flag.BREAK, (player, location) -> canBuild(player, location, Flags.BLOCK_BREAK));
        registerFlagTester(Flag.INTERACT, (player, location) -> canBuild(player, location, Flags.INTERACT));
        registerFlagTester(Flag.INTERACT_ENTITY, this::canInteractEntity);
        registerFlagTester(Flag.DAMAGE_ENTITY, this::canDamageEntity);
        registerFlagTester(Flag.OPEN_CONTAINER, (player, location) -> canBuild(player, location, Flags.CHEST_ACCESS));
        registerFlagTester(Flag.OPEN_DOOR, (player, location) -> canBuild(player, location, Flags.INTERACT));
        registerFlagTester(Flag.USE_BUTTON, (player, location) -> canBuild(player, location, Flags.INTERACT));
        registerFlagTester(Flag.USE_PRESSURE_PLATE, (player, location) -> canBuild(player, location, Flags.INTERACT));
    }

    private boolean canInteractEntity(Player player, Entity entity) {
        return canBuild(player, entity.getLocation(), Flags.INTERACT);
    }

    private boolean canDamageEntity(Player player, Entity entity) {
        StateFlag flag;
        if (entity instanceof Player) flag = Flags.PVP;
        else if (Entities.isHostile(entity)) flag = Flags.MOB_DAMAGE;
        else if (Entities.isNonHostile(entity)) flag = Flags.DAMAGE_ANIMALS;
        else flag = Flags.INTERACT;

        return canBuild(player, entity.getLocation(), flag);
    }

    private boolean canBuild(Player player, Location location, StateFlag flag) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        return hasBypass(player, localPlayer, location)
                || container.createQuery().testBuild(BukkitAdapter.adapt(location), localPlayer, flag);
    }

    private boolean hasBypass(Player player, LocalPlayer localPlayer, Location location) {
        World world = location.getWorld() != null ? location.getWorld() : player.getWorld();
        return WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(world));
    }
}
