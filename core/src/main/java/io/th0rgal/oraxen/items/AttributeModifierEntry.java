package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Holds a single attribute modifier entry with its associated attribute, slot,
 * and optional display configuration for modern tooltip control (1.21.6+).
 *
 * <p>Supports both the modern {@code DataComponent} API (1.21.2+) and the legacy
 * {@code ItemMeta} multimap approach for older versions.</p>
 */
public final class AttributeModifierEntry {

    private final Attribute attribute;
    private final AttributeModifier modifier;
    private final EquipmentSlotGroup slot;
    @Nullable
    private final DisplayMode displayMode;
    @Nullable
    private final String displayText;

    public AttributeModifierEntry(
            @NotNull Attribute attribute,
            @NotNull AttributeModifier modifier,
            @NotNull EquipmentSlotGroup slot,
            @Nullable DisplayMode displayMode,
            @Nullable String displayText
    ) {
        this.attribute = Objects.requireNonNull(attribute, "attribute");
        this.modifier = Objects.requireNonNull(modifier, "modifier");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.displayMode = displayMode;
        this.displayText = displayText;
    }

    public AttributeModifierEntry(
            @NotNull Attribute attribute,
            @NotNull AttributeModifier modifier,
            @NotNull EquipmentSlotGroup slot
    ) {
        this(attribute, modifier, slot, null, null);
    }

    @NotNull
    public Attribute attribute() {
        return attribute;
    }

    @NotNull
    public AttributeModifier modifier() {
        return modifier;
    }

    @NotNull
    public EquipmentSlotGroup slot() {
        return slot;
    }

    @Nullable
    public DisplayMode displayMode() {
        return displayMode;
    }

    @Nullable
    public String displayText() {
        return displayText;
    }

    /**
     * Adds this entry to a Paper {@code ItemAttributeModifiers.Builder}.
     * Only callable on Paper 1.21.2+ where the DataComponent API exists.
     *
     * <p>On 1.21.6+, if a {@link DisplayMode} is set, it will be applied to
     * control how this modifier appears in tooltips.</p>
     */
    public void addToComponentBuilder(@NotNull Object builder) {
        try {
            var iamBuilder = (io.papermc.paper.datacomponent.item.ItemAttributeModifiers.Builder) builder;

            if (VersionUtil.atOrAbove("1.21.6") && displayMode != null) {
                var display = buildDisplayComponent();
                if (display != null) {
                    iamBuilder.addModifier(attribute, modifier, slot,
                            (io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay) display);
                    return;
                }
            }

            iamBuilder.addModifier(attribute, modifier, slot);
        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            // Graceful fallback — caller should use legacy path
        }
    }

    @Nullable
    private Object buildDisplayComponent() {
        try {
            return switch (displayMode) {
                case HIDDEN -> io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay.hidden();
                case RESET -> io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay.reset();
                case OVERRIDE -> {
                    if (displayText == null || displayText.isEmpty()) {
                        yield io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay.reset();
                    }
                    yield io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay.override(
                            AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(displayText));
                }
            };
        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            return null;
        }
    }

    /**
     * Controls how an attribute modifier is displayed in item tooltips (1.21.6+).
     */
    public enum DisplayMode {
        /** Hide the modifier from tooltips entirely. */
        HIDDEN,
        /** Show using the default vanilla tooltip format. */
        RESET,
        /** Show custom text instead of the default tooltip line. */
        OVERRIDE;

        @Nullable
        public static DisplayMode fromString(@Nullable String value) {
            if (value == null || value.isEmpty()) return null;
            return switch (value.toLowerCase()) {
                case "hidden" -> HIDDEN;
                case "reset", "default" -> RESET;
                case "override", "custom" -> OVERRIDE;
                default -> null;
            };
        }
    }

    /**
     * Creates an entry from a modern config section format:
     * <pre>
     * AttributeModifiers:
     *   modifier_name:
     *     attribute: ATTACK_DAMAGE
     *     amount: 5.0
     *     operation: ADD_NUMBER
     *     slot: HAND
     *     display:
     *       type: hidden
     * </pre>
     */
    public static @Nullable AttributeModifierEntry fromConfigSection(
            @NotNull String itemId,
            @NotNull String key,
            @NotNull org.bukkit.configuration.ConfigurationSection section
    ) {
        String attributeStr = section.getString("attribute");
        if (attributeStr == null || attributeStr.isEmpty()) return null;

        Attribute attribute = io.th0rgal.oraxen.utils.wrappers.AttributeWrapper.fromString(attributeStr);
        if (attribute == null) return null;

        double amount = section.getDouble("amount", 0.0);

        String operationStr = section.getString("operation", "ADD_NUMBER");
        AttributeModifier.Operation operation;
        try {
            operation = AttributeModifier.Operation.valueOf(operationStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            operation = AttributeModifier.Operation.ADD_NUMBER;
        }

        String slotStr = section.getString("slot");
        EquipmentSlotGroup slot = parseSlotGroup(slotStr);

        NamespacedKey modifierKey = NamespacedKey.fromString("oraxen:" + itemId + "_" + key);
        if (modifierKey == null) {
            modifierKey = NamespacedKey.fromString("oraxen:modifier");
        }

        AttributeModifier modifier = new AttributeModifier(modifierKey, amount, operation, slot);

        DisplayMode displayMode = null;
        String displayText = null;
        if (section.isConfigurationSection("display")) {
            var displaySection = section.getConfigurationSection("display");
            if (displaySection != null) {
                displayMode = DisplayMode.fromString(displaySection.getString("type"));
                displayText = displaySection.getString("text");
            }
        }

        return new AttributeModifierEntry(attribute, modifier, slot, displayMode, displayText);
    }

    /**
     * Parses an {@link EquipmentSlotGroup} from a string, with common alias handling.
     * Returns {@link EquipmentSlotGroup#ANY} if the input is null or unrecognized.
     */
    @NotNull
    public static EquipmentSlotGroup parseSlotGroup(@Nullable String slot) {
        if (slot == null || slot.isEmpty()) return EquipmentSlotGroup.ANY;

        // Handle common alias: "hand" -> "mainhand"
        String normalized = slot.toLowerCase().replace("main_hand", "mainhand").replace("off_hand", "offhand");
        if (normalized.equals("hand")) normalized = "mainhand";

        EquipmentSlotGroup group = EquipmentSlotGroup.getByName(normalized);
        return group != null ? group : EquipmentSlotGroup.ANY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeModifierEntry that)) return false;
        return attribute.equals(that.attribute)
                && modifier.equals(that.modifier)
                && slot.equals(that.slot)
                && displayMode == that.displayMode
                && Objects.equals(displayText, that.displayText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, modifier, slot, displayMode, displayText);
    }

    @Override
    public String toString() {
        return "AttributeModifierEntry{" +
                "attribute=" + attribute +
                ", modifier=" + modifier +
                ", slot=" + slot +
                ", displayMode=" + displayMode +
                '}';
    }
}
