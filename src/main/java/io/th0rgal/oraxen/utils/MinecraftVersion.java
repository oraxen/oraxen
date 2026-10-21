package io.th0rgal.oraxen.utils;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Facade representing a Minecraft version, backed by {@link SemanticServerVersion}
 * while preserving 100% binary compatibility with existing Oraxen and third-party callers.
 */
public final class MinecraftVersion implements Comparable<MinecraftVersion>, Serializable {

    private static final long serialVersionUID = -8695133558996459770L;

    private static MinecraftVersion currentVersion;

    private final SemanticServerVersion semanticVersion;
    private final int major;
    private final int minor;
    private final int build;
    private final String development;
    private final SnapshotVersion snapshot;
    private volatile Boolean atCurrentOrAbove;

    public MinecraftVersion(String versionOnly) {
        this(versionOnly, true);
    }

    private MinecraftVersion(String versionOnly, boolean parseSnapshot) {
        SemanticServerVersion parsed = SemanticServerVersion.parse(versionOnly);
        if (parsed.snapshot()) {
            if (!parseSnapshot) {
                throw new NumberFormatException("Snapshot not allowed: " + versionOnly);
            }
            try {
                SnapshotVersion snap = new SnapshotVersion(
                        parsed.snapshotYear(),
                        parsed.snapshotWeek(),
                        parsed.snapshotRevision()
                );
                var plugin = OraxenPlugin.get();
                var adapter = plugin != null ? plugin.getPacketAdapter() : null;
                String latestVersion = adapter != null
                        ? adapter.getLatestMCVersion()
                        : ResourcePackFormatUtil.getLatestKnownVersion().getVersion();
                MinecraftVersion latest = new MinecraftVersion(latestVersion, false);
                boolean newer = adapter == null || adapter.isNewer(snap);

                this.major = latest.getMajor();
                this.minor = latest.getMinor() + (newer ? 1 : -1);
                this.build = 0;
                this.development = "snapshot";
                this.snapshot = snap;
                this.semanticVersion = parsed;
            } catch (Exception e) {
                throw new IllegalStateException("Cannot parse " + versionOnly, e);
            }
        } else {
            this.major = parsed.major();
            this.minor = parsed.minor();
            this.build = parsed.patch();
            this.development = parsed.qualifier();
            this.snapshot = null;
            this.semanticVersion = parsed;
        }
    }

    public MinecraftVersion(int major, int minor, int build) {
        this(major, minor, build, null);
    }

    public MinecraftVersion(int major, int minor, int build, @Nullable String development) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.development = development;
        this.snapshot = null;
        this.semanticVersion = SemanticServerVersion.ofRelease(major, minor, build, development);
    }

    public MinecraftVersion(@NotNull SemanticServerVersion semanticVersion) {
        this.semanticVersion = semanticVersion;
        this.major = semanticVersion.major();
        this.minor = semanticVersion.minor();
        this.build = semanticVersion.patch();
        this.development = semanticVersion.snapshot() ? "snapshot" : semanticVersion.qualifier();
        this.snapshot = semanticVersion.snapshot()
                ? new SnapshotVersion(
                        semanticVersion.snapshotYear(),
                        semanticVersion.snapshotWeek(),
                        semanticVersion.snapshotRevision())
                : null;
    }

    public static MinecraftVersion getCurrentVersion() {
        if (currentVersion == null) {
            currentVersion = new MinecraftVersion(Bukkit.getMinecraftVersion());
        }
        return currentVersion;
    }

    private static boolean atOrAbove(MinecraftVersion version) {
        return getCurrentVersion().isAtLeast(version);
    }

    public @NotNull SemanticServerVersion toSemanticVersion() {
        return semanticVersion;
    }

    public int getMajor() {
        return this.major;
    }

    public int getMinor() {
        return this.minor;
    }

    public int getBuild() {
        return this.build;
    }

    public int getPatch() {
        return this.build;
    }

    public String getDevelopmentStage() {
        return this.development;
    }

    @Nullable
    public String getQualifier() {
        return this.development;
    }

    public SnapshotVersion getSnapshot() {
        return this.snapshot;
    }

    public boolean isSnapshot() {
        return this.snapshot != null;
    }

    public boolean isPreRelease() {
        return semanticVersion.isPreRelease();
    }

    public boolean atOrAbove() {
        if (this.atCurrentOrAbove == null) {
            this.atCurrentOrAbove = atOrAbove(this);
        }
        return this.atCurrentOrAbove;
    }

    public String getVersion() {
        if (this.getDevelopmentStage() == null) {
            return String.format("%s.%s.%s", this.getMajor(), this.getMinor(), this.getBuild());
        } else {
            return String.format("%s.%s.%s-%s%s", this.getMajor(), this.getMinor(), this.getBuild(),
                    this.getDevelopmentStage(), this.isSnapshot() ? this.snapshot : "");
        }
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        if (o == null) {
            return 1;
        }

        return ComparisonChain.start()
                .compare(this.getMajor(), o.getMajor())
                .compare(this.getMinor(), o.getMinor())
                .compare(this.getBuild(), o.getBuild())
                .compare(this.getDevelopmentStage(), o.getDevelopmentStage(), Ordering.natural().nullsLast())
                .compare(this.getSnapshot(), o.getSnapshot(), Ordering.natural().nullsFirst())
                .result();
    }

    public boolean isAtLeast(MinecraftVersion other) {
        if (other == null) {
            return false;
        }
        return this.compareTo(other) >= 0;
    }

    public boolean isBelow(MinecraftVersion other) {
        if (other == null) {
            return false;
        }
        return this.compareTo(other) < 0;
    }

    public boolean isAbove(MinecraftVersion other) {
        if (other == null) {
            return true;
        }
        return this.compareTo(other) > 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof MinecraftVersion other) {
            return this.getMajor() == other.getMajor()
                    && this.getMinor() == other.getMinor()
                    && this.getBuild() == other.getBuild()
                    && Objects.equals(this.getDevelopmentStage(), other.getDevelopmentStage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getMajor(), this.getMinor(), this.getBuild());
    }

    @Override
    public String toString() {
        return this.getVersion();
    }
}
