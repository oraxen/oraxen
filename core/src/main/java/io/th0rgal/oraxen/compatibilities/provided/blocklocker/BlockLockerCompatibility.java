package io.th0rgal.oraxen.compatibilities.provided.blocklocker;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import nl.rutgerkok.blocklocker.BlockLockerAPIv2;
import nl.rutgerkok.blocklocker.ProtectableBlocksSettings;
import nl.rutgerkok.blocklocker.ProtectionType;
import nl.rutgerkok.blocklocker.impl.BlockLockerPluginImpl;
import org.bukkit.block.Block;

public class BlockLockerCompatibility extends CompatibilityProvider<BlockLockerPluginImpl> {
    public BlockLockerCompatibility() {
        BlockLockerAPIv2.getPlugin().getChestSettings().getExtraProtectables().add(new ProtectableBlocksSettings() {

            @Override
            public boolean canProtect(Block block) {
                BlockLockerMechanic blockLocker = null;
                NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (noteMechanic != null) blockLocker = noteMechanic.getBlockLocker();
                StringBlockMechanic stringMechanic = OraxenBlocks.getStringMechanic(block);
                if (stringMechanic != null) blockLocker = stringMechanic.getBlockLocker();
                FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (furnitureMechanic != null) blockLocker = furnitureMechanic.getBlockLocker();

                return blockLocker != null && blockLocker.canProtect();
            }

            @Override
            public boolean canProtect(ProtectionType type, Block block) {
                BlockLockerMechanic blockLocker = null;
                NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (noteMechanic != null) blockLocker = noteMechanic.getBlockLocker();
                StringBlockMechanic stringMechanic = OraxenBlocks.getStringMechanic(block);
                if (stringMechanic != null) blockLocker = stringMechanic.getBlockLocker();
                FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (furnitureMechanic != null) blockLocker = furnitureMechanic.getBlockLocker();

                return blockLocker != null && blockLocker.canProtect() && blockLocker.getProtectionType() == type;
            }});
    }


}
