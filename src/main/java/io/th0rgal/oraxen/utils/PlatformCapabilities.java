package io.th0rgal.oraxen.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Centralizes server & client version capability detection so feature checks
 * across Oraxen do not rely on scattered raw version strings.
 */
public final class PlatformCapabilities {

    private static final SemanticServerVersion V1_20_4 = SemanticServerVersion.ofRelease(1, 20, 4);
    private static final SemanticServerVersion V1_20_5 = SemanticServerVersion.ofRelease(1, 20, 5);
    private static final SemanticServerVersion V1_21_0 = SemanticServerVersion.ofRelease(1, 21, 0);
    private static final SemanticServerVersion V1_21_2 = SemanticServerVersion.ofRelease(1, 21, 2);
    private static final SemanticServerVersion V1_21_4 = SemanticServerVersion.ofRelease(1, 21, 4);
    private static final SemanticServerVersion V1_21_6 = SemanticServerVersion.ofRelease(1, 21, 6);
    private static final SemanticServerVersion V1_21_7 = SemanticServerVersion.ofRelease(1, 21, 7);
    private static final SemanticServerVersion V1_21_9 = SemanticServerVersion.ofRelease(1, 21, 9);
    private static final SemanticServerVersion V1_21_11 = SemanticServerVersion.ofRelease(1, 21, 11);

    private PlatformCapabilities() {
    }

    public static @NotNull SemanticServerVersion currentVersion() {
        return MinecraftVersion.getCurrentVersion().toSemanticVersion();
    }

    public static boolean isModernVersionNamespace(@NotNull SemanticServerVersion version) {
        return version.isModernNamespace();
    }

    public static boolean isModernVersionNamespace(@NotNull MinecraftVersion version) {
        return isModernVersionNamespace(version.toSemanticVersion());
    }

    public static boolean supportsShapedBlocks(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_20_4);
    }

    public static boolean supportsDataComponents(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_20_5);
    }

    public static boolean supportsCustomJukeboxSongs(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_0);
    }

    public static boolean supportsNmsHandler(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_2);
    }

    public static boolean supportsNmsHandler(@NotNull MinecraftVersion version) {
        return supportsNmsHandler(version.toSemanticVersion());
    }

    public static boolean supportsItemModelComponent(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_2);
    }

    public static boolean supportsEquippableComponent(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_2);
    }

    public static boolean supportsModernItemDefinitions(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_4);
    }

    public static boolean supportsSingleAxisFreeRotation(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_6);
    }

    public static boolean supportsDynamicDialogs(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_6);
    }

    public static boolean supportsHappyGhastHitbox(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_6);
    }

    public static boolean supportsScoreboardBackgroundHiding(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isBetween(V1_21_6, V1_21_7);
    }

    public static boolean supportsScoreboardBackgroundHiding(@NotNull MinecraftVersion version) {
        return supportsScoreboardBackgroundHiding(version.toSemanticVersion());
    }

    public static boolean usesMinMaxPackFormatMetadata(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_9);
    }

    /**
     * In Minecraft 1.21.11+ and 26.x, the client separates the {@code items} atlas
     * (no mipmaps) from the {@code blocks} atlas (mipmapped) and enforces strict
     * single-atlas resolution across model parent/texture hierarchies.
     */
    public static boolean supportsSplitItemAtlas(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_11);
    }

    public static boolean supportsSplitItemAtlas(@NotNull MinecraftVersion version) {
        return supportsSplitItemAtlas(version.toSemanticVersion());
    }

    /**
     * In Minecraft 1.21.11+ and 26.x, block/item model elements support arbitrary
     * multi-axis rotation angles rather than only 22.5-degree increments.
     */
    public static boolean supportsFreeBlockModelRotations(@NotNull SemanticServerVersion version) {
        return version.toCanonicalNamespace().isAtLeast(V1_21_11);
    }

    public static boolean uses263PacketAccessors(@NotNull SemanticServerVersion version) {
        return version.targetsAtLeast(26, 3, 0);
    }

    public static boolean uses263PacketAccessors(@NotNull MinecraftVersion version) {
        return uses263PacketAccessors(version.toSemanticVersion());
    }
}
