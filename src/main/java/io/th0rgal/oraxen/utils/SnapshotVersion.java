package io.th0rgal.oraxen.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * Facade representing a Minecraft weekly snapshot version (e.g. {@code 25w03a}),
 * backed by {@link SemanticServerVersion}.
 */
public class SnapshotVersion implements Comparable<SnapshotVersion> {

    private final SemanticServerVersion semanticVersion;
    private final String snapshotString;
    private @Nullable LocalDate snapshotDate;

    public SnapshotVersion(@NotNull String snapshotString) {
        SemanticServerVersion parsed = SemanticServerVersion.parse(snapshotString.trim());
        if (!parsed.snapshot()) {
            throw new IllegalArgumentException("Snapshot string (" + snapshotString + ") was in an invalid format.");
        }
        this.semanticVersion = parsed;
        this.snapshotString = parsed.toString();
    }

    public SnapshotVersion(int snapshotYear, int snapshotWeek, char snapshotIdentifier) {
        this.semanticVersion = SemanticServerVersion.ofSnapshot(snapshotYear, snapshotWeek, snapshotIdentifier);
        this.snapshotString = this.semanticVersion.toString();
    }

    public @NotNull SemanticServerVersion toSemanticVersion() {
        return semanticVersion;
    }

    public char getSnapshotIdentifier() {
        return semanticVersion.snapshotRevision();
    }

    public int getSnapshotWeek() {
        return semanticVersion.snapshotWeek();
    }

    public int getSnapshotYear() {
        return semanticVersion.snapshotYear();
    }

    public @NotNull String getSnapshotString() {
        return snapshotString;
    }

    public @NotNull LocalDate getSnapshotDate() {
        if (snapshotDate == null) {
            snapshotDate = LocalDate.now()
                    .withYear(2000 + getSnapshotYear())
                    .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, getSnapshotWeek())
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        return snapshotDate;
    }

    public boolean isAfter(@NotNull SnapshotVersion other) {
        return this.compareTo(other) > 0;
    }

    public boolean isBefore(@NotNull SnapshotVersion other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAtOrAfter(@NotNull SnapshotVersion other) {
        return this.compareTo(other) >= 0;
    }

    @Override
    public int compareTo(@NotNull SnapshotVersion other) {
        return this.semanticVersion.compareTo(other.semanticVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semanticVersion);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SnapshotVersion other)) return false;
        return Objects.equals(semanticVersion, other.semanticVersion);
    }

    @Override
    public String toString() {
        return snapshotString;
    }
}
