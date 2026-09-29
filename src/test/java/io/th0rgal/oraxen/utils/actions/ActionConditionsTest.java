package io.th0rgal.oraxen.utils.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionConditionsTest {

    @Test
    void prefixesPlayerAndServerReferences() {
        assertEquals("#player.isSneaking()", ActionConditions.normalize("player.isSneaking()"));
        assertEquals("#server.getOnlinePlayers().size() > 10", ActionConditions.normalize("server.getOnlinePlayers().size() > 10"));
        assertEquals("#player.isSneaking()", ActionConditions.normalize("#player.isSneaking()"));
    }

    @Test
    void leavesQuotedStringsUntouched() {
        assertEquals("#player.hasPermission('oraxen.player.use')",
                ActionConditions.normalize("player.hasPermission('oraxen.player.use')"));
        assertEquals("#player.getName() == \"server.gamemode\"",
                ActionConditions.normalize("player.getName() == \"server.gamemode\""));
    }

    @Test
    void negatesSingleComparison() {
        assertEquals("!(#player.gameMode.name() == \"ADVENTURE\")",
                ActionConditions.normalize("!#player.gamemode.name() == \"ADVENTURE\""));
    }

    @Test
    void keepsNegationScopeInCompoundConditions() {
        assertEquals("!#player.isSneaking() && #player.getLevel() > 5",
                ActionConditions.normalize("!#player.isSneaking() && #player.getLevel() > 5"));
        assertEquals("!#player.isFlying() or #player.getLevel() > 5",
                ActionConditions.normalize("!#player.isFlying() or #player.getLevel() > 5"));
    }

    @Test
    void ignoresOperatorsInsideStrings() {
        assertEquals("!(#player.getName() == 'a && b')",
                ActionConditions.normalize("!#player.getName() == 'a && b'"));
    }
}
