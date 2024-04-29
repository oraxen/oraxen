package io.th0rgal.oraxen.nms.v1_20_R1.protocol;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanicFactory;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class EfficiencyPacketListener {
    public static PacketListener INSTANCE = new PacketListenerBuilder()
            .add(ServerboundPlayerActionPacket.class, (serverPlayer, serverboundPlayerActionPacket) -> {
                final Player player = serverPlayer.getBukkitEntity();
                if (player.getGameMode() == GameMode.CREATIVE) return true;
                final EfficiencyMechanic mechanic = EfficiencyMechanicFactory.get().getMechanic(player.getInventory().getItemInMainHand());
                if (mechanic == null) return true;
                ServerboundPlayerActionPacket.Action action = serverboundPlayerActionPacket.getAction();
                if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                    Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                            player.addPotionEffect(new PotionEffect(mechanic.getType(),
                                    20 * 60 * 5,
                                    mechanic.getAmount() - 1,
                                    false, false, false)));
                } else {
                    Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                            player.removePotionEffect(mechanic.getType()));
                }
                return true;
            })
            .build();
}
