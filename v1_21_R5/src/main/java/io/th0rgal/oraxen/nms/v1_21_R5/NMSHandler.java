package io.th0rgal.oraxen.nms.v1_21_R5;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.nms.GlyphHandler;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
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
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.consume_effects.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    private final GlyphHandler glyphHandler;

    public NMSHandler() {
        this.glyphHandler = new io.th0rgal.oraxen.nms.v1_21_R5.GlyphHandler();

        // mineableWith tag handling
        NamespacedKey tagKey = NamespacedKey.fromString("mineable_with_key", OraxenPlugin.get());
        if (!VersionUtil.isPaperServer())
            return;
        if (ChannelInitializeListenerHolder.hasListener(tagKey))
            return;
        ChannelInitializeListenerHolder.addListener(tagKey, (channel -> channel.pipeline().addBefore("packet_handler",
                tagKey.asString(), new ChannelDuplexHandler() {
                    Connection connection = (Connection) channel.pipeline().get("packet_handler");
                    TagNetworkSerialization.NetworkPayload payload = createPayload();

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                        if (msg instanceof ClientboundUpdateTagsPacket updateTagsPacket) {
                            Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = updateTagsPacket
                                    .getTags();
                            if (NoteBlockMechanicFactory.isEnabled()
                                    && NoteBlockMechanicFactory.getInstance().removeMineableTag())
                                tags.put(Registries.BLOCK, payload);
                            msg = new ClientboundUpdateTagsPacket(tags);
                        }
                        ctx.write(msg, promise);
                    }
                })));
    }

    @Override
    public GlyphHandler glyphHandler() {
        return glyphHandler;
    }

    @Override
    public boolean tripwireUpdatesDisabled() {
        return VersionUtil.isPaperServer() && GlobalConfiguration.get().blockUpdates.disableTripwireUpdates;
    }

    @Override
    public boolean noteblockUpdatesDisabled() {
        return VersionUtil.isPaperServer() && GlobalConfiguration.get().blockUpdates.disableNoteblockUpdates;
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

        // Cancels if null.
        if (oldData == null || newData == null)
            return newItem;
        // Creates new nbt compound.
        CompoundTag oldTag = oldData.copyTag();
        CompoundTag newTag = newData.copyTag();

        for (String key : oldTag.keySet()) {
            if (vanillaKeys.contains(key))
                continue;
            Tag value = oldTag.get(key);
            if (value != null)
                newTag.put(key, value);
            else
                newTag.remove(key);
        }

        newNmsItem.set(type, CustomData.of(newTag));
        return CraftItemStack.asBukkitCopy(newNmsItem);
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

        BlockPos pos = hitResult.getBlockPos();
        InteractionResult result = blockItem.place(placeContext);
        if (result == InteractionResult.FAIL)
            return null;
        if (placeContext instanceof DirectionalPlaceContext && player.getGameMode() != org.bukkit.GameMode.CREATIVE)
            itemStack.setAmount(itemStack.getAmount() - 1);
        World world = player.getWorld();

        if (!player.isSneaking()) {
            BlockPos clickPos = placeContext.getClickedPos();
            Block block = world.getBlockAt(clickPos.getX(), clickPos.getY(), clickPos.getZ());
            SoundGroup sound = block.getBlockData().getSoundGroup();

            world.playSound(
                    BlockHelpers.toCenterBlockLocation(block.getLocation()), sound.getPlaceSound(),
                    SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }

        return world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getBlockData();
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

    @Override
    public void customBlockDefaultTools(Player player) {

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
            e.printStackTrace();
        }
        return null;
    }

    private final Map<ResourceLocation, IntList> tagRegistryMap = new HashMap();// createTagRegistryMap();

    /*
     * private static Map<ResourceLocation, IntList> createTagRegistryMap() {
     * return BuiltInRegistries.BLOCK.getTags().map(pair -> {
     * IntArrayList list = new IntArrayList(pair.getSecond().size());
     * if (pair.getFirst().location() == BlockTags.MINEABLE_WITH_AXE.location()) {
     * pair.getSecond().stream()
     * .filter(block -> !block.value().getDescriptionId().endsWith("note_block"))
     * .forEach(block -> list.add(BuiltInRegistries.BLOCK.getId(block.value())));
     * } else pair.getSecond().forEach(block ->
     * list.add(BuiltInRegistries.BLOCK.getId(block.value())));
     * 
     * return Map.of(pair.getFirst().location(), list);
     * }).collect(HashMap::new, Map::putAll, Map::putAll);
     * }
     */

    @Override
    public boolean getSupported() {
        return true;
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
            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(new ItemStack(item.getType()));
            net.minecraft.resources.ResourceLocation componentLocation = net.minecraft.resources.ResourceLocation
                    .tryParse("minecraft:" + componentKey.toLowerCase());
            if (componentLocation == null)
                return false;

            net.minecraft.core.component.DataComponentType<?> componentType = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                    .getOptional(componentLocation)
                    .orElse(null);
            if (componentType == null)
                return false;

            if (component instanceof ConfigurationSection config) {
                // Handle YAML configuration
                net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
                convertConfigToNBT(config, nbt);

                // Get default component
                Object defaultComponent = nmsItem.getComponents().get(componentType);
                if (defaultComponent == null) {
                    try {
                        Class<?> componentClass = componentType.getClass();
                        if (componentClass.getMethod("builder").getReturnType().getSimpleName().endsWith("Builder")) {
                            defaultComponent = componentClass.getMethod("builder").invoke(null);
                            defaultComponent = componentClass.getMethod("build").invoke(defaultComponent);
                        } else {
                            Constructor<?> constructor = componentClass.getDeclaredConstructor();
                            constructor.setAccessible(true);
                            defaultComponent = constructor.newInstance();
                        }
                    } catch (Exception e) {
                        io.th0rgal.oraxen.utils.logs.Logs
                                .logWarning("Failed to create default component for " + componentKey);
                        return false;
                    }
                }

                // Apply NBT to component
                try {
                    Method fromTag = defaultComponent.getClass().getMethod("fromTag",
                            net.minecraft.nbt.CompoundTag.class);
                    fromTag.setAccessible(true);
                    Object parsedComponent = fromTag.invoke(defaultComponent, nbt);

                    // Use reflection to access and modify the components map
                    Field componentsField = net.minecraft.world.item.ItemStack.class.getDeclaredField("components");
                    componentsField.setAccessible(true);
                    Map components = (Map) componentsField.get(nmsItem);
                    components.put(componentType, parsedComponent);

                    return true;
                } catch (Exception e) {
                    io.th0rgal.oraxen.utils.logs.Logs
                            .logWarning("Failed to apply NBT data to component " + componentKey);
                    if (io.th0rgal.oraxen.config.Settings.DEBUG.toBool())
                        e.printStackTrace();
                    return false;
                }
            } else {
                // Handle direct component object
                try {
                    // Use reflection to access and modify the components map
                    Field componentsField = net.minecraft.world.item.ItemStack.class.getDeclaredField("components");
                    componentsField.setAccessible(true);
                    Map components = (Map) componentsField.get(nmsItem);
                    components.put(componentType, component);

                    return true;
                } catch (Exception e) {
                    io.th0rgal.oraxen.utils.logs.Logs.logWarning("Failed to set component " + componentKey);
                    if (io.th0rgal.oraxen.config.Settings.DEBUG.toBool())
                        e.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e) {
            io.th0rgal.oraxen.utils.logs.Logs.logWarning("Failed to set component " + componentKey);
            if (io.th0rgal.oraxen.config.Settings.DEBUG.toBool())
                e.printStackTrace();
            return false;
        }
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
        } else if (first instanceof Integer) {
            nbt.putIntArray(key, list.stream().mapToInt(i -> (Integer) i).toArray());
        } else if (first instanceof Long) {
            nbt.putLongArray(key, list.stream().mapToLong(l -> (Long) l).toArray());
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void foodComponent(ItemBuilder item, ConfigurationSection foodSection) {
        FoodComponent foodComponent = new ItemStack(item.getType()).getItemMeta().getFood();

        // Ensure nutrition is non-negative
        int nutrition = Math.max(foodSection.getInt("nutrition"), 0);
        foodComponent.setNutrition(nutrition);

        // Ensure saturation is non-negative
        float saturation = Math.max((float) foodSection.getDouble("saturation", 0.0), 0f);
        foodComponent.setSaturation(saturation);

        foodComponent.setCanAlwaysEat(foodSection.getBoolean("can_always_eat", false));

        item.setFoodComponent(foodComponent);
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
            ResourceLocation soundLocation = ResourceLocation.parse(soundId);
            consumable.sound(Holder.direct(new SoundEvent(soundLocation, Optional.empty())));
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
                        float diameter = Optional.ofNullable(effectSection.get("diameter"))
                                .map(d -> Float.parseFloat(d.toString()))
                                .orElse(16f);
                        consumable.onConsume(new TeleportRandomlyConsumeEffect(diameter));
                    }
                    case "play_sound" -> handlePlaySound(consumable, effectSection, template);
                    default -> Logs.logWarning("Invalid ConsumeEffect-Type " + type);
                }
            }
        }

        item.setConsumableComponent(consumable.build());
    }

    private void handleApplyEffects(Consumable.Builder consumable, Map<?, ?> effectSection) {
        if (!(effectSection.get("effects") instanceof Map<?, ?> effects))
            return;

        float probability = Optional.ofNullable(effectSection.get("probability"))
                .map(p -> Float.parseFloat(p.toString()))
                .orElse(1.0f);

        for (Map.Entry<?, ?> entry : effects.entrySet()) {
            String effectId = entry.getKey().toString();
            Map<String, Object> effectData = (Map<String, Object>) entry.getValue();

            BuiltInRegistries.MOB_EFFECT.getOptional(ResourceLocation.parse(effectId))
                    .map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
                    .ifPresent(effect -> {
                        int duration = Optional.ofNullable(effectData.get("duration"))
                                .map(d -> Integer.parseInt(d.toString()) * 20)
                                .orElse(20);
                        int amplifier = Optional.ofNullable(effectData.get("amplifier"))
                                .map(a -> Integer.parseInt(a.toString()))
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

    private void handleRemoveEffects(Consumable.Builder consumable, Map<?, ?> effectSection) {
        if (!(effectSection.get("effects") instanceof List<?> effects))
            return;

        List<Holder<MobEffect>> mobEffects = effects.stream()
                .map(Object::toString)
                .map(ResourceLocation::parse)
                .map(BuiltInRegistries.MOB_EFFECT::getOptional)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
                .toList();

        if (!mobEffects.isEmpty()) {
            consumable.onConsume(new RemoveStatusEffectsConsumeEffect(HolderSet.direct(mobEffects)));
        }
    }

    private void handlePlaySound(Consumable.Builder consumable, Map<?, ?> effectSection, Consumable template) {
        ResourceLocation soundKey = Optional.ofNullable(effectSection.get("sound"))
                .map(Object::toString)
                .map(ResourceLocation::parse)
                .orElse(template.sound().value().location());

        BuiltInRegistries.SOUND_EVENT.getOptional(soundKey)
                .map(BuiltInRegistries.SOUND_EVENT::wrapAsHolder)
                .map(PlaySoundConsumeEffect::new)
                .ifPresent(consumable::onConsume);
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
            e.printStackTrace();
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
            return CraftItemStack.asBukkitCopy(nmsItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemStack;
    }
}
