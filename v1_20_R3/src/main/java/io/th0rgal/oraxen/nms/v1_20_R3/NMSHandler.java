package io.th0rgal.oraxen.nms.v1_20_R3;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.nms.GlyphHandler;
import io.th0rgal.oraxen.nms.v1_20_R3.furniture.FurniturePacketManager;
import io.th0rgal.oraxen.pack.server.OraxenPackServer;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.InteractionResult;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Optional;

@SuppressWarnings("unused")
public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    private final io.th0rgal.oraxen.nms.GlyphHandler glyphHandler;
    FurniturePacketManager furniturePacketManager = new FurniturePacketManager();

    public NMSHandler() {
        this.glyphHandler = new io.th0rgal.oraxen.nms.v1_20_R3.GlyphHandler();
    }

    @Override
    public GlyphHandler glyphHandler() {
        return glyphHandler;
    }

    @Override
    public IFurniturePacketManager furniturePacketManager() {
        return furniturePacketManager;
    }

    private static Field serverResourcePackInfo;

    static {
        try {
            serverResourcePackInfo = DedicatedServerProperties.class.getDeclaredField("serverResourcePackInfo");
            serverResourcePackInfo.setAccessible(true);
        } catch (Exception ignored) {}
    }

    @Override
    public void setServerResourcePack() {
        DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
        OraxenPackServer packServer = OraxenPlugin.get().packServer();
        try {
            serverResourcePackInfo.set(dedicatedServer.settings.getProperties(), Optional.ofNullable(packServer.packInfo()).map(packInfo ->
                    new MinecraftServer.ServerResourcePackInfo(
                            packInfo.id(), packServer.packUrl(), packInfo.hash(), packServer.mandatory,
                            PaperAdventure.asVanilla(packServer.prompt)
                    )
            ));
        } catch (IllegalAccessException e) {
            Logs.logWarning("Failed to set ServerResourcePack...");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }
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
    public ItemStack copyItemNBTTags(@NotNull ItemStack oldItem, @NotNull ItemStack newItem) {
        CompoundTag oldTag = CraftItemStack.asNMSCopy(oldItem).getOrCreateTag();
        net.minecraft.world.item.ItemStack newNmsItem = CraftItemStack.asNMSCopy(newItem);
        CompoundTag newTag = newNmsItem.getOrCreateTag();
        oldTag.getAllKeys().stream().filter(key -> !vanillaKeys.contains(key)).forEach(key -> newTag.put(key, oldTag.get(key)));
        newNmsItem.setTag(newTag);
        return CraftItemStack.asBukkitCopy(newNmsItem);
    }

    @Override
    @Nullable
    public InteractionResult correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
        InteractionHand hand = slot == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        BlockHitResult hitResult = getPlayerPOVHitResult(serverPlayer.level(), serverPlayer, ClipContext.Fluid.NONE);
        BlockPlaceContext placeContext = new BlockPlaceContext(serverPlayer.level(), serverPlayer, hand, nmsStack, hitResult);

        if (!(nmsStack.getItem() instanceof BlockItem blockItem)) {
            InteractionResult result = InteractionResult.fromNms(nmsStack.getItem().useOn(new UseOnContext(serverPlayer, hand, hitResult)));
            return player.isSneaking() && player.getGameMode() != GameMode.CREATIVE ? result
                    : InteractionResult.fromNms(serverPlayer.gameMode.useItem(serverPlayer, serverPlayer.level(), nmsStack, hand));
        }

        InteractionResult result = InteractionResult.fromNms(blockItem.place(placeContext));
        if (result == InteractionResult.FAIL) return null;

        if (!player.isSneaking()) {
            World world = player.getWorld();
            BlockPos clickPos = placeContext.getClickedPos();
            Block block = world.getBlockAt(clickPos.getX(), clickPos.getY(), clickPos.getZ());
            SoundGroup sound = block.getBlockData().getSoundGroup();

            world.playSound(
                    BlockHelpers.toCenterBlockLocation(block.getLocation()), sound.getPlaceSound(),
                    SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F
            );
        }

        return result;
    }

    public BlockHitResult getPlayerPOVHitResult(Level world, net.minecraft.world.entity.player.Player player, ClipContext.Fluid fluidHandling) {
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
    public int playerProtocolVersion(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection.protocolVersion;
    }

    @Override
    public boolean getSupported() {
        return true;
    }

    @Override
    public void applyMiningEffect(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(
                new ClientboundUpdateMobEffectPacket(player.getEntityId(),
                        new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 0, -1,
                                true, false, false))
        );
    }

    @Override
    public void removeMiningEffect(Player player) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveMobEffectPacket(player.getEntityId(), MobEffects.DIG_SLOWDOWN));
    }

    @Override
    public String getNoteBlockInstrument(Block block) {
        return ((CraftBlock) block).getNMS().instrument().toString();
    }
}
