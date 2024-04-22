package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;

public class EfficiencyMechanicListener implements Listener {

    public EfficiencyMechanicListener() {
        PacketAdapter listener = new PacketAdapter(OraxenPlugin.get(),
                ListenerPriority.LOW, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(final PacketEvent event) {
                final PacketContainer packet = event.getPacket();
                final Player player = event.getPlayer();
                if (player.getGameMode() == GameMode.CREATIVE) return;
                final EfficiencyMechanic mechanic = EfficiencyMechanicFactory.get().getMechanic(player.getInventory().getItemInMainHand());
                if (mechanic == null) return;
                final StructureModifier<EnumWrappers.PlayerDigType> data = packet
                        .getEnumModifier(EnumWrappers.PlayerDigType.class, 2);
                EnumWrappers.PlayerDigType type;
                try {
                    type = data.getValues().get(0);
                } catch (IllegalArgumentException exception) {
                    type = EnumWrappers.PlayerDigType.SWAP_HELD_ITEMS;
                }
                if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK)
                    Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                            player.addPotionEffect(new PotionEffect(mechanic.getType(),
                                    20 * 60 * 5,
                                    mechanic.getAmount() - 1,
                                    false, false, false)));
                else
                    Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                            player.removePotionEffect(mechanic.getType()));
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
    }

}
