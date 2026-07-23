package com.crest.client.ui;

import com.crest.client.ui.layout.LayoutNode;

import java.util.HashMap;
import java.util.Map;

public class LayoutCache {
    private static final Map<String, CachedLayout> cache = new HashMap<>();
    private static int frameCounter = 0;

    private static class CachedLayout {
        final int x, y, width, height;
        final int screenWidth, screenHeight;
        final long frame;

        CachedLayout(int x, int y, int width, int height, int screenWidth, int screenHeight, long frame) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.screenWidth = screenWidth; this.screenHeight = screenHeight;
            this.frame = frame;
        }
    }

    public static boolean isValid(String key, LayoutNode node, int screenW, int screenH) {
        CachedLayout cached = cache.get(key);
        if (cached == null) return false;
        return cached.x == node.x && cached.y == node.y
            && cached.width == node.width && cached.height == node.height
            && cached.screenWidth == screenW && cached.screenHeight == screenH;
    }

    public static void store(String key, LayoutNode node, int screenW, int screenH) {
        cache.put(key, new CachedLayout(node.x, node.y, node.width, node.height, screenW, screenH, frameCounter));
    }

    public static void clear() {
        cache.clear();
    }

    public static void nextFrame() {
        frameCounter++;
        if (frameCounter > 1000) {
            cache.clear();
            frameCounter = 0;
        }
    }
}
