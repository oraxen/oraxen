package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.custom_block.noteblock.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.api.events.custom_block.stringblock.OraxenStringBlockPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteMechanicHelpers;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.protectionlib.ProtectionLib;
import net.minecraft.world.InteractionResult;
import org.apache.commons.lang3.Range;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CustomBlockHelpers {

    public static void makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                            final Block placedAgainst, final BlockFace face, @Nullable final CustomBlockMechanic newMechanic, final BlockData newData) {
        final Block target;
        final Material itemMaterial = item.getType();
        final Material type = placedAgainst.getType();
        final Range<Integer> worldHeightRange = Range.between(placedAgainst.getWorld().getMinHeight(), placedAgainst.getWorld().getMaxHeight() - 1);

        if (BlockHelpers.isReplaceable(type)) target = placedAgainst;
        else {
            target = placedAgainst.getRelative(face);
            if (newMechanic != null && !BlockHelpers.isReplaceable(target.getType())) return;
            else if (Tag.DOORS.isTagged(target.getType())) return;
        }

        final Block blockAbove = target.getRelative(BlockFace.UP);
        final BlockData oldData = target.getBlockData();
        Enum<InteractionResult> result = null;
        if (newMechanic == null) {
            //TODO Fix boats, currently Item#use in BoatItem calls PlayerInteractEvent
            // thus causing a StackOverflow, find a workaround
            if (Tag.ITEMS_BOATS.isTagged(itemMaterial)) return;
            result = NMSHandlers.getHandler().correctBlockStates(player, hand, item);
            if (target.getState() instanceof Sign sign && target.getType() != oldData.getMaterial())
                player.openSign(sign, Side.FRONT);
        }

        if (newData != null)
            if (result == null) target.setBlockData(newData);
            else target.setBlockData(target.getBlockData(), false);

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst, item, player, true, hand);

        if (!ProtectionLib.canBuild(player, target.getLocation()))
            blockPlaceEvent.setCancelled(true);
        if (!worldHeightRange.contains(target.getY()))
            blockPlaceEvent.setCancelled(true);

        if (newMechanic != null) {
            if (BlockHelpers.isStandingInside(player, target)) blockPlaceEvent.setCancelled(true);
        } else {
            if (!itemMaterial.isBlock() && itemMaterial != Material.FLINT_AND_STEEL && itemMaterial != Material.FIRE_CHARGE && itemMaterial != Material.STRING)
                return;
            if (target.getBlockData().equals(oldData)) blockPlaceEvent.setCancelled(true);
            if (result == null) blockPlaceEvent.setCancelled(true);
        }

        // Handling placing against noteblock
        if (OraxenBlocks.getCustomBlockMechanic(target.getBlockData()) instanceof NoteBlockMechanic noteMechanic) {
            if (noteMechanic.isStorage() || noteMechanic.hasClickActions())
                blockPlaceEvent.setCancelled(true);
        }

        if (newMechanic instanceof StringBlockMechanic stringMechanic && stringMechanic.isTall()) {
            if (!BlockHelpers.REPLACEABLE_BLOCKS.contains(blockAbove.getType()))
                blockPlaceEvent.setCancelled(true);
            else if (!worldHeightRange.contains(blockAbove.getY()))
                blockPlaceEvent.setCancelled(true);
            else blockAbove.setType(Material.TRIPWIRE);
        }

        // Call the event and check if it is cancelled, if so reset BlockData
        if (!EventUtils.callEvent(blockPlaceEvent) || !blockPlaceEvent.canBuild()) {
            target.setBlockData(oldData, false);
            return;
        }

        if (newMechanic != null) {
            OraxenBlocks.place(newMechanic.getItemID(), target.getLocation());
            Event customBlockPlaceEvent;
            if (newMechanic instanceof NoteBlockMechanic noteMechanic) {
                customBlockPlaceEvent = new OraxenNoteBlockPlaceEvent(noteMechanic, target, player, item, hand);
            } else if (newMechanic instanceof StringBlockMechanic stringMechanic) {
                customBlockPlaceEvent = new OraxenStringBlockPlaceEvent(stringMechanic, target, player, item, hand);
            } else return;

            if (!EventUtils.callEvent(customBlockPlaceEvent)) {
                target.setBlockData(oldData, false);
                return;
            }

            // Handle Falling NoteBlock-Mechanic blocks
            if (newMechanic instanceof NoteBlockMechanic noteMechanic) {
                if (noteMechanic.isFalling() && target.getRelative(BlockFace.DOWN).getType().isAir()) {
                    Location fallingLocation = BlockHelpers.toCenterBlockLocation(target.getLocation());
                    OraxenBlocks.remove(target.getLocation(), null);
                    if (fallingLocation.getNearbyEntitiesByType(FallingBlock.class, 0.25).isEmpty())
                        target.getWorld().spawnFallingBlock(fallingLocation, newData);
                    NoteMechanicHelpers.handleFallingOraxenBlockAbove(target);
                }
            }

            if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
            Utils.swingHand(player, hand);
        }
        if (VersionUtil.isPaperServer()) target.getWorld().sendGameEvent(player, GameEvent.BLOCK_PLACE, target.getLocation().toVector());

    }
}
