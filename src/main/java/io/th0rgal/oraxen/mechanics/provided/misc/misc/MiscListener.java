package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.Arrays;

public class MiscListener implements Listener {

    private final MiscMechanicFactory factory;

    public MiscListener(MiscMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreventAnvilRename(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        ItemStack inputItem = anvil.getFirstItem();
        ItemStack resultItem = event.getResult();
        if (inputItem == null || resultItem == null || inputItem.getType() == Material.AIR) return;

        MiscMechanic mechanic = factory.getMechanic(inputItem);
        if (mechanic == null || !mechanic.preventsRenaming()) return;

        // Only cancel when the user actually typed a rename. Repair/enchant operations leave
        // renameText empty (or matching the original plain-text display name) and must go through.
        // Use the item-meta name rather than ItemStack.displayName(): the latter wraps the
        // name in square brackets ([Cool Sword]) but the anvil view returns the raw text,
        // so comparing against the bracketed form would never match.
        String renameText = VersionUtil.atOrAbove("1.21.1")
                ? ((AnvilView) event.getView()).getRenameText()
                : anvil.getRenameText();
        if (renameText == null || renameText.isEmpty()) return;

        ItemMeta inputMeta = inputItem.getItemMeta();
        if (inputMeta != null && ItemUtils.hasDisplayName(inputMeta)) {
            String inputPlain = AdventureUtils.PLAIN_TEXT.serialize(ItemUtils.getDisplayName(inputMeta));
            if (renameText.equals(inputPlain)) return;
        }

        event.setResult(null);
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

        MiscMechanic mechanic = factory.getMechanic(event.getItem());
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
        if (!AntiGriefLib.canInteract(player, block.getLocation())) return;
        MiscMechanic mechanic = factory.getMechanic(item);
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

        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || item == null) return;
        MiscMechanic mechanic = factory.getMechanic(item);
        if (mechanic == null || !mechanic.canStripLogs()) return;
        Material strippedLog = Material.matchMaterial("STRIPPED_" + block.getType().name());
        if (strippedLog == null) return;
        if (!AntiGriefLib.canBuild(player, block.getLocation())) return;

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
        BlockData strippedData = strippedLog.createBlockData();
        if (block.getBlockData() instanceof Orientable log && strippedData instanceof Orientable stripped)
            stripped.setAxis(log.getAxis());
        block.setBlockData(strippedData);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableVanillaInteraction(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        MiscMechanic mechanic = factory.getMechanic(event.getItem());
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setUseItemInHand(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableItemConsume(PlayerItemConsumeEvent event) {
        MiscMechanic mechanic = factory.getMechanic(event.getItem());
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        MiscMechanic mechanic = factory.getMechanic(event.getConsumable());
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setConsumeItem(false);
        event.setCancelled(true);
        // Resync the inventory so the client does not keep showing the consumable as spent
        player.updateInventory();
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


        MiscMechanic mechanic = factory.getMechanic(item);
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDisableHorseArmorEquip(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Horse)) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        MiscMechanic mechanic = factory.getMechanic(item);
        if (mechanic == null || !mechanic.isVanillaInteractionDisabled()) return;
        if (item.getType().name().endsWith("_HORSE_ARMOR")) {
            event.setCancelled(true);
        }
    }

    private boolean shouldPreventPiglinAggro(ItemStack itemStack) {
        MiscMechanic mechanic = factory.getMechanic(OraxenItems.getIdByItem(itemStack));
        return mechanic != null && mechanic.piglinIgnoreWhenEquipped();
    }

}
