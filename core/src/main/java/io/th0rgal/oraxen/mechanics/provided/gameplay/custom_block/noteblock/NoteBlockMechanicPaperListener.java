package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class NoteBlockMechanicPaperListener implements Listener {

    @EventHandler
    public void onFallingBlockLandOnCarpet(EntityRemoveFromWorldEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(fallingBlock.getBlockData());
        if (mechanic == null || Objects.equals(OraxenBlocks.getCustomBlockMechanic(fallingBlock.getLocation()), mechanic))
            return;
        if (mechanic.isDirectional() && !mechanic.directional().isParentBlock())
            mechanic = mechanic.directional().getParentMechanic();

        ItemStack itemStack = OraxenItems.getItemById(mechanic.getItemID()).build();
        fallingBlock.setDropItem(false);
        fallingBlock.getWorld().dropItemNaturally(fallingBlock.getLocation(), itemStack);
    }
}
