package com.crest.client.waypoints;

import net.minecraft.core.BlockPos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses coordinate strings from chat and clipboard. */
public final class ChatCoordinateService {
    private ChatCoordinateService() {}

    private static final Pattern COORD = Pattern.compile(
            "(?:\\b|x\\D*)?(-?\\d{1,7})[\\s,.]+(-?\\d{1,7})[\\s,.]+(-?\\d{1,7})\\b");

    public static final class Coordinates {
        public final int x, y, z;
        public Coordinates(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    }

    public static Coordinates parse(String text) {
        if (text == null) return null;
        Matcher m = COORD.matcher(text.replace('~', ' '));
        while (m.find()) {
            try {
                int x = Integer.parseInt(m.group(1));
                int y = Integer.parseInt(m.group(2));
                int z = Integer.parseInt(m.group(3));
                return new Coordinates(x, y, z);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public static String format(Coordinates c) {
        return c.x + " " + c.y + " " + c.z;
    }

    /** Parses only when the entire trimmed message is a coordinate triple. */
    public static Coordinates parseStrict(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (!t.matches("-?\\d{1,7}[\\s,.]+-?\\d{1,7}[\\s,.]+-?\\d{1,7}")) return null;
        return parse(t);
    }
}
