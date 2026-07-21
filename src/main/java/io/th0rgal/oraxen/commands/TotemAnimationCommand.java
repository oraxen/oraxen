package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.commands.arguments.ArgumentSuggestions;
import io.th0rgal.oraxen.commands.arguments.EntitySelectorArgument;
import io.th0rgal.oraxen.commands.arguments.TextArgument;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.packets.PacketAdapter;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class TotemAnimationCommand {

    private static final AtomicReference<DeathProtectionAPI> DEATH_PROTECTION_API = new AtomicReference<>();
    private static final AtomicReference<PacketEventsAPI> PACKET_EVENTS_API = new AtomicReference<>();
    private static final Object DEATH_PROTECTION_INIT_LOCK = new Object();
    private static final Object PACKET_EVENTS_INIT_LOCK = new Object();
    private static volatile boolean deathProtectionInitialized;
    private static volatile boolean packetEventsInitialized;
    private static volatile boolean loggedDeathProtectionFailure;
    private static volatile boolean loggedPacketEventsFailure;

    OraxenCommand getTotemAnimationCommand() {
        return new OraxenCommand("totem-animation")
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
        if (itemBuilder != null) return itemBuilder.build();

        String materialName = itemId.toUpperCase(Locale.ROOT);
        if (materialName.startsWith("MINECRAFT:")) materialName = materialName.substring("MINECRAFT:".length());

        Material material = Material.matchMaterial(materialName);
        return material != null && material.isItem() ? new ItemStack(material) : null;
    }

    private void playAnimation(Player target, ItemStack animationItem) {
        ItemStack previousMainHand = target.getInventory().getItemInMainHand().clone();
        ItemStack previousOffHand = target.getInventory().getItemInOffHand().clone();
        boolean mainHandIsTotem = isDeathProtectionItem(previousMainHand);

        if (mainHandIsTotem) target.sendEquipmentChange(target, EquipmentSlot.HAND, null);

        target.sendEquipmentChange(target, EquipmentSlot.OFF_HAND, animationItem);
        sendTotemStatus(target);

        if (mainHandIsTotem) target.sendEquipmentChange(target, EquipmentSlot.HAND, previousMainHand);

        target.sendEquipmentChange(target, EquipmentSlot.OFF_HAND, previousOffHand);
    }

    @SuppressWarnings("deprecation")
    private void sendTotemStatus(Player target) {
        EntityEffect protectedFromDeath = getProtectedFromDeathEffect();
        if (VersionUtil.isPaperServer() && protectedFromDeath != null) {
            target.sendEntityEffect(protectedFromDeath, target);
        } else if (PacketAdapter.isPacketEventsEnabled() && sendPacketEventsTotemStatus(target)) {
            return;
        } else {
            target.playEffect(EntityEffect.TOTEM_RESURRECT);
        }
    }

    private static EntityEffect getProtectedFromDeathEffect() {
        try {
            return EntityEffect.valueOf("PROTECTED_FROM_DEATH");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean sendPacketEventsTotemStatus(Player target) {
        PacketEventsAPI api = getPacketEventsAPI();
        if (api == null) return false;

        try {
            Object packetEventsAPI = api.getApiMethod.invoke(null);
            Object playerManager = api.getPlayerManagerMethod.invoke(packetEventsAPI);
            // entityStatusPacketUsesByte is derived from the resolved constructor signature in getPacketEventsAPI(),
            // so the boxed status value below always matches the primitive parameter type PacketEvents expects.
            Object packet = api.entityStatusPacketConstructor.newInstance(target.getEntityId(), api.entityStatusPacketUsesByte ? (byte) 35 : 35);
            api.sendPacketMethod.invoke(playerManager, target, packet);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            logPacketEventsFailure(e);
            return false;
        }
    }

    private ItemStack addDeathProtection(ItemStack itemStack) {
        if (!supportsDeathProtectionComponent() || itemStack.getType() == Material.AIR) return itemStack;

        DeathProtectionAPI api = getDeathProtectionAPI();
        if (api == null) return itemStack;

        try {
            Object deathProtection = api.deathProtectionMethod.invoke(null);
            api.setDataMethod.invoke(itemStack, api.deathProtectionType, deathProtection);
        } catch (ReflectiveOperationException | LinkageError e) {
            logDeathProtectionFailure(e);
        }

        return itemStack;
    }

    private boolean isDeathProtectionItem(ItemStack itemStack) {
        if (itemStack.getType() == Material.TOTEM_OF_UNDYING) return true;
        if (!supportsDeathProtectionComponent()) return false;

        DeathProtectionAPI api = getDeathProtectionAPI();
        if (api == null) return false;

        try {
            return (boolean) api.hasDataMethod.invoke(itemStack, api.dataComponentType.cast(api.deathProtectionType));
        } catch (ReflectiveOperationException | LinkageError e) {
            Logs.debug(e);
            return false;
        }
    }

    private boolean supportsDeathProtectionComponent() {
        return VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.21.2");
    }

    public static void clearReflectionCaches() {
        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            DEATH_PROTECTION_API.set(null);
            deathProtectionInitialized = false;
            loggedDeathProtectionFailure = false;
        }

        synchronized (PACKET_EVENTS_INIT_LOCK) {
            PACKET_EVENTS_API.set(null);
            packetEventsInitialized = false;
            loggedPacketEventsFailure = false;
        }
    }

    private static @Nullable DeathProtectionAPI getDeathProtectionAPI() {
        if (deathProtectionInitialized) return DEATH_PROTECTION_API.get();

        synchronized (DEATH_PROTECTION_INIT_LOCK) {
            if (deathProtectionInitialized) return DEATH_PROTECTION_API.get();

            try {
                Class<?> dataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType");
                Class<?> valuedDataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
                Field deathProtectionField = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes").getField("DEATH_PROTECTION");
                Object deathProtectionType = deathProtectionField.get(null);
                Method deathProtectionMethod = Class.forName("io.papermc.paper.datacomponent.item.DeathProtection").getMethod("deathProtection");
                Method setDataMethod = ItemStack.class.getMethod("setData", valuedDataComponentTypeClass, Object.class);
                Method hasDataMethod = ItemStack.class.getMethod("hasData", dataComponentTypeClass);
                DEATH_PROTECTION_API.set(new DeathProtectionAPI(deathProtectionType, dataComponentTypeClass, deathProtectionMethod, setDataMethod, hasDataMethod));
            } catch (ReflectiveOperationException | LinkageError e) {
                logDeathProtectionFailure(e);
                DEATH_PROTECTION_API.set(null);
            } finally {
                deathProtectionInitialized = true;
            }

            return DEATH_PROTECTION_API.get();
        }
    }

    private static @Nullable PacketEventsAPI getPacketEventsAPI() {
        if (packetEventsInitialized) return PACKET_EVENTS_API.get();

        synchronized (PACKET_EVENTS_INIT_LOCK) {
            if (packetEventsInitialized) return PACKET_EVENTS_API.get();

            try {
                Class<?> packetEventsClass = Class.forName("com.github.retrooper.packetevents.PacketEvents");
                Class<?> packetEventsAPIClass = Class.forName("com.github.retrooper.packetevents.PacketEventsAPI");
                Class<?> playerManagerClass = Class.forName("com.github.retrooper.packetevents.manager.player.PlayerManager");
                Class<?> packetWrapperClass = Class.forName("com.github.retrooper.packetevents.wrapper.PacketWrapper");
                Class<?> entityStatusPacketClass = Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus");

                Constructor<?> entityStatusPacketConstructor;
                boolean entityStatusPacketUsesByte;
                try {
                    entityStatusPacketConstructor = entityStatusPacketClass.getConstructor(int.class, int.class);
                    entityStatusPacketUsesByte = false;
                } catch (NoSuchMethodException ignored) {
                    entityStatusPacketConstructor = entityStatusPacketClass.getConstructor(int.class, byte.class);
                    entityStatusPacketUsesByte = true;
                }

                PACKET_EVENTS_API.set(new PacketEventsAPI(
                        packetEventsClass.getMethod("getAPI"),
                        packetEventsAPIClass.getMethod("getPlayerManager"),
                        playerManagerClass.getMethod("sendPacket", Object.class, packetWrapperClass),
                        entityStatusPacketConstructor,
                        entityStatusPacketUsesByte));
            } catch (ReflectiveOperationException | LinkageError e) {
                logPacketEventsFailure(e);
                PACKET_EVENTS_API.set(null);
            } finally {
                packetEventsInitialized = true;
            }

            return PACKET_EVENTS_API.get();
        }
    }

    private static void logDeathProtectionFailure(Throwable throwable) {
        if (!loggedDeathProtectionFailure) {
            synchronized (DEATH_PROTECTION_INIT_LOCK) {
                if (!loggedDeathProtectionFailure) {
                    Logs.logWarning("Failed to apply Paper death-protection component for totem animation; the animation item may not trigger the protected-from-death effect on this server build. See debug log for details.");
                    loggedDeathProtectionFailure = true;
                }
            }
        }
        Logs.debug(throwable);
    }

    private static void logPacketEventsFailure(Throwable throwable) {
        if (!loggedPacketEventsFailure) {
            synchronized (PACKET_EVENTS_INIT_LOCK) {
                if (!loggedPacketEventsFailure) {
                    Logs.logWarning("Failed to send totem animation via PacketEvents; falling back to Bukkit's deprecated totem effect. See debug log for details.");
                    loggedPacketEventsFailure = true;
                }
            }
        }
        Logs.debug(throwable);
    }

    private record DeathProtectionAPI(Object deathProtectionType, Class<?> dataComponentType, Method deathProtectionMethod,
                                      Method setDataMethod, Method hasDataMethod) {}

    private record PacketEventsAPI(Method getApiMethod, Method getPlayerManagerMethod, Method sendPacketMethod,
                                   Constructor<?> entityStatusPacketConstructor, boolean entityStatusPacketUsesByte) {}
}
