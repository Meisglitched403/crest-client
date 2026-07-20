package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Keystrokes HUD. Shows W/A/S/D + LMB/RMB keys, highlighting the key
 * while it is held (polled from GLFW each frame). Optionally shows live CPS.
 */
public class KeystrokesModule extends HudModule {
    private static final int PAD = 3;
    private static final int KEY = 18;   // square key size
    private static final int GAP = 2;

    private final BooleanSetting showMouse = new BooleanSetting("Show Mouse", true);
    private final BooleanSetting showCps = new BooleanSetting("Show CPS", true);
    private final ColorSetting textColor = new ColorSetting("Text Color", 0xFFFFFFFF);
    private final ColorSetting pressedColor = new ColorSetting("Pressed Color", 0xFF4CC4FF);
    private final ColorSetting bgColor = new ColorSetting("BG Color", 0x80000000);

    public KeystrokesModule() {
        super(-1, 90);
    }

    @Override public String getId() { return "keystrokes"; }
    @Override public String getName() { return "Keystrokes"; }
    @Override public String getDescription() { return "Shows WASD and mouse buttons, highlighting while held."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(showMouse);
        s.add(showCps);
        s.add(textColor);
        s.add(pressedColor);
        s.add(bgColor);
        return s;
    }

    @Override
    public int getWidth() {
        int cols = 3;
        int w = cols * KEY + (cols - 1) * GAP + PAD * 2;
        if (showMouse.get()) w = Math.max(w, 2 * KEY + GAP + PAD * 2);
        return w;
    }

    @Override
    public int getHeight() {
        int rows = 2; // W row + ASD row
        int h = rows * KEY + (rows - 1) * GAP + PAD * 2;
        if (showMouse.get()) h += GAP + KEY;
        if (showCps.get()) h += GAP + 10;
        return h;
    }

    private static boolean down(int key) {
        long window = GLFW.glfwGetCurrentContext();
        return window != 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        int boxW = getRenderWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;

        Font font = mc.font;
        int txtCol = textColor.get();
        int prCol = pressedColor.get();
        int bg = bgColor.get();

        int rowW = 3 * KEY + 2 * GAP;
        int startX = rx + (boxW - rowW) / 2;
        int cy = ry + PAD;

        // Row 1: W (centered)
        drawKey(g, startX + KEY + GAP, cy, KEY, "W", down(GLFW.GLFW_KEY_W), font, txtCol, prCol, bg);
        // Row 2: A S D
        int y2 = cy + KEY + GAP;
        drawKey(g, startX, y2, KEY, "A", down(GLFW.GLFW_KEY_A), font, txtCol, prCol, bg);
        drawKey(g, startX + KEY + GAP, y2, KEY, "S", down(GLFW.GLFW_KEY_S), font, txtCol, prCol, bg);
        drawKey(g, startX + 2 * (KEY + GAP), y2, KEY, "D", down(GLFW.GLFW_KEY_D), font, txtCol, prCol, bg);

        int nextY = y2 + KEY + GAP;
        if (showMouse.get()) {
            drawKey(g, startX, nextY, KEY, "L", down(GLFW.GLFW_MOUSE_BUTTON_1), font, txtCol, prCol, bg);
            drawKey(g, startX + KEY + GAP, nextY, KEY, "R", down(GLFW.GLFW_MOUSE_BUTTON_2), font, txtCol, prCol, bg);
            nextY += KEY + GAP;
        }
        if (showCps.get()) {
            String cps = "L " + CpsTracker.leftCps() + "  R " + CpsTracker.rightCps();
            g.text(font, Component.literal(cps), rx + PAD, nextY, txtCol);
        }
    }

    private void drawKey(GuiGraphicsExtractor g, int x, int y, int size, String label,
            boolean pressed, Font font, int txtCol, int prCol, int bg) {
        g.fill(x, y, x + size, y + size, bg);
        int col = pressed ? prCol : txtCol;
        g.centeredText(font, Component.literal(label), x + size / 2, y + size / 2 - 4, col);
    }
}
