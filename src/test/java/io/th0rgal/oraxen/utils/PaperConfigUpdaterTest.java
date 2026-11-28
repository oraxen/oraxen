package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaperConfigUpdaterTest {

    private static final String SETTING = "disable-noteblock-updates";

    @Test
    void enablesSettingWrittenWithCapitalOrTrailingComment() {
        List<String> updated = new ArrayList<>();
        assertEquals("    disable-noteblock-updates: true",
                PaperConfigUpdater.tryUpdateSetting("    disable-noteblock-updates: False", SETTING, updated));
        assertEquals("    disable-noteblock-updates: true # set by admin",
                PaperConfigUpdater.tryUpdateSetting("    disable-noteblock-updates: false # set by admin", SETTING, updated));
        assertEquals(List.of(SETTING, SETTING), updated);
    }

    @Test
    void leavesEnabledOrUnrelatedSettingsAlone() {
        List<String> updated = new ArrayList<>();
        assertNull(PaperConfigUpdater.tryUpdateSetting("    disable-noteblock-updates: true", SETTING, updated));
        assertNull(PaperConfigUpdater.tryUpdateSetting("    disable-tripwire-updates: false", SETTING, updated));
        assertEquals(List.of(), updated);
    }
}
