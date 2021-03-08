package io.th0rgal.oraxen.utils.armorequipevent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.InventoryType.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Arnah
 * @since Jul 30, 2015
 */
public class ArmorListener implements Listener {

    private final List<Material> blockedMaterials = new ArrayList<>();

    public ArmorListener(List<String> blockedMaterials) {
        for (String material : blockedMaterials)
            this.blockedMaterials.add(Material.getMaterial(material));
    }

    public void registerEvents(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void dispenseArmorEvent(BlockDispenseArmorEvent event) {
        ArmorType type = ArmorType.matchType(event.getItem());
        if (type != null) {
            if (event.getTargetEntity() instanceof Player) {
                Player p = (Player) event.getTargetEntity();
                ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(p, ArmorEquipEvent.EquipMethod.DISPENSER, type, null, event.getItem());
                Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                if (armorEquipEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Event Priority is highest because other plugins might cancel the events
    // before we check.

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public final void inventoryClick(final InventoryClickEvent e) {

        boolean shift = false, numberkey = false;
        if (e.getAction() == InventoryAction.NOTHING)
            return;// Why does this get called if nothing happens??
        if (e.getClick().equals(ClickType.SHIFT_LEFT) || e.getClick().equals(ClickType.SHIFT_RIGHT))
            shift = true;
        if (e.getClick().equals(ClickType.NUMBER_KEY))
            numberkey = true;

        if (e.getSlotType() != SlotType.ARMOR && e.getSlotType() != SlotType.QUICKBAR && e.getSlotType() != SlotType.CONTAINER)
            return;
        if (e.getClickedInventory() != null && !e.getClickedInventory().getType().equals(InventoryType.PLAYER))
            return;
        if (!e.getInventory().getType().equals(InventoryType.CRAFTING) && !e.getInventory().getType().equals(InventoryType.PLAYER))
            return;
        if (!(e.getWhoClicked() instanceof Player))
            return;
        ItemStack currentItem = e.getCurrentItem();
        ArmorType newArmorType = ArmorType.matchType(shift ? currentItem : e.getCursor());
        if (!shift && newArmorType != null && e.getRawSlot() != newArmorType.getSlot())
            // Used for drag and drop checking to make sure you aren't trying to place a
            // helmet in the boots slot.
            return;

        if (shift) {
            newArmorType = ArmorType.matchType(currentItem);
            if (newArmorType != null) {
                boolean equipping = true;
                if (e.getRawSlot() >= 5 && e.getRawSlot() <= 8)
                    equipping = false;
                if (newArmorType.equals(ArmorType.HELMET) && (equipping == isAirOrNull(e.getWhoClicked().getInventory().getHelmet()))
                    || newArmorType.equals(ArmorType.CHESTPLATE) && (equipping == isAirOrNull(e.getWhoClicked().getInventory().getChestplate()))
                    || newArmorType.equals(ArmorType.LEGGINGS) && (equipping == isAirOrNull(e.getWhoClicked().getInventory().getLeggings()))
                    || newArmorType.equals(ArmorType.BOOTS) && (equipping == isAirOrNull(e.getWhoClicked().getInventory().getBoots()))) {
                    ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) e.getWhoClicked(), EquipMethod.SHIFT_CLICK, newArmorType,
                        equipping ? null : currentItem, equipping ? currentItem : null);
                    Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                    if (armorEquipEvent.isCancelled()) {
                        e.setCancelled(true);
                    }
                }
            }
        } else {
            ItemStack newArmorPiece = e.getCursor();
            ItemStack oldArmorPiece = currentItem;
            if (numberkey) {
                if (e.getClickedInventory().getType().equals(InventoryType.PLAYER)) {// Prevents shit in the 2by2
                    // crafting
                    // e.getClickedInventory() == The players inventory
                    // e.getHotBarButton() == key people are pressing to equip or unequip the item
                    // to or from.
                    // e.getRawSlot() == The slot the item is going to.
                    // e.getSlot() == Armor slot, can't use e.getRawSlot() as that gives a hotbar
                    // slot ;-;
                    ItemStack hotbarItem = e.getClickedInventory().getItem(e.getHotbarButton());
                    if (!isAirOrNull(hotbarItem)) {// Equipping
                        newArmorType = ArmorType.matchType(hotbarItem);
                        newArmorPiece = hotbarItem;
                        oldArmorPiece = e.getClickedInventory().getItem(e.getSlot());
                    } else {// Unequipping
                        newArmorType = ArmorType.matchType(!isAirOrNull(currentItem) ? currentItem : e.getCursor());
                    }
                }
            } else {
                if (isAirOrNull(e.getCursor()) && !isAirOrNull(currentItem)) {// unequip with no new item going into the
                    // slot.
                    newArmorType = ArmorType.matchType(currentItem);
                }
                // currentItem == Unequip
                // e.getCursor() == Equip
                // newArmorType = ArmorType.matchType(!isAirOrNull(currentItem) ? currentItem :
                // e.getCursor());
            }
            if (newArmorType != null && e.getRawSlot() == newArmorType.getSlot()) {
                EquipMethod method = EquipMethod.PICK_DROP;
                if (e.getAction().equals(InventoryAction.HOTBAR_SWAP) || numberkey)
                    method = EquipMethod.HOTBAR_SWAP;
                ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) e.getWhoClicked(), method, newArmorType, oldArmorPiece, newArmorPiece);
                Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                if (armorEquipEvent.isCancelled()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerInteractEvent(PlayerInteractEvent e) {
        if (e.useItemInHand().equals(Event.Result.DENY))
            return;
        //
        if (e.getAction() == Action.PHYSICAL)
            return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = e.getPlayer();
            if (!e.useInteractedBlock().equals(Event.Result.DENY)) {
                // Having both of these checks is useless, might as well do it though.
                if (e.getClickedBlock() != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()) {
                    // Some blocks have actions when you right click them which stops the client
                    // from equipping the armor in hand.
                    Material mat = e.getClickedBlock().getType();
                    if (blockedMaterials.contains(mat))
                        return;
                }
            }
            ArmorType newArmorType = ArmorType.matchType(e.getItem());
            if (newArmorType != null) {
                if (newArmorType.equals(ArmorType.HELMET) && isAirOrNull(e.getPlayer().getInventory().getHelmet())
                    || newArmorType.equals(ArmorType.CHESTPLATE) && isAirOrNull(e.getPlayer().getInventory().getChestplate())
                    || newArmorType.equals(ArmorType.LEGGINGS) && isAirOrNull(e.getPlayer().getInventory().getLeggings())
                    || newArmorType.equals(ArmorType.BOOTS) && isAirOrNull(e.getPlayer().getInventory().getBoots())) {
                    ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(e.getPlayer(), EquipMethod.HOTBAR, ArmorType.matchType(e.getItem()), null,
                        e.getItem());
                    Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                    if (armorEquipEvent.isCancelled()) {
                        e.setCancelled(true);
                        player.updateInventory();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void inventoryDrag(InventoryDragEvent event) {
        // getType() seems to always be even.
        // Old Cursor gives the item you are equipping
        // Raw slot is the ArmorType slot
        // Can't replace armor using this method making getCursor() useless.
        ArmorType type = ArmorType.matchType(event.getOldCursor());
        if (event.getRawSlots().isEmpty())
            return;// Idk if this will ever happen
        if (type != null && type.getSlot() == event.getRawSlots().stream().findFirst().orElse(0)) {
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) event.getWhoClicked(), EquipMethod.DRAG, type, null, event.getOldCursor());
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled()) {
                event.setResult(Event.Result.DENY);
                event.setCancelled(true);
            }
        }
        // Debug shit
        /*
         * System.out.println("Slots: " + event.getInventorySlots().toString());
         * System.out.println("Raw Slots: " + event.getRawSlots().toString());
         * if(event.getCursor() != null){ System.out.println("Cursor: " +
         * event.getCursor().getType().name()); } if(event.getOldCursor() != null){
         * System.out.println("OldCursor: " + event.getOldCursor().getType().name()); }
         * System.out.println("Type: " + event.getType().name());
         */
    }

    @EventHandler
    public void itemBreakEvent(PlayerItemBreakEvent e) {
        ArmorType type = ArmorType.matchType(e.getBrokenItem());
        if (type != null) {
            Player player = e.getPlayer();
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(player, EquipMethod.BROKE, type, e.getBrokenItem(), null);
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled()) {
                ItemStack item = e.getBrokenItem().clone();
                item.setAmount(1);
                org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) item.getItemMeta();
                damageable.setDamage((short) (damageable.getDamage() - 1));
                item.setItemMeta((ItemMeta) damageable);

                if (type.equals(ArmorType.HELMET)) {
                    player.getInventory().setHelmet(item);
                } else if (type.equals(ArmorType.CHESTPLATE)) {
                    player.getInventory().setChestplate(item);
                } else if (type.equals(ArmorType.LEGGINGS)) {
                    player.getInventory().setLeggings(item);
                } else if (type.equals(ArmorType.BOOTS)) {
                    player.getInventory().setBoots(item);
                }
            }
        }
    }

    @EventHandler
    public void playerDeathEvent(PlayerDeathEvent e) {
        Player player = e.getEntity();
        if (e.getKeepInventory())
            return;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (!isAirOrNull(item)) {
                Bukkit.getServer().getPluginManager().callEvent(new ArmorEquipEvent(player, EquipMethod.DEATH, ArmorType.matchType(item), item, null));
                // No way to cancel a death event.
            }
        }
    }

    /**
     * A utility method to support versions that use null or air ItemStacks.
     *
     * @param item The item to check
     * @return wether or not the item equals None
     */
    public static boolean isAirOrNull(ItemStack item) {
        return item == null || item.getType().equals(Material.AIR);
    }
}