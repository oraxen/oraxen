/*
 *  ProtocolLib - Bukkit server library that allows access to the Minecraft protocol.
 *  Copyright (C) 2012 Kristian S. Stangeland
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU General Public License as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program;
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307 USA
 */

package io.th0rgal.oraxen.utils;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;

import java.io.Serializable;
import java.util.Objects;

/**
 * Determine the current Minecraft version.
 *
 * @author Kristian
 */
public final class MinecraftVersion implements Comparable<MinecraftVersion>, Serializable {

    // used when serializing
    private static final long serialVersionUID = -8695133558996459770L;

    /**
     * The current version of minecraft, lazy initialized by MinecraftVersion.getCurrentVersion()
     */
    private static MinecraftVersion currentVersion;

    private final int major;
    private final int minor;
    private final int build;
    // The development stage
    private final String development;

    // Snapshot?
    private final SnapshotVersion snapshot;
    private volatile Boolean atCurrentOrAbove;

    /**
     * Construct a version object from the format major.minor.build, or the snapshot format.
     *
     * @param versionOnly - the version in text form.
     */
    public MinecraftVersion(String versionOnly) {
        this(versionOnly, true);
    }

    /**
     * Construct a version format from the standard release version or the snapshot verison.
     *
     * @param versionOnly   - the version.
     * @param parseSnapshot - TRUE to parse the snapshot, FALSE otherwise.
     */
    private MinecraftVersion(String versionOnly, boolean parseSnapshot) {
        String[] section = versionOnly.split("-");
        SnapshotVersion snapshot = null;
        int[] numbers = new int[3];

        try {
            numbers = this.parseVersion(section[0]);
        } catch (NumberFormatException cause) {
            // Skip snapshot parsing
            if (!parseSnapshot) {
                throw cause;
            }

            try {
                // Determine if the snapshot is newer than the current release version
                snapshot = new SnapshotVersion(section[0]);
                var adapter = OraxenPlugin.get().getPacketAdapter();
                MinecraftVersion latest = new MinecraftVersion(adapter.getLatestMCVersion(), false);
                boolean newer = adapter.isNewer(snapshot);

                numbers[0] = latest.getMajor();
                numbers[1] = latest.getMinor() + (newer ? 1 : -1);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot parse " + section[0], e);
            }
        }

        this.major = numbers[0];
        this.minor = numbers[1];
        this.build = numbers[2];
        this.development = section.length > 1 ? section[1] : (snapshot != null ? "snapshot" : null);
        this.snapshot = snapshot;
    }

    /**
     * Construct a version object directly.
     *
     * @param major - major version number.
     * @param minor - minor version number.
     * @param build - build version number.
     */
    public MinecraftVersion(int major, int minor, int build) {
        this(major, minor, build, null);
    }

    /**
     * Construct a version object directly.
     *
     * @param major       - major version number.
     * @param minor       - minor version number.
     * @param build       - build version number.
     * @param development - development stage.
     */
    public MinecraftVersion(int major, int minor, int build, String development) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.development = development;
        this.snapshot = null;
    }

    /**
     * Retrieve the Minecraft version the server is running, as reported by Paper's
     * {@link Bukkit#getMinecraftVersion()} (e.g. {@code "1.21.4"}).
     *
     * @return The current Minecraft version.
     */
    public static MinecraftVersion getCurrentVersion() {
        if (currentVersion == null) {
            currentVersion = new MinecraftVersion(Bukkit.getMinecraftVersion());
        }

        return currentVersion;
    }

    private static boolean atOrAbove(MinecraftVersion version) {
        return getCurrentVersion().isAtLeast(version);
    }

    private int[] parseVersion(String version) {
        String[] elements = version.split("\\.");
        int[] numbers = new int[3];

        // Make sure it's even a valid version
        if (elements.length < 1) {
            throw new IllegalStateException("Corrupt MC version: " + version);
        }

        // The String 1 or 1.2 is interpreted as 1.0.0 and 1.2.0 respectively.
        for (int i = 0; i < Math.min(numbers.length, elements.length); i++) {
            numbers[i] = Integer.parseInt(elements[i].trim());
        }
        return numbers;
    }

    /**
     * Major version number
     *
     * @return Current major version number.
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * Minor version number
     *
     * @return Current minor version number.
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     * Build version number
     *
     * @return Current build version number.
     */
    public int getBuild() {
        return this.build;
    }

    /**
     * Retrieve the development stage.
     *
     * @return Development stage, or NULL if this is a release.
     */
    public String getDevelopmentStage() {
        return this.development;
    }

    /**
     * Retrieve the snapshot version, or NULL if this is a release.
     *
     * @return The snapshot version.
     */
    public SnapshotVersion getSnapshot() {
        return this.snapshot;
    }

    /**
     * Determine if this version is a snapshot.
     *
     * @return The snapshot version.
     */
    public boolean isSnapshot() {
        return this.snapshot != null;
    }

    /**
     * Checks if this version is at or above the current version the server is running.
     *
     * @return true if this version is equal or newer than the server version, false otherwise.
     */
    public boolean atOrAbove() {
        if (this.atCurrentOrAbove == null) {
            this.atCurrentOrAbove = atOrAbove(this);
        }

        return this.atCurrentOrAbove;
    }

    /**
     * Retrieve the version String (major.minor.build) only.
     *
     * @return A normal version string.
     */
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        if (obj instanceof MinecraftVersion) {
            MinecraftVersion other = (MinecraftVersion) obj;

            return this.getMajor() == other.getMajor() &&
                    this.getMinor() == other.getMinor() &&
                    this.getBuild() == other.getBuild() &&
                    Objects.equals(this.getDevelopmentStage(), other.getDevelopmentStage());
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
