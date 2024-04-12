package io.th0rgal.oraxen.compatibilities.provided.blocklocker;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
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
                CustomBlockMechanic customBlockMechanic = OraxenBlocks.getCustomBlockMechanic(block.getBlockData());
                if (customBlockMechanic != null) blockLocker = customBlockMechanic.blockLocker();

                if (blockLocker == null) {
                    FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block.getLocation());
                    if (furnitureMechanic != null) blockLocker = furnitureMechanic.blocklocker();
                }

                return blockLocker != null && blockLocker.canProtect();
            }

            @Override
            public boolean canProtect(ProtectionType type, Block block) {
                BlockLockerMechanic blockLocker = null;
                NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (noteMechanic != null) blockLocker = noteMechanic.blockLocker();
                StringBlockMechanic stringMechanic = OraxenBlocks.getStringMechanic(block);
                if (stringMechanic != null) blockLocker = stringMechanic.blockLocker();
                FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block.getLocation());
                if (furnitureMechanic != null) blockLocker = furnitureMechanic.blocklocker();

                return blockLocker != null && blockLocker.canProtect() && blockLocker.getProtectionType() == type;
            }});
    }


}
