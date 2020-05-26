package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.settings.Message;
import io.th0rgal.oraxen.settings.Plugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class Repair implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        boolean repairAll = args.length >= 2 && args[1].equals("all");

        if (repairAll) {
            if (!sender.hasPermission("oraxen.command.repair.all")) {
                Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.repair.all");
                return false;
            }
        } else if (!sender.hasPermission("oraxen.command.repair.hand")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.repair.hand");
            return false;
        }

        if (!(sender instanceof Player)) {
            Message.NOT_A_PLAYER_ERROR.send(sender);
            return true;
        }
        Player player = (Player) sender;

        if (repairAll) {
            boolean repaired = false;
            for (ItemStack item : player.getInventory().getContents())
                if (!repaired && repairPlayerItem(item))
                    repaired = true;
            if (!repaired)
                Message.CANNOT_BE_REPAIRED.send(sender);
        } else {
            boolean repairResult = repairPlayerItem(player.getInventory().getItemInMainHand());
            if (!repairResult)
                Message.CANNOT_BE_REPAIRED.send(sender);
        }
        return true;
    }

    private boolean repairPlayerItem(ItemStack toRepair) {
        String toRepairId = OraxenItems.getIdByItem(toRepair);
        ItemMeta toRepairMeta = toRepair.getItemMeta();
        if (!(toRepairMeta instanceof Damageable))
            return false;
        Damageable damageable = (Damageable) toRepairMeta;
        DurabilityMechanicFactory durabilityFactory = DurabilityMechanicFactory.get();
        if (durabilityFactory.isNotImplementedIn(toRepairId)) {
            if ((boolean) Plugin.REPAIR_COMMAND_ORAXEN_DURABILITY.getValue()) // not oraxen item
                return false;
            if (damageable.getDamage() == 0) // full durability
                return false;
        } else {
            DurabilityMechanic durabilityMechanic = (DurabilityMechanic) durabilityFactory.getMechanic(toRepairId);
            PersistentDataContainer persistentDataContainer = toRepairMeta.getPersistentDataContainer();
            int realMaxDurability = durabilityMechanic.getItemMaxDurability();
            int damage = realMaxDurability - persistentDataContainer.get(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER);
            if (damage == 0) // full durability
                return false;
            persistentDataContainer.set(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER,
                    realMaxDurability);
        }
        damageable.setDamage(0);
        toRepair.setItemMeta((ItemMeta) damageable);
        return true;
    }


}
