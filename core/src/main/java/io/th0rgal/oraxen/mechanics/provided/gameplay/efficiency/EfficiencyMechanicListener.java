package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import fr.euphyllia.energie.model.SchedulerType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class EfficiencyMechanicListener implements Listener {

    public EfficiencyMechanicListener(final MechanicFactory factory) {
        EfficiencyMechanicFactory factory1 = (EfficiencyMechanicFactory) factory;
        PacketAdapter listener = new PacketAdapter(OraxenPlugin.get(),
                ListenerPriority.LOW, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(final PacketEvent event) {
                final PacketContainer packet = event.getPacket();
                final Player player = event.getPlayer();
                if (player.getGameMode() == GameMode.CREATIVE)
                    return;
                final ItemStack item = player.getInventory().getItemInMainHand();
                final String itemID = OraxenItems.getIdByItem(item);
                if (factory.isNotImplementedIn(itemID))
                    return;
                final EfficiencyMechanic mechanic = (EfficiencyMechanic) factory.getMechanic(itemID);
                final StructureModifier<EnumWrappers.PlayerDigType> data = packet
                        .getEnumModifier(EnumWrappers.PlayerDigType.class, 2);
                EnumWrappers.PlayerDigType type;
                try {
                    type = data.getValues().get(0);
                } catch (IllegalArgumentException exception) {
                    type = EnumWrappers.PlayerDigType.SWAP_HELD_ITEMS;
                }
                if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK)
                    OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, player, task ->
                            player.addPotionEffect(new PotionEffect(mechanic.getType(),
                                    20 * 60 * 5,
                                    mechanic.getAmount() - 1,
                                    false, false, false)), null);
                else
                    OraxenPlugin.getScheduler().runTask(SchedulerType.SYNC, player, task ->
                            player.removePotionEffect(mechanic.getType()), null);
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
    }

}
