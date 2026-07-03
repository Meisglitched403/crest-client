package com.crest.client.bongocat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class VirtualKeyboard {
    private static final int KEY_W = 18;
    private static final int KEY_H = 14;
    private static final int PAD = 1;

    private static final int BG_NORMAL = 0xFF444444;
    private static final int BG_WASD = 0xFF5555AA;
    private static final int BG_PRESSED = 0xFFFFD700;
    private static final int TEXT_NORMAL = 0xFFFFFFFF;
    private static final int TEXT_PRESSED = 0xFF000000;

    private static final KeyInfo[] KEYS = {
        new KeyInfo("1", 0, 0, 1, GLFW.GLFW_KEY_1),
        new KeyInfo("2", 1, 0, 1, GLFW.GLFW_KEY_2),
        new KeyInfo("3", 2, 0, 1, GLFW.GLFW_KEY_3),
        new KeyInfo("4", 3, 0, 1, GLFW.GLFW_KEY_4),
        new KeyInfo("5", 4, 0, 1, GLFW.GLFW_KEY_5),
        new KeyInfo("6", 5, 0, 1, GLFW.GLFW_KEY_6),
        new KeyInfo("7", 6, 0, 1, GLFW.GLFW_KEY_7),
        new KeyInfo("8", 7, 0, 1, GLFW.GLFW_KEY_8),
        new KeyInfo("9", 8, 0, 1, GLFW.GLFW_KEY_9),

        new KeyInfo("W", 2, 1, 1, GLFW.GLFW_KEY_W, true),
        new KeyInfo("A", 3, 1, 1, GLFW.GLFW_KEY_A, true),
        new KeyInfo("S", 4, 1, 1, GLFW.GLFW_KEY_S, true),
        new KeyInfo("D", 5, 1, 1, GLFW.GLFW_KEY_D, true),

        new KeyInfo("Shft", 0, 2, 2, GLFW.GLFW_KEY_LEFT_SHIFT),
        new KeyInfo("Spc", 3, 2, 3, GLFW.GLFW_KEY_SPACE),
        new KeyInfo("Ctrl", 7, 2, 2, GLFW.GLFW_KEY_LEFT_CONTROL),
    };

    public static void render(GuiGraphicsExtractor g, int x, int y, float scale, boolean[] keyStates) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null) return;

        for (KeyInfo key : KEYS) {
            boolean pressed = key.glfwKey < keyStates.length && keyStates[key.glfwKey];

            int kx = x + (int) (key.col * KEY_W * scale);
            int ky = y + (int) (key.row * KEY_H * scale);
            int kw = (int) (KEY_W * scale);
            int kh = (int) (KEY_H * scale);
            int pad = Math.max(1, (int) (PAD * scale));

            int innerX = kx + pad;
            int innerY = ky + pad;
            int innerW = (int) (key.width * KEY_W * scale) - pad * 2;
            int innerH = kh - pad * 2;

            int bgColor = pressed ? BG_PRESSED : (key.isWASD ? BG_WASD : BG_NORMAL);
            int textColor = pressed ? TEXT_PRESSED : TEXT_NORMAL;

            g.fill(innerX, innerY, innerX + innerW, innerY + innerH, bgColor);

            if (font != null) {
                Component label = Component.literal(key.label);
                int textX = innerX + innerW / 2;
                int textY = innerY + (innerH - font.lineHeight) / 2 + 1;
                g.text(font, label, textX - font.width(label) / 2, textY, textColor);
            }
        }
    }

    public static int getWidth(float scale) {
        return (int) (9 * KEY_W * scale);
    }

    public static int getHeight(float scale) {
        return (int) (3 * KEY_H * scale);
    }

    private record KeyInfo(String label, int col, int row, int width, int glfwKey, boolean isWASD) {
        KeyInfo(String label, int col, int row, int width, int glfwKey) {
            this(label, col, row, width, glfwKey, false);
        }
    }
}
