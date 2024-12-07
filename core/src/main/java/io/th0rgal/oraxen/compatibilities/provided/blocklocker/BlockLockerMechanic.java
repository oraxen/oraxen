package io.th0rgal.oraxen.compatibilities.provided.blocklocker;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import nl.rutgerkok.blocklocker.ProtectionType;
import org.bukkit.configuration.ConfigurationSection;

public class BlockLockerMechanic {
    private final boolean canProtect;
    private final ProtectionType protectionType;

    public BlockLockerMechanic(ConfigurationSection section) {
        ProtectionType protectionType;
        try {
            protectionType = ProtectionType.valueOf(section.getString("protection_type", "CONTAINER"));
        } catch (IllegalArgumentException e) {
            protectionType = ProtectionType.CONTAINER;
            Message.INVALID_BLOCKLOCKER_PROTECTION_TYPE
                    .log(AdventureUtils.tagResolver("item", section.getParent().getParent().toString()));
        }

        this.canProtect = section.getBoolean("can_protect", true);
        this.protectionType = protectionType;
    }

    public BlockLockerMechanic(boolean canProtect, ProtectionType protectionType) {
        this.canProtect = canProtect;
        this.protectionType = protectionType;
    }

    public boolean canProtect() {
        return canProtect;
    }

    public ProtectionType getProtectionType() {
        return protectionType;
    }
}
