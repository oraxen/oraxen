package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

public class WorldEditHandlers {



    public WorldEditHandlers(boolean register) {
        if (register) {
            WorldEdit.getInstance().getEventBus().register(this);
        } else {
            WorldEdit.getInstance().getEventBus().unregister(this);
        }
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getWorld() == null) return;

        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block) throws WorldEditException {
                BlockData blockData = BukkitAdapter.adapt(block);
                World world = Bukkit.getWorld(event.getWorld().getName());
                Location loc = new Location(world, pos.getX(), pos.getY(), pos.getZ());
                Mechanic mechanic = OraxenBlocks.getOraxenBlock(blockData);
                if (blockData.getMaterial() == Material.NOTE_BLOCK) {
                    if (mechanic != null && Settings.WORLDEDIT_NOTEBLOCKS.toBool()) {
                        OraxenBlocks.place(mechanic.getItemID(), loc);
                    }
                } else if (blockData.getMaterial() == Material.TRIPWIRE) {
                    if (mechanic != null && Settings.WORLDEDIT_STRINGBLOCKS.toBool()) {
                        OraxenBlocks.place(mechanic.getItemID(), loc);
                    }
                } else {
                    if (world == null) return super.setBlock(pos, block);
                    Mechanic replacingMechanic = OraxenBlocks.getOraxenBlock(loc);
                    if (replacingMechanic == null) return super.setBlock(pos, block);
                    if (replacingMechanic instanceof StringBlockMechanic && !Settings.WORLDEDIT_STRINGBLOCKS.toBool()) return super.setBlock(pos, block);
                    if (replacingMechanic instanceof NoteBlockMechanic && !Settings.WORLDEDIT_NOTEBLOCKS.toBool()) return super.setBlock(pos, block);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> OraxenBlocks.remove(loc, null));
                }

                return getExtent().setBlock(pos, block);
            }
        });
    }
}
