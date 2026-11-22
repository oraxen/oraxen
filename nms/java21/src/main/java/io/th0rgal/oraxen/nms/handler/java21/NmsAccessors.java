package io.th0rgal.oraxen.nms.handler.java21;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Version-specific calls used by the shared handler sources in {@code nms/shared}.
 * This implementation targets Paper 1.20.x through 1.21.11.
 */
final class NmsAccessors {

    private static final Method tagKeyLocationMethod;
    private static final Method resourceKeyLocationMethod;
    private static final Method tagKeysMethod = findTagKeysMethod();

    static {
        try {
            tagKeyLocationMethod = Class.forName("net.minecraft.tags.TagKey").getMethod("location");
        } catch (ClassNotFoundException | NoSuchMethodException error) {
            throw new RuntimeException("Could not find TagKey#location", error);
        }

        try {
            Class<?> resourceKey = Class.forName("net.minecraft.resources.ResourceKey");
            resourceKeyLocationMethod = resolveResourceKeyLocationMethod(resourceKey,
                    ResourceLocationHelper.getResourceLocationClass());
        } catch (ReflectiveOperationException error) {
            throw new RuntimeException("Could not find ResourceKey location accessor", error);
        }
    }

    private NmsAccessors() {
    }

    static Method resolveResourceKeyLocationMethod(Class<?> resourceKeyClass, Class<?> resourceLocationClass)
            throws NoSuchMethodException {
        return resourceKeyClass.getMethod(
                resourceLocationClass.getSimpleName().equals("Identifier") ? "identifier" : "location");
    }

    private static Method findTagKeysMethod() {
        for (String methodName : List.of("keySet", "getAllKeys")) {
            try {
                return CompoundTag.class.getMethod(methodName);
            } catch (NoSuchMethodException ignored) {
                // CompoundTag renamed this method between 1.21.4 and 1.21.5.
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static Set<String> tagKeys(CompoundTag tag) {
        if (tagKeysMethod == null) throw new IllegalStateException("Could not find a CompoundTag key iterator");
        try {
            return (Set<String>) tagKeysMethod.invoke(tag);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not read custom item data keys", exception);
        }
    }

    static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> packetTags(
            ClientboundUpdateTagsPacket packet) {
        return packet.getTags();
    }

    static BlockHitResult useItemOnHitResult(ServerboundUseItemOnPacket packet) {
        return packet.getHitResult();
    }

    static int useItemOnSequence(ServerboundUseItemOnPacket packet) {
        return packet.getSequence();
    }

    /**
     * Gets a tag key's runtime identifier without linking against its version-specific return type.
     */
    static Object tagLocation(TagKey<?> tagKey) {
        try {
            return tagKeyLocationMethod.invoke(tagKey);
        } catch (Exception error) {
            throw new RuntimeException("Failed to get tag key location", error);
        }
    }

    static Object resourceKeyLocation(Object key) {
        try {
            return resourceKeyLocationMethod.invoke(key);
        } catch (ReflectiveOperationException error) {
            throw new RuntimeException("Failed to get resource key location", error);
        }
    }

    static NamespacedKey recipeKey(ResourceKey<?> key) {
        return CraftNamespacedKey.fromString(resourceKeyLocation(key).toString());
    }

    static SlotDisplay stackDisplay(ItemStack item) {
        return new SlotDisplay.ItemStackSlotDisplay(CraftItemStack.asNMSCopy(item));
    }

    static TeleportRandomlyConsumeEffect teleportRandomlyEffect(float diameter, boolean directionalParticles) {
        return new TeleportRandomlyConsumeEffect(diameter);
    }

    static ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack nmsItem) {
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    static Optional<Holder<JukeboxSong>> jukeboxSong(ServerLevel level, net.minecraft.world.item.ItemStack nmsItem) {
        return JukeboxSong.fromStack(level.registryAccess(), nmsItem);
    }
}
