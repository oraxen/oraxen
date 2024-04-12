package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class StringMechanicHelpers {

    public static void fixClientsideUpdate(Location loc) {
        List<Entity> players =
                Objects.requireNonNull(loc.getWorld()).getNearbyEntities(loc, 20, 20, 20)
                        .stream().filter(entity -> entity.getType() == EntityType.PLAYER).toList();

        for (double x = loc.getX() - 10; x < loc.getX() + 10; x++) {
            for (double y = loc.getY() - 4; y < loc.getY() + 4; y++) {
                for (double z = loc.getZ() - 10; z < loc.getZ() + 10; z++) {
                    if (loc.getBlock().getType() == Material.TRIPWIRE) {
                        Location newLoc = new Location(loc.getWorld(), x, y, z);
                        for (Entity e : players) {
                            ((Player) e).sendBlockChange(newLoc, newLoc.getBlock().getBlockData());
                        }
                    }
                }
            }
        }
    }
}
