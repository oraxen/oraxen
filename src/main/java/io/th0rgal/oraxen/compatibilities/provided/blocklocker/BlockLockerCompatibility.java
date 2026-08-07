package io.th0rgal.oraxen.compatibilities.provided.blocklocker;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import nl.rutgerkok.blocklocker.BlockLockerAPIv2;
import nl.rutgerkok.blocklocker.ProtectableBlocksSettings;
import nl.rutgerkok.blocklocker.ProtectionType;
import nl.rutgerkok.blocklocker.impl.BlockLockerPluginImpl;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockLockerCompatibility extends CompatibilityProvider<BlockLockerPluginImpl> {
    public BlockLockerCompatibility() {
        BlockLockerAPIv2.getPlugin().getChestSettings().getExtraProtectables()
                .removeIf(protectable -> protectable.getClass().getName().equals(BlockLockerProtection.class.getName()));
        BlockLockerAPIv2.getPlugin().getChestSettings().getExtraProtectables().add(new BlockLockerProtection());
    }

    private static class BlockLockerProtection implements ProtectableBlocksSettings {

        @Override
        public boolean canProtect(Block block) {
            BlockLockerMechanic blockLocker = getBlockLocker(block);
            return blockLocker != null && blockLocker.canProtect();
        }

        @Override
        public boolean canProtect(ProtectionType type, Block block) {
            BlockLockerMechanic blockLocker = getBlockLocker(block);
            return blockLocker != null && blockLocker.canProtect() && blockLocker.getProtectionType() == type;
        }
    }

    public static boolean canInteract(Player player, Block block) {
        return canInteract(player, block, getBlockLocker(block));
    }

    public static boolean canInteract(Player player, Block block, FurnitureMechanic furnitureMechanic) {
        BlockLockerMechanic blockLocker = furnitureMechanic != null ? furnitureMechanic.getBlockLocker() : getBlockLocker(block);
        if (furnitureMechanic != null && blockLocker == null) blockLocker = getBlockLocker(block, false);
        return canInteract(player, block, blockLocker);
    }

    private static boolean canInteract(Player player, Block block, BlockLockerMechanic blockLocker) {
        if (!CompatibilitiesManager.isCompatibilityEnabled("BlockLocker") || blockLocker == null || !blockLocker.canProtect())
            return true;

        return BlockLockerAPIv2.isAllowed(player, block, true);
    }

    private static BlockLockerMechanic getBlockLocker(Block block) {
        return getBlockLocker(block, true);
    }

    private static BlockLockerMechanic getBlockLocker(Block block, boolean includeFurniture) {
        NoteBlockMechanic noteBlockMechanic = OraxenBlocks.getNoteBlockMechanic(block);
        BlockLockerMechanic blockLocker = noteBlockMechanic != null ? noteBlockMechanic.getBlockLocker() : null;
        if (blockLocker != null) return blockLocker;

        StringBlockMechanic stringBlockMechanic = OraxenBlocks.getStringMechanic(block);
        blockLocker = stringBlockMechanic != null ? stringBlockMechanic.getBlockLocker() : null;
        if (blockLocker != null) return blockLocker;

        if (!includeFurniture) return null;
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        return furnitureMechanic != null ? furnitureMechanic.getBlockLocker() : null;
    }
}
