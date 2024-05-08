package io.th0rgal.oraxen.nms;

import net.minecraft.world.InteractionResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
    public Enum<InteractionResult> correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
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
    public void applyMiningFatigue(Player player) {

    }

    @Override
    public void removeMiningFatigue(Player player) {

    }
}
