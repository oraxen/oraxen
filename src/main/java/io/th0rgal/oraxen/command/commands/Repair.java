package io.th0rgal.oraxen.command.commands;

import static io.th0rgal.oraxen.command.argument.ArgumentHelper.get;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;
import com.syntaxphoenix.syntaxapi.utils.java.Arrays;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.settings.MessageOld;
import io.th0rgal.oraxen.settings.Plugin;

public class Repair extends OraxenCommand {

    public static final OraxenCommand COMMAND = new Repair();

    public static CommandInfo info() {
        return new CommandInfo("repair", COMMAND);
    }

    private Repair() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {
        CommandSender sender = info.getSender();

        if (Conditions.mixed(Conditions.reqPerm(OraxenPermission.COMMAND_REPAIR), Conditions.player(Message.NOT_PLAYER)).isFalse(sender)) {
            return;
        }

        Player player = (Player) sender;
        if (get(arguments, 1, ArgumentType.STRING).map(argument -> argument.asString().getValue().equals("all")).orElse(false)
            && OraxenPermission.COMMAND_REPAIR_EVERYTHING.has(sender)) {
            ItemStack[] items = Arrays.merge(ItemStack[]::new, player.getInventory().getStorageContents(), player.getInventory().getArmorContents());
            int failed = 0;
            for (ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR)
                    continue;
                if (!repairPlayerItem(item)) {
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
            if (!repairPlayerItem(item))
                MessageOld.CANNOT_BE_REPAIRED.send(sender, item.getItemMeta().getDisplayName());
        }
    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        DefaultCompletion completion = new DefaultCompletion();

        if (Conditions.mixed(Conditions.hasPerm(OraxenPermission.COMMAND_REPAIR_EVERYTHING), Conditions.player()).isFalse(info.getSender())) {
            return completion;
        }

        if (arguments.count() == 1) {
            completion.add(new StringArgument("all"));
        }

        return completion;
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
            int damage = realMaxDurability - persistentDataContainer.get(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER);
            if (damage == 0) // full durability
                return false;
            persistentDataContainer.set(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER, realMaxDurability);
        }
        damageable.setDamage(0);
        itemStack.setItemMeta((ItemMeta) damageable);
        return true;
    }
}
