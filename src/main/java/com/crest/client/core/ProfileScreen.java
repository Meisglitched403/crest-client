package com.crest.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import com.crest.client.ui.Theme;
import com.crest.client.ui.Panel;
import com.crest.client.ui.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Settings profiles screen. Type a name + Save to snapshot the current
 * config; click a profile to Apply it; right-click (or Del) to delete. Built with
 * the same low-level drawing style as HudEditScreen for consistency.
 */
public class ProfileScreen extends Screen {
    private final Screen parent;
    private String nameInput = "";
    private boolean inputFocused;
    private int mx, my;
    private int scroll;

    protected ProfileScreen(Screen parent) {
        super(Component.literal("Profiles"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new ProfileScreen(parent));
    }

    private int rowY(int i) { return 70 + i * 34 - scroll; }

    @Override
    protected void init() {
        Theme.load();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        this.mx = mx; this.my = my;
        Theme.tick(delta);
        g.fill(0, 0, width, height, Theme.GLASS_BG);
        Panel.draw(g, 40, 40, width - 80, height - 80, Theme.GLASS_BG);
        Panel.drawHollowRect(g, 40, 40, width - 80, height - 80, Theme.BORDER_LIGHT);

        g.text(font, Component.literal("Config Profiles"), 60, 56, Theme.FOREGROUND);

        // Name input
        int inX = 60, inY = 84, inW = 260, inH = 28;
        g.fill(inX, inY, inX + inW, inY + inH,
                inputFocused ? ColorUtil.withAlpha(Theme.PRIMARY, 60) : ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 80));
        Panel.drawHollowRect(g, inX, inY, inW, inH, Theme.BORDER_LIGHT);
        String shown = inputFocused ? nameInput : (nameInput.isEmpty() ? "Profile name..." : nameInput);
        g.text(font, Component.literal(shown), inX + 6, inY + 8,
                nameInput.isEmpty() && !inputFocused ? Theme.MUTED_FOREGROUND : Theme.ON_SURFACE);
        if (inputFocused && (int) (delta * 2) % 2 == 0) {
            int cx = inX + 6 + font.width(nameInput);
            g.fill(cx, inY + 6, cx + 1, inY + 22, Theme.PRIMARY);
        }

        // Save button
        drawButton(g, "Save", inX + inW + 12, inY, 90, inH, true);

        // Profile list
        List<String> names = new ArrayList<>(Profiles.names());
        int listX = 60, listW = width - 160;
        for (int i = 0; i < names.size(); i++) {
            int y = rowY(i);
            if (y < 130 || y > height - 70) continue;
            String n = names.get(i);
            boolean hover = mx >= listX && mx <= listX + listW && my >= y && my <= y + 28;
            g.fill(listX, y, listX + listW, y + 28, hover ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 40)
                    : ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 60));
            Panel.drawHollowRect(g, listX, y, listW, 28, Theme.BORDER_LIGHT);
            g.text(font, Component.literal(n), listX + 10, y + 9, Theme.FOREGROUND);
            g.text(font, Component.literal("Apply"), listX + listW - 120, y + 9, Theme.getAnimatedAccent());
            g.text(font, Component.literal("Delete"), listX + listW - 50, y + 9, Theme.DESTRUCTIVE);
        }

        g.text(font, Component.literal("ESC to go back  |  click a profile to Apply, Delete to remove"),
                60, height - 56, Theme.MUTED_FOREGROUND);
    }

    private void drawButton(GuiGraphicsExtractor g, String label, int x, int y, int w, int h, boolean enabled) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        g.fill(x, y, x + w, y + h, hover ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 200)
                : ColorUtil.withAlpha(Theme.getAnimatedAccent(), 150));
        Panel.drawHollowRect(g, x, y, w, h, Theme.BORDER_LIGHT);
        g.text(font, Component.literal(label), x + (w - font.width(label)) / 2, y + (h - font.lineHeight) / 2, Theme.FOREGROUND);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mxx = event.x(), myy = event.y();
        int inX = 60, inY = 84, inW = 260, inH = 28;
        if (mxx >= inX && mxx <= inX + inW && myy >= inY && myy <= inY + inH) {
            inputFocused = true;
            return true;
        }
        inputFocused = false;

        // Save
        if (mxx >= inX + inW + 12 && mxx <= inX + inW + 102 && myy >= inY && myy <= inY + inH) {
            if (!nameInput.trim().isEmpty()) {
                Profiles.save(nameInput.trim());
                nameInput = "";
            }
            return true;
        }

        List<String> names = new ArrayList<>(Profiles.names());
        int listX = 60, listW = width - 160;
        for (int i = 0; i < names.size(); i++) {
            int y = rowY(i);
            if (y < 130 || y > height - 70) continue;
            if (mxx >= listX && mxx <= listX + listW && myy >= y && myy <= y + 28) {
                String n = names.get(i);
                if (mxx >= listX + listW - 120 && mxx <= listX + listW - 55) {
                    Profiles.apply(n);
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("[Crest] Applied profile '" + n + "'"));
                } else if (mxx >= listX + listW - 50 && mxx <= listX + listW) {
                    Profiles.delete(n);
                } else {
                    Profiles.apply(n);
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("[Crest] Applied profile '" + n + "'"));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (inputFocused) {
            int cp = event.codepoint();
            if (cp >= 32 && cp < 127) nameInput += event.codepointAsString();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (inputFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !nameInput.isEmpty()) {
                nameInput = nameInput.substring(0, nameInput.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) { inputFocused = false; return true; }
        } else if (key == GLFW.GLFW_KEY_DELETE) {
            List<String> names = new ArrayList<>(Profiles.names());
            for (int i = 0; i < names.size(); i++) {
                int y = rowY(i);
                if (y < 130 || y > height - 70) continue;
                if (mx >= 60 && mx <= width - 100 && my >= y && my <= y + 28) {
                    Profiles.delete(names.get(i));
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
