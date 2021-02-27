package org.playuniverse.snowypine.utils.reflection.version;

import com.syntaxphoenix.syntaxapi.version.DefaultVersion;
import com.syntaxphoenix.syntaxapi.version.VersionAnalyzer;

public class MinecraftVersion extends DefaultVersion {

    public static final MinecraftVersion NONE = new MinecraftVersion(false);
    public static final MinecraftAnalyzer ANALYZER = new MinecraftAnalyzer();

    private final boolean valid;

    private MinecraftVersion(boolean valid) {
        super();
        this.valid = valid;
    }

    public MinecraftVersion() {
        super();
        this.valid = true;
    }

    public MinecraftVersion(int major, int minor, int patch) {
        super(major, minor, patch);
        this.valid = true;
    }

    public final boolean isValid() {
        return valid;
    }

    @Override
    protected MinecraftVersion setMajor(int major) {
        super.setMajor(major);
        return this;
    }

    @Override
    protected MinecraftVersion setMinor(int minor) {
        super.setMinor(minor);
        return this;
    }

    @Override
    protected MinecraftVersion setPatch(int patch) {
        super.setPatch(patch);
        return this;
    }

    @Override
    public MinecraftVersion clone() {
        return (MinecraftVersion) super.clone();
    }

    @Override
    public MinecraftVersion update(int major, int minor, int patch) {
        return (MinecraftVersion) super.update(major, minor, patch);
    }

    @Override
    protected MinecraftVersion init(int major, int minor, int patch) {
        return new MinecraftVersion(major, minor, patch);
    }

    @Override
    public MinecraftAnalyzer getAnalyzer() {
        return ANALYZER;
    }

    public static MinecraftVersion fromString(String versionString) {
        return ANALYZER.analyze(versionString);
    }

    public static MinecraftVersion[] fromStringArray(String... versionStrings) {
        MinecraftVersion[] versions = new MinecraftVersion[versionStrings.length];
        int index = 0;
        for (String versionString : versionStrings) {
            versions[index] = ANALYZER.analyze(versionString);
            index++;
        }
        return versions;
    }

    public static class MinecraftAnalyzer implements VersionAnalyzer {
        @Override
        public MinecraftVersion analyze(String formatted) {
            String[] parts;
            boolean bukkit = false;
            if (formatted.contains(".")) {
                parts = formatted.split("\\.");
            } else if (formatted.contains("_")) {
                bukkit = true;
                parts = formatted.split("_");
            } else {
                return MinecraftVersion.NONE;
            }
            if (bukkit && parts.length == 3) {
                MinecraftVersion version = new MinecraftVersion();
                version.setMajor(Integer.parseInt(parts[0]));
                version.setMinor(Integer.parseInt(parts[1]));
                return version;
            } else if (!bukkit && (parts.length == 2 || parts.length == 3)) {
                MinecraftVersion version = new MinecraftVersion();
                version.setMajor(Integer.parseInt(parts[0]));
                version.setMinor(Integer.parseInt(parts[1]));
                version.setPatch(parts.length == 3 ? Integer.parseInt(parts[2]) : 0);
                return version;
            } else {
                return MinecraftVersion.NONE;
            }
        }
    }

}
