// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.util;

import org.bukkit.Bukkit;

public final class ServerVersion {

    private static ServerVersion current;

    private final int major;
    private final int minor;
    private final String display;
    private final boolean parsed;

    private ServerVersion(int major, int minor, String display, boolean parsed) {
        this.major = major;
        this.minor = minor;
        this.display = display;
        this.parsed = parsed;
    }

    public static ServerVersion getCurrent() {
        if (current == null) {
            current = detect();
        }
        return current;
    }

    private static ServerVersion detect() {
        String raw = Bukkit.getBukkitVersion(); // e.g. "1.21.4-R0.1-SNAPSHOT" or "26.1.2-R0.1-SNAPSHOT"
        try {
            String versionPart = raw.split("-")[0];      // "1.21.4" / "26.1.2" / "26.1"
            String[] parts = versionPart.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return new ServerVersion(major, minor, versionPart, true);
        } catch (Exception e) {
            // Unknown / future format: stay lenient and let the plugin run.
            return new ServerVersion(-1, -1, raw, false);
        }
    }

    /**
     * Supported when:
     *  - legacy scheme (major 1): minor >= 19
     *  - year-based scheme (2026+, major >= 2): always, it is newer than 1.21
     *  - unparseable: lenient (true) so a new/unknown scheme never blocks startup
     */
    public boolean isSupported() {
        if (!parsed) return true;
        if (major == 1) return minor >= 19;
        return major >= 2;
    }

    public int getMajor() { return major; }

    public int getMinor() { return minor; }

    public String getDisplayVersion() { return display; }
}
