package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class RepairCommand {

    @Deprecated(forRemoval = true, since = "1.20.6")
    CommandAPICommand getRepairCommand() {
        return new CommandAPICommand("repair")
                .withPermission("oraxen.command.repair")
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("hand", "all")))
                .executes((sender, args) -> {

                    if (sender instanceof Player player) if (args.get("type").equals("hand")) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item.getType() == Material.AIR) {
                            Message.CANNOT_BE_REPAIRED_INVALID.send(sender);
                            return;
                        }
                        if (repairPlayerItem(item))
                            Message.CANNOT_BE_REPAIRED.send(sender);

                    } else if (player.hasPermission("oraxen.command.repair.all")) {
                        ItemStack[] items = ArrayUtils.addAll(player.getInventory().getStorageContents(),
                                player.getInventory().getArmorContents());
                        int failed = 0;
                        for (ItemStack item : items) {
                            if (item == null || item.getType() == Material.AIR)
                                continue;
                            if (repairPlayerItem(item)) {
                                Message.CANNOT_BE_REPAIRED.send(sender);
                                failed++;
                            }
                        }
                        Message.REPAIRED_ITEMS.send(sender,
                                AdventureUtils.tagResolver("amount", String.valueOf(items.length - failed)));
                    } else
                        Message.NO_PERMISSION.send(sender,
                                AdventureUtils.tagResolver("permission", "oraxen.command.repair.all"));
                    else
                        Message.NOT_PLAYER.send(sender);
                });
    }


    private static boolean repairPlayerItem(ItemStack itemStack) {
        String itemId = OraxenItems.getIdByItem(itemStack);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable)) return true;
        DurabilityMechanicFactory durabilityFactory = DurabilityMechanicFactory.get();
        if (durabilityFactory.isNotImplementedIn(itemId)) {
            if ((boolean) Settings.REPAIR_COMMAND_ORAXEN_DURABILITY.getValue()) // not oraxen item
                return true;
            if (damageable.getDamage() == 0) // full durability
                return true;
        } else {
            DurabilityMechanic durabilityMechanic = durabilityFactory.getMechanic(itemId);
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            int realMaxDurability = durabilityMechanic.getItemMaxDurability();
            int damage = realMaxDurability - pdc.get(DurabilityMechanic.DURABILITY_KEY, PersistentDataType.INTEGER);
            if (damage == 0)
                return true; // full durability
            pdc.set(DurabilityMechanic.DURABILITY_KEY, PersistentDataType.INTEGER, realMaxDurability);
        }
        damageable.setDamage(0);
        itemStack.setItemMeta(damageable);
        return false;
    }

}
