package io.th0rgal.oraxen.nms.handler.java21;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.consume_effects.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    private final Listener packDispatchListener;
    private final Map<io.netty.channel.Channel, Deque<PendingBlockChange>> pendingBlockChanges = new ConcurrentHashMap<>();

    private record PendingBlockChange(int sequence, int x, int y, int z, boolean placement) {
    }

    public NMSHandler() {
        // Paper exposed the configuration/reconfiguration events used by the pre-join
        // dispatcher starting with 1.21.7. Do not load that listener earlier: its class
        // references APIs that do not exist on 1.21.2 through 1.21.6.
        this.packDispatchListener = VersionUtil.atOrAbove("1.21.7")
                ? new PackDispatchListener()
                : null;

        // mineableWith tag handling
        NamespacedKey tagKey = NamespacedKey.fromString("mineable_with_key", OraxenPlugin.get());
        if (ChannelInitializeListenerHolder.hasListener(tagKey))
            return;
        ChannelInitializeListenerHolder.addListener(tagKey, (channel -> channel.pipeline().addBefore("packet_handler",
                tagKey.asString(), new ChannelDuplexHandler() {
                    TagNetworkSerialization.NetworkPayload payload = createPayload();

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                        if (msg instanceof ClientboundUpdateTagsPacket updateTagsPacket) {
                            Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = new HashMap<>(updateTagsPacket.getTags());
                            if (payload != null
                                    && NoteBlockMechanicFactory.isEnabled()
                                    && NoteBlockMechanicFactory.getInstance().removeMineableTag())
                                tags.put(Registries.BLOCK, payload);
                            msg = new ClientboundUpdateTagsPacket(tags);
                        }
                        if (msg instanceof ClientboundBlockChangedAckPacket packet) {
                            final Deque<PendingBlockChange> pending = pendingBlockChanges.get(ctx.channel());
                            if (pending != null)
                                pending.removeIf(change -> change.sequence() <= packet.sequence());
                        }
                        ctx.write(msg, promise);
                    }

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        // Bukkit block events do not expose the packet sequence. Retain the
                        // position and operation so the matching prediction can be settled as
                        // soon as its authoritative block states have been sent.
                        final Deque<PendingBlockChange> pending =
                                pendingBlockChanges.computeIfAbsent(ctx.channel(), ignored -> new ConcurrentLinkedDeque<>());
                        if (msg instanceof ServerboundUseItemOnPacket packet) {
                            final BlockPos pos = packet.getHitResult().getBlockPos();
                            pending.addLast(new PendingBlockChange(packet.getSequence(), pos.getX(), pos.getY(), pos.getZ(), true));
                        } else if (msg instanceof ServerboundPlayerActionPacket packet
                                && packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                            final BlockPos pos = packet.getPos();
                            pending.addLast(new PendingBlockChange(packet.getSequence(), pos.getX(), pos.getY(), pos.getZ(), false));
                        }
                        while (pending.size() > 16) pending.pollFirst();
                        super.channelRead(ctx, msg);
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        pendingBlockChanges.remove(ctx.channel());
                        super.channelInactive(ctx);
                    }
                })));
    }

    @Override
    public void acknowledgeBlockChanges(Player player, Location packetBlock, boolean placement) {
        final ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        final Connection connection = serverPlayer.connection.connection;
        final Deque<PendingBlockChange> pending = pendingBlockChanges.get(connection.channel);
        if (pending == null) return;

        PendingBlockChange matched = null;
        for (final PendingBlockChange change : pending) {
            if (change.placement() == placement && change.x() == packetBlock.getBlockX()
                    && change.y() == packetBlock.getBlockY() && change.z() == packetBlock.getBlockZ()) {
                matched = change;
                break;
            }
        }
        if (matched == null) return;

        final int sequence = matched.sequence();
        pending.removeIf(change -> change.sequence() <= sequence);
        serverPlayer.connection.send(new ClientboundBlockChangedAckPacket(sequence));
    }

    @Override
    public Listener packDispatchListener() {
        return packDispatchListener;
    }

    @Override
    public boolean tripwireUpdatesDisabled() {
        return GlobalConfiguration.get().blockUpdates.disableTripwireUpdates;
    }

    @Override
    public boolean noteblockUpdatesDisabled() {
        return GlobalConfiguration.get().blockUpdates.disableNoteblockUpdates;
    }

    @Override
    public boolean chorusPlantUpdatesDisabled() {
        return GlobalConfiguration.get().blockUpdates.disableChorusPlantUpdates;
    }

    @Override
    /* This method copies custom NBT data from one item to another */
    public ItemStack copyItemNBTTags(@NotNull ItemStack oldItem, @NotNull ItemStack newItem) {
        net.minecraft.world.item.ItemStack newNmsItem = CraftItemStack.asNMSCopy(newItem);
        net.minecraft.world.item.ItemStack oldItemStack = CraftItemStack.asNMSCopy(oldItem);
        // Gets data component's nbt data.
        DataComponentType<CustomData> type = DataComponents.CUSTOM_DATA;
        CustomData oldData = oldItemStack.getComponents().get(type);
        CustomData newData = newNmsItem.getComponents().get(type);

        if (oldData == null)
            return newItem;
        CompoundTag oldTag = oldData.copyTag();
        CompoundTag newTag = newData != null ? newData.copyTag() : new CompoundTag();

        for (String key : tagKeys(oldTag)) {
            if (vanillaKeys.contains(key))
                continue;
            Tag value = oldTag.get(key);
            if (value != null)
                newTag.put(key, value);
            else
                removeTag(newTag, key);
        }

        newNmsItem.set(type, CustomData.of(newTag));
        return asBukkitCopy(newNmsItem);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> tagKeys(CompoundTag tag) {
        for (String methodName : List.of("keySet", "getAllKeys")) {
            try {
                return (Set<String>) CompoundTag.class.getMethod(methodName).invoke(tag);
            } catch (NoSuchMethodException ignored) {
                // CompoundTag renamed this method between 1.21.4 and 1.21.5.
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not read custom item data keys", exception);
            }
        }
        throw new IllegalStateException("Could not find a CompoundTag key iterator");
    }

    private static void removeTag(CompoundTag tag, String key) {
        try {
            CompoundTag.class.getMethod("remove", String.class).invoke(tag, key);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not remove custom item data key " + key, exception);
        }
    }

    @Override
    @Nullable
    public BlockData correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
        InteractionHand hand = slot == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        BlockHitResult hitResult = getPlayerPOVHitResult(serverPlayer.level(), serverPlayer, ClipContext.Fluid.NONE);
        BlockPlaceContext placeContext = new BlockPlaceContext(new UseOnContext(serverPlayer, hand, hitResult));

        if (!(nmsStack.getItem() instanceof BlockItem blockItem)) {
            nmsStack.getItem().useOn(new UseOnContext(serverPlayer, hand, hitResult));
            if (!player.isSneaking())
                serverPlayer.gameMode.useItem(serverPlayer, serverPlayer.level(), nmsStack, hand);
            return null;
        }

        // Shulker-Boxes are DirectionalPlace based unlike other directional-blocks
        if (org.bukkit.Tag.SHULKER_BOXES.isTagged(itemStack.getType())) {
            placeContext = new DirectionalPlaceContext(serverPlayer.level(), hitResult.getBlockPos(),
                    hitResult.getDirection(), nmsStack, hitResult.getDirection().getOpposite());
        }

        InteractionResult result = blockItem.place(placeContext);
        if (result == InteractionResult.FAIL)
            return null;
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE)
            itemStack.setAmount(nmsStack.getCount());
        World world = player.getWorld();
        BlockPos placedPos = placeContext.getClickedPos();

        if (!player.isSneaking()) {
            Block block = world.getBlockAt(placedPos.getX(), placedPos.getY(), placedPos.getZ());
            SoundGroup sound = block.getBlockData().getSoundGroup();

            world.playSound(
                    BlockHelpers.toCenterBlockLocation(block.getLocation()), sound.getPlaceSound(),
                    SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }

        return world.getBlockAt(placedPos.getX(), placedPos.getY(), placedPos.getZ()).getBlockData();
    }

    public BlockHitResult getPlayerPOVHitResult(Level world, net.minecraft.world.entity.player.Player player,
            ClipContext.Fluid fluidHandling) {
        float f = player.getXRot();
        float g = player.getYRot();
        Vec3 vec3 = player.getEyePosition();
        float h = Mth.cos(-g * ((float) Math.PI / 180F) - (float) Math.PI);
        float i = Mth.sin(-g * ((float) Math.PI / 180F) - (float) Math.PI);
        float j = -Mth.cos(-f * ((float) Math.PI / 180F));
        float k = Mth.sin(-f * ((float) Math.PI / 180F));
        float l = i * j;
        float n = h * j;
        double d = 5.0D;
        Vec3 vec32 = vec3.add((double) l * d, (double) k * d, (double) n * d);
        return world.clip(new ClipContext(vec3, vec32, ClipContext.Block.OUTLINE, fluidHandling, player));
    }

    private TagNetworkSerialization.NetworkPayload createPayload() {
        Constructor<?> constructor = Arrays
                .stream(TagNetworkSerialization.NetworkPayload.class.getDeclaredConstructors()).findFirst()
                .orElse(null);
        if (constructor == null)
            return null;
        constructor.setAccessible(true);
        try {
            return (TagNetworkSerialization.NetworkPayload) constructor.newInstance(tagRegistryMap);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            Logs.debug(e);
        }
        return null;
    }

    // Use Object as key type to support both ResourceLocation and Identifier
    // (renamed across mapping generations before/into 26.x).
    private final Map<Object, IntList> tagRegistryMap = createTagRegistryMap();

    private Map<Object, IntList> createTagRegistryMap() {
        return BuiltInRegistries.BLOCK.getTags().map(named -> {
            IntArrayList list = new IntArrayList(named.size());
            Object location = ResourceLocationHelper.location(named.key());
            if (location.equals(ResourceLocationHelper.location(BlockTags.MINEABLE_WITH_AXE))) {
                named.stream()
                        .filter(block -> !block.value().getDescriptionId().endsWith("note_block"))
                        .forEach(block -> list.add(BuiltInRegistries.BLOCK.getId(block.value())));
            } else {
                named.forEach(block -> list.add(BuiltInRegistries.BLOCK.getId(block.value())));
            }
            return Map.of(location, (IntList) list);
        }).collect(HashMap::new, Map::putAll, Map::putAll);
    }

    /**
     * Sets a component on an item using the DataComponents registry
     * 
     * @param item         The ItemBuilder to modify
     * @param componentKey The component key (e.g. "food", "tool", etc.)
     * @param component    The component object
     * @return true if the component was successfully set
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean setComponent(ItemBuilder item, String componentKey, Object component) {
        try {
            Object componentLocation = ResourceLocationHelper.parse("minecraft:" + componentKey.toLowerCase(java.util.Locale.ROOT));
            if (componentLocation == null)
                return false;

            net.minecraft.core.component.DataComponentType<?> componentType = getDataComponentType(componentLocation);
            if (componentType == null)
                return false;

            if (component instanceof ConfigurationSection config) {
                net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
                convertConfigToNBT(config, nbt);

                // Use Codec-based deserialization (works for all components including records)
                com.mojang.serialization.Codec<?> codec = componentType.codecOrThrow();
                var registryAccess = ((org.bukkit.craftbukkit.CraftServer) org.bukkit.Bukkit.getServer())
                        .getServer().registryAccess();
                var ops = net.minecraft.resources.RegistryOps.create(
                        net.minecraft.nbt.NbtOps.INSTANCE, registryAccess);
                var result = codec.parse(ops, nbt);

                var parsedOptional = result.result();
                if (parsedOptional.isPresent()) {
                    item.setComponent(componentKey, parsedOptional.get());
                    return true;
                } else {
                    Logs.logWarning("Failed to parse component '" + componentKey + "': " +
                            result.error().map(e -> e.message()).orElse("unknown error"));
                    return false;
                }
            } else {
                item.setComponent(componentKey, component);
                return true;
            }
        } catch (Exception e) {
            Logs.logWarning("Failed to set component " + componentKey);
            if (io.th0rgal.oraxen.configs.Settings.DEBUG.toBool())
                Logs.debug(e);
            return false;
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ItemStack applyGenericComponents(ItemStack itemStack, java.util.Map<String, Object> components) {
        if (components.isEmpty()) return itemStack;

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        for (var entry : components.entrySet()) {
            try {
                Object location = ResourceLocationHelper.parse("minecraft:" + entry.getKey().toLowerCase());
                if (location == null) continue;

                DataComponentType componentType = getDataComponentType(location);
                if (componentType == null) continue;

                nmsItem.set(componentType, entry.getValue());
            } catch (Exception e) {
                Logs.logWarning("Failed to apply component '" + entry.getKey() + "'");
                if (io.th0rgal.oraxen.configs.Settings.DEBUG.toBool())
                    Logs.debug(e);
            }
        }
        return asBukkitCopy(nmsItem);
    }

    private void convertConfigToNBT(ConfigurationSection config, net.minecraft.nbt.CompoundTag nbt) {
        for (String key : config.getKeys(false)) {
            Object value = config.get(key);
            if (value instanceof ConfigurationSection section) {
                handleConfigSectionValue(nbt, key, section);
            } else if (value instanceof Number number) {
                handleNumberValue(nbt, key, number);
            } else if (value instanceof Boolean boolValue) {
                nbt.putBoolean(key, boolValue);
            } else if (value instanceof String stringValue) {
                nbt.putString(key, stringValue);
            } else if (value instanceof List<?> list) {
                handleListValue(nbt, key, list);
            }
        }
    }

    private void handleConfigSectionValue(net.minecraft.nbt.CompoundTag nbt, String key, ConfigurationSection section) {
        net.minecraft.nbt.CompoundTag compound = new net.minecraft.nbt.CompoundTag();
        convertConfigToNBT(section, compound);
        nbt.put(key, compound);
    }

    private void handleNumberValue(net.minecraft.nbt.CompoundTag nbt, String key, Number number) {
        if (number instanceof Integer)
            nbt.putInt(key, number.intValue());
        else if (number instanceof Double)
            nbt.putDouble(key, number.doubleValue());
        else if (number instanceof Float)
            nbt.putFloat(key, number.floatValue());
        else if (number instanceof Long)
            nbt.putLong(key, number.longValue());
        else if (number instanceof Byte)
            nbt.putByte(key, number.byteValue());
        else if (number instanceof Short)
            nbt.putShort(key, number.shortValue());
    }

    private void handleListValue(net.minecraft.nbt.CompoundTag nbt, String key, List<?> list) {
        if (list.isEmpty()) {
            return;
        }

        Object first = list.get(0);
        if (first instanceof String) {
            net.minecraft.nbt.ListTag stringList = new net.minecraft.nbt.ListTag();
            for (Object s : list) {
                stringList.add(net.minecraft.nbt.StringTag.valueOf(s.toString()));
            }
            nbt.put(key, stringList);
        } else if (first instanceof Number) {
            if (list.stream().anyMatch(element -> !(element instanceof Number))) {
                Logs.logWarning("Mixed-type numeric list in NBT config for key: " + key);
                return;
            }

            List<? extends Number> numbers = list.stream()
                    .map(element -> (Number) element)
                    .toList();
            boolean hasFractional = numbers.stream()
                    .anyMatch(number -> number instanceof Float || number instanceof Double);

            if (hasFractional) {
                net.minecraft.nbt.ListTag decimalList = new net.minecraft.nbt.ListTag();
                numbers.forEach(number -> decimalList.add(net.minecraft.nbt.DoubleTag.valueOf(number.doubleValue())));
                nbt.put(key, decimalList);
                return;
            }

            boolean requiresLong = numbers.stream()
                    .anyMatch(number -> number.longValue() > Integer.MAX_VALUE || number.longValue() < Integer.MIN_VALUE);
            if (requiresLong) {
                nbt.putLongArray(key, numbers.stream().mapToLong(Number::longValue).toArray());
            } else {
                nbt.putIntArray(key, numbers.stream().mapToInt(Number::intValue).toArray());
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void consumableComponent(ItemBuilder item, ConfigurationSection section) {
        Consumable.Builder consumable = Consumable.builder();
        Consumable template = Optional.ofNullable(CraftItemStack.asNMSCopy(new ItemStack(item.getType()))
                .getComponents().get(DataComponents.CONSUMABLE))
                .orElse(Consumable.builder().build());

        // Basic properties
        consumable.consumeSeconds((float) section.getDouble("consume_seconds", template.consumeSeconds()));
        consumable.animation(Optional.ofNullable(EnumUtils.getEnum(ItemUseAnimation.class,
                section.getString("animation", "").toUpperCase()))
                .orElse(template.animation()));
        consumable.hasConsumeParticles(section.getBoolean("has_consume_particles", template.hasConsumeParticles()));

        // Sound handling
        String soundId = section.getString("sound");
        if (soundId != null) {
            SoundEvent soundEvent = getSoundEventFromId(soundId);
            if (soundEvent != null) {
                consumable.sound(Holder.direct(soundEvent));
            } else {
                consumable.sound(template.sound());
            }
        } else {
            consumable.sound(template.sound());
        }

        // Effects handling
        List<Map<?, ?>> effectsMap = section.getMapList("on_consume_effects");
        if (effectsMap.isEmpty()) {
            template.onConsumeEffects().forEach(consumable::onConsume);
        } else {
            for (Map<?, ?> effectSection : effectsMap) {
                String type = Optional.ofNullable(effectSection.get("type"))
                        .map(Object::toString)
                        .orElse("");

                switch (type.toLowerCase()) {
                    case "apply_effects" -> handleApplyEffects(consumable, effectSection);
                    case "remove_effects" -> handleRemoveEffects(consumable, effectSection);
                    case "clear_all_effects" -> consumable.onConsume(new ClearAllStatusEffectsConsumeEffect());
                    case "teleport_randomly" -> {
                        float diameter = parseFloatValue(effectSection.get("diameter"), 16f, "teleport_randomly.diameter");
                        consumable.onConsume(new TeleportRandomlyConsumeEffect(diameter));
                    }
                    case "play_sound" -> handlePlaySound(consumable, effectSection, template);
                    default -> Logs.logWarning("Invalid ConsumeEffect-Type " + type);
                }
            }
        }

        item.setConsumableComponent(consumable.build());
    }

    @Override
    public void deathProtectionComponent(ItemBuilder item, ConfigurationSection section) {
        List<ConsumeEffect> effects = parseDeathProtectionEffects(section.getMapList("death_effects"));
        item.setDeathProtectionComponent(new DeathProtection(effects));
    }

    private List<ConsumeEffect> parseDeathProtectionEffects(List<Map<?, ?>> effectSections) {
        List<ConsumeEffect> effects = new ArrayList<>();

        for (Map<?, ?> effectSection : effectSections) {
            String type = Optional.ofNullable(effectSection.get("type"))
                    .map(Object::toString)
                    .orElse("");

            switch (type.toLowerCase(Locale.ROOT)) {
                case "apply_effects" -> addDeathProtectionStatusEffects(effects, effectSection);
                case "remove_effects" -> addDeathProtectionRemoveEffects(effects, effectSection);
                case "clear_all_effects" -> effects.add(new ClearAllStatusEffectsConsumeEffect());
                case "teleport_randomly" -> {
                    float diameter = parseFloatValue(effectSection.get("diameter"), 16f,
                            "death_protection.teleport_randomly.diameter");
                    effects.add(new TeleportRandomlyConsumeEffect(diameter));
                }
                case "play_sound" -> {
                    String soundId = Optional.ofNullable(effectSection.get("sound"))
                            .map(Object::toString)
                            .orElse(null);
                    if (soundId != null) {
                        SoundEvent soundEvent = getSoundEventFromId(soundId);
                        if (soundEvent != null)
                            effects.add(new PlaySoundConsumeEffect(Holder.direct(soundEvent)));
                    }
                }
                default -> Logs.logWarning("Invalid death_protection ConsumeEffect-Type " + type);
            }
        }

        return effects;
    }

    private void addDeathProtectionStatusEffects(List<ConsumeEffect> effects, Map<?, ?> effectSection) {
        if (!(effectSection.get("effects") instanceof Map<?, ?> configuredEffects))
            return;

        float probability = Math.max(0f, Math.min(1f,
                parseFloatValue(effectSection.get("probability"), 1f, "death_protection.probability")));
        List<MobEffectInstance> statusEffects = new ArrayList<>();

        for (Map.Entry<?, ?> entry : configuredEffects.entrySet()) {
            String effectId = entry.getKey().toString();
            if (!(entry.getValue() instanceof Map<?, ?> rawMap)) {
                Logs.logWarning("Invalid death_protection effect data for " + effectId + ": expected map");
                continue;
            }

            Map<String, Object> effectData = new HashMap<>();
            for (Map.Entry<?, ?> effectEntry : rawMap.entrySet())
                effectData.put(String.valueOf(effectEntry.getKey()), effectEntry.getValue());

            getMobEffectOptional(effectId)
                    .map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
                    .ifPresentOrElse(effect -> {
                        int duration = Math.max(parseIntegerValue(effectData.get("duration"), 1, "duration", effectId), 0) * 20;
                        int amplifier = Math.max(parseIntegerValue(effectData.get("amplifier"), 0, "amplifier", effectId), 0);
                        boolean ambient = Optional.ofNullable(effectData.get("ambient"))
                                .map(value -> Boolean.parseBoolean(value.toString()))
                                .orElse(false);
                        boolean particles = Optional.ofNullable(effectData.get("show_particles"))
                                .map(value -> Boolean.parseBoolean(value.toString()))
                                .orElse(true);
                        boolean icon = Optional.ofNullable(effectData.get("show_icon"))
                                .map(value -> Boolean.parseBoolean(value.toString()))
                                .orElse(true);

                        statusEffects.add(new MobEffectInstance(
                                effect, duration, amplifier, ambient, particles, icon));
                    }, () -> Logs.logWarning("Invalid potion effect in death_protection: " + effectId));
        }

        if (!statusEffects.isEmpty())
            effects.add(new ApplyStatusEffectsConsumeEffect(statusEffects, probability));
    }

    private void addDeathProtectionRemoveEffects(List<ConsumeEffect> effects, Map<?, ?> effectSection) {
        if (!(effectSection.get("effects") instanceof List<?> effectIds))
            return;

        List<Holder<MobEffect>> mobEffects = effectIds.stream()
                .map(Object::toString)
                .map(this::getMobEffectOptional)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
                .toList();

        if (!mobEffects.isEmpty())
            effects.add(new RemoveStatusEffectsConsumeEffect(HolderSet.direct(mobEffects)));
    }

    private void handleApplyEffects(Consumable.Builder consumable, Map<?, ?> effectSection) {
        if (!(effectSection.get("effects") instanceof Map<?, ?> effects))
            return;

        float probability = parseFloatValue(effectSection.get("probability"), 1.0f, "probability");

        for (Map.Entry<?, ?> entry : effects.entrySet()) {
            String effectId = entry.getKey().toString();
            if (!(entry.getValue() instanceof Map<?, ?> rawMap)) {
                Logs.logWarning("Invalid effect data for " + effectId + ": expected map");
                continue;
            }
            Map<String, Object> effectData = new HashMap<>();
            for (Map.Entry<?, ?> effectEntry : rawMap.entrySet()) {
                effectData.put(String.valueOf(effectEntry.getKey()), effectEntry.getValue());
            }

            getMobEffectOptional(effectId)
                    .map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
                    .ifPresent(effect -> {
                        int duration = Optional.ofNullable(effectData.get("duration"))
                                .map(d -> parseIntegerValue(d, 1, "duration", effectId) * 20)
                                .orElse(20);
                        int amplifier = Optional.ofNullable(effectData.get("amplifier"))
                                .map(a -> parseIntegerValue(a, 0, "amplifier", effectId))
                                .orElse(0);
                        boolean ambient = Optional.ofNullable(effectData.get("ambient"))
                                .map(a -> Boolean.parseBoolean(a.toString()))
                                .orElse(false);
                        boolean particles = Optional.ofNullable(effectData.get("show_particles"))
                                .map(p -> Boolean.parseBoolean(p.toString()))
                                .orElse(true);
                        boolean icon = Optional.ofNullable(effectData.get("show_icon"))
                                .map(i -> Boolean.parseBoolean(i.toString()))
                                .orElse(true);

                        MobEffectInstance instance = new MobEffectInstance(
                                effect, duration, amplifier, ambient, particles, icon);
                        consumable.onConsume(new ApplyStatusEffectsConsumeEffect(instance, probability));
                    });
        }
    }

    private float parseFloatValue(Object value, float fallback, String fieldName) {
        if (value == null) return fallback;
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException exception) {
            Logs.logWarning("Invalid float value for " + fieldName + ": " + value + ", using " + fallback);
            return fallback;
        }
    }

    private int parseIntegerValue(Object value, int fallback, String fieldName, String effectId) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            Logs.logWarning("Invalid integer value for " + fieldName + " in effect " + effectId + ": " + value + ", using " + fallback);
            return fallback;
        }
    }

    private void handleRemoveEffects(Consumable.Builder consumable, Map<?, ?> effectSection) {
        if (!(effectSection.get("effects") instanceof List<?> effects))
            return;

        List<Holder<MobEffect>> mobEffects = effects.stream()
                .map(Object::toString)
                .map(this::getMobEffectOptional)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
                .toList();

        if (!mobEffects.isEmpty()) {
            consumable.onConsume(new RemoveStatusEffectsConsumeEffect(HolderSet.direct(mobEffects)));
        }
    }

    private void handlePlaySound(Consumable.Builder consumable, Map<?, ?> effectSection, Consumable template) {
        String soundIdStr = Optional.ofNullable(effectSection.get("sound"))
                .map(Object::toString)
                .orElse(null);

        if (soundIdStr != null) {
            getSoundEventOptional(soundIdStr)
                    .map(BuiltInRegistries.SOUND_EVENT::wrapAsHolder)
                    .map(PlaySoundConsumeEffect::new)
                    .ifPresent(consumable::onConsume);
        } else {
            // Use template sound
            consumable.onConsume(new PlaySoundConsumeEffect(template.sound()));
        }
    }

    // ============ Reflection helpers for ResourceLocation/Identifier compatibility
    // ============

    /**
     * Get DataComponentType by looking up the component location via reflection.
     */
    @SuppressWarnings("unchecked")
    private net.minecraft.core.component.DataComponentType<?> getDataComponentType(Object location) {
        try {
            java.lang.reflect.Method getOptional = BuiltInRegistries.DATA_COMPONENT_TYPE.getClass()
                    .getMethod("getOptional", ResourceLocationHelper.getResourceLocationClass());
            Optional<net.minecraft.core.component.DataComponentType<?>> result = (Optional<net.minecraft.core.component.DataComponentType<?>>) getOptional
                    .invoke(
                            BuiltInRegistries.DATA_COMPONENT_TYPE, location);
            return result.orElse(null);
        } catch (Exception e) {
            Logs.logWarning("Failed to get data component type: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get MobEffect optional by effect ID string.
     */
    @SuppressWarnings("unchecked")
    private Optional<MobEffect> getMobEffectOptional(String effectId) {
        try {
            Object location = ResourceLocationHelper.parse(effectId);
            java.lang.reflect.Method getOptional = BuiltInRegistries.MOB_EFFECT.getClass()
                    .getMethod("getOptional", ResourceLocationHelper.getResourceLocationClass());
            return (Optional<MobEffect>) getOptional.invoke(BuiltInRegistries.MOB_EFFECT, location);
        } catch (Exception e) {
            Logs.logWarning("Failed to get mob effect: " + effectId);
            return Optional.empty();
        }
    }

    /**
     * Get SoundEvent optional by sound ID string.
     */
    @SuppressWarnings("unchecked")
    private Optional<SoundEvent> getSoundEventOptional(String soundId) {
        try {
            Object location = ResourceLocationHelper.parse(soundId);
            java.lang.reflect.Method getOptional = BuiltInRegistries.SOUND_EVENT.getClass()
                    .getMethod("getOptional", ResourceLocationHelper.getResourceLocationClass());
            return (Optional<SoundEvent>) getOptional.invoke(BuiltInRegistries.SOUND_EVENT, location);
        } catch (Exception e) {
            Logs.logWarning("Failed to get sound event: " + soundId);
            return Optional.empty();
        }
    }

    /**
     * Create a SoundEvent from a sound ID string.
     */
    private SoundEvent getSoundEventFromId(String soundId) {
        try {
            Object location = ResourceLocationHelper.parse(soundId);
            // SoundEvent constructor takes the location - use reflection since type differs
            java.lang.reflect.Constructor<?> constructor = SoundEvent.class.getConstructor(
                    ResourceLocationHelper.getResourceLocationClass(), Optional.class);
            return (SoundEvent) constructor.newInstance(location, Optional.empty());
        } catch (Exception e) {
            Logs.logWarning("Failed to create sound event: " + soundId);
            return null;
        }
    }

    private ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack nmsItem) {
        if (VersionUtil.atOrAbove("26.2")) {
            // Paper 26.2 made CraftItemStack#asBukkitCopy private. asCraftMirror is still
            // public and returns a Bukkit ItemStack view, so mirror a copied NMS stack to
            // preserve the previous copy semantics.
            return CraftItemStack.asCraftMirror(nmsItem.copy());
        }
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    @Nullable
    public Object consumableComponent(final ItemStack itemStack) {
        if (itemStack == null)
            return null;
        try {
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
            return nmsItem.get(DataComponents.CONSUMABLE);
        } catch (Exception e) {
            Logs.debug(e);
        }
        return null;
    }

    @Override
    @Nullable
    public Object deathProtectionComponent(final ItemStack itemStack) {
        if (itemStack == null)
            return null;
        try {
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
            return nmsItem.get(DataComponents.DEATH_PROTECTION);
        } catch (Exception e) {
            Logs.debug(e);
        }
        return null;
    }

    @Override
    public ItemStack consumableComponent(final ItemStack itemStack, @Nullable Object consumable) {
        if (consumable == null)
            return itemStack;
        try {
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
            nmsItem.set(DataComponents.CONSUMABLE, (Consumable) consumable);
            return asBukkitCopy(nmsItem);
        } catch (Exception e) {
            Logs.debug(e);
        }
        return itemStack;
    }

    @Override
    public ItemStack deathProtectionComponent(final ItemStack itemStack, @Nullable Object deathProtection) {
        if (!(deathProtection instanceof DeathProtection component))
            return itemStack;
        try {
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
            nmsItem.set(DataComponents.DEATH_PROTECTION, component);
            return asBukkitCopy(nmsItem);
        } catch (Exception e) {
            Logs.debug(e);
        }
        return itemStack;
    }

    @Override
    public boolean supportsJukeboxPlaying() {
        return true;
    }

    @Override
    public void playJukeBoxSong(Location location, ItemStack itemStack) {
        if (location == null || location.getWorld() == null) return;
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle().getLevel();
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        Optional<Holder<JukeboxSong>> optional = JukeboxSong.fromStack(level.registryAccess(), nmsItem);
        if (!optional.isPresent())
            return; // should never happen if the itemstack has the jukeboxPlayable component
        int id = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(optional.get().value());
        level.levelEvent(null, LevelEvent.SOUND_PLAY_JUKEBOX_SONG,
                new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), id);
    }

    // ============ Backpack Cosmetic Packet Methods ============

    private static EntityType<?> getEntityType(String entityId) {
        try {
            Object location = ResourceLocationHelper.parse(entityId);
            Method getValue = BuiltInRegistries.ENTITY_TYPE.getClass()
                    .getMethod("getValue", ResourceLocationHelper.getResourceLocationClass());
            Object entityType = getValue.invoke(BuiltInRegistries.ENTITY_TYPE, location);
            if (entityType instanceof EntityType<?> type) return type;
        } catch (ReflectiveOperationException e) {
            Logs.debug(e);
        }
        throw new IllegalStateException("Could not resolve entity type: " + entityId);
    }

    @Override
    public void spawnBackpackArmorStand(Player viewer, int entityId, Location location, ItemStack displayItem, boolean small) {
        ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
        Connection connection = serverPlayer.connection.connection;

        // Debug: Logs.logSuccess("[Backpack] Spawning armor stand for " + viewer.getName() + " at " + location + " (entityId: " + entityId + ")");

        // Create spawn packet for armor stand
        UUID uuid = UUID.randomUUID();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();

        // Spawn entity packet
        ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(
                entityId,
                uuid,
                x, y, z,
                pitch, yaw,
                getEntityType("minecraft:armor_stand"),
                0, // data
                Vec3.ZERO,
                0.0 // head yaw
        );
        connection.send(spawnPacket);

        // Set entity metadata (invisible, small - NO marker flag so equipment renders)
        List<SynchedEntityData.DataValue<?>> metadata = new ArrayList<>();

        // Byte flags at index 0: 0x20 = invisible (armor stand body invisible, equipment still renders)
        metadata.add(SynchedEntityData.DataValue.create(
                new EntityDataAccessor<>(0, EntityDataSerializers.BYTE),
                (byte) 0x20  // Invisible - only equipment renders
        ));

        // Armor stand flags at index 15: 0x01 = small (NO marker flag - marker prevents equipment rendering)
        byte armorStandFlags = (byte) (small ? 0x01 : 0x00);
        // Don't set marker flag (0x10) - it prevents equipment from rendering
        metadata.add(SynchedEntityData.DataValue.create(
                new EntityDataAccessor<>(15, EntityDataSerializers.BYTE),
                armorStandFlags
        ));

        ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(entityId, metadata);
        connection.send(metadataPacket);

        // Set equipment - use HEAD slot to display the item as a head decoration
        // This renders the item model on top of the armor stand
        if (displayItem != null && !displayItem.getType().isAir()) {
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(displayItem);
            List<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> equipment = new ArrayList<>();
            // Set item in head slot - renders as a floating item on the armor stand
            equipment.add(new com.mojang.datafixers.util.Pair<>(
                    net.minecraft.world.entity.EquipmentSlot.HEAD,
                    nmsItem
            ));
            ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(entityId, equipment);
            connection.send(equipmentPacket);
            // Debug: Logs.logSuccess("[Backpack] Sent equipment packet with item in HEAD slot: " + displayItem.getType());
        }
    }

    @Override
    public void sendEntityHeadRotation(Player viewer, int entityId, float yaw) {
        ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
        Connection connection = serverPlayer.connection.connection;

        // Following HMCCosmetics approach: send BOTH rotation packets for proper rotation
        // "First person backpacks need both packets to rotate properly, otherwise they look off"

        // Convert yaw to protocol format (256 steps per rotation)
        byte protocolYaw = (byte) ((int) (yaw * 256.0F / 360.0F));

        // 1. Send entity body rotation packet (ClientboundMoveEntityPacket.Rot)
        connection.send(new ClientboundMoveEntityPacket.Rot(entityId, protocolYaw, (byte) 0, false));

        // 2. Send entity head rotation packet - write manually since we have a fake entity
        // Packet format: VarInt entityId, Byte headYaw
        io.netty.buffer.ByteBuf byteBuf = io.netty.buffer.Unpooled.buffer();
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeVarInt(entityId);
            buf.writeByte(protocolYaw);

            // Create and send the packet using the registry
            ClientboundRotateHeadPacket headPacket = ClientboundRotateHeadPacket.STREAM_CODEC.decode(buf);
            connection.send(headPacket);
        } finally {
            byteBuf.release();
        }
    }

    @Override
    public void sendEntityDestroy(Player viewer, int... entityIds) {
        ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
        Connection connection = serverPlayer.connection.connection;

        ClientboundRemoveEntitiesPacket destroyPacket = new ClientboundRemoveEntitiesPacket(entityIds);
        connection.send(destroyPacket);
    }

    @Override
    public void sendMountPacket(Player viewer, int vehicleId, int... passengerIds) {
        ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
        Connection connection = serverPlayer.connection.connection;

        // Create mount packet by writing to a buffer
        io.netty.buffer.ByteBuf byteBuf = io.netty.buffer.Unpooled.buffer();
        try {
            net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(byteBuf);

            // Write vehicle entity ID as VarInt
            buf.writeVarInt(vehicleId);

            // Write passenger count as VarInt
            buf.writeVarInt(passengerIds.length);

            // Write each passenger ID as VarInt
            for (int passengerId : passengerIds) {
                buf.writeVarInt(passengerId);
            }

            // Create packet using the buffer constructor
            ClientboundSetPassengersPacket mountPacket = ClientboundSetPassengersPacket.STREAM_CODEC.decode(buf);
            connection.send(mountPacket);
        } catch (Exception e) {
            Logs.logWarning("Failed to send mount packet: " + e.getMessage());
            Logs.debug(e);
        } finally {
            byteBuf.release();
        }
    }

}
