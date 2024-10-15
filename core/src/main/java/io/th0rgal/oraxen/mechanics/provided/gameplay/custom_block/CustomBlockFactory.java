package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.sound.SoundEvent;
import team.unnamed.creative.sound.SoundRegistry;

import java.util.*;

public class CustomBlockFactory extends MechanicFactory {

    public CustomBlockType NOTEBLOCK;
    public CustomBlockType STRINGBLOCK;
    private static CustomBlockFactory instance;

    public CustomBlockFactory(String mechanicId) {
        super(mechanicId);
        instance = this;
        NOTEBLOCK = new DefaultBlockType("NOTEBLOCK", NoteBlockMechanicFactory.isEnabled() ? NoteBlockMechanicFactory.get() : null);
        STRINGBLOCK = new DefaultBlockType("STRINGBLOCK", StringBlockMechanicFactory.isEnabled() ? StringBlockMechanicFactory.get() : null);
        CustomBlockRegistry.register(NOTEBLOCK);
        CustomBlockRegistry.register(STRINGBLOCK);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new CustomBlockListener(), new CustomBlockMiningListener());
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    public static CustomBlockFactory get() {
        return instance;
    }

    public void blockStates(ResourcePack resourcePack) {
        List<BlockState> blockStates = new ArrayList<>();

        for (CustomBlockType type : CustomBlockRegistry.getTypes()) {
            if (type.factory() == null) continue;

            //TODO Implement this by having factories inherit and override CustomBlockFactory
            //blockStates.add(type.factory().generateBlockStateFile());
        }

        if (NoteBlockMechanicFactory.isEnabled()) blockStates.add(NoteBlockMechanicFactory.get().generateBlockState());
        if (StringBlockMechanicFactory.isEnabled()) blockStates.add(StringBlockMechanicFactory.get().generateBlockState());

        blockStates.forEach(blockState -> {
            Optional.ofNullable(resourcePack.blockState(blockState.key())).ifPresent(base -> {
                blockState.multipart().addAll(base.multipart());
                blockState.variants().putAll(base.variants());
            });
            blockState.addTo(resourcePack);
        });
    }

    public void soundRegistries(ResourcePack resourcePack) {
        List<SoundRegistry> soundRegistries = new ArrayList<>();

        for (CustomBlockType type : CustomBlockRegistry.getTypes()) {
            if (type.factory() == null) continue;

            //TODO Implement this by having factories inherit and override CustomBlockFactory
            //blockStates.add(type.factory().generateBlockStateFile());
        }
        if (NoteBlockMechanicFactory.isEnabled()) soundRegistries.addAll(List.of(BlockSounds.ORAXEN_WOOD_SOUND_REGISTRY, BlockSounds.VANILLA_WOOD_SOUND_REGISTRY));
        if (StringBlockMechanicFactory.isEnabled()) soundRegistries.addAll(List.of(BlockSounds.ORAXEN_STONE_SOUND_REGISTRY, BlockSounds.VANILLA_STONE_SOUND_REGISTRY));

        soundRegistries.forEach(soundRegistry -> {
            SoundRegistry baseRegistry = resourcePack.soundRegistry(soundRegistry.namespace());
            if (baseRegistry != null) {
                Collection<SoundEvent> mergedEvents = new LinkedHashSet<>(baseRegistry.sounds());
                mergedEvents.addAll(soundRegistry.sounds());
                SoundRegistry.soundRegistry(baseRegistry.namespace(), mergedEvents).addTo(resourcePack);
            } else soundRegistry.addTo(resourcePack);
        });
    }

    public List<String> toolTypes(CustomBlockType type) {
        if (type == STRINGBLOCK && type.factory() != null)
            return StringBlockMechanicFactory.get().toolTypes;
        else if (type == NOTEBLOCK && type.factory() != null)
            return NoteBlockMechanicFactory.get().toolTypes;
        else return new ArrayList<>();
    }

    @Override
    public CustomBlockMechanic parse(ConfigurationSection section) {
        String itemId = section.getParent().getParent().getName();
        CustomBlockType type = CustomBlockRegistry.fromMechanicSection(section);
        CustomBlockMechanic mechanic;

        if (type == null) return null;
        if (type.factory() == null) {
            Logs.logError(itemId + " attempted to use " + type.name() + "-type but it has been disabled");
            return null;
        }

        mechanic = (CustomBlockMechanic) type.factory().parse(section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Nullable
    public static CustomBlockMechanic getMechanic(@NotNull BlockData blockData) {
        if (blockData instanceof NoteBlock noteBlock) return NoteBlockMechanicFactory.getMechanic(noteBlock);
        else if (blockData instanceof Tripwire tripwire) return StringBlockMechanicFactory.getMechanic(tripwire);
        else return null;
    }

    @Override
    public CustomBlockMechanic getMechanic(String itemID) {
        return (CustomBlockMechanic) super.getMechanic(itemID);
    }

    @Override
    public CustomBlockMechanic getMechanic(ItemStack itemStack) {
        return (CustomBlockMechanic) super.getMechanic(itemStack);
    }
}
