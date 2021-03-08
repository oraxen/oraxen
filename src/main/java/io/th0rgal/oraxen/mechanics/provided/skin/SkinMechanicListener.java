package io.th0rgal.oraxen.mechanics.provided.skin;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.skinnable.SkinnableMechanicFactory;
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
        String skinItemID = OraxenItems.getIdByItem(skin);
        String skinnableItemID = OraxenItems.getIdByItem(skinnable);
        if (factory.isNotImplementedIn(skinItemID) || SkinnableMechanicFactory.get().isNotImplementedIn(skinnableItemID))
            return;

        if (!skin.getItemMeta().hasCustomModelData() || skin.getType() != skinnable.getType())
            return;

        int changeSkin = skin.getItemMeta().getCustomModelData();

        if (skinnable.getItemMeta().hasCustomModelData() && changeSkin == skinnable.getItemMeta().getCustomModelData())
            return;

        ItemMeta skinnableMeta = skinnable.getItemMeta();
        skinnableMeta.setCustomModelData(changeSkin);
        skinnable.setItemMeta(skinnableMeta);

        SkinMechanic skinMechanic = (SkinMechanic) factory.getMechanic(skinItemID);

        if (skinMechanic.doConsume())
            skin.setAmount(skin.getAmount() - 1);

        event.setCancelled(true);
    }

}
