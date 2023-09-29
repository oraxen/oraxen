package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.audience.Audience;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Tripwire;

import java.util.Map;

public class BlockInfoCommand {

    CommandAPICommand getBlockInfoCommand() {
        return new CommandAPICommand("blockinfo")
                .withPermission("oraxen.command.blockinfo")
                .withArguments(new StringArgument("itemid").replaceSuggestions(ArgumentSuggestions.strings(OraxenItems.getItemNames())))
                .executes((commandSender, args) -> {
                    String argument = (String) args.get("itemid");
                    Audience audience = OraxenPlugin.get().getAudience().sender(commandSender);
                    if (argument == null) return;
                    if (argument.equals("all")) {
                        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
                            if (!OraxenBlocks.isOraxenBlock(entry.getKey())) continue;
                            sendBlockInfo(audience, entry.getKey());
                        }
                    } else {
                        ItemBuilder ib = OraxenItems.getItemById(argument);
                        if (ib == null)
                            audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>No block found with ID</red> <dark_red>" + argument));
                        else sendBlockInfo(audience, argument);
                    }
                });
    }

    private void sendBlockInfo(Audience sender, String itemId) {
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>ItemID: <aqua>" + itemId));
        if (OraxenBlocks.isOraxenNoteBlock(itemId)) {
            NoteBlockMechanic mechanic = (NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(itemId);
            if (mechanic == null) return;
            NoteBlock data = NoteBlockMechanicFactory.createNoteBlockData(mechanic.getCustomVariation());
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Instrument: " + data.getInstrument()));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Note: " + data.getNote().getId()));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Powered: " + data.isPowered()));
        } else if (OraxenBlocks.isOraxenStringBlock(itemId)) {
            StringBlockMechanic mechanic = (StringBlockMechanic) StringBlockMechanicFactory.getInstance().getMechanic(itemId);
            if (mechanic == null) return;
            Tripwire data = (Tripwire) StringBlockMechanicFactory.createTripwireData(mechanic.getCustomVariation());
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Facing: " + data.getFaces()));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Powered: " + data.isPowered()));
            sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Disarmed: " + data.isDisarmed()));
        }
        Logs.newline();
    }
}
