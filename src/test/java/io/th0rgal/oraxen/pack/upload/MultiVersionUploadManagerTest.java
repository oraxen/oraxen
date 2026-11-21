package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackPreUploadEvent;
import io.th0rgal.oraxen.api.events.OraxenPackUploadEvent;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.dispatch.PlayerVersionDetector;
import io.th0rgal.oraxen.pack.generation.PackVersion;
import io.th0rgal.oraxen.pack.generation.PackVersionManager;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiVersionUploadManagerTest {

    @TempDir
    Path packFolder;

    private final YamlConfiguration settings = new YamlConfiguration();
    private final List<Event> calledEvents = new ArrayList<>();
    private final List<MockedStatic<?>> statics = new ArrayList<>();
    private final HostingProvider provider = mock(HostingProvider.class);
    private Predicate<Integer> cancelPreUpload = index -> false;
    private int preUploadCalls;

    @BeforeEach
    void setUp() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        settings.set(Settings.UPLOAD.getPath(), true);
        settings.set(Settings.UPLOAD_TYPE.getPath(), "polymath");
        settings.set(Settings.RECEIVE_ENABLED.getPath(), false);
        settings.set(Settings.SEND_PACK_MANDATORY.getPath(), false);
        settings.set(Settings.DEBUG.getPath(), false);
        Settings.invalidateCache();

        MockedStatic<OraxenPlugin> plugins = register(mockStatic(OraxenPlugin.class));
        plugins.when(OraxenPlugin::get).thenReturn(plugin);

        PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            calledEvents.add(event);
            if (event instanceof OraxenPackPreUploadEvent preUpload) {
                preUpload.setCancelled(cancelPreUpload.test(preUploadCalls++));
            }
            return null;
        }).when(pluginManager).callEvent(any(Event.class));
        MockedStatic<Bukkit> bukkit = register(mockStatic(Bukkit.class));
        bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

        MockedStatic<SchedulerUtil> scheduler = register(mockStatic(SchedulerUtil.class));
        scheduler.when(() -> SchedulerUtil.runTaskAsync(any(Runnable.class))).thenAnswer(runInline());
        scheduler.when(() -> SchedulerUtil.runTask(any(Runnable.class))).thenAnswer(runInline());

        MockedStatic<HostingProviderFactory> providers = register(mockStatic(HostingProviderFactory.class));
        providers.when(() -> HostingProviderFactory.createHostingProvider(anyBoolean())).thenReturn(provider);
        when(provider.uploadPack(any(File.class), anyString())).thenReturn(true);
        when(provider.getPackURL()).thenReturn("https://packs.example/pack.zip");
        when(provider.getSHA1()).thenReturn(new byte[]{1, 2, 3});
        when(provider.getOriginalSHA1()).thenReturn("010203");
        when(provider.getPackUUID()).thenReturn(UUID.randomUUID());

        register(mockStatic(Logs.class));
        register(mockStatic(PlayerVersionDetector.class));
        register(mockStatic(PackSender.class));
    }

    @AfterEach
    void tearDown() {
        statics.forEach(MockedStatic::close);
        Settings.invalidateCache();
    }

    @Test
    void cancelledPreUploadSkipsUploadAndDistribution() {
        cancelPreUpload = index -> true;
        PackVersion legacy = packVersion("1.20.4", 22);
        PackVersion modern = packVersion("1.21.4", 46);
        MultiVersionUploadManager manager = new MultiVersionUploadManager(OraxenPlugin.get());

        manager.uploadAndSendToPlayers(versionManager(legacy, modern), true, true);

        assertEquals(2, preUploadCalls);
        verify(provider, never()).uploadPack(any(File.class), anyString());
        assertNull(legacy.getPackURL());
        assertNull(modern.getPackURL());
        assertEquals(0, uploadEvents());
        assertNull(manager.getPackSender());
    }

    @Test
    void cancelledVersionIsSkippedWhileOthersUpload() {
        cancelPreUpload = index -> index == 0;
        PackVersion legacy = packVersion("1.20.4", 22);
        PackVersion modern = packVersion("1.21.4", 46);
        MultiVersionUploadManager manager = new MultiVersionUploadManager(OraxenPlugin.get());

        manager.uploadAndSendToPlayers(versionManager(legacy, modern), true, true);

        verify(provider, never()).uploadPack(eq(legacy.getPackFile()), anyString());
        verify(provider).uploadPack(modern.getPackFile(), "1.21.4");
        assertNull(legacy.getPackURL());
        assertNull(legacy.getPackSHA1());
        assertNull(legacy.getPackUUID());
        assertEquals("https://packs.example/pack.zip", modern.getPackURL());
        assertEquals(1, uploadEvents());
        assertNotNull(manager.getPackSender());
    }

    @Test
    void uncancelledPreUploadUploadsEveryVersion() {
        PackVersion legacy = packVersion("1.20.4", 22);
        PackVersion modern = packVersion("1.21.4", 46);
        MultiVersionUploadManager manager = new MultiVersionUploadManager(OraxenPlugin.get());

        manager.uploadAndSendToPlayers(versionManager(legacy, modern), true, true);

        verify(provider).uploadPack(legacy.getPackFile(), "1.20.4");
        verify(provider).uploadPack(modern.getPackFile(), "1.21.4");
        assertEquals("https://packs.example/pack.zip", legacy.getPackURL());
        assertEquals("https://packs.example/pack.zip", modern.getPackURL());
        assertEquals(2, uploadEvents());
        assertNotNull(manager.getPackSender());
    }

    private <T> MockedStatic<T> register(MockedStatic<T> mocked) {
        statics.add(mocked);
        return mocked;
    }

    private static org.mockito.stubbing.Answer<Object> runInline() {
        return invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        };
    }

    private PackVersion packVersion(String minecraftVersion, int format) {
        File packFile = packFolder.resolve("pack_" + minecraftVersion.replace(".", "_") + ".zip").toFile();
        return new PackVersion(minecraftVersion, format, format, format, packFile);
    }

    private static PackVersionManager versionManager(PackVersion... versions) {
        PackVersionManager versionManager = mock(PackVersionManager.class);
        when(versionManager.getAllVersions()).thenReturn(List.of(versions));
        return versionManager;
    }

    private long uploadEvents() {
        return calledEvents.stream().filter(OraxenPackUploadEvent.class::isInstance).count();
    }
}
