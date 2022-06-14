package io.th0rgal.oraxen.mechanics.provided.gameplay.general;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
import java.util.logging.Level;

public class Swap {
	public String toblock;
	public String action;
	public Boolean shift;
	public List<String> items;
	public Swap(ConfigurationSection SwapSection) {
		shift = SwapSection.getBoolean("shift");
		action = SwapSection.getString("action");
		toblock = SwapSection.getString("switch");
		items = SwapSection.getStringList("object");
	}
}
