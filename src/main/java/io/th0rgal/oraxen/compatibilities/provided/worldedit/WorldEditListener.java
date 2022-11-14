package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
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
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WorldEditListener implements Listener {

    public WorldEditListener() {
        WorldEdit.getInstance().getEventBus().register(this);
        try {
            // Try and load class, if it fails it is not Paper server so don't register event
            Class.forName("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
            Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
        }
        catch (ClassNotFoundException ignored) {
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
                    OraxenBlocks.remove(loc, null);
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
