package io.th0rgal.oraxen.utils;

import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.StringUtils;

public class KeyUtils {

    public static Key dropPngSuffix(Key key) {
        return dropPngSuffix(key.asString());
    }

    public static Key dropPngSuffix(String key) {
        return Key.key(key.replace(".png", ""));
    }

    public static Key appendPngSuffix(Key key) {
        return appendPngSuffix(key.asString());
    }

    public static Key appendPngSuffix(String key) {
        return Key.key(StringUtils.appendIfMissing(key, ".png"));
    }
}
