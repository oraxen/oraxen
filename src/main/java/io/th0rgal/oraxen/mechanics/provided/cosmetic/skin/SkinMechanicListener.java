package io.th0rgal.oraxen.mechanics.provided.cosmetic.skin;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.cosmetic.skinnable.SkinnableMechanicFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SkinMechanicListener implements Listener {
    private final SkinMechanicFactory factory;

    public SkinMechanicListener(SkinMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack skin = event.getCursor();
        ItemStack skinnable = event.getCurrentItem();
        String skinID = OraxenItems.getIdByItem(skin);
        String skinnableID = OraxenItems.getIdByItem(skinnable);
        if (factory.isNotImplementedIn(skinID)
            || SkinnableMechanicFactory.get().isNotImplementedIn(skinnableID))
            return;
        if (skin == null || skinnable == null) return;

        ItemMeta skinMeta = skin.getItemMeta();
        ItemMeta skinnableMeta = skinnable.getItemMeta();
        if (skinMeta == null || skinnableMeta == null) return;
        if (!skinMeta.hasCustomModelData() || skin.getType() != skinnable.getType()) return;

        int changeSkin = skinMeta.getCustomModelData();

        if (skinnableMeta.hasCustomModelData() && changeSkin == skinnableMeta.getCustomModelData()) return;

        skinnableMeta.setCustomModelData(changeSkin);
        skinnable.setItemMeta(skinnableMeta);

        SkinMechanic skinMechanic = (SkinMechanic) factory.getMechanic(skinID);

        if (skinMechanic.doConsume())
            skin.setAmount(skin.getAmount() - 1);

        event.setCancelled(true);
    }

}
