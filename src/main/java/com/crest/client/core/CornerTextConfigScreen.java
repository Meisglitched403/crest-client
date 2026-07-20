package com.crest.client.core;

import com.crest.client.core.setting.*;
import com.crest.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CornerTextConfigScreen extends Screen {
    private static final int HEADER_H = 56;
    private static final int ROW_H = 34;
    private static final int PREVIEW_W = 220;
    private static final int PREVIEW_H = 180;
    private static final int SLIDER_W = 132;
    private static final int DROPDOWN_H = 24;
    private static final int PAD = Spacing.S3;

    private final CornerTextModule module;
    private final Screen parent;
    private final List<Setting<?>> settings;

    private final Animated openAnim = new Animated(0f, 10f);
    private int activeTextIdx = -1;
    private String editBuffer;

    private int openDropdown = -1;
    private int draggingSlider = -1;
    private int mx, my;

    private ColorSetting editingColor = null;
    private float cpHue = 0f, cpSat = 1f, cpVal = 1f, cpAlpha = 1f;
    private int cpDrag = -1; // 0 = SV square, 1 = hue bar, 2 = alpha bar
    private final Animated cpAnim = new Animated(0f, 12f);

    private final Map<Integer, Animated> rowHover = new HashMap<>();
    private final Map<String, Animated> toggleAnim = new HashMap<>();

    protected CornerTextConfigScreen(CornerTextModule module, Screen parent) {
        super(Component.literal(module.getName()));
        this.module = module;
        this.parent = parent;
        this.settings = module.getSettings();
        openAnim.setImmediate(0f);
        openAnim.set(1f);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        this.mx = mx;
        this.my = my;
        Theme.tick(delta);
        openAnim.tick(delta);
        cpAnim.tick(delta);
        float open = openAnim.get();
        if (open < 0.01f) return;

        g.fill(0, 0, width, height, ColorUtil.withAlpha(Theme.GLASS_BG, (int) (Theme.glassOpacity * open)));

        int wy = (int) ((1 - open) * -14);
        g.pose().pushMatrix();
        g.pose().translate(0, wy);

        renderHeader(g, mx, my);
        renderBody(g, mx, my, delta);
        renderPreviewPanel(g, mx, my);

        renderColorPicker(g, mx, my);

        g.pose().popMatrix();
    }

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my) {
        int accent = Theme.getAnimatedAccent();

        String back = "\u2190";
        int backW = font.width(back) + Spacing.S2;
        boolean backHover = mx >= PAD && mx <= PAD + backW && my >= Spacing.S2 && my <= HEADER_H - Spacing.S2;
        if (backHover)
            g.fill(PAD - 2, Spacing.S2, PAD + backW, HEADER_H - Spacing.S2, ColorUtil.withAlpha(accent, 22));
        g.text(font, Component.literal(back), PAD, (HEADER_H - font.lineHeight) / 2, backHover ? accent : Theme.FOREGROUND);

        int nameX = PAD + Spacing.S6;
        g.text(font, Component.literal(module.getName()), nameX, Spacing.S3, Theme.FOREGROUND);
        g.text(font, Component.literal(module.getDescription()), nameX,
            Spacing.S3 + font.lineHeight + 2, ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 200));

        int toggleW = 46, toggleH = 24;
        int toggleX = width - PAD - toggleW;
        int toggleY = (HEADER_H - toggleH) / 2;
        drawToggle(g, toggleX, toggleY, toggleW, toggleH, CrestModules.isEnabled(module.getId()), accent);

        g.fill(PAD, HEADER_H - 1, width - PAD, HEADER_H, Theme.BORDER_LIGHT);
    }

    private void drawToggle(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean on, int accent) {
        int track = on ? ColorUtil.withAlpha(accent, 210) : ColorUtil.withAlpha(Theme.MUTED, 150);
        g.fill(x, y, x + w, y + h, track);
        int knob = 0xFFFFFFFF;
        int kx = on ? x + w - h + 3 : x + 3;
        g.fill(kx, y + 3, kx + h - 6, y + h - 3, knob);
    }

    private void renderBody(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int contentX = PAD;
        int contentY = HEADER_H + Spacing.S3;
        int previewX = width - PAD - PREVIEW_W;
        int contentW = previewX - Spacing.S4 - contentX;

        int rowX = contentX;
        int rowW = contentW;
        int listH = settings.size() * ROW_H + Spacing.S2;
        int bh = Math.min(height - contentY - Spacing.S3, listH);

        Panel.drawGlassElevated(g, contentX, contentY, contentW, bh,
            ColorUtil.withAlpha(Theme.CARD, 220), Theme.getAnimatedAccent(), Theme.ELEVATION_1);

        g.enableScissor(contentX, contentY, contentX + contentW, contentY + bh);

        int settingsY = contentY + Spacing.S2;
        for (int i = 0; i < settings.size(); i++) {
            int ry = settingsY + i * ROW_H;
            Setting<?> s = settings.get(i);
            boolean hover = mx >= rowX && mx <= rowX + rowW && my >= ry && my <= ry + ROW_H;

            Animated ha = rowHover.computeIfAbsent(i, k -> new Animated(0f, 14f));
            ha.set(hover ? 1f : 0f);
            ha.tick(delta);
            float hAmt = ha.get();

            if (hAmt > 0.01f) {
                g.fill(rowX + 4, ry + 3, rowX + rowW - 4, ry + ROW_H - 3,
                    ColorUtil.withAlpha(Theme.getAnimatedAccent(), (int) (26 * hAmt)));
            }
            g.fill(rowX + 6, ry + 3, rowX + 8, ry + ROW_H - 3, ColorUtil.withAlpha(Theme.getAnimatedAccent(), (int) (140 * hAmt)));

            g.text(font, Component.literal(s.getName()), rowX + Spacing.S3 + 4,
                ry + (ROW_H - font.lineHeight) / 2, Theme.FOREGROUND);

            renderSettingValue(g, s, mx, my, rowX, rowW, ry, i);
        }

        g.disableScissor();

        // Draw open dropdown popup outside the scissor region so it isn't clipped
        if (openDropdown >= 0 && openDropdown < settings.size()) {
            Setting<?> s = settings.get(openDropdown);
            if (s instanceof ModeSetting ms) {
                int i = openDropdown;
                int ry = settingsY + i * ROW_H;
                int dw = SLIDER_W, dh = DROPDOWN_H;
                int dx = rowX + rowW - SLIDER_W - Spacing.S3, dy = ry + (ROW_H - dh) / 2;
                int popX = dx, popY = dy + dh + 4, popW = dw, popH = ms.getModes().length * DROPDOWN_H;
                Panel.drawGlassElevated(g, popX, popY, popW, popH, ColorUtil.withAlpha(0x14141F, 250), Theme.getAnimatedAccent(), Theme.ELEVATION_2);
                for (int m = 0; m < ms.getModes().length; m++) {
                    int itemY = popY + m * DROPDOWN_H;
                    boolean mHover = mx >= popX && mx <= popX + popW && my >= itemY && my <= itemY + DROPDOWN_H;
                    if (mHover) g.fill(popX + 3, itemY + 2, popX + popW - 3, itemY + DROPDOWN_H - 2, ColorUtil.withAlpha(Theme.getAnimatedAccent(), 55));
                    g.text(font, Component.literal(ms.getModes()[m]), popX + Spacing.S2, itemY + (DROPDOWN_H - font.lineHeight) / 2,
                        ms.getModes()[m].equals(ms.getMode()) ? Theme.getAnimatedAccent() : Theme.FOREGROUND);
                }
            }
        }
    }

    private void renderSettingValue(GuiGraphicsExtractor g, Setting<?> s, int mx, int my, int rowX, int rowW, int ry, int idx) {
        int vx = rowX + rowW - SLIDER_W - Spacing.S3;
        int vy = ry + (ROW_H - 16) / 2;

        if (s instanceof StringSetting ss) {
            String val = ss.get();
            if (activeTextIdx == idx && editBuffer != null) val = editBuffer + "\u258C";
            int maxW = SLIDER_W;
            String shown = font.width(val) > maxW ? font.plainSubstrByWidth(val, maxW - 8) + "\u2026" : val;
            g.text(font, Component.literal(shown), vx, vy + 4,
                activeTextIdx == idx ? Theme.getAnimatedAccent() : Theme.MUTED_FOREGROUND);

        } else if (s instanceof ModeSetting ms) {
            int dw = SLIDER_W, dh = DROPDOWN_H;
            int dx = vx, dy = ry + (ROW_H - dh) / 2;
            boolean boxHover = mx >= dx && mx <= dx + dw && my >= dy && my <= dy + dh;
            int boxTint = (openDropdown == idx) ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 40)
                : (boxHover ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 26) : ColorUtil.withAlpha(Theme.MUTED, 70));
            g.fill(dx, dy, dx + dw, dy + dh, boxTint);
            Panel.drawHollowRect(g, dx, dy, dw, dh, Theme.BORDER_LIGHT);
            g.text(font, Component.literal(ms.getMode()), dx + Spacing.S2, dy + (dh - font.lineHeight) / 2, Theme.FOREGROUND);
            g.text(font, Component.literal("\u25BE"), dx + dw - Spacing.S3, dy + (dh - font.lineHeight) / 2, Theme.MUTED_FOREGROUND);
        } else if (s instanceof BooleanSetting bs) {
            int tw = 40, th = 22;
            int tx = vx + SLIDER_W - tw, ty = ry + (ROW_H - th) / 2;
            drawToggle(g, tx, ty, tw, th, bs.get(), Theme.getAnimatedAccent());

        } else if (s instanceof IntegerSetting is) {
            renderSlider(g, mx, my, vx, ry, is.getMin(), is.getMax(), is.get(), String.valueOf(is.get()), idx);

        } else if (s instanceof FloatSetting fs) {
            renderSlider(g, mx, my, vx, ry, fs.getMin(), fs.getMax(), fs.get(), String.format("%.2f", fs.get()), idx);

        } else if (s instanceof ColorSetting cs) {
            int c = cs.get();
            int sw = 28, sh = 18;
            int sx = vx + SLIDER_W - sw, sy = ry + (ROW_H - sh) / 2;
            g.fill(sx, sy, sx + sw, sy + sh, 0x40000000);
            g.fill(sx, sy, sx + sw, sy + sh, c);
            Panel.drawHollowRect(g, sx, sy, sw, sh, Theme.BORDER_LIGHT);
        }
    }

    private void renderSlider(GuiGraphicsExtractor g, int mx, int my, int vx, int ry, float min, float max, float val, String label, int idx) {
        int trackY = ry + ROW_H / 2;
        int h = 6;
        int knobR = 6;
        float t = (val - min) / (max - min);
        t = Math.max(0f, Math.min(1f, t));
        int kx = vx + 3 + (int) (t * (SLIDER_W - 6));

        // track
        g.fill(vx, trackY - h / 2, vx + SLIDER_W, trackY + h / 2, ColorUtil.withAlpha(Theme.MUTED, 130));
        // filled
        g.fill(vx, trackY - h / 2, kx, trackY + h / 2, ColorUtil.withAlpha(Theme.getAnimatedAccent(), 215));
        // knob
        boolean kh = draggingSlider == idx || (Math.abs(mx - kx) <= knobR + 3 && Math.abs(my - trackY) <= 10);
        int kCol = kh ? Theme.FOREGROUND : Theme.getAnimatedAccent();
        g.fill(kx - knobR, trackY - knobR, kx + knobR, trackY + knobR, kCol);
        g.fill(kx - knobR + 2, trackY - knobR + 2, kx + knobR - 2, trackY + knobR - 2, ColorUtil.withAlpha(0xFFFFFFFF, 60));

        // value label above-right
        int lw = font.width(label);
        g.text(font, Component.literal(label), vx + SLIDER_W - lw, ry + 3, Theme.MUTED_FOREGROUND);
    }

    private void renderPreviewPanel(GuiGraphicsExtractor g, int mx, int my) {
        int px = width - PAD - PREVIEW_W;
        int py = HEADER_H + Spacing.S3;
        int ph = PREVIEW_H;

        Panel.drawGlassElevated(g, px, py, PREVIEW_W, ph, ColorUtil.withAlpha(Theme.CARD, 220), Theme.getAnimatedAccent(), Theme.ELEVATION_1);
        g.fill(px + 3, py + 3, px + PREVIEW_W - 3, py + 4, ColorUtil.withAlpha(Theme.getAnimatedAccent(), 70));
        g.text(font, Component.literal("Preview"), px + Spacing.S3, py + Spacing.S3 - 2, Theme.MUTED_FOREGROUND);

        int innerX = px + 8, innerY = py + 26, innerW = PREVIEW_W - 16, innerH = ph - 34;
        g.fill(innerX, innerY, innerX + innerW, innerY + innerH, 0xFF0E0E18);
        Panel.drawHollowRect(g, innerX, innerY, innerW, innerH, Theme.BORDER_LIGHT);

        String text = CornerTextModule.getText();
        if (text == null || text.isEmpty()) return;

        int color = CornerTextModule.getColor();
        boolean shadow = CornerTextModule.hasShadow();
        boolean bg = CornerTextModule.isBackgroundEnabled();
        int offX = CornerTextModule.getOffsetX();
        int offY = CornerTextModule.getOffsetY();
        String corner = CornerTextModule.getCorner();
        float s = CornerTextModule.getScale();

        Font f = Minecraft.getInstance().font;
        int tw = f.width(text);
        int lh = f.lineHeight;

        int tx, ty;
        switch (corner) {
            case "Top Left" -> { tx = innerX + 4 + offX; ty = innerY + 4 + offY; }
            case "Top Right" -> { tx = innerX + innerW - 4 - tw - offX; ty = innerY + 4 + offY; }
            case "Bottom Left" -> { tx = innerX + 4 + offX; ty = innerY + innerH - 4 - lh - offY; }
            default -> { tx = innerX + innerW - 4 - tw - offX; ty = innerY + innerH - 4 - lh - offY; }
        }

        g.pose().pushMatrix();
        g.pose().translate(tx, ty);
        g.pose().scale(s);
        g.pose().translate(-tx, -ty);

        if (bg) HudBackground.draw(g, tx - 2, ty - 2, tw + 8, lh + 8);
        if (shadow) g.text(f, text, tx + 1, ty + 1, (color & 0x00FFFFFF) | 0x80000000);
        g.text(f, text, tx, ty, color);

        g.pose().popMatrix();
    }

    private void renderColorPicker(GuiGraphicsExtractor g, int mx, int my) {
        if (editingColor == null) return;
        if (cpAnim.get() < 0.01f) return;
        float open = cpAnim.get();
        int alpha = (int) (Theme.glassOpacity * open);

        int pw = 260, ph = 250;
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;

        g.fill(0, 0, width, height, ColorUtil.withAlpha(0x000000, (int) (110 * open)));
        int wy = (int) ((1 - open) * -12);
        g.pose().pushMatrix();
        g.pose().translate(0, wy);

        Panel.drawGlassElevated(g, px, py, pw, ph, ColorUtil.withAlpha(0x14141F, 250), Theme.getAnimatedAccent(), Theme.ELEVATION_2);
        g.text(font, Component.literal("Text Color"), px + Spacing.S4, py + Spacing.S3, ColorUtil.withAlpha(Theme.FOREGROUND, alpha));

        int svX = px + Spacing.S4, svY = py + Spacing.S6 + 8;
        int svW = 150, svH = 150;
        int hueX = svX + svW + Spacing.S3, hueY = svY, hueW = 16, hueH = svH;
        int alphaX = hueX + hueW + Spacing.S3, alphaY = svY, alphaW = 16, alphaH = svH;
        int barY = svY + svH + Spacing.S3;
        int previewW = svW + hueW + alphaW + Spacing.S3 * 2;

        // SV square background (hue base)
        g.fill(svX, svY, svX + svW, svY + svH, ColorUtil.hsv(cpHue, 1f, 1f));
        // white->saturation horizontal
        drawGradientH(g, svX, svY, svW, svH, 0xFFFFFFFF, 0x00FFFFFF);
        // black vertical bottom
        drawGradientV(g, svX, svY, svW, svH, 0x00000000, 0xFF000000);
        Panel.drawHollowRect(g, svX, svY, svW, svH, Theme.BORDER_LIGHT);

        // SV knob
        int kx = svX + (int) (cpSat * svW);
        int ky = svY + (int) ((1f - cpVal) * svH);
        g.fill(kx - 3, ky - 3, kx + 3, ky + 3, 0xFFFFFFFF);
        g.fill(kx - 2, ky - 2, kx + 2, ky + 2, 0xFF000000);

        // Hue bar
        for (int i = 0; i < hueH; i++) {
            float h = (i / (float) hueH);
            g.fill(hueX, hueY + i, hueX + hueW, hueY + i + 1, ColorUtil.hsv(h, 1f, 1f));
        }
        Panel.drawHollowRect(g, hueX, hueY, hueW, hueH, Theme.BORDER_LIGHT);
        int hSel = hueY + (int) (cpHue * hueH);
        g.fill(hueX - 2, hSel - 2, hueX + hueW + 2, hSel + 2, 0xFFFFFFFF);

        // Alpha bar
        int base = ColorUtil.hsv(cpHue, cpSat, cpVal) & 0x00FFFFFF;
        for (int i = 0; i < alphaH; i++) {
            float a = i / (float) alphaH;
            g.fill(alphaX, alphaY + i, alphaX + alphaW, alphaY + i + 1,
                ColorUtil.withAlpha(base, (int) (a * 255)));
        }
        Panel.drawHollowRect(g, alphaX, alphaY, alphaW, alphaH, Theme.BORDER_LIGHT);
        int aSel = alphaY + (int) (cpAlpha * alphaH);
        g.fill(alphaX - 2, aSel - 2, alphaX + alphaW + 2, aSel + 2, 0xFFFFFFFF);

        // Swatch preview + hex
        int swY = barY;
        int cur = ColorUtil.withAlpha(base, (int) (cpAlpha * 255));
        g.fill(svX, swY, svX + 36, swY + 22, 0x40000000);
        g.fill(svX, swY, svX + 36, swY + 22, cur);
        Panel.drawHollowRect(g, svX, swY, 36, 22, Theme.BORDER_LIGHT);
        String hex = String.format("#%08X", cur);
        g.text(font, Component.literal(hex), svX + 44, swY + (22 - font.lineHeight) / 2, Theme.FOREGROUND);

        // Done button
        int doneW = 80, doneH = 28;
        int doneX = px + pw - doneW - Spacing.S4;
        int doneY = py + ph - doneH - Spacing.S3;
        int doneHover = (mx >= doneX && mx <= doneX + doneW && my >= doneY && my <= doneY + doneH) ? 1 : 0;
        g.fill(doneX, doneY, doneX + doneW, doneY + doneH, ColorUtil.withAlpha(Theme.getAnimatedAccent(), doneHover == 1 ? 230 : 180));
        Panel.drawHollowRect(g, doneX, doneY, doneW, doneH, Theme.BORDER_LIGHT);
        g.text(font, Component.literal("Done"), doneX + (doneW - font.width("Done")) / 2, doneY + (doneH - font.lineHeight) / 2, 0xFFFFFFFF);

        g.pose().popMatrix();
    }

    private void drawGradientH(GuiGraphicsExtractor g, int x, int y, int w, int h, int left, int right) {
        for (int i = 0; i < w; i++) {
            float t = i / (float) w;
            g.fill(x + i, y, x + i + 1, y + h, ColorUtil.lerpARGB(left, right, t));
        }
    }

    private void drawGradientV(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            float t = i / (float) h;
            g.fill(x, y + i, x + w, y + i + 1, ColorUtil.lerpARGB(top, bottom, t));
        }
    }

    private void openColorPicker(ColorSetting cs) {
        editingColor = cs;
        int c = cs.get();
        float[] hsv = ColorUtil.toHSV(c);
        cpHue = hsv[0]; cpSat = hsv[1]; cpVal = hsv[2];
        cpAlpha = (c >> 24 & 0xFF) / 255f;
        cpDrag = -1;
        cpAnim.setImmediate(0f);
        cpAnim.set(1f);
    }

    private void closeColorPicker() {
        if (editingColor != null) {
            int base = ColorUtil.hsv(cpHue, cpSat, cpVal) & 0x00FFFFFF;
            editingColor.set(ColorUtil.withAlpha(base, (int) (cpAlpha * 255)));
        }
        editingColor = null;
        cpDrag = -1;
        cpAnim.set(0f);
    }

    private void updateSV(double mx, double my, int svX, int svY, int svW, int svH) {
        cpSat = (float) Math.max(0, Math.min(1, (mx - svX) / (double) svW));
        cpVal = (float) Math.max(0, Math.min(1, 1f - (my - svY) / (double) svH));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.buttonInfo().input();
        if (btn != 0) return super.mouseClicked(event, doubleClick);

        if (editingColor != null) {
            int pw = 260, ph = 250;
            int px = (width - pw) / 2;
            int py = (height - ph) / 2;
            int svX = px + Spacing.S4, svY = py + Spacing.S6 + 8;
            int svW = 150, svH = 150;
            int hueX = svX + svW + Spacing.S3, hueY = svY, hueW = 16, hueH = svH;
            int alphaX = hueX + hueW + Spacing.S3, alphaY = svY, alphaW = 16, alphaH = svH;
            int doneW = 80, doneH = 28;
            int doneX = px + pw - doneW - Spacing.S4;
            int doneY = py + ph - doneH - Spacing.S3;

            if (mx >= svX && mx <= svX + svW && my >= svY && my <= svY + svH) {
                cpDrag = 0;
                updateSV(mx, my, svX, svY, svW, svH);
                return true;
            }
            if (mx >= hueX && mx <= hueX + hueW && my >= hueY && my <= hueY + hueH) {
                cpDrag = 1;
                cpHue = (float) Math.max(0, Math.min(1, (my - hueY) / (double) hueH));
                return true;
            }
            if (mx >= alphaX && mx <= alphaX + alphaW && my >= alphaY && my <= alphaY + alphaH) {
                cpDrag = 2;
                cpAlpha = (float) Math.max(0, Math.min(1, (my - alphaY) / (double) alphaH));
                return true;
            }
            if (mx >= doneX && mx <= doneX + doneW && my >= doneY && my <= doneY + doneH) {
                closeColorPicker();
                return true;
            }
            if (mx < px || mx > px + pw || my < py || my > py + ph) {
                closeColorPicker();
                return true;
            }
            return true;
        }

        int back = Spacing.S2, backB = font.width("\u2190") + Spacing.S2;
        if (mx >= PAD && mx <= PAD + backB && my >= back && my <= HEADER_H - back) {
            onClose();
            return true;
        }

        int toggleW = 46, toggleH = 24;
        int toggleX = width - PAD - toggleW;
        int toggleY = (HEADER_H - toggleH) / 2;
        if (mx >= toggleX && mx <= toggleX + toggleW && my >= toggleY && my <= toggleY + toggleH) {
            CrestModules.setEnabled(module.getId(), !CrestModules.isEnabled(module.getId()));
            return true;
        }

        if (openDropdown >= 0 && openDropdown < settings.size()) {
            Setting<?> s = settings.get(openDropdown);
            if (s instanceof ModeSetting ms) {
                int contentY0 = HEADER_H + Spacing.S3;
                int rowW = width - PAD - Spacing.S4 - PREVIEW_W - PAD;
                int rowX = PAD;
                int ry = contentY0 + Spacing.S2 + openDropdown * ROW_H;
                int dw = SLIDER_W, dh = DROPDOWN_H;
                int dx = rowX + rowW - SLIDER_W - Spacing.S3, dy = ry + (ROW_H - dh) / 2;
                int popX = dx, popY = dy + dh + 4, popW = dw, popH = ms.getModes().length * DROPDOWN_H;
                if (mx >= popX && mx <= popX + popW && my >= popY && my <= popY + popH) {
                    int sel = (int) ((my - popY) / (double) DROPDOWN_H);
                    if (sel >= 0 && sel < ms.getModes().length) ms.setMode(ms.getModes()[sel]);
                    openDropdown = -1;
                    return true;
                }
                // Click outside popup closes it instead of hitting a row beneath
                openDropdown = -1;
                return true;
            }
        }

        int contentX = PAD;
        int contentY = HEADER_H + Spacing.S3;
        int rowW = width - PAD - Spacing.S4 - PREVIEW_W - PAD;
        int rowX = contentX;

        for (int i = 0; i < settings.size(); i++) {
            int ry = contentY + Spacing.S2 + i * ROW_H;
            if (mx < rowX || mx > rowX + rowW || my < ry || my > ry + ROW_H) continue;

            Setting<?> s = settings.get(i);
            int vx = rowX + rowW - SLIDER_W - Spacing.S3;

            if (s instanceof StringSetting) {
                activeTextIdx = i;
                editBuffer = "";
                openDropdown = -1;
                return true;
            } else if (s instanceof ModeSetting ms) {
                int dw = SLIDER_W, dh = DROPDOWN_H;
                int dx = vx, dy = ry + (ROW_H - dh) / 2;
                if (mx >= dx && mx <= dx + dw && my >= dy && my <= dy + dh) {
                    openDropdown = (openDropdown == i) ? -1 : i;
                    return true;
                }
            } else if (s instanceof BooleanSetting bs) {
                bs.set(!bs.get());
                openDropdown = -1;
                return true;
            } else if (s instanceof IntegerSetting is) {
                int nv = (int) Math.round(is.getMin() + ((mx - vx) / (double) SLIDER_W) * (is.getMax() - is.getMin()));
                is.set(nv);
                draggingSlider = i;
                openDropdown = -1;
                return true;
            } else if (s instanceof FloatSetting fs) {
                float nv = fs.getMin() + (float) ((mx - vx) / (double) SLIDER_W) * (fs.getMax() - fs.getMin());
                fs.set(nv);
                draggingSlider = i;
                openDropdown = -1;
                return true;
            } else if (s instanceof ColorSetting cs) {
                openColorPicker(cs);
                openDropdown = -1;
                return true;
            }
            return true;
        }

        openDropdown = -1;
        activeTextIdx = -1;
        editBuffer = null;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (editingColor != null && cpDrag >= 0) {
            double mx = event.x(), my = event.y();
            int pw = 260, ph = 250;
            int px = (width - pw) / 2;
            int py = (height - ph) / 2;
            int svX = px + Spacing.S4, svY = py + Spacing.S6 + 8;
            int svW = 150, svH = 150;
            int hueX = svX + svW + Spacing.S3, hueY = svY, hueW = 16, hueH = svH;
            int alphaX = hueX + hueW + Spacing.S3, alphaY = svY, alphaW = 16, alphaH = svH;
            if (cpDrag == 0) updateSV(mx, my, svX, svY, svW, svH);
            else if (cpDrag == 1) cpHue = (float) Math.max(0, Math.min(1, (my - hueY) / (double) hueH));
            else if (cpDrag == 2) cpAlpha = (float) Math.max(0, Math.min(1, (my - alphaY) / (double) alphaH));
            return true;
        }
        if (draggingSlider >= 0 && draggingSlider < settings.size()) {
            Setting<?> s = settings.get(draggingSlider);
            double mx = event.x();
            int rowW = width - PAD - Spacing.S4 - PREVIEW_W - PAD;
            int rowX = PAD;
            int vx = rowX + rowW - SLIDER_W - Spacing.S3;
            float t = (float) Math.max(0, Math.min(1, (mx - vx) / (double) SLIDER_W));
            if (s instanceof IntegerSetting is) {
                is.set((int) Math.round(is.getMin() + t * (is.getMax() - is.getMin())));
            } else if (s instanceof FloatSetting fs) {
                fs.set(fs.getMin() + t * (fs.getMax() - fs.getMin()));
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingSlider = -1;
        if (editingColor != null) cpDrag = -1;
        return super.mouseReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (activeTextIdx >= 0 && editBuffer != null) {
            int cp = event.codepoint();
            if (cp >= 32) {
                editBuffer += event.codepointAsString();
                Setting<?> s = settings.get(activeTextIdx);
                if (s instanceof StringSetting ss) ss.set(editBuffer);
            }
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (activeTextIdx >= 0 && editBuffer != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) { activeTextIdx = -1; editBuffer = null; return true; }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !editBuffer.isEmpty()) {
                editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                Setting<?> s = settings.get(activeTextIdx);
                if (s instanceof StringSetting ss) ss.set(editBuffer);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) { activeTextIdx = -1; editBuffer = null; return true; }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
    @Override
    public boolean isPauseScreen() { return false; }
}
