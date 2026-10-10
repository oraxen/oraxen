package io.th0rgal.oraxen.items;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AttributeModifierEntryTest {

    @Nested
    class DisplayModeTests {

        @Test
        void fromString_hidden() {
            assertEquals(
                    AttributeModifierEntry.DisplayMode.HIDDEN,
                    AttributeModifierEntry.DisplayMode.fromString("hidden"));
        }

        @Test
        void fromString_reset() {
            assertEquals(
                    AttributeModifierEntry.DisplayMode.RESET,
                    AttributeModifierEntry.DisplayMode.fromString("reset"));
        }

        @Test
        void fromString_default_aliasesReset() {
            assertEquals(
                    AttributeModifierEntry.DisplayMode.RESET,
                    AttributeModifierEntry.DisplayMode.fromString("default"));
        }

        @Test
        void fromString_override() {
            assertEquals(
                    AttributeModifierEntry.DisplayMode.OVERRIDE,
                    AttributeModifierEntry.DisplayMode.fromString("override"));
        }

        @Test
        void fromString_custom_aliasesOverride() {
            assertEquals(
                    AttributeModifierEntry.DisplayMode.OVERRIDE,
                    AttributeModifierEntry.DisplayMode.fromString("custom"));
        }

        @Test
        void fromString_caseInsensitive() {
            assertEquals(
                    AttributeModifierEntry.DisplayMode.HIDDEN,
                    AttributeModifierEntry.DisplayMode.fromString("HIDDEN"));
            assertEquals(
                    AttributeModifierEntry.DisplayMode.OVERRIDE,
                    AttributeModifierEntry.DisplayMode.fromString("Override"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        void fromString_nullOrEmpty_returnsNull(String input) {
            assertNull(AttributeModifierEntry.DisplayMode.fromString(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"unknown", "invisible", "show", "none", "   "})
        void fromString_unknownValues_returnsNull(String input) {
            assertNull(AttributeModifierEntry.DisplayMode.fromString(input));
        }
    }
}
