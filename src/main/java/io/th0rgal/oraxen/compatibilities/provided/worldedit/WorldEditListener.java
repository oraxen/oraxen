package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WorldEditListener implements Listener {

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getWorld() == null) return;

        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block) throws WorldEditException {
                BlockData blockData = BukkitAdapter.adapt(block);
                World world = Bukkit.getWorld(event.getWorld().getName());
                Location loc = new Location(world, pos.getX(), pos.getY(), pos.getZ());

                //TODO Add more mechanics here
                switch (blockData.getMaterial()) {
                    case NOTE_BLOCK -> {
                        NoteBlockMechanic mechanic = NoteBlockMechanicListener.getNoteBlockMechanic((NoteBlock) blockData);
                        if (mechanic != null && mechanic.hasLight()) {
                            WrappedLightAPI.createBlockLight(loc, mechanic.getLight());
                        }
                    }
                    case TRIPWIRE -> {
                        StringBlockMechanic mechanic = StringBlockMechanicListener.getStringMechanic((Tripwire) blockData);
                        if (mechanic != null && mechanic.hasLight()) {
                            WrappedLightAPI.createBlockLight(loc, mechanic.getLight());
                        }
                    }
                    // Otherwise it is perhaps removing a block so check and remove mechanics
                    default -> {
                        if (world == null) return super.setBlock(pos, block);
                        Block oldBlock = world.getBlockAt(loc);
                        switch (oldBlock.getType()) {
                            case NOTE_BLOCK -> {
                                NoteBlockMechanic mechanic = NoteBlockMechanicListener.getNoteBlockMechanic((NoteBlock) oldBlock.getBlockData());
                                if (mechanic != null && mechanic.hasLight()) {
                                    WrappedLightAPI.removeBlockLight(loc);
                                }
                            }
                            case TRIPWIRE -> {
                                StringBlockMechanic mechanic = StringBlockMechanicListener.getStringMechanic((Tripwire) oldBlock.getBlockData());
                                if (mechanic != null && mechanic.hasLight()) {
                                    WrappedLightAPI.removeBlockLight(loc);
                                }
                            }
                        }
                    }
                }

                return getExtent().setBlock(pos, block);
            }
        });
    }

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        List<String> args = Arrays.stream(event.getBuffer().split(" ")).toList();
        if (!event.getBuffer().startsWith("//") || args.isEmpty()) return;

        List<String> ids = oraxenBlockIDs.stream()
                .filter(id -> ("oraxen:" + id).startsWith(args.get(args.size() - 1)))
                .map("oraxen:"::concat).collect(Collectors.toList());
        ids.addAll(event.getCompletions());
        event.setCompletions(ids);
    }

    private final List<String> oraxenBlockIDs = OraxenItems.getEntries().stream()
            .map(entry -> entry.getKey().toLowerCase()).filter(this::isOraxenBlock).toList();

    private boolean isOraxenBlock(String id) {
        NoteBlockMechanic nMechanic = (NoteBlockMechanic) MechanicsManager.getMechanicFactory("noteblock").getMechanic(id);
        StringBlockMechanic sMechanic = (StringBlockMechanic) MechanicsManager.getMechanicFactory("stringblock").getMechanic(id);
        return nMechanic != null || sMechanic != null;
    }
}
