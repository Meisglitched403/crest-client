package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Copy/paste waypoints via the system clipboard. */
public final class WaypointClipboard {
    private WaypointClipboard() {}

    private static final Pattern WP = Pattern.compile("(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\s+(.+)");

    public static void copy(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (Exception ignored) {}
    }

    public static String toClipboardString(Waypoint wp) {
        return wp.getX() + " " + wp.getY() + " " + wp.getZ() + " " + wp.getName();
    }

    public static BlockPos parsePosition(String text) {
        if (text == null) return null;
        Matcher m = WP.matcher(text.trim());
        if (m.find()) {
            return new BlockPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        }
        ChatCoordinateService.Coordinates c = ChatCoordinateService.parse(text);
        return c == null ? null : new BlockPos(c.x, c.y, c.z);
    }

    public static void copyWaypoint(Minecraft mc, Waypoint wp) {
        copy(toClipboardString(wp));
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[Crest] Copied waypoint to clipboard"));
    }
}
