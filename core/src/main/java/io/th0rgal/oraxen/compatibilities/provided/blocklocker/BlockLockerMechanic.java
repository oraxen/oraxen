package io.th0rgal.oraxen.compatibilities.provided.blocklocker;

import io.th0rgal.oraxen.EnumUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import nl.rutgerkok.blocklocker.ProtectionType;
import org.bukkit.configuration.ConfigurationSection;

public class BlockLockerMechanic {
    private final boolean canProtect;
    private final ProtectionType protectionType;

    public BlockLockerMechanic(ConfigurationSection section) {
        this.canProtect = section.getBoolean("can_protect", true);
        this.protectionType = EnumUtils.getEnumOrElse(ProtectionType.class, section.getString("protection_type"), (protType) -> {
            Logs.logError("Invalid protection type " + protType + " for BlockLocker mechanic in item " + section.getParent().getParent().toString() + ", defaulting to CONTAINER");
            return ProtectionType.CONTAINER;
        });
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
