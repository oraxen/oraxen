package io.th0rgal.oraxen.utils.fastinv;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Small and easy Bukkit inventory API with 1.7 to 1.14 support !
 * The project is on <a href="https://github.com/MrMicky-FR/FastInv">GitHub</a>
 *
 * @author MrMicky
 * @version 3.0
 */
public class FastInv implements InventoryHolder {

    private final Map<Integer, Consumer<InventoryClickEvent>> itemHandlers = new HashMap<>();

    private Set<Consumer<InventoryOpenEvent>> openHandlers;
    private Set<Consumer<InventoryCloseEvent>> closeHandlers;
    private Set<Consumer<InventoryClickEvent>> clickHandlers;

    private Predicate<Player> closeFilter;
    private final Inventory inventory;

    /**
     * Create a new FastInv with a custom size.
     *
     * @param size The size of the inventory.
     */
    public FastInv(int size) {
        this(size, InventoryType.CHEST.getDefaultTitle());
    }

    /**
     * Create a new FastInv with a custom size and title.
     *
     * @param size  The size of the inventory.
     * @param title The title (name) of the inventory.
     */
    public FastInv(int size, String title) {
        this(size, InventoryType.CHEST, title);
    }

    /**
     * Create a new FastInv with a custom type.
     *
     * @param type The type of the inventory.
     */
    public FastInv(InventoryType type) {
        this(type, type.getDefaultTitle());
    }

    /**
     * Create a new FastInv with a custom type and title.
     *
     * @param type  The type of the inventory.
     * @param title The title of the inventory.
     */
    public FastInv(InventoryType type, String title) {
        this(0, type, title);
    }

    private FastInv(int size, InventoryType type, String title) {
        if (type == InventoryType.CHEST && size > 0) {
            inventory = Bukkit.createInventory(this, size, title);
        } else {
            inventory = Bukkit.createInventory(this, Objects.requireNonNull(type, "type"), title);
        }

        if (inventory.getHolder() != this) {
            throw new IllegalStateException("Inventory holder is not FastInv, found: " + inventory.getHolder());
        }
    }

    protected void onOpen(InventoryOpenEvent event) {
    }

    protected void onClick(InventoryClickEvent event) {
    }

    protected void onClose(InventoryCloseEvent event) {
    }

    /**
     * Add an {@link ItemStack} to the inventory on the first empty slot.
     *
     * @param item The ItemStack to add
     */
    public void addItem(ItemStack item) {
        addItem(item, null);
    }

    /**
     * Add an {@link ItemStack} to the inventory on the first empty slot with a click handler.
     *
     * @param item    The item to add.
     * @param handler The the click handler for the item.
     */
    public void addItem(ItemStack item, Consumer<InventoryClickEvent> handler) {
        int slot = inventory.firstEmpty();
        if (slot >= 0) {
            setItem(slot, item, handler);
        }
    }

    /**
     * Add an {@link ItemStack} to the inventory on a specific slot.
     *
     * @param slot The slot where to add the item.
     * @param item The item to add.
     */
    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    /**
     * Add an {@link ItemStack} to the inventory on specific slot with a click handler.
     *
     * @param slot    The slot where to add the item.
     * @param item    The item to add.
     * @param handler The click handler for the item
     */
    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);

        if (handler != null) {
            itemHandlers.put(slot, handler);
        } else {
            itemHandlers.remove(slot);
        }
    }

    /**
     * Add an {@link ItemStack} to the inventory on a range of slots.
     *
     * @param slotFrom Starting slot to add the item in.
     * @param slotTo   Ending slot to add the item in.
     * @param item     The item to add.
     */
    public void setItems(int slotFrom, int slotTo, ItemStack item) {
        setItems(slotFrom, slotTo, item, null);
    }

    /**
     * Add an {@link ItemStack} to the inventory on a range of slots with a click handler.
     *
     * @param slotFrom Starting slot to put the item in.
     * @param slotTo   Ending slot to put the item in.
     * @param item     The item to add.
     * @param handler  The click handler for the item
     */
    public void setItems(int slotFrom, int slotTo, ItemStack item, Consumer<InventoryClickEvent> handler) {
        for (int i = slotFrom; i <= slotTo; i++) {
            setItem(i, item, handler);
        }
    }

    /**
     * Add an {@link ItemStack} to the inventory on multiple slots.
     *
     * @param slots The slots where to add the item
     * @param item  The item to add.
     */
    public void setItems(int[] slots, ItemStack item) {
        setItems(slots, item, null);
    }

    /**
     * Add an {@link ItemStack} to the inventory on multiples slots with a click handler.
     *
     * @param slots   The slots where to add the item
     * @param item    The item to add.
     * @param handler The click handler for the item
     */
    public void setItems(int[] slots, ItemStack item, Consumer<InventoryClickEvent> handler) {
        for (int slot : slots) {
            setItem(slot, item, handler);
        }
    }

    /**
     * Remove an {@link ItemStack} from the inventory
     *
     * @param slot The slot where to remove the item
     */
    public void removeItem(int slot) {
        inventory.clear(slot);
        itemHandlers.remove(slot);
    }

    /**
     * Remove multiples {@link ItemStack} from the inventory
     *
     * @param slots The slots where to remove the items
     */
    public void removeItems(int... slots) {
        for (int slot : slots) {
            removeItem(slot);
        }
    }

    /**
     * Add a close filter to prevent players from closing the inventory.
     * To prevent a player from closing the inventory the predicate should return {@code true}
     *
     * @param closeFilter The close filter
     */
    public void setCloseFilter(Predicate<Player> closeFilter) {
        this.closeFilter = closeFilter;
    }

    /**
     * Add a handler to handle inventory open.
     *
     * @param openHandler The handler to add.
     */
    public void addOpenHandler(Consumer<InventoryOpenEvent> openHandler) {
        if (openHandlers == null) {
            openHandlers = new HashSet<>();
        }
        openHandlers.add(openHandler);
    }

    /**
     * Add a handler to handle inventory close.
     *
     * @param closeHandler The handler to add
     */
    public void addCloseHandler(Consumer<InventoryCloseEvent> closeHandler) {
        if (closeHandlers == null) {
            closeHandlers = new HashSet<>();
        }
        closeHandlers.add(closeHandler);
    }

    /**
     * Add a handler to handle inventory click.
     *
     * @param clickHandler The handler to add.
     */
    public void addClickHandler(Consumer<InventoryClickEvent> clickHandler) {
        if (clickHandlers == null) {
            clickHandlers = new HashSet<>();
        }
        clickHandlers.add(clickHandler);
    }

    /**
     * Open the inventory to a player.
     *
     * @param player The player to open the menu.
     */
    public void open(Player player) {
        player.openInventory(inventory);
    }

    /**
     * Get borders of the inventory. If the inventory size is under 27, all slots are returned
     *
     * @return inventory borders
     */
    public int[] getBorders() {
        int size = inventory.getSize();
        return IntStream.range(0, size).filter(i -> size < 27 || i < 9 || i % 9 == 0 || (i - 8) % 9 == 0 || i > size - 9).toArray();
    }

    /**
     * Get the Bukkit inventory
     *
     * @return The Bukkit inventory.
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void handleOpen(InventoryOpenEvent e) {
        onOpen(e);

        if (openHandlers != null) {
            openHandlers.forEach(c -> c.accept(e));
        }
    }

    boolean handleClose(InventoryCloseEvent e) {
        onClose(e);

        if (closeHandlers != null) {
            closeHandlers.forEach(c -> c.accept(e));
        }

        return closeFilter != null && closeFilter.test((Player) e.getPlayer());
    }

    void handleClick(InventoryClickEvent e) {
        onClick(e);

        if (clickHandlers != null) {
            clickHandlers.forEach(c -> c.accept(e));
        }

        Consumer<InventoryClickEvent> clickConsumer = itemHandlers.get(e.getRawSlot());

        if (clickConsumer != null) {
            clickConsumer.accept(e);
        }
    }
}
