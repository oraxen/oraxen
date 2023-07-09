package io.th0rgal.oraxen.utils.armorequipevent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.bukkit.event.EventPriority.MONITOR;

/**
 * @author Arnah
 * @since Jul 30, 2015
 */
public class ArmorListener implements Listener {

    private final List<Material> blockedMaterials;

    public ArmorListener(List<String> blockedMaterialNames) {
        this.blockedMaterials = blockedMaterialNames.stream()
                .map(Material::getMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    //Event Priority is highest because other plugins might cancel the events before we check.

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public final void onClick(final InventoryClickEvent event) {
        boolean shift = false, numberkey = false;
        if (event.getAction() == InventoryAction.NOTHING) return;// Why does this get called if nothing happens??
        if (event.getClick().equals(ClickType.SHIFT_LEFT) || event.getClick().equals(ClickType.SHIFT_RIGHT)) shift = true;
        if (event.getClick() == ClickType.NUMBER_KEY) numberkey = true;
        if (event.getClick() == ClickType.SWAP_OFFHAND) numberkey = true;
        if (event.getSlotType() != SlotType.ARMOR && event.getSlotType() != SlotType.QUICKBAR && event.getSlotType() != SlotType.CONTAINER)
            return;
        if (event.getClickedInventory() != null && !event.getClickedInventory().getType().equals(InventoryType.PLAYER))
            return;
        if (!event.getInventory().getType().equals(InventoryType.CRAFTING) && !event.getInventory().getType().equals(InventoryType.PLAYER))
            return;
        if (!(event.getWhoClicked() instanceof Player)) return;


        ArmorType newArmorType = ArmorType.matchType(shift ? event.getCurrentItem() : event.getCursor());
        // Used for drag and drop checking to make sure you aren't trying to place a helmet in the boots slot.
        if (!shift && newArmorType != null && event.getRawSlot() != newArmorType.getSlot()) return;

        if (shift) {
            newArmorType = ArmorType.matchType(event.getCurrentItem());
            if (newArmorType != null) {
                boolean equipping = event.getRawSlot() != newArmorType.getSlot();
                if (newArmorType.equals(ArmorType.HELMET) && (equipping == isEmpty(event.getWhoClicked().getInventory().getHelmet())) || newArmorType.equals(ArmorType.CHESTPLATE) && (equipping == isEmpty(event.getWhoClicked().getInventory().getChestplate())) || newArmorType.equals(ArmorType.LEGGINGS) && (equipping ? isEmpty(event.getWhoClicked().getInventory().getLeggings()) : !isEmpty(event.getWhoClicked().getInventory().getLeggings())) || newArmorType.equals(ArmorType.BOOTS) && (equipping ? isEmpty(event.getWhoClicked().getInventory().getBoots()) : !isEmpty(event.getWhoClicked().getInventory().getBoots()))) {
                    ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) event.getWhoClicked(), ArmorEquipEvent.EquipMethod.SHIFT_CLICK, newArmorType, equipping ? null : event.getCurrentItem(), equipping ? event.getCurrentItem() : null);
                    Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                    if (armorEquipEvent.isCancelled()) {
                        event.setCancelled(true);
                    }
                }
            }
        } else {
            ItemStack newArmorPiece = event.getCursor();
            ItemStack oldArmorPiece = event.getCurrentItem();
            if (numberkey) {
                if (event.getClickedInventory().getType().equals(InventoryType.PLAYER)) {// Prevents shit in the 2by2 crafting
                    // e.getClickedInventory() == The players inventory
                    // e.getHotBarButton() == key people are pressing to equip or unequip the item to or from.
                    // e.getRawSlot() == The slot the item is going to.
                    // e.getSlot() == Armor slot, can't use e.getRawSlot() as that gives a hotbar slot ;-;
                    ItemStack hotbarItem = null;
                    if (event.getHotbarButton() != -1) {
                        hotbarItem = event.getClickedInventory().getItem(event.getHotbarButton());
                    } else if (event.getHotbarButton() == -1 && event.getClickedInventory() instanceof PlayerInventory) {
                        hotbarItem = ((PlayerInventory) event.getClickedInventory()).getItem(EquipmentSlot.OFF_HAND);
                    }
                    if (!isEmpty(hotbarItem)) {// Equipping
                        newArmorType = ArmorType.matchType(hotbarItem);
                        newArmorPiece = hotbarItem;
                        oldArmorPiece = event.getClickedInventory().getItem(event.getSlot());
                    } else {// Unequipping
                        newArmorType = ArmorType.matchType(!isEmpty(event.getCurrentItem()) ? event.getCurrentItem() : event.getCursor());
                    }
                }
            } else {
                if (isEmpty(event.getCursor()) && !isEmpty(event.getCurrentItem())) {// unequip with no new item going into the slot.
                    newArmorType = ArmorType.matchType(event.getCurrentItem());
                }
                // e.getCurrentItem() == Unequip
                // e.getCursor() == Equip
                // newArmorType = ArmorType.matchType(!isEmpty(e.getCurrentItem()) ? e.getCurrentItem() : e.getCursor());
            }
            if (newArmorType != null && event.getRawSlot() == newArmorType.getSlot()) {
                ArmorEquipEvent.EquipMethod method = ArmorEquipEvent.EquipMethod.PICK_DROP;
                if (event.getAction().equals(InventoryAction.HOTBAR_SWAP) || numberkey)
                    method = ArmorEquipEvent.EquipMethod.HOTBAR_SWAP;
                ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) event.getWhoClicked(), method, newArmorType, oldArmorPiece, newArmorPiece);
                Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                if (armorEquipEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.useItemInHand().equals(Result.DENY)) return;

        if (event.getAction() == Action.PHYSICAL) return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (!event.useInteractedBlock().equals(Result.DENY)) {
                if (event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()) {// Having both of these checks is useless, might as well do it though.
                    // Some blocks have actions when you right click them which stops the client from equipping the armor in hand.
                    Material mat = event.getClickedBlock().getType();
                    if (blockedMaterials.contains(mat)) return;
                }
            }
            ArmorType newArmorType = ArmorType.matchType(event.getItem());
            // Carved pumpkins cannot be equipped using right-click
            if (event.getItem() != null && event.getItem().getType() == Material.CARVED_PUMPKIN) return;

            if (newArmorType != null) {
                if (newArmorType.equals(ArmorType.HELMET) && isEmpty(event.getPlayer().getInventory().getHelmet()) || newArmorType.equals(ArmorType.CHESTPLATE) && isEmpty(event.getPlayer().getInventory().getChestplate()) || newArmorType.equals(ArmorType.LEGGINGS) && isEmpty(event.getPlayer().getInventory().getLeggings()) || newArmorType.equals(ArmorType.BOOTS) && isEmpty(event.getPlayer().getInventory().getBoots())) {
                    ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(event.getPlayer(), ArmorEquipEvent.EquipMethod.HOTBAR, ArmorType.matchType(event.getItem()), null, event.getItem());
                    Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                    if (armorEquipEvent.isCancelled()) {
                        event.setCancelled(true);
                        player.updateInventory();
                    }
                }
            }
        }
    }

    static boolean isEmpty(ItemStack item) {
        return (item == null || item.getType().isAir() || item.getAmount() == 0);
    }

//	@EventHandler(priority =  EventPriority.HIGHEST, ignoreCancelled = true)
//	public void onFOnEmptyArmorSlot(InventoryClickEvent event) {
//		if(event.getAction() != InventoryAction.HOTBAR_SWAP) {
//			//System.out.println(1);
//			return;
//		}
//		if(event.getClickedInventory() == null) {
//			//System.out.println(2);
//			return;
//		}
//		if(event.getSlotType() != SlotType.ARMOR) {
//			//System.out.println(3);
//			return;
//		}
//		ItemStack existingArmor = event.getView().getItem(event.getRawSlot());
//		if(event.getHotbarButton() != -1) {
//			//System.out.println(4);
//			return;
//		}
//		if(!isEmpty(existingArmor)) {
//			//System.out.println(5);
//			return;
//		}
//		if(!(event.getClickedInventory() instanceof PlayerInventory)) {
//			//System.out.println(7);
//			return;
//		}
//		PlayerInventory playerInv = (PlayerInventory) event.getClickedInventory();
//		ItemStack newArmor = playerInv.getItem(EquipmentSlot.OFF_HAND);
//		ArmorType newArmorType = ArmorType.matchType(newArmor);
//		if(newArmorType == null) {
//			return;
//		}
//		if(!(event.getWhoClicked() instanceof Player)) {
//			return;
//		}
//		Player player = (Player) event.getWhoClicked();
//		ArmorEquipEvent equipEvent = new ArmorEquipEvent(player, ArmorEquipEvent.EquipMethod.HOTBAR_SWAP, newArmorType, null, newArmor);
//		Bukkit.getServer().getPluginManager().callEvent(equipEvent);
//		if(equipEvent.isCancelled()){
//			event.setCancelled(true);
//			player.updateInventory();
//		}
//	}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        // getType() seems to always be even.
        // Old Cursor gives the item you are equipping
        // Raw slot is the ArmorType slot
        // Can't replace armor using this method making getCursor() useless.
        ArmorType type = ArmorType.matchType(event.getOldCursor());
        if (event.getRawSlots().isEmpty()) return;// Idk if this will ever happen
        if (type != null && type.getSlot() == event.getRawSlots().stream().findFirst().orElse(0)) {
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) event.getWhoClicked(), ArmorEquipEvent.EquipMethod.DRAG, type, null, event.getOldCursor());
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled()) {
                event.setResult(Result.DENY);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBreak(PlayerItemBreakEvent event) {
        ArmorType type = ArmorType.matchType(event.getBrokenItem());
        if (type != null) {
            Player p = event.getPlayer();
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(p, ArmorEquipEvent.EquipMethod.BROKE, type, event.getBrokenItem(), null);
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled()) {
                ItemStack i = event.getBrokenItem().clone();
                i.setAmount(1);
                i.setDurability((short) (i.getDurability() - 1));
                if (type.equals(ArmorType.HELMET)) {
                    p.getInventory().setHelmet(i);
                } else if (type.equals(ArmorType.CHESTPLATE)) {
                    p.getInventory().setChestplate(i);
                } else if (type.equals(ArmorType.LEGGINGS)) {
                    p.getInventory().setLeggings(i);
                } else if (type.equals(ArmorType.BOOTS)) {
                    p.getInventory().setBoots(i);
                }
            }
        }
    }

    @EventHandler(priority = MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (event.getKeepInventory()) return;
        for (ItemStack i : p.getInventory().getArmorContents()) {
            if (!isEmpty(i)) {
                Bukkit.getServer().getPluginManager().callEvent(new ArmorEquipEvent(p, ArmorEquipEvent.EquipMethod.DEATH, ArmorType.matchType(i), i, null));
                // No way to cancel a death event.
            }
        }
    }

}
