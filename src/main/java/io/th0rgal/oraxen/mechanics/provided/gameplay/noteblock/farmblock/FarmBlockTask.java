package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;

public class FarmBlockTask extends BukkitRunnable {

    private final NoteBlockMechanicFactory factory;
    private final int delay;

    public FarmBlockTask(NoteBlockMechanicFactory factory, int delay) {
        this.delay = delay;
        this.factory = factory;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getPersistentDataContainer().has(FARMBLOCK_KEY, PersistentDataType.INTEGER)) {
                String farmBlockId = world.getPersistentDataContainer().get(FARMBLOCK_KEY, PersistentDataType.STRING);
                System.out.println("i have a farmblock pog");
            }
        }
    }
}
