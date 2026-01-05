package io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/**
 * Mechanic for cosmetic backpacks that appear on the player's back.
 * Uses packet-based invisible armor stands to display the backpack model.
 */
public class BackpackCosmeticMechanic extends Mechanic {

    @Nullable
    private final EquipmentSlot triggerSlot;
    private final boolean triggerFromInventory;
    private final String displayModel;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final float scale;
    private final int viewDistance;
    private final boolean hideInSpectator;
    private final boolean smallArmorStand;
    private final boolean visibleToSelf;

    public BackpackCosmeticMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        // Which equipment slot triggers the backpack display
        // Special value "INVENTORY" means anywhere in inventory except hands
        String slotStr = section.getString("slot", "CHEST").toUpperCase();
        if (slotStr.equals("INVENTORY")) {
            this.triggerSlot = null;
            this.triggerFromInventory = true;
        } else {
            try {
                this.triggerSlot = EquipmentSlot.valueOf(slotStr);
                this.triggerFromInventory = false;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid equipment slot: " + slotStr + ". Valid values: HAND, OFF_HAND, HEAD, CHEST, LEGS, FEET, INVENTORY");
            }
        }

        // The model to display on the armor stand's head
        this.displayModel = section.getString("model");

        // Position offset relative to player
        ConfigurationSection offsetSection = section.getConfigurationSection("offset");
        if (offsetSection != null) {
            this.offsetX = offsetSection.getDouble("x", 0.0);
            this.offsetY = offsetSection.getDouble("y", 0.3);
            this.offsetZ = offsetSection.getDouble("z", -0.3);
        } else {
            this.offsetX = 0.0;
            this.offsetY = 0.3;
            this.offsetZ = -0.3;
        }

        // Scale of the armor stand
        this.scale = (float) section.getDouble("scale", 1.0);

        // View distance in blocks
        this.viewDistance = section.getInt("view_distance", 48);

        // Hide in spectator mode
        this.hideInSpectator = section.getBoolean("hide_in_spectator", true);

        // Use small armor stand
        this.smallArmorStand = section.getBoolean("small", false);

        // Whether the owner can see their own backpack
        this.visibleToSelf = section.getBoolean("visible_to_self", true);
    }

    @Nullable
    public EquipmentSlot getTriggerSlot() {
        return triggerSlot;
    }

    /**
     * Returns true if the backpack should trigger when the item is anywhere in inventory (except hands).
     */
    public boolean triggersFromInventory() {
        return triggerFromInventory;
    }

    public String getDisplayModel() {
        return displayModel;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public float getScale() {
        return scale;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public boolean hideInSpectator() {
        return hideInSpectator;
    }

    public boolean isSmallArmorStand() {
        return smallArmorStand;
    }

    public boolean isVisibleToSelf() {
        return visibleToSelf;
    }
}
