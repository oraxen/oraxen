package io.th0rgal.oraxen.command.commands;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.oraxen.chimerate.commons.command.tree.nodes.Argument;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;
import com.syntaxphoenix.syntaxapi.utils.java.Arrays;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.command.types.SpecificWordType;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.settings.MessageOld;
import io.th0rgal.oraxen.settings.Plugin;

public class Repair {

    public static CommandInfo build() {
        return new CommandInfo("repair", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName()).alias(info.getAliases());

            builder.optionally(Argument.of("all", SpecificWordType.of("all")).executes((sender, context) -> {

                if (!OraxenPermission.COMMAND_REPAIR.has(sender)) {
                    MessageOld.DONT_HAVE_PERMISSION.send(sender);
                    return;
                }

                if (!(sender instanceof Player)) {
                    MessageOld.NOT_A_PLAYER_ERROR.send(sender);
                    return;
                }

                Player player = (Player) sender;
                if (context.getOptionalArgument("all", String.class) != null
                        && OraxenPermission.COMMAND_REPAIR_EVERYTHING.has(sender)) {
                    ItemStack[] items = Arrays.merge(size -> new ItemStack[size],
                        player.getInventory().getStorageContents(), player.getInventory().getArmorContents());
                    int failed = 0;
                    for (int index = 0; index < items.length; index++) {
                        ItemStack item = items[index];
                        if (item == null || item.getType() == Material.AIR)
                            continue;
                        if (repairPlayerItem(item)) {
                            MessageOld.CANNOT_BE_REPAIRED.send(sender, item.getItemMeta().getDisplayName());
                            failed++;
                        }
                    }
                    MessageOld.REPAIRED_ITEMS.send(sender, (items.length - failed) + "");
                } else {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item == null || item.getType() == Material.AIR) {
                        MessageOld.CANNOT_BE_REPAIRED_INVALID.send(sender);
                        return;
                    }
                    if (repairPlayerItem(item))
                        MessageOld.CANNOT_BE_REPAIRED.send(sender, item.getItemMeta().getDisplayName());
                }
            }));

            return builder;
        });
    }

    private static boolean repairPlayerItem(ItemStack itemStack) {
        String itemId = OraxenItems.getIdByItem(itemStack);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof Damageable))
            return false;
        Damageable damageable = (Damageable) itemMeta;
        DurabilityMechanicFactory durabilityFactory = DurabilityMechanicFactory.get();
        if (durabilityFactory.isNotImplementedIn(itemId)) {
            if ((boolean) Plugin.REPAIR_COMMAND_ORAXEN_DURABILITY.getValue()) // not oraxen item
                return false;
            if (damageable.getDamage() == 0) // full durability
                return false;
        } else {
            DurabilityMechanic durabilityMechanic = (DurabilityMechanic) durabilityFactory.getMechanic(itemId);
            PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
            int realMaxDurability = durabilityMechanic.getItemMaxDurability();
            int damage = realMaxDurability
                    - persistentDataContainer.get(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER);
            if (damage == 0) // full durability
                return false;
            persistentDataContainer.set(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER,
                realMaxDurability);
        }
        damageable.setDamage(0);
        itemStack.setItemMeta((ItemMeta) damageable);
        return true;
    }
}
