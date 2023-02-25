package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.mechanics.provided.combat.lifeleech.LifeLeechMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.combat.spell.energyblast.EnergyBlastMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.combat.spell.fireball.FireballMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.combat.spell.thor.ThorMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.combat.spell.witherskull.WitherSkullMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.aura.AuraMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.hat.HatMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.skin.SkinMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.skinnable.SkinnableMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak.BedrockBreakMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.farming.bigmining.BigMiningMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.farming.bottledexp.BottledExpMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.farming.harvesting.HarvestingMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.farming.smelting.SmeltingMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.farming.watering.WateringMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.repair.RepairMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.armorpotioneffects.ArmorPotionEffectsMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.commands.CommandsMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.consumable.ConsumableMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.consumablepotioneffects.ConsumablePotionEffectsMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.CustomMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.food.FoodMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.soulbound.SoulBoundMechanicFactory;
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

    public static void registerNativeMechanics() {
        // misc
        registerMechanicFactory("armorpotioneffects", ArmorPotionEffectsMechanicFactory::new);
        registerMechanicFactory("consumablepotioneffects", ConsumablePotionEffectsMechanicFactory::new);
        registerMechanicFactory("soulbound", SoulBoundMechanicFactory::new);
        registerMechanicFactory("itemtype", ItemTypeMechanicFactory::new);
        registerMechanicFactory("consumable", ConsumableMechanicFactory::new);
        registerMechanicFactory("custom", CustomMechanicFactory::new);
        registerMechanicFactory("commands", CommandsMechanicFactory::new);
        registerMechanicFactory("backpack", BackpackMechanicFactory::new);
        registerMechanicFactory("music_disc", MusicDiscMechanicFactory::new);
        registerMechanicFactory("misc", MiscMechanicFactory::new);

        // gameplay
        registerMechanicFactory("food", FoodMechanicFactory::new);
        registerMechanicFactory("repair", RepairMechanicFactory::new);
        registerMechanicFactory("durability", DurabilityMechanicFactory::new);
        registerMechanicFactory("efficiency", EfficiencyMechanicFactory::new);
        registerMechanicFactory("block", BlockMechanicFactory::new);
        registerMechanicFactory("noteblock", NoteBlockMechanicFactory::new);
        registerMechanicFactory("stringblock", StringBlockMechanicFactory::new);
        registerMechanicFactory("furniture", FurnitureFactory::new);

        // cosmetic
        registerMechanicFactory("aura", AuraMechanicFactory::new);
        registerMechanicFactory("hat", HatMechanicFactory::new);
        registerMechanicFactory("skin", SkinMechanicFactory::new);
        registerMechanicFactory("skinnable", SkinnableMechanicFactory::new);

        // combat
        registerMechanicFactory("thor", ThorMechanicFactory::new);
        registerMechanicFactory("lifeleech", LifeLeechMechanicFactory::new);
        registerMechanicFactory("energyblast", EnergyBlastMechanicFactory::new);
        registerMechanicFactory("witherskull", WitherSkullMechanicFactory::new);
        registerMechanicFactory("fireball", FireballMechanicFactory::new);

        // farming
        registerMechanicFactory("bigmining", BigMiningMechanicFactory::new);
        registerMechanicFactory("smelting", SmeltingMechanicFactory::new);
        registerMechanicFactory("bottledexp", BottledExpMechanicFactory::new);
        registerMechanicFactory("harvesting", HarvestingMechanicFactory::new);
        registerMechanicFactory("watering", WateringMechanicFactory::new);
        if (CompatibilitiesManager.hasPlugin("ProtocolLib"))
            registerMechanicFactory("bedrockbreak", BedrockBreakMechanicFactory::new);
    }

    public static void registerMechanicFactory(final String mechanicId,
                                               final FactoryConstructor constructor) {
        final Entry<File, YamlConfiguration> mechanicsEntry = new ResourcesManager(OraxenPlugin.get()).getMechanicsEntry();
        final YamlConfiguration mechanicsConfig = mechanicsEntry.getValue();
        final boolean updated = false;
        if (mechanicsConfig.getKeys(false).contains(mechanicId)) {
            final ConfigurationSection factorySection = mechanicsConfig.getConfigurationSection(mechanicId);
            if (factorySection.getBoolean("enabled")) {
                final MechanicFactory factory = constructor.create(factorySection);
                FACTORIES_BY_MECHANIC_ID.put(mechanicId, factory);
            }
        }
        if (updated)
            try {
                mechanicsConfig.save(mechanicsEntry.getKey());
            } catch (final IOException e) {
                e.printStackTrace();
            }
    }

    public static void registerListeners(final JavaPlugin plugin, final Listener... listeners) {
        for (final Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            MECHANICS_LISTENERS.add(listener);
        }
    }

    public static void unloadListeners() {
        for (final Listener listener : MECHANICS_LISTENERS)
            HandlerList.unregisterAll(listener);
    }

    public static MechanicFactory getMechanicFactory(final String mechanicID) {
        return FACTORIES_BY_MECHANIC_ID.get(mechanicID);
    }

    @FunctionalInterface
    public interface FactoryConstructor {
        MechanicFactory create(ConfigurationSection section);
    }

}
