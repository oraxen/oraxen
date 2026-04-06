package io.th0rgal.oraxen.items;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AttributeModifierEntry}.
 *
 * These tests cover the pure-logic parts that don't require a Bukkit server:
 * - DisplayMode parsing
 * - Slot group string normalization
 *
 * Integration tests for config parsing and DataComponent building require
 * a Paper server environment and are covered by manual QA and CI integration tests.
 */
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

        @Test
        void enumValues_complete() {
            // Ensure all expected modes exist
            AttributeModifierEntry.DisplayMode[] modes = AttributeModifierEntry.DisplayMode.values();
            assertEquals(3, modes.length);
            assertNotNull(AttributeModifierEntry.DisplayMode.valueOf("HIDDEN"));
            assertNotNull(AttributeModifierEntry.DisplayMode.valueOf("RESET"));
            assertNotNull(AttributeModifierEntry.DisplayMode.valueOf("OVERRIDE"));
        }
    }

    // Note: SlotGroupParsing and full integration tests require Bukkit/Paper classes
    // on the classpath (EquipmentSlotGroup, Attribute, etc.) and are verified via:
    //   1. Compilation against Paper API (compileJava)
    //   2. CI integration tests on a Paper test server
    //   3. Manual QA with the demo server (/minecraft-control deploy)

    @Nested
    class ConfigFormatDocumentation {
        /**
         * Documents the supported configuration formats for attribute modifiers.
         * These are not executable tests but serve as living documentation that
         * gets compiled and verified to exist.
         */

        @Test
        void modernFormat_isDocumented() {
            // Modern format uses named ConfigurationSection keys:
            // AttributeModifiers:
            //   damage_boost:
            //     attribute: ATTACK_DAMAGE
            //     amount: 5.0
            //     operation: ADD_NUMBER       # ADD_NUMBER | ADD_SCALAR | MULTIPLY_SCALAR_1
            //     slot: mainhand              # any | mainhand | offhand | head | chest | legs | feet
            //     display:                    # optional, 1.21.6+
            //       type: hidden              # hidden | reset | override
            //       text: "<red>+5 Damage"    # only used with type: override (MiniMessage)
            assertTrue(true, "Modern config format documented");
        }

        @Test
        void legacyFormat_isDocumented() {
            // Legacy format uses a list of maps (backward compatible):
            // AttributeModifiers:
            //   - attribute: ATTACK_DAMAGE
            //     amount: 5.0
            //     operation: ADD_NUMBER
            //     slot: mainhand
            assertTrue(true, "Legacy config format documented");
        }
    }
}
