package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.mechanics.provided.armorpotioneffects.ArmorPotionEffectsMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.bedrockbreak.BedrockBreakMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.bigmining.BigMiningMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.bottledexp.BottledExpMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.commands.CommandsMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.consumable.ConsumableMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.consumablepotioneffects.ConsumablePotionEffectsMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.custom.CustomMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.harvesting.HarvestingMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.hat.HatMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.itemtype.ItemTypeMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.lifeleech.LifeLeechMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.repair.RepairMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.skin.SkinMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.skinnable.SkinnableMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.smelting.SmeltingMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.soulbound.SoulBoundMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.spell.energyblast.EnergyBlastMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.spell.fireball.FireballMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.spell.thor.ThorMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.spell.witherskull.WitherSkullMechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MechanicsManager {

    private static final Map<String, MechanicFactory> FACTORIES_BY_MECHANIC_ID = new HashMap<>();
    private static final List<Listener> MECHANICS_LISTENERS = new ArrayList<>();

    @FunctionalInterface
    public interface FactoryConstructor {
        MechanicFactory create(ConfigurationSection section);
    }

    public static void registerNativeMechanics() {
        // misc
        registerMechanicFactory("durability", DurabilityMechanicFactory::new);
        registerMechanicFactory("repair", RepairMechanicFactory::new);
        registerMechanicFactory("commands", CommandsMechanicFactory::new);
        registerMechanicFactory("armorpotioneffects", ArmorPotionEffectsMechanicFactory::new);
        registerMechanicFactory("consumablepotioneffects", ConsumablePotionEffectsMechanicFactory::new);
        registerMechanicFactory("block", BlockMechanicFactory::new);
        registerMechanicFactory("noteblock", NoteBlockMechanicFactory::new);
        // Available only with 1.16+ (20w10a)
        if (Bukkit.getVersion().contains("1.16") || Bukkit.getVersion().contains("1.17"))
            registerMechanicFactory("furniture", FurnitureFactory::new);
        registerMechanicFactory("hat", HatMechanicFactory::new);
        registerMechanicFactory("soulbound", SoulBoundMechanicFactory::new);
        registerMechanicFactory("skin", SkinMechanicFactory::new);
        registerMechanicFactory("skinnable", SkinnableMechanicFactory::new);
        registerMechanicFactory("itemtype", ItemTypeMechanicFactory::new);
        registerMechanicFactory("consumable", ConsumableMechanicFactory::new);
        registerMechanicFactory("fireball", FireballMechanicFactory::new);
        registerMechanicFactory("custom", CustomMechanicFactory::new);

        // combat
        //
        // native
        registerMechanicFactory("thor", ThorMechanicFactory::new);
        registerMechanicFactory("lifeleech", LifeLeechMechanicFactory::new);
        registerMechanicFactory("energyblast", EnergyBlastMechanicFactory::new);
        registerMechanicFactory("witherskull", WitherSkullMechanicFactory::new);

        // farming
        //
        // native
        registerMechanicFactory("bigmining", BigMiningMechanicFactory::new);
        registerMechanicFactory("smelting", SmeltingMechanicFactory::new);
        registerMechanicFactory("bottledexp", BottledExpMechanicFactory::new);
        registerMechanicFactory("harvesting", HarvestingMechanicFactory::new);
        //
        // dependent
        if (OraxenPlugin.getProtocolLib())
            registerMechanicFactory("bedrockbreak", BedrockBreakMechanicFactory::new);

    }

    public static void registerMechanicFactory(String mechanicId,
                                               FactoryConstructor constructor) {
        Entry<File, YamlConfiguration> mechanicsEntry = new ResourcesManager(OraxenPlugin.get()).getMechanicsEntry();
        YamlConfiguration mechanicsConfig = mechanicsEntry.getValue();
        boolean updated = false;
        if (mechanicsConfig.getKeys(false).contains(mechanicId)) {
            ConfigurationSection factorySection = mechanicsConfig.getConfigurationSection(mechanicId);
            if (factorySection.getBoolean("enabled")) {
                MechanicFactory factory = constructor.create(factorySection);
                FACTORIES_BY_MECHANIC_ID.put(mechanicId, factory);
            }
        }
        if (updated)
            try {
                mechanicsConfig.save(mechanicsEntry.getKey());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static void registerListeners(JavaPlugin plugin, Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            MECHANICS_LISTENERS.add(listener);
        }
    }

    public static void unloadListeners() {
        for (Listener listener : MECHANICS_LISTENERS)
            HandlerList.unregisterAll(listener);
    }

    public static MechanicFactory getMechanicFactory(String mechanicID) {
        return FACTORIES_BY_MECHANIC_ID.get(mechanicID);
    }

}
