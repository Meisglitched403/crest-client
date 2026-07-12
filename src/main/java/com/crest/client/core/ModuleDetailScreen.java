package com.crest.client.core;

import com.crest.client.core.setting.*;
import com.crest.client.ui.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ModuleDetailScreen extends Screen {
    private static final int HEADER_H = 40;
    private static final int PAD = 12;
    private static final int ROW_H = 20;

    private final CrestModule module;
    private final Screen parent;
    private final List<Setting<?>> settings;

    private final Map<Setting<?>, Widget> widgetCache = new HashMap<>();
    private final Map<String, Animated> toggleAnim = new HashMap<>();
    private Widget activeWidget;

    // keybind capture
    private boolean keybindCapture;
    private String keybindCaptureModule;

    // color picker
    private ColorPickerState colorPicker;
    private final Animated colorPickerAnim = new Animated(0f, 12f);

    // scroll
    private float scrollOffset, scrollTarget;
    private int contentH;

    // animations
    private final Animated openAnim = new Animated(0f, 10f);

    public ModuleDetailScreen(CrestModule module, Screen parent) {
        super(Component.literal(module.getName()));
        this.module = module;
        this.parent = parent;
        this.settings = module.getSettings();
        openAnim.setImmediate(0f);
        openAnim.set(1f);
    }

    private Animated toggleFor(String id) {
        return toggleAnim.computeIfAbsent(id, k -> new Animated(0f, 12f));
    }

    private Widget getWidget(Setting<?> setting) {
        return widgetCache.computeIfAbsent(setting, s -> {
            Widget w;
            if (s instanceof BooleanSetting bs) w = new ToggleRow(bs);
            else if (s instanceof IntegerSetting || s instanceof FloatSetting) w = new SliderRow(s);
            else if (s instanceof StringSetting ss) w = new TextRow(ss);
            else if (s instanceof ModeSetting ms) w = new ModeRow(ms);
            else if (s instanceof ColorSetting cs) {
                ColorRow cr = new ColorRow(cs);
                cr.setOnPick(cs2 -> colorPicker = new ColorPickerState(cs2));
                w = cr;
            } else if (s instanceof KeybindSetting ks) {
                KeybindRow kr = new KeybindRow(ks);
                kr.setOnCapture((modId, ks2) -> { keybindCapture = true; keybindCaptureModule = ks2.getModuleId(); });
                w = kr;
            } else w = null;
            return w;
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        openAnim.tick(delta);
        colorPickerAnim.tick(delta);
        float open = openAnim.get();

        int bgAlpha = (int) (Math.round(Theme.OVERLAY * 1.0) & 0xFF);
        g.fill(0, 0, width, height, ColorUtil.withAlpha(Theme.OVERLAY, (int) (bgAlpha * open)));

        int wy = (int) ((1 - open) * 24);
        g.pose().pushMatrix();
        g.pose().translate(0, wy);

        renderHeader(g, mx, my);
        renderBody(g, mx, my, delta);

        // color picker overlay
        if (colorPicker != null) {
            colorPickerAnim.set(1f);
            renderColorPicker(g, mx, my);
        } else {
            colorPickerAnim.set(0f);
        }

        g.pose().popMatrix();
    }

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my) {
        boolean enabled = CrestModules.isEnabled(module.getId());
        Animated ta = toggleFor(module.getId() + "_toggle");
        ta.set(enabled ? 1f : 0f);
        ta.tick(0.016f);

        Panel.drawGlass(g, 0, 0, width, HEADER_H, ColorUtil.withAlpha(Theme.BG_PANEL, 235), Theme.getAnimatedAccent());

        // back button
        String back = "\u2190";
        boolean backHover = mx >= PAD && mx <= PAD + font.width(back) + 8 && my >= 6 && my <= HEADER_H - 6;
        g.text(font, Component.literal(back), PAD, (HEADER_H - font.lineHeight) / 2,
            backHover ? Theme.getAnimatedAccent() : Theme.TEXT);

        // module name
        int nameX = PAD + font.width(back) + 16;
        g.text(font, Component.literal(module.getName()), nameX, (HEADER_H - font.lineHeight) / 2, Theme.TEXT);

        // toggle
        int toggleX = width - PAD - PAD - ToggleSwitch.W;
        ToggleSwitch.render(g, toggleX, (HEADER_H - ToggleSwitch.H) / 2, enabled, ta.get());

        // description
        String desc = module.getDescription();
        if (!desc.isEmpty()) {
            g.text(font, Component.literal(desc), PAD + font.width(back) + 16, (HEADER_H - font.lineHeight) / 2 + font.lineHeight + 1,
                Theme.TEXT_DIM);
        }
    }

    private void renderBody(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int listTop = HEADER_H + 8;
        int visibleSettings = (int) settings.stream().filter(Setting::isVisible).count();
        contentH = visibleSettings * ROW_H + 8;
        int maxH = Math.max(0, contentH - (height - listTop));
        scrollTarget = Anim.clamp(scrollTarget, 0, maxH);
        scrollOffset += (scrollTarget - scrollOffset) * Anim.smooth(delta, 18f);

        g.enableScissor(PAD, listTop, width - PAD, height);

        int sy = listTop - (int) scrollOffset;
        int idx = 0;
        for (Setting<?> setting : settings) {
            if (!setting.isVisible()) continue;
            int cy = sy + idx * ROW_H;

            boolean hover = mx >= PAD && mx <= width - PAD && my >= cy && my <= cy + ROW_H - 2;
            if (hover) {
                g.fill(PAD, cy, width - PAD, cy + ROW_H - 2, ColorUtil.withAlpha(Theme.BG_HOVER, 100));
            }

            Widget widget = getWidget(setting);
            if (widget instanceof KeybindRow kr && setting instanceof KeybindSetting ks) {
                kr.setCapturing(keybindCapture && keybindCaptureModule != null && keybindCaptureModule.equals(ks.getModuleId()));
            }
            if (widget != null) {
                widget.render(g, font, PAD, cy, width - PAD * 2, mx, my, delta);
            }
            idx++;
        }

        g.disableScissor();

        // scrollbar
        if (maxH > 0) {
            int trackX = width - 6;
            float thumbH = (float) (height - listTop) / contentH * (height - listTop);
            float thumbY = scrollOffset / contentH * (height - listTop);
            g.fill(trackX, listTop, trackX + 3, height, ColorUtil.withAlpha(Theme.BG_BASE, 200));
            g.fill(trackX, listTop + (int) thumbY, trackX + 3, listTop + (int) (thumbY + thumbH), Theme.getAnimatedAccent());
        }
    }

    // --- Input ---

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (activeWidget != null && activeWidget.charTyped(cp, 0)) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (keybindCapture) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { keybindCapture = false; keybindCaptureModule = null; }
            else if (keybindCaptureModule != null) {
                CrestModule mod = CrestModules.get(keybindCaptureModule);
                if (mod != null) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (s instanceof KeybindSetting ks) { ks.set(key); CrestModules.getConfigManager().markDirty(); break; }
                    }
                }
                keybindCapture = false; keybindCaptureModule = null;
            }
            return true;
        }

        if (activeWidget != null && activeWidget.keyPressed(key, 0, 0)) return true;

        if (colorPicker != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { closeColorPicker(); return true; }
            return super.keyPressed(event);
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int btn = event.buttonInfo().input();

        if (btn == 0 && colorPicker != null) {
            if (handleColorPickerClick(mx, my)) return true;
            return super.mouseClicked(event, doubleClick);
        }

        // back button
        if (btn == 0 && mx >= PAD && mx <= PAD + font.width("\u2190") + 8 && my >= 6 && my <= HEADER_H - 6) {
            onClose();
            return true;
        }

        // toggle in header
        if (btn == 0) {
            int toggleX = width - PAD - PAD - ToggleSwitch.W;
            if (mx >= toggleX && mx <= toggleX + ToggleSwitch.W && my >= (HEADER_H - ToggleSwitch.H) / 2 && my <= (HEADER_H + ToggleSwitch.H) / 2) {
                CrestModules.setEnabled(module.getId(), !CrestModules.isEnabled(module.getId()));
                return true;
            }
        }

        // settings
        if (mx >= PAD && mx <= width - PAD) {
            int listTop = HEADER_H + 8;
            int idx = 0;
            for (Setting<?> setting : settings) {
                if (!setting.isVisible()) continue;
                int cy = listTop + idx * ROW_H - (int) scrollOffset;
                if (my >= cy && my <= cy + ROW_H - 2) {
                    Widget widget = getWidget(setting);
                    if (widget != null && widget.mouseClicked(mx, my, btn)) {
                        activeWidget = widget;
                    }
                    return true;
                }
                idx++;
            }
        }

        if (activeWidget instanceof TextRow && btn == 0) activeWidget = null;

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        if (colorPicker != null) { handleColorPickerClick(mx, my); return true; }
        if (activeWidget instanceof SliderRow sr && sr.dragging) {
            sr.mouseDragged(mx, my);
            return true;
        }
        // find slider under cursor
        if (mx >= PAD && mx <= width - PAD) {
            int listTop = HEADER_H + 8;
            int idx = 0;
            for (Setting<?> setting : settings) {
                if (!setting.isVisible()) continue;
                int cy = listTop + idx * ROW_H - (int) scrollOffset;
                if (my >= cy && my <= cy + ROW_H - 2 && (setting instanceof IntegerSetting || setting instanceof FloatSetting)) {
                    Widget w = getWidget(setting);
                    if (w instanceof SliderRow sr) { sr.mouseClicked(mx, my, 0); activeWidget = sr; return true; }
                }
                idx++;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int maxH = Math.max(0, contentH - (height - (HEADER_H + 8)));
        scrollTarget = Anim.clamp(scrollTarget - (float) deltaY * 3, 0, maxH);
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // --- Color picker ---

    private static final class ColorPickerState {
        final ColorSetting setting;
        int h, s = 100, v = 100;
        int _sx, _sy, _sq, _hx, _hy, _hh;
        ColorPickerState(ColorSetting cs) {
            this.setting = cs;
            int rgb = cs.getRGB();
            float[] hsv = rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            h = (int) (hsv[0] * 360);
            s = (int) (hsv[1] * 100);
            v = (int) (hsv[2] * 100);
        }
    }

    private void renderColorPicker(GuiGraphicsExtractor g, int mx, int my) {
        int pw = 200, ph = 200;
        int px = Math.min(width / 2 - pw / 2, width - pw - 8);
        int py = HEADER_H + 10;
        int a = (int) (255 * colorPickerAnim.get());
        Panel.drawGlass(g, px, py, pw, ph, ColorUtil.withAlpha(Theme.BG_PANEL, a), Theme.getAnimatedAccent());

        int sq = 130, sx = px + 12, sy = py + 30;
        int hueBarX = sx + sq + 12, hueBarW = 14, hueBarH = sq;

        for (int yy = 0; yy < sq; yy += 2) {
            for (int xx = 0; xx < sq; xx += 2) {
                float sat = xx / (float) sq, val = 1 - yy / (float) sq;
                int col = ColorUtil.hsvToInt(colorPicker.h / 360f, sat, val, 255);
                g.fill(sx + xx, sy + yy, sx + xx + 2, sy + yy + 2, col);
            }
        }

        for (int yy = 0; yy < hueBarH; yy += 2) {
            float hh = 1 - yy / (float) hueBarH;
            g.fill(hueBarX, sy + yy, hueBarX + hueBarW, sy + yy + 2, ColorUtil.hsvToInt(hh, 1, 1, 255));
        }

        int selX = sx + (int) (colorPicker.s / 100f * sq) - 3;
        int selY = sy + (int) ((1 - colorPicker.v / 100f) * sq) - 3;
        g.fill(selX, selY, selX + 6, selY + 6, 0xFFFFFFFF);
        int hueY = sy + (int) ((1 - colorPicker.h / 360f) * hueBarH) - 2;
        g.fill(hueBarX - 2, hueY, hueBarX + hueBarW + 2, hueY + 4, 0xFFFFFFFF);

        g.text(font, Component.literal("Pick a color"), px + 12, py + 10, Theme.TEXT);
        g.text(font, Component.literal("[Done]"), px + pw - 44, py + ph - 16, Theme.getAnimatedAccent());

        colorPicker._sx = sx; colorPicker._sy = sy; colorPicker._sq = sq;
        colorPicker._hx = hueBarX; colorPicker._hy = sy; colorPicker._hh = hueBarH;
    }

    private boolean handleColorPickerClick(double mx, double my) {
        ColorPickerState st = colorPicker;
        int pw = 200, ph = 200;
        int px = Math.min(width / 2 - pw / 2, width - pw - 8);
        int py = HEADER_H + 10;
        if (mx >= px + pw - 44 && mx <= px + pw - 6 && my >= py + ph - 18 && my <= py + ph - 6) { closeColorPicker(); return true; }
        if (mx >= st._hx && mx <= st._hx + 14 && my >= st._hy && my <= st._hy + st._hh) {
            st.h = (int) ((1 - (my - st._hy) / (float) st._hh) * 360); applyColor(); return true;
        }
        if (mx >= st._sx && mx <= st._sx + st._sq && my >= st._sy && my <= st._sy + st._sq) {
            st.s = (int) ((mx - st._sx) / (float) st._sq * 100);
            st.v = (int) ((1 - (my - st._sy) / (float) st._sq) * 100);
            applyColor(); return true;
        }
        return false;
    }

    private void applyColor() {
        ColorPickerState st = colorPicker;
        int rgb = ColorUtil.hsvToInt(st.h / 360f, st.s / 100f, st.v / 100f, 255);
        st.setting.set(rgb);
        CrestModules.getConfigManager().markDirty();
    }

    private void closeColorPicker() { colorPicker = null; CrestModules.getConfigManager().save(); }

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf));
        float h, s, v = max, d = max - min;
        s = max == 0 ? 0 : d / max;
        if (d == 0) h = 0;
        else if (max == rf) h = ((gf - bf) / d) % 6;
        else if (max == gf) h = (bf - rf) / d + 2;
        else h = (rf - gf) / d + 4;
        h /= 6;
        if (h < 0) h += 1;
        return new float[]{h, s, v};
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
