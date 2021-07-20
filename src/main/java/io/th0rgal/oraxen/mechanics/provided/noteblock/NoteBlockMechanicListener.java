package io.th0rgal.oraxen.mechanics.provided.noteblock;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class NoteBlockMechanicListener implements Listener {

    private final MechanicFactory factory;

    public NoteBlockMechanicListener(NoteBlockMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block aboveBlock = event.getBlock().getLocation().add(0, 1, 0).getBlock();
        if (aboveBlock.getType() == Material.NOTE_BLOCK) {
            updateAndCheck(event.getBlock().getLocation());
            event.setCancelled(true);
        }
        if (event.getBlock().getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            event.getBlock().getState().update(true, false);
        }

    }

    public void updateAndCheck(Location loc) {
        Block block = loc.add(0, 1, 0).getBlock();
        if (block.getType() == Material.NOTE_BLOCK)
            block.getState().update(true, true);
        Location nextBlock = block.getLocation().add(0, 1, 0);
        if (nextBlock.getBlock().getType() == Material.NOTE_BLOCK)
            updateAndCheck(block.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && block != null
                && block.getType() == Material.NOTE_BLOCK) {
            NoteBlock blockData = (NoteBlock) block.getBlockData();
            ItemStack clicked = event.getItem();
            event.setCancelled(true);
            if (clicked == null)
                return;
            Material type = clicked.getType();
            if (type == null || clicked.getType().isInteractable())
                return;
            if (type == Material.LAVA_BUCKET)
                type = Material.LAVA;
            if (type == Material.WATER_BUCKET)
                type = Material.WATER;
            if (type.isBlock()) {
                makePlayerPlaceBlock(event.getPlayer(), event.getHand(), event.getItem(), block,
                        event.getBlockFace(), Bukkit.createBlockData(type));
            }
        }
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlayed(NotePlayEvent event) {
        if (event.getInstrument() != Instrument.PIANO)
            event.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NOTE_BLOCK || event.isCancelled() || !event.isDropItems())
            return;
        NoteBlock noteBlok = (NoteBlock) block.getBlockData();
        NoteBlockMechanic noteBlockMechanic = NoteBlockMechanicFactory
                .getBlockMechanic((int) (noteBlok.getInstrument().getType()) * 25
                        + (int) noteBlok.getNote().getId() + (noteBlok.isPowered() ? 400 : 0) - 26);
        if (noteBlockMechanic == null)
            return;
        if (noteBlockMechanic.hasBreakSound())
            block.getWorld().playSound(block.getLocation(), noteBlockMechanic.getBreakSound(), 1.0f, 0.8f);
        noteBlockMechanic.getDrop().spawns(block.getLocation(), event.getPlayer().getInventory().getItemInMainHand());
        event.setDropItems(false);
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingBlock(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.NOTE_BLOCK
                || OraxenItems.exists(OraxenItems.getIdByItem(event.getItemInHand())))
            return;

        Block block = event.getBlock();
        block.setBlockData(Bukkit.createBlockData(Material.NOTE_BLOCK), false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        Player player = event.getPlayer();
        Block placedAgainst = event.getClickedBlock();

        // determines the new block data of the block
        int customVariation = ((NoteBlockMechanic) factory.getMechanic(itemID)).getCustomVariation();

        makePlayerPlaceBlock(player, event.getHand(), event.getItem(),
                placedAgainst, event.getBlockFace(), NoteBlockMechanicFactory.createNoteBlockData(customVariation));

        event.setCancelled(true);

    }

    private boolean isStandingInside(Player player, Block block) {
        Location playerLocation = player.getLocation();
        Location blockLocation = block.getLocation();
        return playerLocation.getBlockX() == blockLocation.getBlockX()
                && (playerLocation.getBlockY() == blockLocation.getBlockY()
                || playerLocation.getBlockY() + 1 == blockLocation.getBlockY())
                && playerLocation.getBlockZ() == blockLocation.getBlockZ();
    }

    private boolean makePlayerPlaceBlock(Player player, EquipmentSlot hand, ItemStack item,
                                         Block placedAgainst, BlockFace face, BlockData newBlock) {
        Block target;
        Material type = placedAgainst.getType();
        if (Utils.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && target.getType() != Material.WATER && target.getType() != Material.LAVA)
                return false;
        }
        if (isStandingInside(player, target))
            return false;

        // determines the old informations of the block
        BlockData curentBlockData = target.getBlockData();
        target.setBlockData(newBlock, false);
        BlockState currentBlockState = target.getState();

        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player,
                true, hand);
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return false;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);

        return true;
    }

}
