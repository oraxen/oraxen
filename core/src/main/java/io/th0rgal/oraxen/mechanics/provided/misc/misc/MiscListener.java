package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;

import java.util.Arrays;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE;
import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK;

public class MiscListener implements Listener {

    public MiscListener(MiscMechanicFactory factory) {
        if (VersionUtil.isPaperServer()) {
            OraxenPlugin.get().getServer().getPluginManager().registerEvents(new PaperOnlyListeners(factory), OraxenPlugin.get());
        }
    }

    @EventHandler
    public void onHopperCompost(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        if (source.getType() != InventoryType.HOPPER || event.getDestination().getType() != InventoryType.COMPOSTER) return;
        Block hopper = source.getLocation() != null ? source.getLocation().getBlock() : null;
        if (hopper == null || hopper.getType() != Material.HOPPER) return;
        Block composter = hopper.getRelative(BlockFace.DOWN);
        if (composter.getType() != Material.COMPOSTER) return;
        if (!(composter.getBlockData() instanceof Levelled levelled)) return;

        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(event.getItem());
        if (mechanic == null || !mechanic.isCompostable()) return;

        if (levelled.getLevel() < levelled.getMaximumLevel()) {
            if (Math.random() <= 0.65) levelled.setLevel(levelled.getLevel() + 1); // Same as wheat
            composter.setBlockData(levelled);
            composter.getWorld().playEffect(composter.getLocation(), Effect.COMPOSTER_FILL_ATTEMPT, 0, 1);

            ItemStack item = event.getItem();
            item.setAmount(item.getAmount() - 1);
            event.setItem(item);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCompost(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null) return;
        if (item == null || block == null || block.getType() != Material.COMPOSTER) return;
        if (!(block.getBlockData() instanceof Levelled levelled)) return;
        if (!ProtectionLib.canInteract(player, block.getLocation())) return;
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(item);
        if (mechanic == null || !mechanic.isCompostable()) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        event.setUseInteractedBlock(Event.Result.ALLOW);
        event.setUseItemInHand(Event.Result.ALLOW);

        if (levelled.getLevel() < levelled.getMaximumLevel()) {
            if (Math.random() <= 0.65) levelled.setLevel(levelled.getLevel() + 1); // Same as wheat
            block.setBlockData(levelled);
            block.getWorld().playEffect(block.getLocation(), Effect.COMPOSTER_FILL_ATTEMPT, 0, 1);
            if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
            Utils.swingHand(player, event.getHand());
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStripLog(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(item);

        if (block == null || !Tag.LOGS.isTagged(block.getType())) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || item == null) return;
        if (mechanic == null || !mechanic.canStripLogs()) return;

        if (item.getItemMeta() instanceof Damageable axeDurabilityMeta) {
            int durability = axeDurabilityMeta.getDamage();
            int maxDurability = item.getType().getMaxDurability();

            if (durability + 1 <= maxDurability) {
                axeDurabilityMeta.setDamage(durability + 1);
                item.setItemMeta(axeDurabilityMeta);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                item.setType(Material.AIR);
            }
        }
        block.setType(getStrippedLog(block.getType()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiglinAggro(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Piglin)) return;
        if (!(event.getTarget() instanceof Player player)) return;

        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return;

        if (shouldPreventPiglinAggro(equipment.getItemInMainHand()))
            event.setCancelled(true);
        if (shouldPreventPiglinAggro(equipment.getItemInOffHand()))
            event.setCancelled(true);
        if (Arrays.stream(equipment.getArmorContents()).anyMatch(this::shouldPreventPiglinAggro))
            event.setCancelled(true);
    }

    // Since EntityDamageByBlockEvent apparently does not trigger for fire, use this aswell
    @EventHandler
    public void onItemBurnFire(EntityDamageEvent event) {
        if (event.getCause() != FIRE && event.getCause() != FIRE_TICK) return;
        MiscMechanic mechanic = getMiscMechanic(event.getEntity());
        if (mechanic == null || mechanic.burnsInFire()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemBurn(EntityDamageByBlockEvent event) {
        MiscMechanic mechanic = getMiscMechanic(event.getEntity());
        if (mechanic == null) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.CONTACT && !mechanic.breaksFromCactus()) {
            event.setCancelled(true);
        } else if (cause == EntityDamageEvent.DamageCause.LAVA && !mechanic.burnsInLava()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisableVanillaInteraction(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(event.getItem());
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setUseItemInHand(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableItemConsume(PlayerItemConsumeEvent event) {
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(event.getItem());
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(event.getConsumable());
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setConsumeItem(false);
        event.setCancelled(true);
        player.updateInventory(); // Client desyncs and "removes" an arrow
        //TODO See if crossbows can have their loading phase cancelled, currently impossible to check loaded projectile
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableHorseArmorEquip(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof HorseInventory)) return;
        ItemStack item;
        if (event.getAction() == InventoryAction.PLACE_ALL && event.getClickedInventory() instanceof HorseInventory)
            item = event.getCursor();
        else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClickedInventory() instanceof PlayerInventory)
            item = event.getCurrentItem();
        else return;


        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(item);
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableHorseArmorEquip(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Horse)) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(item);
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        if (item.getType().name().endsWith("_HORSE_ARMOR")) {
            event.setCancelled(true);
            //player.updateInventory();
        }
    }

    private MiscMechanic getMiscMechanic(Entity entity) {
        if (!(entity instanceof Item item)) return null;
        ItemStack itemStack = item.getItemStack();
        String itemID = OraxenItems.getIdByItem(itemStack);
        if (itemID == null) return null;

        return MiscMechanicFactory.get().getMechanic(itemID);
    }

    private Material getStrippedLog(Material log) {
        return switch (log) {
            case OAK_LOG -> Material.STRIPPED_OAK_LOG;
            case SPRUCE_LOG -> Material.STRIPPED_SPRUCE_LOG;
            case BIRCH_LOG -> Material.STRIPPED_BIRCH_LOG;
            case JUNGLE_LOG -> Material.STRIPPED_JUNGLE_LOG;
            case ACACIA_LOG -> Material.STRIPPED_ACACIA_LOG;
            case DARK_OAK_LOG -> Material.STRIPPED_DARK_OAK_LOG;
            case CRIMSON_STEM -> Material.STRIPPED_CRIMSON_STEM;
            case WARPED_STEM -> Material.STRIPPED_WARPED_STEM;
            case MANGROVE_LOG -> Material.STRIPPED_MANGROVE_LOG;

            case OAK_WOOD -> Material.STRIPPED_OAK_WOOD;
            case SPRUCE_WOOD -> Material.STRIPPED_SPRUCE_WOOD;
            case BIRCH_WOOD -> Material.STRIPPED_BIRCH_WOOD;
            case JUNGLE_WOOD -> Material.STRIPPED_JUNGLE_WOOD;
            case ACACIA_WOOD -> Material.STRIPPED_ACACIA_WOOD;
            case DARK_OAK_WOOD -> Material.STRIPPED_DARK_OAK_WOOD;
            case CRIMSON_HYPHAE -> Material.STRIPPED_CRIMSON_HYPHAE;
            case WARPED_HYPHAE -> Material.STRIPPED_WARPED_HYPHAE;
            case MANGROVE_WOOD -> Material.STRIPPED_MANGROVE_WOOD;

            default -> null;
        };
    }

    private boolean shouldPreventPiglinAggro(ItemStack itemStack) {
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(OraxenItems.getIdByItem(itemStack));
        return mechanic != null && mechanic.piglinIgnoreWhenEquipped();
    }

    private record PaperOnlyListeners(MiscMechanicFactory factory) implements Listener {


    }
}
