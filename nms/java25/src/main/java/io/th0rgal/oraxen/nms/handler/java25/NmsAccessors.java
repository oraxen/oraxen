package io.th0rgal.oraxen.nms.handler.java25;

import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Version-specific calls used by the shared handler sources in {@code nms/shared}.
 * This implementation targets Paper 26.x.
 */
final class NmsAccessors {

    private static final boolean is263OrAbove = VersionUtil.uses263PacketAccessors(MinecraftVersion.getCurrentVersion());
    private static final Method asBukkitMirror = is263OrAbove ? findAsBukkitMirror() : null;

    private NmsAccessors() {
    }

    private static Method findAsBukkitMirror() {
        try {
            return CraftItemStack.class.getMethod("asBukkitMirror", net.minecraft.world.item.ItemStack.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    static Set<String> tagKeys(CompoundTag tag) {
        return tag.keySet();
    }

    @SuppressWarnings("unchecked") // The renamed accessor retains the same Map key/value types.
    static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> packetTags(
            ClientboundUpdateTagsPacket packet) {
        // 26.3 renamed getTags() to the record accessor tags().
        if (is263OrAbove)
            return (Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload>)
                    PacketAccessors263.invoke(PacketAccessors263.UPDATE_TAGS_TAGS, packet, "packet tags");
        return packet.getTags();
    }

    static BlockHitResult useItemOnHitResult(ServerboundUseItemOnPacket packet) {
        if (is263OrAbove)
            return (BlockHitResult) PacketAccessors263.invoke(PacketAccessors263.USE_ITEM_ON_HIT_RESULT, packet,
                    "use-item-on hit result");
        return packet.getHitResult();
    }

    static int useItemOnSequence(ServerboundUseItemOnPacket packet) {
        if (is263OrAbove)
            return (int) PacketAccessors263.invoke(PacketAccessors263.USE_ITEM_ON_SEQUENCE, packet,
                    "use-item-on sequence");
        return packet.getSequence();
    }

    static ClientboundEntityPositionSyncPacket positionSync(int id, PositionMoveRotation position, boolean onGround) {
        if (is263OrAbove) return PositionSync263.create(id, position, onGround);
        return new ClientboundEntityPositionSyncPacket(id, position, onGround);
    }

    // 26.3 sends a position path and separate angles instead of PositionMoveRotation.
    private static final class PositionSync263 {
        private static final MethodHandle pathOf;
        private static final MethodHandle constructor;

        static {
            try {
                Class<?> path = Class.forName("net.minecraft.world.entity.PositionPath");
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                pathOf = lookup.findStatic(path, "of", MethodType.methodType(path, Vec3.class))
                        .asType(MethodType.methodType(Object.class, Vec3.class));
                constructor = lookup.findConstructor(ClientboundEntityPositionSyncPacket.class,
                                MethodType.methodType(void.class, int.class, path, float.class, float.class, boolean.class))
                        .asType(MethodType.methodType(ClientboundEntityPositionSyncPacket.class,
                                int.class, Object.class, float.class, float.class, boolean.class));
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private static ClientboundEntityPositionSyncPacket create(int id, PositionMoveRotation position, boolean onGround) {
            try {
                Object path = pathOf.invokeExact(position.position());
                return (ClientboundEntityPositionSyncPacket) constructor.invokeExact(
                        id, path, position.yRot(), position.xRot(), onGround);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to create 26.3 entity position sync packet", e);
            }
        }
    }

    /**
     * 26.3 record accessors, resolved once on first use (class initialization is thread-safe)
     * since they are read on Netty threads for every matching packet.
     */
    private static final class PacketAccessors263 {
        private static final MethodHandle UPDATE_TAGS_TAGS = find(ClientboundUpdateTagsPacket.class, "tags");
        private static final MethodHandle USE_ITEM_ON_HIT_RESULT = find(ServerboundUseItemOnPacket.class, "hitResult");
        private static final MethodHandle USE_ITEM_ON_SEQUENCE = find(ServerboundUseItemOnPacket.class, "sequence");

        private static MethodHandle find(Class<?> owner, String name) {
            try {
                return MethodHandles.publicLookup().unreflect(owner.getMethod(name))
                        .asType(MethodType.methodType(Object.class, Object.class));
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        private static Object invoke(MethodHandle accessor, Object packet, String description) {
            if (accessor == null) throw new IllegalStateException("Missing 26.3 " + description + " accessor");
            try {
                return (Object) accessor.invokeExact(packet);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to read 26.3 " + description, e);
            }
        }
    }

    static Object tagLocation(TagKey<?> tagKey) {
        return tagKey.location();
    }

    static Optional<DataComponentType<?>> dataComponentType(String id) {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(Identifier.parse(id));
    }

    static Optional<MobEffect> mobEffect(String id) {
        return BuiltInRegistries.MOB_EFFECT.getOptional(Identifier.parse(id));
    }

    static Optional<SoundEvent> soundEvent(String id) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.parse(id));
    }

    static EntityType<?> entityType(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
    }

    static SoundEvent createSoundEvent(String id) {
        return SoundEvent.createVariableRangeEvent(Identifier.parse(id));
    }

    /**
     * Gets the path of a vanilla sound, or an empty string for sounds outside the minecraft namespace.
     */
    static String vanillaSoundPath(SoundEvent sound) {
        Identifier location = sound.location();
        return "minecraft".equals(location.getNamespace()) ? location.getPath() : "";
    }

    static NamespacedKey recipeKey(ResourceKey<?> key) {
        return CraftNamespacedKey.fromResourceKey(key);
    }

    static SlotDisplay stackDisplay(ItemStack item) {
        return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(CraftItemStack.asNMSCopy(item)));
    }

    static TeleportRandomlyConsumeEffect teleportRandomlyEffect(float diameter, boolean directionalParticles) {
        if (is263OrAbove) {
            try {
                return TeleportRandomlyConsumeEffect.class.getConstructor(float.class, boolean.class)
                        .newInstance(diameter, directionalParticles);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to create 26.3 teleport effect", e);
            }
        }
        return new TeleportRandomlyConsumeEffect(diameter);
    }

    static ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack nmsItem) {
        if (is263OrAbove) {
            // 26.3 removed asCraftMirror and changed asBukkitCopy's parameter type.
            // Mirror a copied stack to preserve independent Bukkit copy semantics.
            try {
                if (asBukkitMirror == null) throw new NoSuchMethodException("CraftItemStack#asBukkitMirror");
                return ItemStack.class.cast(asBukkitMirror.invoke(null, nmsItem.copy()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to copy 26.3 item stack", e);
            }
        }
        if (VersionUtil.atOrAbove("26.2")) {
            // Paper 26.2 made CraftItemStack#asBukkitCopy private. asCraftMirror is still
            // public and returns a Bukkit ItemStack view, so mirror a copied NMS stack to
            // preserve the previous copy semantics.
            return CraftItemStack.asCraftMirror(nmsItem.copy());
        }
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    static Optional<Holder<JukeboxSong>> jukeboxSong(ServerLevel level, net.minecraft.world.item.ItemStack nmsItem) {
        return JukeboxSong.fromStack(nmsItem);
    }
}
