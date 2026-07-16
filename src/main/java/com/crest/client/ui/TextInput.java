package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TextInput implements Widget {
    private String text;
    private final Consumer<String> onChange;
    private boolean focused;
    private String editing;
    private float cursorTimer;

    public TextInput(String initial, Consumer<String> onChange) {
        this.text = initial;
        this.onChange = onChange;
    }

    public String getText() { return text; }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        cursorTimer += delta;
        int bg = focused
            ? ColorUtil.withAlpha(Theme.PRIMARY_CONTAINER, 100)
            : ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 80);
        Panel.draw(g, x, y, w, H, bg);

        String display = focused ? editing : text;
        int tw = font.width(display);
        int tx = x + 4;
        int ty = y + (H - font.lineHeight) / 2;
        g.text(font, Component.literal(display), tx, ty, Theme.ON_SURFACE);

        if (focused && (int) (cursorTimer * 2) % 2 == 0) {
            int cx = tx + font.width(editing);
            g.fill(cx, ty, cx + 1, ty + font.lineHeight, Theme.PRIMARY);
        }

        if (!focused && text.isEmpty()) {
            g.text(font, Component.literal("..."), tx, ty, Theme.OUTLINE);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (!focused) {
                focused = true;
                editing = text;
                cursorTimer = 0;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!focused) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
            commit();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !editing.isEmpty()) {
            editing = editing.substring(0, editing.length() - 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(int codepoint, int mods) {
        if (!focused) return false;
        editing += (char) codepoint;
        cursorTimer = 0;
        return true;
    }

    public void blur() {
        if (focused) commit();
    }

    private void commit() {
        text = editing;
        focused = false;
        if (onChange != null) onChange.accept(text);
    }
}
