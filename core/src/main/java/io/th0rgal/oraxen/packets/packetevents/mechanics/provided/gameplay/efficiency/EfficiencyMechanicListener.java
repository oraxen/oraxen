package io.th0rgal.oraxen.packets.packetevents.mechanics.provided.gameplay.efficiency;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class EfficiencyMechanicListener implements PacketListener {

    private final EfficiencyMechanicFactory factory;

    public EfficiencyMechanicListener(final EfficiencyMechanicFactory factory) {
        this.factory = factory;
    }

    @Override public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        final var wrapper = new WrapperPlayClientPlayerDigging(event);

        final ItemStack item = player.getInventory().getItemInMainHand();
        final String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;
        final EfficiencyMechanic mechanic = (EfficiencyMechanic) factory.getMechanic(itemID);
        if (wrapper.getAction() == DiggingAction.START_DIGGING) {
            Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                player.addPotionEffect(new PotionEffect(mechanic.getType(),
                    20 * 60 * 5,
                    mechanic.getAmount() - 1,
                    false, false, false)));
        } else {
            Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                player.removePotionEffect(mechanic.getType()));
        }
    }
}
