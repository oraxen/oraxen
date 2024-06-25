package io.th0rgal.oraxen.nms;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import io.th0rgal.oraxen.utils.InteractionResult;
import io.th0rgal.oraxen.utils.wrappers.PotionEffectTypeWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

public class EmptyNMSHandler implements NMSHandler {
    @Override
    public GlyphHandler glyphHandler() {
        return new GlyphHandler.EmptyGlyphHandler();
    }

    @Override
    public boolean noteblockUpdatesDisabled() {
        return false;
    }

    @Override
    public boolean tripwireUpdatesDisabled() {
        return false;
    }

    @Override
    public ItemStack copyItemNBTTags(@NotNull ItemStack oldItem, @NotNull ItemStack newItem) {
        return newItem;
    }

    @Nullable
    @Override
    public InteractionResult correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
        return null;
    }

    @Override
    public void customBlockDefaultTools(Player player) {

    }

    @NotNull
    @Override
    public @Unmodifiable Set<Material> itemTools() {
        return Set.of();
    }

    @Override
    public void applyMiningEffect(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EFFECT);
            packet.getIntegers().write(0, player.getEntityId()).write(1, -1);
            packet.getEffectTypes().write(0, PotionEffectTypeWrapper.MINING_FATIGUE);
            packet.getBytes().write(0, (byte) -1).write(1, (byte) 0);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } else player.addPotionEffect(new PotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE, -1, Integer.MAX_VALUE, false, false, false));
    }

    @Override
    public void removeMiningEffect(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
            packet.getIntegers().write(0, player.getEntityId());
            packet.getEffectTypes().write(0, PotionEffectTypeWrapper.MINING_FATIGUE);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } else player.removePotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE);
    }

    @Override
    public String getNoteBlockInstrument(Block block) {
        return "block.note_block.harp";
    }
}
