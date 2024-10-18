package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.utils.InteractionResult;
import io.th0rgal.oraxen.utils.wrappers.PotionEffectTypeWrapper;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public int playerProtocolVersion(Player player) {
        return -1;
    }

    @Override
    public void applyMiningEffect(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE, -1, Integer.MAX_VALUE, false, false, false));
    }

    @Override
    public void removeMiningEffect(Player player) {
        player.removePotionEffect(PotionEffectTypeWrapper.MINING_FATIGUE);
    }

    @Override
    public String getNoteBlockInstrument(Block block) {
        return "block.note_block.harp";
    }

    @Override
    public int mcmetaVersion() {
        return 34;
    }
}
