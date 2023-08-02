package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.protocol.MinecraftVersion;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.ProtocolMapping;
import io.th0rgal.oraxen.protocol.handler.NMSPacketHandler;
import io.th0rgal.oraxen.protocol.packet.PacketFlow;
import io.th0rgal.oraxen.utils.CacheInvoker;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class EfficiencyMechanicListener implements Listener, NMSPacketHandler {

    private static final ProtocolMapping<Integer> PLAYER_ACTION_ID_MAPPING =
            new ProtocolMapping<Integer>()
                    .add(MinecraftVersion.MINECRAFT_1_8, 0x07)
                    .add(MinecraftVersion.MINECRAFT_1_9, 0x13)
                    .add(MinecraftVersion.MINECRAFT_1_12, 0x14)
                    .add(MinecraftVersion.MINECRAFT_1_13, 0x18)
                    .add(MinecraftVersion.MINECRAFT_1_14, 0x1A)
                    .add(MinecraftVersion.MINECRAFT_1_16, 0x1B)
                    .add(MinecraftVersion.MINECRAFT_1_17, 0x1A)
                    .add(MinecraftVersion.MINECRAFT_1_19, 0x1C)
                    .add(MinecraftVersion.MINECRAFT_1_19_1, 0x1D)
                    .add(MinecraftVersion.MINECRAFT_1_19_3, 0x1C)
                    .add(MinecraftVersion.MINECRAFT_1_19_4, 0x1D)
                    .build();

    private static final CacheInvoker INVOKER = CacheInvoker.get();

    private final ProtocolInjector protocolInjector;
    private final MechanicFactory factory;
    private final int playerActionId;
    private MethodHandle statusGetter;

    public EfficiencyMechanicListener(MechanicFactory factory) {
        this.protocolInjector = OraxenPlugin.get().getProtocolInjector();
        this.factory = factory;
        this.playerActionId = PLAYER_ACTION_ID_MAPPING.get(protocolInjector.getServerVersion());
        this.protocolInjector.addNMSHandler(this, PacketFlow.SERVERBOUND, playerActionId);
    }

    @Override
    public void initialize() {
        try {
            Class<?> packetClass = protocolInjector.getNMSPacketClass(PacketFlow.SERVERBOUND, playerActionId);
            Class<?> actionClass = packetClass.getClasses()[0];
            MethodHandles.Lookup packetLookup = MethodHandles.privateLookupIn(packetClass, MethodHandles.lookup());
            for (Field field : packetClass.getDeclaredFields()) {
                if (field.getType() != actionClass) {
                    continue;
                }

                field.setAccessible(true);
                statusGetter = packetLookup.unreflectGetter(field);
                break;
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean handle(Player player, PacketFlow flow, int id, Object packet) {
        try {
            if (player.getGameMode() == GameMode.CREATIVE) {
                return true;
            }

            final ItemStack item = player.getInventory().getItemInMainHand();
            final String itemID = OraxenItems.getIdByItem(item);
            if (factory.isNotImplementedIn(itemID)) {
                return true;
            }

            final EfficiencyMechanic mechanic = (EfficiencyMechanic) factory.getMechanic(itemID);

            Enum<?> status = (Enum<?>) INVOKER.cache(statusGetter).invoke(packet);
            int statusId = status.ordinal();
            if (statusId == 0) {
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                            player.addPotionEffect(new PotionEffect(mechanic.getType(),
                                    20 * 60 * 5,
                                    mechanic.getAmount() - 1,
                                    false, false, false)));
            } else {
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                            player.removePotionEffect(mechanic.getType()));
            }
        } catch (Throwable ignored) {
        }

        return true;
    }
}
