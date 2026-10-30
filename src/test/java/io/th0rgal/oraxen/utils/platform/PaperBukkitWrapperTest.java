package io.th0rgal.oraxen.utils.platform;

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackManager;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaperBukkitWrapperTest {
    private final Key trimKey = Key.key("minecraft:file/oraxen_trim_armor");
    private final PaperBukkitWrapper wrapper = new PaperBukkitWrapper();
    private final DatapackManager manager = mock(DatapackManager.class);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getDatapackManager).thenReturn(manager);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void detectsInstalledAndEnabledTrimPackUsingPaperName() {
        Datapack pack = pack("file/oraxen_trim_armor", true);
        when(manager.getPacks()).thenReturn(List.of(pack));

        assertFalse(wrapper.isFirstInstall(trimKey));
        assertTrue(wrapper.isDatapackEnabled(trimKey, null));
    }

    @Test
    void distinguishesDisabledPackFromMissingPack() {
        Datapack disabled = pack("file/oraxen_trim_armor", false);
        when(manager.getPacks()).thenReturn(List.of(disabled));
        assertFalse(wrapper.isFirstInstall(trimKey));
        assertFalse(wrapper.isDatapackEnabled(trimKey, null));

        when(manager.getPacks()).thenReturn(List.of());
        assertTrue(wrapper.isFirstInstall(trimKey));
        assertFalse(wrapper.isDatapackEnabled(trimKey, null));
    }

    @Test
    void enablesAndDisablesOnlyTheGeneratedFilePack() {
        Datapack generated = pack("file/oraxen_trim_armor", false);
        Datapack nested = pack("file/other/oraxen_trim_armor", true);
        Datapack unrelated = pack("oraxen_trim_armor", true);
        when(manager.getPacks()).thenReturn(List.of(nested, unrelated, generated));

        wrapper.setDatapackEnabled("oraxen_trim_armor", true);
        wrapper.setDatapackEnabled("oraxen_trim_armor", false);

        verify(generated).setEnabled(true);
        verify(generated).setEnabled(false);
        verify(nested, never()).setEnabled(anyBoolean());
        verify(unrelated, never()).setEnabled(anyBoolean());
    }

    @Test
    void doesNotDetectAnUnrelatedPackWithTheSameLastPathComponent() {
        Datapack nested = pack("file/other/oraxen_trim_armor", true);
        when(manager.getPacks()).thenReturn(List.of(nested));
        assertTrue(wrapper.isFirstInstall(trimKey));
        assertFalse(wrapper.isDatapackEnabled(trimKey, null));
    }

    private Datapack pack(String name, boolean enabled) {
        Datapack pack = mock(Datapack.class);
        when(pack.getName()).thenReturn(name);
        when(pack.isEnabled()).thenReturn(enabled);
        return pack;
    }
}
