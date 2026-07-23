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
    private int cursorPos;
    private int selectionStart;
    private float cursorTimer;
    private int lastX, lastY, lastW;

    public TextInput(String initial, Consumer<String> onChange) {
        this.text = initial;
        this.onChange = onChange;
    }

    public String getText() { return text; }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        lastX = x; lastY = y; lastW = w;
        cursorTimer += delta;

        String display = focused ? editing : text;
        int bg = focused
            ? ColorUtil.withAlpha(Theme.PRIMARY_CONTAINER, 100)
            : ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 80);
        Panel.draw(g, x, y, w, H, bg);

        int tx = x + 4;
        int ty = y + (H - font.lineHeight) / 2;

        if (focused && selectionStart != cursorPos) {
            int selStart = Math.min(selectionStart, cursorPos);
            int selEnd = Math.max(selectionStart, cursorPos);
            String before = display.substring(0, selStart);
            String selected = display.substring(selStart, selEnd);
            int sx = tx + font.width(before);
            int sw = font.width(selected);
            g.fill(sx, ty, sx + sw, ty + font.lineHeight, ColorUtil.withAlpha(Theme.PRIMARY, 60));
        }

        g.text(font, Component.literal(display), tx, ty, focused ? Theme.ON_SURFACE : Theme.ON_SURFACE_VARIANT);

        if (focused && (int) (cursorTimer * 2) % 2 == 0) {
            String beforeCursor = display.substring(0, Math.min(cursorPos, display.length()));
            int cx = tx + font.width(beforeCursor);
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
                cursorPos = editing.length();
                selectionStart = cursorPos;
                cursorTimer = 0;
            } else {
                int relX = (int) (mx - lastX - 4);
                cursorPos = estimateCharIndex(relX);
                selectionStart = cursorPos;
                cursorTimer = 0;
            }
            return true;
        }
        return false;
    }

    private int estimateCharIndex(int relX) {
        if (relX <= 0) return 0;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Font font = mc.font;
        int w = 0;
        for (int i = 0; i < editing.length(); i++) {
            w += font.width(String.valueOf(editing.charAt(i)));
            if (relX < w) return i;
        }
        return editing.length();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!focused) return false;

        boolean shift = (mods & 1) != 0;
        boolean ctrl = (mods & 2) != 0;

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            blur();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            commit();
            return true;
        }

        if (ctrl) {
            switch (key) {
                case GLFW.GLFW_KEY_C -> {
                    if (selectionStart != cursorPos) {
                        int selStart = Math.min(selectionStart, cursorPos);
                        int selEnd = Math.max(selectionStart, cursorPos);
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        mc.keyboardHandler.setClipboard(editing.substring(selStart, selEnd));
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    if (selectionStart != cursorPos) {
                        int selStart = Math.min(selectionStart, cursorPos);
                        int selEnd = Math.max(selectionStart, cursorPos);
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        mc.keyboardHandler.setClipboard(editing.substring(selStart, selEnd));
                        deleteSelection();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    String clip = mc.keyboardHandler.getClipboard();
                    if (clip != null && !clip.isEmpty()) {
                        if (selectionStart != cursorPos) deleteSelection();
                        editing = editing.substring(0, cursorPos) + clip + editing.substring(cursorPos);
                        cursorPos += clip.length();
                        selectionStart = cursorPos;
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_A -> {
                    selectionStart = 0;
                    cursorPos = editing.length();
                    return true;
                }
            }
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (selectionStart != cursorPos) {
                deleteSelection();
                return true;
            }
            if (cursorPos > 0) {
                editing = editing.substring(0, cursorPos - 1) + editing.substring(cursorPos);
                cursorPos--;
                selectionStart = cursorPos;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_DELETE) {
            if (selectionStart != cursorPos) {
                deleteSelection();
                return true;
            }
            if (cursorPos < editing.length()) {
                editing = editing.substring(0, cursorPos) + editing.substring(cursorPos + 1);
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_LEFT) {
            if (shift) {
                if (cursorPos > 0) cursorPos--;
            } else {
                if (selectionStart != cursorPos) {
                    cursorPos = Math.min(selectionStart, cursorPos);
                } else if (cursorPos > 0) {
                    cursorPos--;
                }
                selectionStart = cursorPos;
            }
            cursorTimer = 0;
            return true;
        }

        if (key == GLFW.GLFW_KEY_RIGHT) {
            if (shift) {
                if (cursorPos < editing.length()) cursorPos++;
            } else {
                if (selectionStart != cursorPos) {
                    cursorPos = Math.max(selectionStart, cursorPos);
                } else if (cursorPos < editing.length()) {
                    cursorPos++;
                }
                selectionStart = cursorPos;
            }
            cursorTimer = 0;
            return true;
        }

        if (key == GLFW.GLFW_KEY_HOME) {
            cursorPos = 0;
            if (!shift) selectionStart = cursorPos;
            cursorTimer = 0;
            return true;
        }

        if (key == GLFW.GLFW_KEY_END) {
            cursorPos = editing.length();
            if (!shift) selectionStart = cursorPos;
            cursorTimer = 0;
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(int codepoint, int mods) {
        if (!focused) return false;
        if (codepoint >= 32 && codepoint < 127) {
            if (selectionStart != cursorPos) deleteSelection();
            char c = (char) codepoint;
            editing = editing.substring(0, cursorPos) + c + editing.substring(cursorPos);
            cursorPos++;
            selectionStart = cursorPos;
            cursorTimer = 0;
            return true;
        }
        return false;
    }

    private void deleteSelection() {
        int selStart = Math.min(selectionStart, cursorPos);
        int selEnd = Math.max(selectionStart, cursorPos);
        editing = editing.substring(0, selStart) + editing.substring(selEnd);
        cursorPos = selStart;
        selectionStart = cursorPos;
    }

    public void blur() {
        if (focused) commit();
    }

    private void commit() {
        text = editing;
        focused = false;
        cursorPos = text.length();
        selectionStart = cursorPos;
        if (onChange != null) onChange.accept(text);
    }
}
