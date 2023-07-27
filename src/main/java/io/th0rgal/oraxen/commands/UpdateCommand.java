package io.th0rgal.oraxen.commands;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureUpdater;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Set;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ORIENTATION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.ROOT_KEY;

public class UpdateCommand {

    CommandAPICommand getUpdateCommand() {
        return new CommandAPICommand("update")
                .withPermission("oraxen.command.update")
                .withSubcommands(getFurnitureUpdateCommand(), getItemUpdateCommand(), getBlockUpdateCommand());
    }

    private CommandAPICommand getBlockUpdateCommand() {
        return new CommandAPICommand("block")
                .withOptionalArguments(new IntegerArgument("radius"))
                .executesPlayer((player, args) -> {
                    int radius = (int) args.getOptional("radius").orElse(10);
                    if (!Settings.EXPERIMENTAL_FIX_BROKEN_FURNITURE.toBool()) return;
                    Set<Block> blocks = CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), player.getLocation().getChunk());

                    for (Block block : blocks.stream().filter(b -> b.getLocation().distance(player.getLocation()) <= radius).toList()) {
                        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                        if (mechanic == null) return;
                        Entity baseEntity = mechanic.getBaseEntity(block);
                        // Return if there is a baseEntity
                        if (baseEntity != null) return;

                        Location rootLoc = new BlockLocation(BlockHelpers.getPDC(block).getOrDefault(ROOT_KEY, DataType.STRING, "")).toLocation(block.getWorld());
                        float yaw = BlockHelpers.getPDC(block).getOrDefault(ORIENTATION_KEY, PersistentDataType.FLOAT, 0f);
                        if (rootLoc == null) return;

                        //OraxenFurniture.remove(block.getLocation(), null);
                        mechanic.getLocations(yaw, rootLoc, mechanic.getBarriers()).forEach(loc -> {
                            loc.getBlock().setType(Material.AIR);
                            new CustomBlockData(loc.getBlock(), OraxenPlugin.get()).clear();
                        });
                        mechanic.place(rootLoc, yaw, BlockFace.UP);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getItemUpdateCommand() {
        return new CommandAPICommand("item")
                .withArguments(new EntitySelectorArgument.ManyEntities("targets"))
                .executesPlayer((player, args) -> {
                    final Collection<Player> targets = ((Collection<Entity>) args.get("targets")).stream().filter(entity -> entity instanceof Player).map(e -> (Player) e).toList();
                    for (Player p : targets) {
                        int updated = 0;
                        for (int i = 0; i < p.getInventory().getSize(); i++) {
                            final ItemStack oldItem = p.getInventory().getItem(i);
                            final ItemStack newItem = ItemUpdater.updateItem(oldItem);
                            if (oldItem == null || oldItem.equals(newItem)) continue;
                            p.getInventory().setItem(i, newItem);
                            updated++;
                        }
                        p.updateInventory();
                        Message.UPDATED_ITEMS.send(player, AdventureUtils.tagResolver("amount", String.valueOf(updated)),
                                AdventureUtils.tagResolver("player", p.getDisplayName()));
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private CommandAPICommand getFurnitureUpdateCommand() {
        return new CommandAPICommand("furniture")
                .withOptionalArguments(new IntegerArgument("radius"))
                .executesPlayer((player, args) -> {
                    int radius = (int) args.getOptional("radius").orElse(10);
                    final Collection<Entity> targets = ((Collection<Entity>) args.getOptional("targets").orElse(player.getNearbyEntities(radius, radius, radius))).stream().filter(OraxenFurniture::isBaseEntity).toList();
                    FurnitureUpdater.furnitureToUpdate.addAll(targets);
                });
    }
}
