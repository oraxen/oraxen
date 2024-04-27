package io.th0rgal.oraxen.nms.v1_20_R3.furniture;

import com.moulberry.axiom.AxiomPaper;
import com.moulberry.axiom.NbtSanitization;
import com.moulberry.axiom.packet.ManipulateEntityPacketListener;
import io.netty.buffer.Unpooled;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureBaseEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.protectionlib.ProtectionLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AxiomCompatibility implements PluginMessageListener {

    private final AxiomPaper axiomPaper;

    public AxiomCompatibility() {
        Logs.logInfo("Registering Axiom-Compatibility for furniture...");
        this.axiomPaper = (AxiomPaper) Bukkit.getPluginManager().getPlugin("AxiomPaper");
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!axiomPaper.canUseAxiom(player)) return;
        if (!player.hasPermission("axiom.entity.*") && !player.hasPermission("axiom.entity.manipulate")) return;
        if (!axiomPaper.canModifyWorld(player, player.getWorld())) return;

        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
        List<ManipulateEntry> entries = friendlyByteBuf.readCollection(
                FriendlyByteBuf.limitValue(ArrayList::new, 1000), ManipulateEntry::read);

        ServerLevel serverLevel = ((CraftWorld) player.getWorld()).getHandle();
        List<String> whitelistedEntities = axiomPaper.configuration.getStringList("whitelist-entities");
        List<String> blacklistedEntities = axiomPaper.configuration.getStringList("blacklist-entities");

        for (ManipulateEntry entry : entries) {
            //TODO needs to be via entityId, if null check map
            Entity entity = Optional.ofNullable(serverLevel.getEntity(entry.uuid())).orElseGet(() -> {
                UUID uuid = FurniturePacketManager.furnitureBaseMap.stream().filter(base -> base.uuid(player).equals(entry.uuid)).findFirst().map(FurnitureBaseEntity::baseUUID).orElse(null);
                return uuid != null ? serverLevel.getEntity(uuid) : null;
            });
            if (entity == null) continue;

            String type = EntityType.getKey(entity.getType()).toString();
            if (!whitelistedEntities.isEmpty() && !whitelistedEntities.contains(type)) continue;
            if (blacklistedEntities.contains(type)) continue;

            Vec3 position = entity.position();
            BlockPos containing = BlockPos.containing(position.x, position.y, position.z);

            if (!ProtectionLib.canBuild(player, new Location(player.getWorld(), containing.getX(), containing.getY(), containing.getZ())))
                continue;

            if (entry.merge != null && !entry.merge.isEmpty()) {
                NbtSanitization.sanitizeEntity(entry.merge);

                CompoundTag compoundTag = entity.saveWithoutId(new CompoundTag());
                compoundTag = merge(compoundTag, entry.merge);
                entity.load(compoundTag);
            }

            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(bukkitEntity);
            if (mechanic == null) continue;

            entity.setPosRaw(position.x, position.y, position.z);

            Vec3 entryPos = entry.position();
            if (entryPos != null && entry.relativeMovementSet != null) {
                double newX = entry.relativeMovementSet.contains(RelativeMovement.X) ? entity.position().x + entryPos.x : entryPos.x;
                double newY = entry.relativeMovementSet.contains(RelativeMovement.Y) ? entity.position().y + entryPos.y : entryPos.y;
                double newZ = entry.relativeMovementSet.contains(RelativeMovement.Z) ? entity.position().z + entryPos.z : entryPos.z;
                float newYaw = entry.relativeMovementSet.contains(RelativeMovement.Y_ROT) ? entity.getYRot() + entry.yaw : entry.yaw;
                float newPitch = entry.relativeMovementSet.contains(RelativeMovement.X_ROT) ? entity.getXRot() + entry.pitch : entry.pitch;

                if (entity instanceof HangingEntity hangingEntity) {
                    float changedYaw = newYaw - entity.getYRot();
                    int rotations = Math.round(changedYaw / 90);
                    hangingEntity.rotate(ROTATION_VALUES[rotations & 3]);

                    if (entity instanceof ItemFrame itemFrame && itemFrame.getDirection().getAxis() == Direction.Axis.Y) {
                        itemFrame.setRotation(itemFrame.getRotation() - Math.round(changedYaw / 45));
                    }
                }

                containing = BlockPos.containing(newX, newY, newZ);

                if (ProtectionLib.canBuild(player, new Location(player.getWorld(),
                        containing.getX(), containing.getY(), containing.getZ()))) {
                    //entity.teleportTo(serverLevel, newX, newY, newZ, Set.of(), newYaw, newPitch);
                    entity.teleportTo(serverLevel, newX, newY, newZ, Set.of(), newYaw, newPitch);
                }

                entity.setYHeadRot(newYaw);
            }

            IFurniturePacketManager packetManager = FurnitureFactory.get().packetManager();
            packetManager.removeInteractionHitboxPacket(bukkitEntity, mechanic);
            packetManager.removeBarrierHitboxPacket(bukkitEntity, mechanic);

            packetManager.sendFurnitureEntityPacket(bukkitEntity, mechanic, player);
            packetManager.sendInteractionEntityPacket(bukkitEntity, mechanic, player);
            packetManager.sendBarrierHitboxPacket(bukkitEntity, mechanic, player);

            switch (entry.passengerManipulation) {
                case NONE -> {}
                case REMOVE_ALL -> entity.ejectPassengers();
                case ADD_LIST -> {
                    for (UUID passengerUuid : entry.passengers) {
                        Entity passenger = serverLevel.getEntity(passengerUuid);

                        if (passenger == null || passenger.isPassenger() ||
                                passenger instanceof net.minecraft.world.entity.player.Player || passenger.hasPassenger(AxiomCompatibility::isPlayer)) continue;

                        String passengerType = EntityType.getKey(passenger.getType()).toString();

                        if (!whitelistedEntities.isEmpty() && !whitelistedEntities.contains(passengerType)) continue;
                        if (blacklistedEntities.contains(passengerType)) continue;

                        // Prevent mounting loop
                        if (passenger.getSelfAndPassengers().anyMatch(entity2 -> entity2 == entity)) {
                            continue;
                        }

                        position = passenger.position();
                        containing = BlockPos.containing(position.x, position.y, position.z);

                        if (!ProtectionLib.canBuild(player, new Location(player.getWorld(),
                                containing.getX(), containing.getY(), containing.getZ()))) {
                            continue;
                        }

                        passenger.startRiding(entity, true);
                    }
                }
                case REMOVE_LIST -> {
                    for (UUID passengerUuid : entry.passengers) {
                        Entity passenger = serverLevel.getEntity(passengerUuid);
                        if (passenger == null || passenger == entity || passenger instanceof net.minecraft.world.entity.player.Player ||
                                passenger.hasPassenger(AxiomCompatibility::isPlayer)) continue;

                        String passengerType = EntityType.getKey(passenger.getType()).toString();

                        if (!whitelistedEntities.isEmpty() && !whitelistedEntities.contains(passengerType)) continue;
                        if (blacklistedEntities.contains(passengerType)) continue;

                        Entity vehicle = passenger.getVehicle();
                        if (vehicle == entity) {
                            passenger.stopRiding();
                        }
                    }
                }
            }
        }
    }


    private static CompoundTag merge(CompoundTag left, CompoundTag right) {
        if (right.contains("axiom:modify")) {
            right.remove("axiom:modify");
            return right;
        }

        for (String key : right.getAllKeys()) {
            Tag tag = right.get(key);
            if (tag instanceof CompoundTag compound) {
                if (compound.isEmpty()) {
                    left.remove(key);
                } else if (left.contains(key, Tag.TAG_COMPOUND)) {
                    CompoundTag child = left.getCompound(key);
                    child = merge(child, compound);
                    left.put(key, child);
                } else {
                    CompoundTag copied = compound.copy();
                    if (copied.contains("axiom:modify")) {
                        copied.remove("axiom:modify");
                    }
                    left.put(key, copied);
                }
            } else {
                left.put(key, tag.copy());
            }
        }
        return left;
    }

    private static boolean isPlayer(Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player;
    }

    private record ManipulateEntry(UUID uuid, @Nullable Set<RelativeMovement> relativeMovementSet, @Nullable Vec3 position,
                                  float yaw, float pitch, CompoundTag merge, ManipulateEntityPacketListener.PassengerManipulation passengerManipulation, List<UUID> passengers) {
        public static ManipulateEntry read(FriendlyByteBuf friendlyByteBuf) {
            UUID uuid = friendlyByteBuf.readUUID();

            int flags = friendlyByteBuf.readByte();
            Set<RelativeMovement> relativeMovementSet = null;
            Vec3 position = null;
            float yaw = 0;
            float pitch = 0;
            if (flags >= 0) {
                relativeMovementSet = RelativeMovement.unpack(flags);
                position = new Vec3(friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble());
                yaw = friendlyByteBuf.readFloat();
                pitch = friendlyByteBuf.readFloat();
            }

            CompoundTag nbt = friendlyByteBuf.readNbt();

            ManipulateEntityPacketListener.PassengerManipulation passengerManipulation = friendlyByteBuf.readEnum(ManipulateEntityPacketListener.PassengerManipulation.class);
            List<UUID> passengers = List.of();
            if (passengerManipulation == ManipulateEntityPacketListener.PassengerManipulation.ADD_LIST || passengerManipulation == ManipulateEntityPacketListener.PassengerManipulation.REMOVE_LIST) {
                passengers = friendlyByteBuf.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 1000),
                        FriendlyByteBuf::readUUID);
            }

            return new ManipulateEntry(uuid, relativeMovementSet, position, yaw, pitch, nbt,
                    passengerManipulation, passengers);
        }
    }

    private static final Rotation[] ROTATION_VALUES = Rotation.values();
}
