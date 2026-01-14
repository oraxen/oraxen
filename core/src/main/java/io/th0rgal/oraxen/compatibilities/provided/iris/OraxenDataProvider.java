package io.th0rgal.oraxen.compatibilities.provided.iris;

import com.volmit.iris.core.link.ExternalDataProvider;
import com.volmit.iris.core.link.Identifier;
import com.volmit.iris.core.link.data.DataType;
import com.volmit.iris.core.service.ExternalDataSVC;
import com.volmit.iris.engine.framework.Engine;
import com.volmit.iris.util.collection.KMap;
import com.volmit.iris.util.data.IrisCustomData;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;

public class OraxenDataProvider extends ExternalDataProvider {

    public OraxenDataProvider() {
        super("Oraxen");
    }

    @Override
    public void init() {
    }

    @NotNull
    @Override
    public BlockData getBlockData(@NotNull Identifier blockId, @NotNull KMap<String, String> state) throws MissingResourceException {
        if (!OraxenItems.exists(blockId.key())) {
            throw new MissingResourceException("Failed to find BlockData!", blockId.namespace(), blockId.key());
        }

        if (!OraxenBlocks.isOraxenBlock(blockId.key())) {
            throw new MissingResourceException("Failed to find BlockData!", blockId.namespace(), blockId.key());
        }

        BlockData data = OraxenBlocks.getOraxenBlockData(blockId.key());
        if (data == null) {
            throw new MissingResourceException("Failed to find BlockData!", blockId.namespace(), blockId.key());
        }

        Identifier blockState = ExternalDataSVC.buildState(blockId, state);
        return IrisCustomData.of(data, blockState);
    }

    @NotNull
    @Override
    public ItemStack getItemStack(@NotNull Identifier itemId, @NotNull KMap<String, Object> customNbt) throws MissingResourceException {
        ItemBuilder builder = OraxenItems.getItemById(itemId.key());
        if (builder == null) {
            throw new MissingResourceException("Failed to find ItemData!", itemId.namespace(), itemId.key());
        }
        try {
            return builder.build();
        } catch (Exception e) {
            throw new MissingResourceException("Failed to find ItemData!", itemId.namespace(), itemId.key());
        }
    }

    @Override
    public void processUpdate(@NotNull Engine engine, @NotNull Block block, @NotNull Identifier blockId) {
        var statePair = ExternalDataSVC.parseState(blockId);
        blockId = statePair.getA();

        if (OraxenBlocks.isOraxenBlock(blockId.key())) {
            OraxenBlocks.place(blockId.key(), block.getLocation());
        }
    }

    @Override
    public @NotNull Collection<Identifier> getTypes(@NotNull DataType dataType) {
        if (dataType == DataType.ENTITY) return List.of();
        return Arrays.stream(OraxenItems.getItemNames())
                .map(i -> new Identifier("oraxen", i))
                .filter(dataType.asPredicate(this))
                .toList();
    }

    @Override
    public boolean isValidProvider(@NotNull Identifier id, DataType dataType) {
        if (dataType == DataType.ENTITY) return false;
        return "oraxen".equalsIgnoreCase(id.namespace());
    }
}
