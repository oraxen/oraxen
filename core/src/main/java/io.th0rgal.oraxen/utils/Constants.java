package io.th0rgal.oraxen.utils;

import com.google.common.collect.Sets;
import org.bukkit.Material;

import java.util.Set;

public class Constants {

    private Constants() {}

    public static final Set<Material> UNBREAKABLE_BLOCKS = Sets.newHashSet(Material.BEDROCK, Material.BARRIER, Material.NETHER_PORTAL, Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.END_GATEWAY);

}
