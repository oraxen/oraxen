package io.th0rgal.oraxen;

import io.th0rgal.oraxen.fonts.FontManager;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OraxenPluginTest {

    @Test
    void setFontManagerRegistersNewManagerWithoutPreviousManager() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        FontManager manager = mock(FontManager.class);

        plugin.setFontManager(manager);

        verify(manager).registerEvents();
        verify(manager, never()).unregisterEvents();
        assertSame(manager, plugin.getFontManager());
    }

    @Test
    void setFontManagerUnregistersPreviousManagerWhenReplacing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        FontManager previous = mock(FontManager.class);
        FontManager replacement = mock(FontManager.class);

        plugin.setFontManager(previous);
        clearInvocations(previous);

        plugin.setFontManager(replacement);

        verify(previous).unregisterEvents();
        verify(replacement).registerEvents();
        assertSame(replacement, plugin.getFontManager());
    }

    @Test
    void setHudManagerRegistersNewManagerWithoutPreviousManager() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        HudManager manager = mock(HudManager.class);

        plugin.setHudManager(manager);

        verify(manager).registerEvents();
        verify(manager, never()).unregisterEvents();
        assertSame(manager, plugin.getHudManager());
    }

    @Test
    void setHudManagerUnregistersPreviousManagerWhenReplacing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        HudManager previous = mock(HudManager.class);
        HudManager replacement = mock(HudManager.class);

        plugin.setHudManager(previous);
        clearInvocations(previous);

        plugin.setHudManager(replacement);

        verify(previous).unregisterTask();
        verify(previous).unregisterEvents();
        verify(replacement).registerEvents();
        assertSame(replacement, plugin.getHudManager());
    }

    @Test
    void setHudManagerUnregistersTaskAndEventsWhenClearing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        HudManager previous = mock(HudManager.class);

        plugin.setHudManager(previous);
        clearInvocations(previous);
        plugin.setHudManager(null);

        verify(previous).unregisterTask();
        verify(previous).unregisterEvents();
        assertNull(plugin.getHudManager());
    }

    @Test
    void setUploadManagerUnregistersPreviousManagerWhenReplacing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        UploadManager previous = mock(UploadManager.class);
        UploadManager replacement = mock(UploadManager.class);

        plugin.setUploadManager(previous);
        doAnswer(invocation -> {
            assertSame(previous, plugin.getUploadManager());
            return null;
        }).when(previous).unregister();

        plugin.setUploadManager(replacement);

        verify(previous).unregister();
        verify(replacement, never()).unregister();
        assertSame(replacement, plugin.getUploadManager());
    }

    @Test
    void setUploadManagerDoesNotUnregisterSameManagerInstance() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        UploadManager manager = mock(UploadManager.class);

        plugin.setUploadManager(manager);
        clearInvocations(manager);
        plugin.setUploadManager(manager);

        verify(manager, never()).unregister();
        assertSame(manager, plugin.getUploadManager());
    }

    @Test
    void setUploadManagerUnregistersPreviousManagerWhenClearing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        UploadManager previous = mock(UploadManager.class);

        plugin.setUploadManager(previous);
        clearInvocations(previous);
        plugin.setUploadManager(null);

        verify(previous).unregister();
        assertNull(plugin.getUploadManager());
    }

    @Test
    void setMultiVersionUploadManagerUnregistersPreviousManagerWhenReplacing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        MultiVersionUploadManager previous = mock(MultiVersionUploadManager.class);
        MultiVersionUploadManager replacement = mock(MultiVersionUploadManager.class);

        plugin.setMultiVersionUploadManager(previous);
        clearInvocations(previous);
        plugin.setMultiVersionUploadManager(replacement);

        verify(previous).unregister();
        verify(replacement, never()).unregister();
        assertSame(replacement, plugin.getMultiVersionUploadManager());
    }

    @Test
    void setMultiVersionUploadManagerDoesNotUnregisterSameManagerInstance() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        MultiVersionUploadManager manager = mock(MultiVersionUploadManager.class);

        plugin.setMultiVersionUploadManager(manager);
        clearInvocations(manager);
        plugin.setMultiVersionUploadManager(manager);

        verify(manager, never()).unregister();
        assertSame(manager, plugin.getMultiVersionUploadManager());
    }

    @Test
    void setMultiVersionUploadManagerUnregistersPreviousManagerWhenClearing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        MultiVersionUploadManager previous = mock(MultiVersionUploadManager.class);

        plugin.setMultiVersionUploadManager(previous);
        clearInvocations(previous);
        plugin.setMultiVersionUploadManager(null);

        verify(previous).unregister();
        assertNull(plugin.getMultiVersionUploadManager());
    }
}
