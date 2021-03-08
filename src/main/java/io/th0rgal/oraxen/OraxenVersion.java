package io.th0rgal.oraxen;

import org.bukkit.Bukkit;

import com.syntaxphoenix.syntaxapi.utils.java.tools.Container;
import com.syntaxphoenix.syntaxapi.version.Version;

import io.th0rgal.oraxen.utils.plugin.PluginSettings;
import io.th0rgal.oraxen.utils.version.MinecraftVersion;
import io.th0rgal.oraxen.utils.version.ServerVersion;

public final class OraxenVersion {
    
    public static final PluginSettings PLUGIN = new PluginSettings();
    public static final OraxenVersion INSTANCE = new OraxenVersion();

    public static MinecraftVersion oraxen() {
        return INSTANCE.getOraxen();
    }

    public static MinecraftVersion minecraft() {
        return INSTANCE.getMinecraft();
    }

    public static ServerVersion server() {
        return INSTANCE.getServer();
    }

    public static String oraxenAsString() {
        return INSTANCE.getOraxenAsString();
    }

    public static String minecraftAsString() {
        return INSTANCE.getMinecraftAsString();
    }

    public static String serverAsString() {
        return INSTANCE.getServerAsString();
    }

    private OraxenVersion() {
    }

    private final Container<ServerVersion> server = Container.of();
    private final Container<MinecraftVersion> minecraft = Container.of();
    private final Container<MinecraftVersion> oraxen = Container.of();

    private final Container<String> serverString = Container.of();
    private final Container<String> minecraftString = Container.of();
    private final Container<String> oraxenString = Container.of();

    public String getOraxenAsString() {
        return oraxenString.orElseGet(() -> {
            String versionString = OraxenPlugin.get().getDescription().getVersion();
            oraxenString.replace(versionString);
            return versionString;
        });
    }

    public MinecraftVersion getOraxen() {
        return oraxen.orElseGet(() -> {
            MinecraftVersion version = MinecraftVersion.fromString(getOraxenAsString());
            oraxen.replace(version);
            return version;
        });
    }

    public String getMinecraftAsString() {
        return minecraftString.orElseGet(() -> {
            String versionString = Bukkit.getVersion().split(" ")[2].replace(")", "");
            oraxenString.replace(versionString);
            return versionString;
        });
    }

    public MinecraftVersion getMinecraft() {
        return minecraft.orElseGet(() -> {
            MinecraftVersion version = MinecraftVersion.fromString(getMinecraftAsString());
            minecraft.replace(version);
            return version;
        });
    }

    public String getServerAsString() {
        return serverString.orElseGet(() -> {
            String versionString = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
            oraxenString.replace(versionString);
            return versionString;
        });
    }

    public ServerVersion getServer() {
        return server.orElseGet(() -> {
            ServerVersion version = ServerVersion.ANALYZER.analyze(getServerAsString());
            server.replace(version);
            return version;
        });
    }

    /*
     * Utilities
     */

    private static int compare(int original, int comparision) {
        return original >= comparision ? (original == comparision ? 0 : 1) : -1;
    }

    private static int merge(int pass, int original, int comparision) {
        return pass != 0 ? pass : compare(original, comparision);
    }

    public static int major(Version version, int major) {
        return compare(version.getMajor(), major);
    }

    public static int minor(Version version, int major, int minor) {
        return merge(major(version, major), version.getMinor(), minor);
    }

    public static int patch(Version version, int major, int minor, int patch) {
        return merge(minor(version, major, minor), version.getPatch(), patch);
    }

    public static int refaction(ServerVersion version, int major, int minor, int patch, int refaction) {
        return merge(patch(version, major, minor, patch), version.getRefaction(), refaction);
    }

}
