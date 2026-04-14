package io.th0rgal.oraxen.pack.generation;

public enum ShaderOverlay {

    V1_20_2("overlay_1_20_2", 18, 45, "1.20.2"),
    V1_21_4("overlay_1_21_4", 46, 62, "1.21.4"),
    V1_21_6("overlay_1_21_6", 63, 68, "1.21.6"),
    V1_21_9("overlay_1_21_9", 69, 83, "1.21.9"),
    V26("overlay_26", 84, 999, "26");

    private final String directory;
    private final int minFormat;
    private final int maxFormat;
    private final String representativeVersion;

    ShaderOverlay(String directory, int minFormat, int maxFormat, String representativeVersion) {
        this.directory = directory;
        this.minFormat = minFormat;
        this.maxFormat = maxFormat;
        this.representativeVersion = representativeVersion;
    }

    public String directory() { return directory; }
    public int minFormat() { return minFormat; }
    public int maxFormat() { return maxFormat; }
    public String representativeVersion() { return representativeVersion; }

    public boolean containsFormat(int format) {
        return format >= minFormat && format <= maxFormat;
    }

    public static ShaderOverlay forPackFormat(int packFormat) {
        for (ShaderOverlay overlay : values()) {
            if (overlay.containsFormat(packFormat)) return overlay;
        }
        return null;
    }
}
