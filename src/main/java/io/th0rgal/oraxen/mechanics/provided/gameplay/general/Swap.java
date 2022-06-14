package io.th0rgal.oraxen.mechanics.provided.gameplay.general;

import org.bukkit.configuration.ConfigurationSection;
import java.util.logging.Level;

public class Swap {
	private final String toblock;
	private final List<String> items;
	public Swap(ConfigurationSection SwapSection) {
		toblock = SwapSection.getString("switch");
		items = SwapSection.getStringList("object");
	}
}
