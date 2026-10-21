package io.th0rgal.oraxen.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable representation of a Minecraft server or client version.
 * Supports:
 * <ul>
 *     <li>Legacy semantic versions ({@code 1.20.1}, {@code 1.21.11}, {@code 1.21.4-pre1})</li>
 *     <li>Mojang 2026+ drop versions ({@code 26.1}, {@code 26.1.2}, {@code 26.3-rc1}) and {@code 1.26.x} aliases</li>
 *     <li>Weekly snapshots ({@code 24w14a}, {@code 25w03a})</li>
 * </ul>
 */
public record SemanticServerVersion(
        int major,
        int minor,
        int patch,
        @Nullable String qualifier,
        boolean snapshot,
        int snapshotYear,
        int snapshotWeek,
        char snapshotRevision
) implements Comparable<SemanticServerVersion> {

    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("^(\\d{2})w(\\d{2})([a-z])$");
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[-_]([0-9A-Za-z.-]+))?$"
    );

    public SemanticServerVersion {
        qualifier = (qualifier == null || qualifier.isBlank()) ? null : qualifier.trim();
    }

    public static @NotNull SemanticServerVersion ofRelease(int major, int minor, int patch) {
        return new SemanticServerVersion(major, minor, patch, null, false, 0, 0, '\0');
    }

    public static @NotNull SemanticServerVersion ofRelease(int major, int minor, int patch, @Nullable String qualifier) {
        return new SemanticServerVersion(major, minor, patch, qualifier, false, 0, 0, '\0');
    }

    public static @NotNull SemanticServerVersion ofSnapshot(int year, int week, char revision) {
        int syntheticMajor = year >= 26 ? year : 1;
        int syntheticMinor = year >= 26 ? 0 : (year >= 24 ? 21 : 20);
        return new SemanticServerVersion(syntheticMajor, syntheticMinor, 0, null, true, year, week, revision);
    }

    public static @NotNull SemanticServerVersion parse(@Nullable String rawVersion) {
        if (rawVersion == null || rawVersion.isBlank()) {
            return ofRelease(1, 20, 0);
        }

        String cleaned = rawVersion.trim();
        int spaceIdx = cleaned.indexOf(' ');
        if (spaceIdx > 0) {
            cleaned = cleaned.substring(0, spaceIdx);
        }

        Matcher snapshotMatcher = SNAPSHOT_PATTERN.matcher(cleaned.toLowerCase(Locale.ROOT));
        if (snapshotMatcher.matches()) {
            int year = Integer.parseInt(snapshotMatcher.group(1));
            int week = Integer.parseInt(snapshotMatcher.group(2));
            char rev = snapshotMatcher.group(3).charAt(0);
            return ofSnapshot(year, week, rev);
        }

        // Strip trailing Paper/Bukkit build metadata like "-R0.1-SNAPSHOT"
        String normalized = cleaned.replace("_R", ".");
        if (normalized.endsWith("-SNAPSHOT")) {
            normalized = normalized.substring(0, normalized.length() - "-SNAPSHOT".length());
        }
        int rIndex = normalized.indexOf("-R");
        if (rIndex > 0) {
            normalized = normalized.substring(0, rIndex);
        }

        Matcher semverMatcher = SEMVER_PATTERN.matcher(normalized);
        if (semverMatcher.matches()) {
            int major = Integer.parseInt(semverMatcher.group(1));
            int minor = Integer.parseInt(semverMatcher.group(2));
            int patch = semverMatcher.group(3) != null ? Integer.parseInt(semverMatcher.group(3)) : 0;
            String qualifier = semverMatcher.group(4);
            return ofRelease(major, minor, patch, qualifier);
        }

        // Fallback permissive parsing for non-standard strings
        String[] qualifierSplit = normalized.split("-", 2);
        String qualifier = qualifierSplit.length > 1 ? qualifierSplit[1] : null;
        String[] numbers = qualifierSplit[0].split("\\.");
        int major = parseSafeInt(numbers, 0, 1);
        int minor = parseSafeInt(numbers, 1, 0);
        int patch = parseSafeInt(numbers, 2, 0);
        return ofRelease(major, minor, patch, qualifier);
    }

    private static int parseSafeInt(String[] parts, int index, int fallback) {
        if (index >= parts.length) return fallback;
        String digits = parts[index].replaceAll("\\D", "");
        if (digits.isEmpty()) return fallback;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean isPreRelease() {
        return qualifier != null && !qualifier.isEmpty();
    }

    /**
     * Returns true if this version uses the Mojang 2026+ year-based numbering
     * (e.g. {@code 26.1}, {@code 26.2}, or legacy-prefixed {@code 1.26.2}).
     */
    public boolean isModernNamespace() {
        if (snapshot) {
            return snapshotYear >= 26;
        }
        return major >= 26 || (major == 1 && minor >= 26);
    }

    /**
     * Normalizes {@code 1.26.x} aliases into canonical {@code 26.x.0} coordinates
     * so comparisons between {@code 1.26.2} and {@code 26.2} are consistent.
     */
    public @NotNull SemanticServerVersion toCanonicalNamespace() {
        if (!snapshot && major == 1 && minor >= 26) {
            return new SemanticServerVersion(minor, patch, 0, qualifier, false, 0, 0, '\0');
        }
        return this;
    }

    public boolean isAtLeast(@NotNull SemanticServerVersion other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isAtLeast(@NotNull String otherVersion) {
        return isAtLeast(parse(otherVersion));
    }

    /**
     * Checks if this version is at least the given release target {@code (targetMajor, targetMinor, targetPatch)},
     * including pre-releases and release candidates of that target (e.g. {@code 26.3-pre1} matches {@code (26, 3, 0)}).
     */
    public boolean targetsAtLeast(int targetMajor, int targetMinor, int targetPatch) {
        SemanticServerVersion self = this.toCanonicalNamespace();
        SemanticServerVersion target = ofRelease(targetMajor, targetMinor, targetPatch).toCanonicalNamespace();
        int majorDiff = Integer.compare(self.major, target.major);
        if (majorDiff != 0) return majorDiff > 0;
        int minorDiff = Integer.compare(self.minor, target.minor);
        if (minorDiff != 0) return minorDiff > 0;
        return self.patch >= target.patch;
    }

    public boolean isBetween(@NotNull SemanticServerVersion minInclusive, @NotNull SemanticServerVersion maxInclusive) {
        return this.compareTo(minInclusive) >= 0 && this.compareTo(maxInclusive) <= 0;
    }

    @Override
    public int compareTo(@NotNull SemanticServerVersion other) {
        if (this.snapshot && other.snapshot) {
            int yearDiff = Integer.compare(this.snapshotYear, other.snapshotYear);
            if (yearDiff != 0) return yearDiff;
            int weekDiff = Integer.compare(this.snapshotWeek, other.snapshotWeek);
            if (weekDiff != 0) return weekDiff;
            return Character.compare(this.snapshotRevision, other.snapshotRevision);
        }

        int majorDiff = Integer.compare(this.major, other.major);
        if (majorDiff != 0) return majorDiff;
        int minorDiff = Integer.compare(this.minor, other.minor);
        if (minorDiff != 0) return minorDiff;
        int patchDiff = Integer.compare(this.patch, other.patch);
        if (patchDiff != 0) return patchDiff;

        // A full release (qualifier == null) orders after any pre-release of the same (major, minor, patch)
        if (this.qualifier == null && other.qualifier == null) return 0;
        if (this.qualifier == null) return 1;
        if (other.qualifier == null) return -1;
        return this.qualifier.compareToIgnoreCase(other.qualifier);
    }

    @Override
    public @NotNull String toString() {
        if (snapshot) {
            return String.format(Locale.ROOT, "%02dw%02d%c", snapshotYear, snapshotWeek, snapshotRevision);
        }
        String base = major + "." + minor + (patch > 0 ? "." + patch : "");
        return qualifier != null ? base + "-" + qualifier : base;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemanticServerVersion that)) return false;
        return major == that.major
                && minor == that.minor
                && patch == that.patch
                && snapshot == that.snapshot
                && snapshotYear == that.snapshotYear
                && snapshotWeek == that.snapshotWeek
                && snapshotRevision == that.snapshotRevision
                && Objects.equals(qualifier, that.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, qualifier, snapshot, snapshotYear, snapshotWeek, snapshotRevision);
    }
}
