package io.th0rgal.oraxen.mechanics.provided.gameplay.general;


import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener.getStringMechanic;
import java.util.List;
import java.util.logging.Level;

public class SwapListener implements Listener {
	private final MechanicFactory factory;

	public SwapListener(MechanicFactory factory) {
		this.factory = factory;
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();

		if(block==null)return;
		if(block.getType() ==null)return;
		
		if (block.getType() == Material.NOTE_BLOCK) {
			NoteBlockMechanic m = getNoteBlockMechanic(block);

			if (m == null)
				return;
			if (m.isDirectional())
				m = (NoteBlockMechanic) factory.getMechanic(m.getDirectional().getParentBlock());

			if (!m.getSection().contains("swap"))
				return;

			for (String key : m.getSection().getConfigurationSection("swap").getKeys(false)) {
				Swap swap = new Swap(m.getSection().getConfigurationSection("swap." + key));
				Boolean swa = false;
				if (swap.items.contains("HAND") && event.getPlayer().getInventory().getItemInMainHand() == null) {
					swa = true;
				}

				if (io.th0rgal.oraxen.items.OraxenItems
						.getIdByItem(event.getPlayer().getInventory().getItemInMainHand()) != null) {
					if (swap.items.contains(io.th0rgal.oraxen.items.OraxenItems
							.getIdByItem(event.getPlayer().getInventory().getItemInMainHand()))) {
						swa = true;
					}
				} else if (swap.items
						.contains(event.getPlayer().getInventory().getItemInMainHand().getType().toString())) {
					swa = true;
				}
				try {
					NoteBlockMechanicFactory.setBlockModel(block, swap.toblock);
				} catch (Exception e) {
					io.th0rgal.oraxen.OraxenPlugin.get().getLogger().log(Level.WARNING,
							"Model: " + swap.toblock + " Error in config: " + m.getSection().getCurrentPath());
				}

			}
		}

	}

}
