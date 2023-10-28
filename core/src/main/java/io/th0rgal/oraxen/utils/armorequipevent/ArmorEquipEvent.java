package io.th0rgal.oraxen.utils.armorequipevent;

import io.th0rgal.oraxen.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called when a player equips or unequips a piece of armor.
 *
 * @author Arnah
 * @since Jul 30, 2015
 */
public final class ArmorEquipEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancel = false;
    private final EquipMethod equipType;
    private final ArmorType type;
    private ItemStack oldArmorPiece, newArmorPiece;

    /**
     * Registers the listeners for this event. If you forget to call this method, then the event will never get caled.
     * @param plugin Plugin to call this event from
     */
    public static void registerListener(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(new ArmorListener(getBlockedMaterialNames()), plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new DispenserArmorListener(), plugin);
    }

    private static List<String> getBlockedMaterialNames() {
        return Settings.ARMOR_EQUIP_EVENT_BYPASS.toStringList();
    }

    public static ArmorEquipEvent OraxenHatEquipEvent(Player player, ItemStack oldArmorPiece, ItemStack newArmorPiece) {
        return new ArmorEquipEvent(player, EquipMethod.ORAXEN_HAT, ArmorType.HELMET, oldArmorPiece, newArmorPiece);
    }

    /**
     * @param player The player who put on / removed the armor.
     * @param type The ArmorType of the armor added
     * @param oldArmorPiece The ItemStack of the armor removed.
     * @param newArmorPiece The ItemStack of the armor added.
     */
    public ArmorEquipEvent(final Player player, final EquipMethod equipType, final ArmorType type, final ItemStack oldArmorPiece, final ItemStack newArmorPiece){
        super(player);
        this.equipType = equipType;
        this.type = type;
        this.oldArmorPiece = oldArmorPiece;
        this.newArmorPiece = newArmorPiece;
    }

    /**
     * Gets a list of handlers handling this event.
     *
     * @return A list of handlers handling this event.
     */
    public static HandlerList getHandlerList(){
        return handlers;
    }

    /**
     * Gets a list of handlers handling this event.
     *
     * @return A list of handlers handling this event.
     */
    @NotNull
    @Override
    public HandlerList getHandlers(){
        return handlers;
    }

    /**
     * Sets if this event should be cancelled.
     *
     * @param cancel If this event should be cancelled. When the event is cancelled, the armor is not changed.
     */
    public void setCancelled(final boolean cancel){
        this.cancel = cancel;
    }

    /**
     * Gets if this event is cancelled.
     *
     * @return If this event is cancelled
     */
    public boolean isCancelled(){
        return cancel;
    }

    /**
     * Returns the type of armor involved in this event
     * @return ArmorType
     */
    public ArmorType getType(){
        return type;
    }

    /**
     * Returns the last equipped armor piece, could be a piece of armor, or null
     */
    public ItemStack getOldArmorPiece(){
        if(ArmorListener.isEmpty(oldArmorPiece)){
            return null;
        }
        return oldArmorPiece;
    }

    public void setOldArmorPiece(final ItemStack oldArmorPiece){
        this.oldArmorPiece = oldArmorPiece;
    }

    /**
     * Returns the newly equipped armor, could be a piece of armor, or null
     */
    public ItemStack getNewArmorPiece(){
        if(ArmorListener.isEmpty(newArmorPiece)){
            return null;
        }
        return newArmorPiece;
    }

    public final void setNewArmorPiece(final ItemStack newArmorPiece){
        this.newArmorPiece = newArmorPiece;
    }

    /**
     * Gets the method used to either equip or unequip an armor piece.
     */
    public EquipMethod getMethod(){
        return equipType;
    }

    /**
     * Represents the way of equipping or uneqipping armor.
     */
    public enum EquipMethod{// These have got to be the worst documentations ever.
        /**
         * When you shift click an armor piece to equip or unequip
         */
        SHIFT_CLICK,
        /**
         * When you drag and drop the item to equip or unequip
         */
        DRAG,
        /**
         * When you manually equip or unequip the item. Use to be DRAG
         */
        PICK_DROP,
        /**
         * When you right click an armor piece in the hotbar without the inventory open to equip.
         */
        HOTBAR,
        /**
         * When you press the hotbar slot number while hovering over the armor slot to equip or unequip
         */
        HOTBAR_SWAP,
        /**
         * When in range of a dispenser that shoots an armor piece to equip.<br>
         * Requires the spigot version to have {@link org.bukkit.event.block.BlockDispenseArmorEvent} implemented.
         */
        DISPENSER,
        /**
         * When an armor piece is removed due to it losing all durability.
         */
        BROKE,
        /**
         * When you die causing all armor to unequip
         */
        DEATH,
        /**
         * When you use HatMechanic from Oraxen
         */
        ORAXEN_HAT,
    }
}
