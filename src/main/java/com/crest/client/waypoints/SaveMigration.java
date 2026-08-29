package com.crest.client.waypoints;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Detects old save versions and migrates them to the current format (V10). */
public final class SaveMigration {
    public static final int CURRENT_VERSION = 10;

    private SaveMigration() {}

    public static int detectVersion(Path file) {
        if (!Files.exists(file)) return CURRENT_VERSION;
        try {
            String head = new String(Files.readAllBytes(file));
            int idx = head.indexOf("\"version\"");
            if (idx < 0) return CURRENT_VERSION;
            int colon = head.indexOf(':', idx);
            int comma = head.indexOf(',', colon);
            String num = head.substring(colon + 1, comma < 0 ? head.length() : comma).replaceAll("[^0-9]", "");
            return num.isEmpty() ? CURRENT_VERSION : Integer.parseInt(num);
        } catch (IOException | NumberFormatException e) {
            return CURRENT_VERSION;
        }
    }

    public static boolean needsMigration(Path file) {
        return detectVersion(file) < CURRENT_VERSION;
    }
}
