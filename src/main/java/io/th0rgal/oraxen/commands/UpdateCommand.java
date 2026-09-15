package io.th0rgal.oraxen.commands;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.commands.arguments.EntitySelectorArgument;
import io.th0rgal.oraxen.commands.arguments.IntegerArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ORIENTATION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ROOT_KEY;

public class UpdateCommand {

    // Keeps the scanned area within the chunks Folia guarantees to be owned by
    // the region around the invoking player, and bounds the per-command work.
    private static final int MAX_RADIUS = 32;

    OraxenCommand getUpdateCommand() {
        return new OraxenCommand("update")
                .withPermission("oraxen.command.update")
                .withSubcommands(getFurnitureUpdateCommand(), getItemUpdateCommand());
    }

    @SuppressWarnings("unchecked")
    private OraxenCommand getItemUpdateCommand() {
        return new OraxenCommand("item")
                .withArguments(new EntitySelectorArgument.ManyEntities("targets"))
                .executesPlayer((player, args) -> {
                    final Collection<Player> targets = ((Collection<Entity>) args.get("targets")).stream().filter(entity -> entity instanceof Player).map(e -> (Player) e).toList();
                    // Inventory mutation must happen on the thread owning each target (Folia).
                    for (Player p : targets) {
                        SchedulerUtil.runForEntity(p, () -> {
                            int updated = 0;
                            for (int i = 0; i < p.getInventory().getSize(); i++) {
                                final ItemStack oldItem = p.getInventory().getItem(i);
                                final ItemStack newItem = ItemUpdater.updateItem(oldItem);
                                if (oldItem == null || oldItem.equals(newItem)) continue;
                                p.getInventory().setItem(i, newItem);
                                updated++;
                            }
                            final int updatedCount = updated;
                            final Component targetName = p.displayName();
                            // The invoker may be owned by a different region than the
                            // target; feedback must run on the invoker's owning thread.
                            SchedulerUtil.runForEntity(player, () -> Message.UPDATED_ITEMS.send(player,
                                    AdventureUtils.tagResolver("amount", String.valueOf(updatedCount)),
                                    AdventureUtils.tagResolver("player", targetName)));
                        });
                    }
                });
    }

    private OraxenCommand getFurnitureUpdateCommand() {
        return new OraxenCommand("furniture")
                .withOptionalArguments(new IntegerArgument("radius"))
                .executesPlayer((player, args) -> {
                    int radius = Math.min((int) args.getOptional("radius").orElse(10), MAX_RADIUS);
                    final Collection<Entity> targets = player.getNearbyEntities(radius, radius, radius).stream().filter(OraxenFurniture::isBaseEntity).toList();
                    // Entity mutation belongs on each entity's owning thread (Folia).
                    for (Entity entity : targets) SchedulerUtil.runForEntity(entity, () -> OraxenFurniture.updateFurniture(entity));
                    cleanupOrphanFurniture(player, radius);
                    updateBrokenFurnitureBlocks(player, radius);
                });
    }

    private void cleanupOrphanFurniture(Player player, int radius) {
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            SchedulerUtil.runForEntity(entity, () -> {
                if (!OraxenFurniture.isOrphanFurnitureEntity(entity)) return;
                OraxenFurniture.remove(entity, null);
            });
        }

        Location playerLoc = player.getLocation();
        forEachLoadedChunkAround(player, radius, chunk -> {
            Set<Block> blocks = new HashSet<>(BlockHelpers.getBlocksWithCustomData(OraxenPlugin.get(), chunk));
            for (Block block : blocks.stream().filter(b -> b.getLocation().distance(playerLoc) <= radius).toList()) {
                if (!OraxenFurniture.hasFurnitureBlockMarker(block)) continue;
                if (OraxenFurniture.getFurnitureMechanic(block) != null) continue;
                OraxenFurniture.remove(block.getLocation(), null);
            }
        });
    }

    /**
     * Fixes furniture where only the barrier block remains for xyz reason
     */
    private void updateBrokenFurnitureBlocks(Player player, int radius) {
        if (!Settings.EXPERIMENTAL_FIX_BROKEN_FURNITURE.toBool()) return;
        Location playerLoc = player.getLocation();
        forEachLoadedChunkAround(player, radius, chunk -> {
            Set<Block> blocks = new HashSet<>(BlockHelpers.getBlocksWithCustomData(OraxenPlugin.get(), chunk));
            for (Block block : blocks.stream().filter(b -> b.getLocation().distance(playerLoc) <= radius).toList()) {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (mechanic == null) continue;
                Entity baseEntity = mechanic.getBaseEntity(block);
                // Return if there is a baseEntity
                if (baseEntity != null) continue;

                Location rootLoc = new BlockLocation(BlockHelpers.getPDC(block).getOrDefault(ROOT_KEY, DataType.STRING, "")).toLocation(block.getWorld());
                float yaw = BlockHelpers.getPDC(block).getOrDefault(ORIENTATION_KEY, PersistentDataType.FLOAT, 0f);
                if (rootLoc == null) continue;

                // Stored root data may point anywhere (it exists precisely to repair
                // broken data); repair on the region thread that owns the root.
                SchedulerUtil.runAtLocation(rootLoc, () -> {
                    mechanic.getLocations(yaw, rootLoc, mechanic.getBarriers()).forEach(loc -> {
                        loc.getBlock().setType(Material.AIR);
                        BlockHelpers.removePDC(loc.getBlock());
                    });
                    mechanic.place(rootLoc, yaw, BlockFace.UP);
                });
            }
        });
    }

    /**
     * Visits every loaded chunk intersecting the radius around the player,
     * running the action on the region thread owning each chunk. Chunk handles
     * are resolved inside the region task so the calling thread never performs
     * a cross-region (or chunk-loading) lookup, which Folia rejects.
     */
    private static void forEachLoadedChunkAround(Player player, int radius, java.util.function.Consumer<Chunk> action) {
        Location loc = player.getLocation();
        org.bukkit.World world = player.getWorld();
        int minCx = (loc.getBlockX() - radius) >> 4, maxCx = (loc.getBlockX() + radius) >> 4;
        int minCz = (loc.getBlockZ() - radius) >> 4, maxCz = (loc.getBlockZ() + radius) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++)
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                final int fcx = cx, fcz = cz;
                Location chunkLoc = new Location(world, cx << 4, 0, cz << 4);
                SchedulerUtil.runAtLocation(chunkLoc, () -> {
                    if (!world.isChunkLoaded(fcx, fcz)) return;
                    action.accept(world.getChunkAt(fcx, fcz));
                });
            }
    }
}
