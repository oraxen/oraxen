package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DeathProtection;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public class TotemAnimationCommand {

    CommandAPICommand getTotemAnimationCommand() {
        return new CommandAPICommand("totem-animation")
                .withPermission("oraxen.command.totem-animation")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("player"),
                        new TextArgument("item")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> getItemSuggestions()))
                )
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    String itemId = (String) args.get("item");
                    ItemStack itemStack = parseItem(itemId);
                    if (itemStack == null) {
                        Message.ITEM_NOT_FOUND.send(sender, AdventureUtils.tagResolver("item", itemId));
                        return;
                    }

                    playAnimation(target, addDeathProtection(itemStack));
                    Message.TOTEM_ANIMATION_SUCCESS.send(sender,
                            AdventureUtils.tagResolver("player", target.getName()),
                            AdventureUtils.tagResolver("item", itemId));
                });
    }

    private String[] getItemSuggestions() {
        return Stream.concat(
                Arrays.stream(OraxenItems.getItemNames()),
                Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .map(material -> "minecraft:" + material.name().toLowerCase(Locale.ROOT))
        ).toArray(String[]::new);
    }

    private ItemStack parseItem(String itemId) {
        ItemBuilder itemBuilder = OraxenItems.getItemById(itemId);
        if (itemBuilder != null) {
            return itemBuilder.build();
        }

        String materialName = itemId.toUpperCase(Locale.ROOT);
        if (materialName.startsWith("MINECRAFT:")) {
            materialName = materialName.substring("MINECRAFT:".length());
        }

        Material material = Material.matchMaterial(materialName);
        return material != null && material.isItem() ? new ItemStack(material) : null;
    }

    private void playAnimation(Player target, ItemStack animationItem) {
        ItemStack previousMainHand = target.getInventory().getItemInMainHand().clone();
        ItemStack previousOffHand = target.getInventory().getItemInOffHand().clone();
        boolean mainHandIsTotem = isDeathProtectionItem(previousMainHand);

        if (mainHandIsTotem) {
            target.sendEquipmentChange(target, EquipmentSlot.HAND, null);
        }

        target.sendEquipmentChange(target, EquipmentSlot.OFF_HAND, animationItem);
        sendTotemStatus(target);

        if (mainHandIsTotem) {
            target.sendEquipmentChange(target, EquipmentSlot.HAND, previousMainHand);
        }

        target.sendEquipmentChange(target, EquipmentSlot.OFF_HAND, previousOffHand);
    }

    @SuppressWarnings("deprecation")
    private void sendTotemStatus(Player target) {
        if (VersionUtil.isPaperServer()) {
            target.sendEntityEffect(EntityEffect.PROTECTED_FROM_DEATH, target);
        } else {
            target.playEffect(EntityEffect.TOTEM_RESURRECT);
        }
    }

    private ItemStack addDeathProtection(ItemStack itemStack) {
        if (!VersionUtil.atOrAbove("1.21.2") || itemStack.getType() == Material.AIR) {
            return itemStack;
        }

        itemStack.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection());
        return itemStack;
    }

    private boolean isDeathProtectionItem(ItemStack itemStack) {
        if (itemStack.getType() == Material.TOTEM_OF_UNDYING) {
            return true;
        }

        return VersionUtil.atOrAbove("1.21.2") && itemStack.hasData(DataComponentTypes.DEATH_PROTECTION);
    }
}
