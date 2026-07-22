// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.util;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ServerVersion {

    V1_19(19),
    V1_20(20),
    V1_21(21),
    V1_22(22),
    UNKNOWN(Integer.MAX_VALUE);

    private static final Pattern VERSION_PATTERN = Pattern.compile("(?:1\\.)(\\d+)");
    private static ServerVersion current;

    private final int minor;

    ServerVersion(int minor) {
        this.minor = minor;
    }

    public static ServerVersion getCurrent() {
        if (current == null) {
            detectVersion();
        }
        return current;
    }

    private static void detectVersion() {
        String versionString = Bukkit.getBukkitVersion();
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (matcher.find()) {
            try {
                int minor = Integer.parseInt(matcher.group(1));
                for (ServerVersion v : values()) {
                    if (v.minor == minor) {
                        current = v;
                        return;
                    }
                }
                current = minor > values()[values().length - 2].minor ? values()[values().length - 2] : UNKNOWN;
            } catch (NumberFormatException e) {
                current = UNKNOWN;
            }
        } else {
            current = UNKNOWN;
        }
    }

    public boolean isAtLeast(ServerVersion other) {
        return this.minor >= other.minor;
    }

    public boolean isSupported() {
        return this != UNKNOWN && this.minor >= 19;
    }

    public int getMinor() {
        return minor;
    }
}
