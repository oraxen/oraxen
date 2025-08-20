package io.th0rgal.oraxen.nms.v1_21_R1;

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
import io.th0rgal.oraxen.utils.PotionUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    private final GlyphHandler glyphHandler;

    public NMSHandler() {
        this.glyphHandler = new io.th0rgal.oraxen.nms.v1_21_R1.GlyphHandler();

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

    @Override // TODO Fix this
    public ItemStack copyItemNBTTags(@NotNull ItemStack oldItem, @NotNull ItemStack newItem) {
        net.minecraft.world.item.ItemStack newNmsItem = CraftItemStack.asNMSCopy(newItem);
        net.minecraft.world.item.ItemStack oldItemStack = CraftItemStack.asNMSCopy(oldItem);
        CraftItemStack.asNMSCopy(oldItem).getTags().forEach(tag -> {
            if (!tag.location().getNamespace().equals("minecraft"))
                return;
            if (vanillaKeys.contains(tag.location().getPath()))
                return;

            DataComponentType<Object> type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE
                .get(tag.location());
            if (type != null)
                newNmsItem.set(type, oldItemStack.get(type));
        });
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private final Map<ResourceLocation, IntList> tagRegistryMap = createTagRegistryMap();

    private static Map<ResourceLocation, IntList> createTagRegistryMap() {
        return BuiltInRegistries.BLOCK.getTags().map(pair -> {
            IntArrayList list = new IntArrayList(pair.getSecond().size());
            if (pair.getFirst().location() == BlockTags.MINEABLE_WITH_AXE.location()) {
                pair.getSecond().stream()
                    .filter(block -> !block.value().getDescriptionId().endsWith("note_block"))
                    .forEach(block -> list.add(BuiltInRegistries.BLOCK.getId(block.value())));
            } else
                pair.getSecond().forEach(block -> list.add(BuiltInRegistries.BLOCK.getId(block.value())));

            return Map.of(pair.getFirst().location(), list);
        }).collect(HashMap::new, Map::putAll, Map::putAll);
    }

    @Override
    public boolean getSupported() {
        return true;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void foodComponent(ItemBuilder item, ConfigurationSection foodSection) {
        FoodComponent foodComponent = new ItemStack(item.getType()).getItemMeta().getFood();
        foodComponent.setNutrition(foodSection.getInt("nutrition"));
        foodComponent.setSaturation((float) foodSection.getDouble("saturation", 0.0));
        foodComponent.setCanAlwaysEat(foodSection.getBoolean("can_always_eat"));

        foodComponent.setEatSeconds((float) foodSection.getDouble("eat_seconds", 1.6));

        ConfigurationSection effectsSection = foodSection.getConfigurationSection("effects");
        if (effectsSection != null)
            for (String effect : effectsSection.getKeys(false)) {
                ConfigurationSection effectSection = effectsSection.getConfigurationSection(effect);
                PotionEffectType effectType = PotionUtils.getEffectType(effect);
                if (effectSection == null || effectType == null)
                    Logs.logError("Invalid potion effect: " + effect + ", in "
                        + StringUtils.substringBefore(effectsSection.getCurrentPath(), ".") + " food-property!");
                else {
                    foodComponent.addEffect(
                        new PotionEffect(effectType,
                            effectSection.getInt("duration", 1) * 20,
                            effectSection.getInt("amplifier", 0),
                            effectSection.getBoolean("ambient", true),
                            effectSection.getBoolean("show_particles", true),
                            effectSection.getBoolean("show_icon", true)),
                        (float) effectSection.getDouble("probability", 1.0));
                }
            }

        item.setFoodComponent(foodComponent);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
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

    private void convertConfigToNBT(ConfigurationSection config, CompoundTag nbt) {
        for (String key : config.getKeys(false)) {
            Object value = config.get(key);
            if (value instanceof ConfigurationSection section) {
                CompoundTag compound = new CompoundTag();
                convertConfigToNBT(section, compound);
                nbt.put(key, compound);
            } else if (value instanceof Number number) {
                if (value instanceof Integer)
                    nbt.putInt(key, number.intValue());
                else if (value instanceof Double)
                    nbt.putDouble(key, number.doubleValue());
                else if (value instanceof Float)
                    nbt.putFloat(key, number.floatValue());
                else if (value instanceof Long)
                    nbt.putLong(key, number.longValue());
                else if (value instanceof Byte)
                    nbt.putByte(key, number.byteValue());
                else if (value instanceof Short)
                    nbt.putShort(key, number.shortValue());
            } else if (value instanceof Boolean) {
                nbt.putBoolean(key, (Boolean) value);
            } else if (value instanceof String) {
                nbt.putString(key, (String) value);
            } else if (value instanceof List<?> list) {
                if (!list.isEmpty()) {
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
            }
        }
    }
}
