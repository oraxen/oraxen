package io.th0rgal.oraxen.commands;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.utils.java.Arrays;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanicFactory;
import io.th0rgal.oraxen.utils.itemsvisualizer.AllItemsInventory;
import io.th0rgal.oraxen.utils.itemsvisualizer.FileInventory;
import org.bukkit.Material;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public class CommandsManager {

    public void loadCommands() {
        new CommandAPICommand("oraxen")
                .withAliases("o", "oxn")
                .withPermission("oraxen.command")
                .withSubcommand(getInvCommand())
                .withSubcommand(getGiveCommand())
                .withSubcommand(getRepairCommand())
                .executes((sender, args) -> {
                    Message.COMMAND_HELP.send(sender);
                })
                .register();
    }

    private CommandAPICommand getInvCommand() {
        return new CommandAPICommand("inventory")
                .withAliases("inv")
                .withPermission("oraxen.command.inventory")
                .withArguments(new StringArgument("type").replaceSuggestions(info -> new String[]{"all", "sorted"}))
                .executes((sender, args) -> {
                    if (sender instanceof Player player) {
                        if (args[0].equals("sorted"))
                            new FileInventory(0).open(player);
                        else new AllItemsInventory(0).open(player);
                    } else
                        Message.NOT_PLAYER.send(sender);
                });
    }

    private CommandAPICommand getGiveCommand() {
        return new CommandAPICommand("give")
                .withPermission("oraxen.command.give")
                .withArguments(new PlayerArgument("target"))
                .withArguments(new StringArgument("item").replaceSuggestions(info -> OraxenItems.getItemNames()))
                .withArguments(new IntegerArgument("amount"))
                .executes((sender, args) -> {
                    Player target = (Player) args[0];
                    ItemBuilder itemBuilder = OraxenItems.getItemById((String) args[1]);
                    int amount = (int) args[2];
                    int max = itemBuilder.getMaxStackSize();
                    int slots = amount / max + (max % amount > 0 ? 1 : 0);
                    ItemStack[] items = itemBuilder.buildArray(slots > 36 ? (amount = max * 36) : amount);
                    target.getInventory().addItem(items);
                    Message.GIVE_PLAYER
                            .send(sender, "player", target.getName(), "amount", String.valueOf(amount),
                                    "item", OraxenItems.getIdByItem(itemBuilder));
                });
    }

    private CommandAPICommand getRepairCommand() {
        return new CommandAPICommand("repair")
                .withPermission("oraxen.command.repair")
                .withArguments(new StringArgument("type").replaceSuggestions(info -> new String[]{"hand", "all"}))
                .executes((sender, args) -> {

                    if (sender instanceof Player player) {
                        if ((args[0]).equals("hand")) {
                            ItemStack item = player.getInventory().getItemInMainHand();
                            if (item == null || item.getType() == Material.AIR) {
                                Message.CANNOT_BE_REPAIRED_INVALID.send(sender);
                                return;
                            }
                            if (!repairPlayerItem(item))
                                Message.CANNOT_BE_REPAIRED.send(sender);

                        } else {
                            if (player.hasPermission("oraxen.command.repair.all")) {
                                ItemStack[] items = Arrays
                                        .merge(ItemStack[]::new, player.getInventory().getStorageContents(),
                                                player.getInventory().getArmorContents());
                                int failed = 0;
                                for (ItemStack item : items) {
                                    if (item == null || item.getType() == Material.AIR)
                                        continue;
                                    if (!repairPlayerItem(item)) {
                                        Message.CANNOT_BE_REPAIRED.send(sender);
                                        failed++;
                                    }
                                }
                                Message.REPAIRED_ITEMS.send(sender);
                            } else {
                                Message.NO_PERMISSION.send(sender);
                            }
                        }
                    } else {
                        Message.NOT_PLAYER.send(sender);
                    }
                });
    }


    private static boolean repairPlayerItem(ItemStack itemStack) {
        String itemId = OraxenItems.getIdByItem(itemStack);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable))
            return false;
        DurabilityMechanicFactory durabilityFactory = DurabilityMechanicFactory.get();
        if (durabilityFactory.isNotImplementedIn(itemId)) {
            if ((boolean) Settings.REPAIR_COMMAND_ORAXEN_DURABILITY.getValue()) // not oraxen item
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
            persistentDataContainer
                    .set(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER, realMaxDurability);
        }
        damageable.setDamage(0);
        itemStack.setItemMeta((ItemMeta) damageable);
        return true;
    }


}
