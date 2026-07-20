package com.crest.client.core;

import com.crest.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * In-app theme editor. Edits a working copy of {@link ThemeData}; [Apply] commits it
 * to the live theme and persists. Live preview uses the working copy so nothing is
 * saved until the user applies.
 *
 * Layout: left rail (presets + preview), right scrollable settings grouped into
 * Colors / Appearance / Layout sections.
 */
public class ThemeEditorScreen extends Screen {
    private final Screen parent;

    private ThemeData work = Theme.get().clone();
    private ColorPicker colorPicker;

    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int railX, railY, railW, railH;

    private final List<Item> items = new ArrayList<>();
    private final Animated openAnim = new Animated(0f, 10f);
    private final Animated colorAnim = new Animated(0f, 12f);

    private float scrollOffset, scrollTarget, maxScroll;
    private int mx, my;
    private String message = "";
    private int messageTimer;

    public ThemeEditorScreen(Screen parent) {
        super(Component.literal("Theme Editor"));
        this.parent = parent;
        openAnim.setImmediate(0f);
        openAnim.set(1f);
        buildItems();
    }

    // ---- Item model ----

    private interface Item {
        int height();
        void render(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my);
        boolean mouseClicked(double mx, double my, int x, int y, int w);
        boolean mouseDragged(double mx, double my, int x, int y, int w);
    }

    private static final class Header implements Item {
        final String text;
        Header(String t) { this.text = t; }
        public int height() { return Spacing.S6; }
        public void render(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my) {
            int ay = y + Spacing.S2;
            g.fill(x, ay, x + 3, ay + f.lineHeight + 2, Theme.getAnimatedAccent());
            g.text(f, Component.literal(text), x + Spacing.S2, ay, Theme.FOREGROUND);
        }
        public boolean mouseClicked(double mx, double my, int x, int y, int w) { return false; }
        public boolean mouseDragged(double mx, double my, int x, int y, int w) { return false; }
    }

    private class ColorItem implements Item {
        final String name;
        final Supplier<Integer> get;
        final Consumer<Integer> set;
        ColorItem(String n, Supplier<Integer> g, Consumer<Integer> s) { this.name = n; this.get = g; this.set = s; }
        public int height() { return Theme.ROW_H(); }
        public void render(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my) {
            int rh = Theme.ROW_H();
            g.text(f, Component.literal(name), x, y + (rh - f.lineHeight) / 2, work.foreground);
            int sw = 22, sx = x + w - sw - 4, sy = y + (rh - sw) / 2;
            g.fill(sx - 1, sy - 1, sx + sw + 1, sy + sw + 1, 0x44000000);
            g.fill(sx, sy, sx + sw, sy + sw, 0xFF000000 | get.get());
            if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sw) {
                g.fill(sx - 1, sy - 1, sx + sw + 1, sy + sw + 1, work.accent);
            }
        }
        public boolean mouseClicked(double mx, double my, int x, int y, int w) {
            int rh = Theme.ROW_H();
            int sw = 22, sx = x + w - sw - 4, sy = y + (rh - sw) / 2;
            if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sw) {
                colorPicker = new ColorPicker().setValue(get.get())
                    .setOnPick(c -> set.accept(c));
                return true;
            }
            return false;
        }
        public boolean mouseDragged(double mx, double my, int x, int y, int w) { return false; }
    }

    private class SliderItem implements Item {
        final String name;
        final int min, max;
        final Supplier<Integer> get;
        final Consumer<Integer> set;
        final Function<Integer, String> fmt;
        boolean dragging;
        SliderItem(String n, int mn, int mx2, Supplier<Integer> g, Consumer<Integer> s) {
            this(n, mn, mx2, g, s, v -> String.valueOf(v));
        }
        SliderItem(String n, int mn, int mx2, Supplier<Integer> g, Consumer<Integer> s, Function<Integer, String> fmt) {
            this.name = n; this.min = mn; this.max = mx2; this.get = g; this.set = s; this.fmt = fmt;
        }
        public int height() { return Theme.ROW_H(); }
        public void render(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my) {
            int rh = Theme.ROW_H();
            int valW = 46;
            int trackX = x, trackW = w - valW - Spacing.S2, trackY = y + rh - 12, trackH = 6;
            g.text(f, Component.literal(name), x, y + 4, work.foreground);
            g.text(f, Component.literal(fmt.apply(get.get())), x + w - valW, y + 4, ColorUtil.withAlpha(work.mutedForeground, 220));
            g.fill(trackX, trackY, trackX + trackW, trackY + trackH, ColorUtil.withAlpha(work.border, 200));
            float t = (get.get() - min) / (float) (max - min);
            int kw = 10;
            int kx = trackX + (int) (t * (trackW - kw));
            g.fill(kx, trackY - 2, kx + kw, trackY + trackH + 2, work.accent);
        }
        public boolean mouseClicked(double mx, double my, int x, int y, int w) {
            int rh = Theme.ROW_H();
            int valW = 46;
            int trackX = x, trackW = w - valW - Spacing.S2, trackY = y + rh - 12, trackH = 6;
            if (mx >= trackX && mx <= trackX + trackW && my >= trackY - 5 && my <= trackY + trackH + 5) {
                dragging = true; update((int) mx, trackX, trackW); return true;
            }
            return false;
        }
        public boolean mouseDragged(double mx, double my, int x, int y, int w) {
            if (!dragging) return false;
            int valW = 46;
            update((int) mx, x, w - valW - Spacing.S2); return true;
        }
        private void update(int mxp, int trackX, int trackW) {
            float t = Anim.clamp((float) (mxp - trackX) / trackW, 0, 1);
            set.accept(min + Math.round(t * (max - min)));
        }
    }

    private class ModeItem implements Item {
        final String name;
        final String[] opts;
        final Supplier<Integer> get;
        final Consumer<Integer> set;
        ModeItem(String n, String[] o, Supplier<Integer> g, Consumer<Integer> s) {
            this.name = n; this.opts = o; this.get = g; this.set = s;
        }
        public int height() { return Theme.ROW_H(); }
        public void render(GuiGraphicsExtractor g, Font f, int x, int y, int w, int mx, int my) {
            int rh = Theme.ROW_H();
            g.text(f, Component.literal(name), x, y + (rh - f.lineHeight) / 2, work.foreground);
            int sel = (int) Anim.clamp(get.get(), 0, opts.length - 1);
            int lw = f.width(opts[sel]) + Spacing.S4;
            int lx = x + w - lw;
            g.fill(lx, y + (rh - 22) / 2, lx + lw, y + (rh + 22) / 2, ColorUtil.withAlpha(work.border, 120));
            g.text(f, Component.literal(opts[sel]), lx + Spacing.S2, y + (rh - f.lineHeight) / 2, work.accent);
        }
        public boolean mouseClicked(double mx, double my, int x, int y, int w) {
            int rh = Theme.ROW_H();
            int sel = (int) Anim.clamp(get.get(), 0, opts.length - 1);
            int lw = font.width(opts[sel]) + Spacing.S4;
            int lx = x + w - lw;
            if (mx >= lx && mx <= lx + lw && my >= y && my <= y + rh) {
                set.accept((get.get() + 1) % opts.length);
                work.preset = "Custom";
                return true;
            }
            return false;
        }
        public boolean mouseDragged(double mx, double my, int x, int y, int w) { return false; }
    }

    private void buildItems() {
        items.clear();
        items.add(new Header("Colors"));
        items.add(new ColorItem("Accent", () -> work.accent, c -> work.accent = c));
        items.add(new ColorItem("Background", () -> work.background, c -> work.background = c));
        items.add(new ColorItem("Foreground", () -> work.foreground, c -> work.foreground = c));
        items.add(new ColorItem("Card", () -> work.card, c -> work.card = c));
        items.add(new ColorItem("Muted Text", () -> work.mutedForeground, c -> work.mutedForeground = c));
        items.add(new ColorItem("Destructive", () -> work.destructive, c -> work.destructive = c));
        items.add(new ColorItem("Border", () -> work.border, c -> work.border = c));
        items.add(new ColorItem("Glass Tint", () -> work.glassBg, c -> work.glassBg = c));
        items.add(new ColorItem("Sidebar", () -> work.sidebarBg, c -> work.sidebarBg = c));

        items.add(new Header("Appearance"));
        items.add(new SliderItem("Glass Opacity", 0, 255, () -> work.glassOpacity, v -> work.glassOpacity = v));
        items.add(new SliderItem("Corner Radius", 0, 24, () -> work.radius, v -> work.radius = v));
        items.add(new SliderItem("Font Scale", 70, 150, () -> (int) (work.fontScale * 100),
            v -> work.fontScale = v / 100f, v -> v + "%"));

        items.add(new Header("Layout"));
        items.add(new ModeItem("Density", new String[]{"Compact", "Normal", "Comfortable"},
            () -> work.density.ordinal(), i -> work.density = Theme.Density.values()[i]));
    }

    // ---- Layout ----

    @Override
    protected void init() {
        panelW = Math.min(620, width - Spacing.S10);
        panelX = (width - panelW) / 2;
        panelY = Spacing.S8;
        panelH = height - Spacing.S10;

        int railW = 200;
        railX = panelX + Spacing.S4;
        railY = panelY + 56;
        this.railW = railW;
        railH = panelH - 56 - Spacing.S10;

        listX = railX + railW + Spacing.S6;
        listY = panelY + 56;
        listW = panelX + panelW - Spacing.S4 - listX;
        listH = panelH - 56 - Spacing.S10;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        this.mx = mx; this.my = my;
        Theme.tick(delta);
        openAnim.tick(delta);
        colorAnim.tick(delta);
        if (messageTimer > 0) messageTimer--;
        float open = openAnim.get();

        g.fill(0, 0, width, height, ColorUtil.withAlpha(work.glassBg, (int) (work.glassOpacity * open)));
        int wy = (int) ((1 - open) * 16);
        g.pose().pushMatrix();
        g.pose().translate(0, wy);

        Panel.drawElevated(g, panelX, panelY, panelW, panelH, ColorUtil.withAlpha(work.card, 235), Theme.ELEVATION_3);
        g.fill(panelX + 3, panelY + 2, panelX + panelW - 3, panelY + 3, ColorUtil.withAlpha(work.accent, Theme.topStripAlpha));

        g.text(font, Component.literal("Theme Editor"), panelX + Spacing.S4, panelY + Spacing.S3, work.foreground);
        g.text(font, Component.literal("Customize the look  •  Apply to save"),
            panelX + Spacing.S4, panelY + Spacing.S3 + font.lineHeight + 2, ColorUtil.withAlpha(work.mutedForeground, 200));

        drawPresets(g);
        drawPreview(g);

        drawSettings(g);

        drawFooter(g);

        if (messageTimer > 0) {
            int mw = font.width(message) + Spacing.S4;
            int myy = panelY + panelH - Spacing.S10 - 34;
            Panel.draw(g, panelX + panelW / 2 - mw / 2, myy, mw, 26, ColorUtil.withAlpha(work.popover, 240));
            g.text(font, Component.literal(message), panelX + panelW / 2 - font.width(message) / 2, myy + 6, work.foreground);
        }

        if (colorPicker != null) {
            colorAnim.set(1f);
            int pw = 220, ph = 260;
            int px = Math.min(width / 2 - pw / 2, width - pw - Spacing.S2);
            int py = Spacing.S8;
            Panel.drawElevated(g, px, py, pw, ph, ColorUtil.withAlpha(work.card, 255), Theme.ELEVATION_3);
            g.fill(px + 2, py, px + pw - 2, py + 1, ColorUtil.withAlpha(work.accent, Theme.topStripAlpha));
            colorPicker.layout(px, py, pw, ph);
            colorPicker.render(g, font, mx, my, (int) (255 * colorAnim.get()));
        } else {
            colorAnim.set(0f);
        }

        g.pose().popMatrix();
    }

    private void drawPresets(GuiGraphicsExtractor g) {
        String[] names = {"Dark", "Light", "Amoled"};
        ThemeData[] presets = {ThemePresets.DARK, ThemePresets.LIGHT, ThemePresets.AMOLED};
        int cardH = 36;
        for (int i = 0; i < 3; i++) {
            int x = railX, y = railY + i * (cardH + Spacing.S2), w = railW;
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + cardH;
            boolean active = work.preset.equalsIgnoreCase(names[i]);
            Panel.draw(g, x, y, w, cardH, ColorUtil.withAlpha(presets[i].card, hover ? 255 : 220));
            g.fill(x, y, 4, cardH, presets[i].accent);
            g.text(font, Component.literal(names[i]), x + Spacing.S3, y + (cardH - font.lineHeight) / 2, presets[i].foreground);
            if (active) g.text(font, Component.literal("\u2713"), x + w - Spacing.S4, y + (cardH - font.lineHeight) / 2, presets[i].accent);
        }
    }

    private void drawPreview(GuiGraphicsExtractor g) {
        int px = railX, py = railY + 3 * (36 + Spacing.S2) + Spacing.S2;
        int pw = railW, ph = railH - (py - railY) - Spacing.S2;
        if (ph < 80) return;
        g.text(font, Component.literal("Preview"), px, py - font.lineHeight - 4, ColorUtil.withAlpha(work.mutedForeground, 220));
        Panel.draw(g, px, py, pw, ph, ColorUtil.withAlpha(work.card, 235));
        int cx = px + Spacing.S3, cy = py + Spacing.S3;
        Panel.draw(g, cx, cy, pw - Spacing.S6, 40, ColorUtil.withAlpha(work.background, 220));
        g.text(font, Component.literal("Sample Module"), cx + Spacing.S2, cy + Spacing.S2, work.foreground);
        g.text(font, Component.literal("Subtle description text"), cx + Spacing.S2, cy + Spacing.S2 + font.lineHeight, ColorUtil.withAlpha(work.mutedForeground, 220));
        int tw = 44, th = 22, tx = px + Spacing.S3, ty = cy + 50;
        g.fillGradient(tx, ty, tx + tw, ty + th, work.accent, ColorUtil.withAlpha(work.accent, 180));
        Panel.drawHollowRect(g, tx, ty, tw, th, work.border);
        g.fill(tx + tw - 20, ty + 3, tx + tw - 4, ty + th - 3, 0xFFFFFFFF);
        int bw = 90, bh = 26, bx = tx, by = ty + th + Spacing.S2;
        boolean bhov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        g.fill(bx, by, bx + bw, by + bh, ColorUtil.lerpARGB(work.background, work.foreground, bhov ? 0.25f : 0.12f));
        Panel.drawHollowRect(g, bx, by, bw, bh, work.border);
        g.text(font, Component.literal("Button"), bx + (bw - font.width("Button")) / 2, by + (bh - font.lineHeight) / 2, work.foreground);
    }

    private void drawSettings(GuiGraphicsExtractor g) {
        int total = 0;
        for (Item it : items) total += it.height() + Spacing.S1;
        int visible = listH;
        maxScroll = Math.max(0, total - visible);
        scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
        scrollOffset += (scrollTarget - scrollOffset) * 0.3f;

        g.enableScissor(listX, listY, listX + listW, listY + listH);
        int y = listY - (int) scrollOffset;
        for (Item it : items) {
            int h = it.height();
            if (y + h >= listY && y <= listY + listH) {
                it.render(g, font, listX, y, listW, mx, my);
            }
            y += h + Spacing.S1;
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int trackX = listX + listW + 2;
            float ratio = visible / (float) total;
            int thumbH = Math.max(20, (int) (listH * ratio));
            int thumbY = listY + (int) (scrollOffset / maxScroll * (listH - thumbH));
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ColorUtil.withAlpha(work.accent, 180));
        }
    }

    private void drawFooter(GuiGraphicsExtractor g) {
        int by = panelY + panelH - Spacing.S8;
        int bw = 96, bh = 28, gap = Spacing.S3;
        int total = bw * 3 + gap * 2;
        int startX = panelX + (panelW - total) / 2;
        drawButton(g, startX, by, bw, bh, "Apply", true);
        drawButton(g, startX + bw + gap, by, bw, bh, "Export", false);
        drawButton(g, startX + (bw + gap) * 2, by, bw, bh, "Import", false);
    }

    private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean primary) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int fill = primary ? ColorUtil.withAlpha(work.accent, hover ? 235 : 200)
                           : ColorUtil.lerpARGB(work.background, work.foreground, hover ? 0.22f : 0.12f);
        g.fill(x, y, x + w, y + h, fill);
        Panel.drawHollowRect(g, x, y, w, h, primary ? work.accent : work.border);
        g.text(font, Component.literal(label), x + (w - font.width(label)) / 2, y + (h - font.lineHeight) / 2,
            primary ? 0xFFFFFFFF : work.foreground);
    }

    private boolean inButton(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mxx = event.x(), myy = event.y();
        int btn = event.buttonInfo().input();
        if (btn != 0) return super.mouseClicked(event, doubleClick);

        if (colorPicker != null) {
            if (colorPicker.handleClick(mxx, myy)) return true;
            colorPicker = null;
            return true;
        }

        // Presets
        String[] names = {"Dark", "Light", "Amoled"};
        ThemeData[] presets = {ThemePresets.DARK, ThemePresets.LIGHT, ThemePresets.AMOLED};
        int cardH = 36;
        for (int i = 0; i < 3; i++) {
            int x = railX, y = railY + i * (cardH + Spacing.S2), w = railW;
            if (inButton(mxx, myy, x, y, w, cardH)) {
                work = presets[i].clone();
                work.preset = names[i];
                buildItems();
                return true;
            }
        }

        // Footer
        int by = panelY + panelH - Spacing.S8;
        int bw = 96, bh = 28, gap = Spacing.S3;
        int total = bw * 3 + gap * 2;
        int startX = panelX + (panelW - total) / 2;
        if (inButton(mxx, myy, startX, by, bw, bh)) { apply(); return true; }
        if (inButton(mxx, myy, startX + bw + gap, by, bw, bh)) { exportTheme(); return true; }
        if (inButton(mxx, myy, startX + (bw + gap) * 2, by, bw, bh)) { importTheme(); return true; }

        // Settings list (respect scissor)
        if (mxx >= listX && mxx <= listX + listW && myy >= listY && myy <= listY + listH) {
            int y = listY - (int) scrollOffset;
            for (Item it : items) {
                int h = it.height();
                if (myy >= y && myy <= y + h) {
                    if (it.mouseClicked(mxx, myy, listX, y, listW)) return true;
                    break;
                }
                y += h + Spacing.S1;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mxx = event.x(), myy = event.y();
        if (colorPicker != null) { colorPicker.handleClick(mxx, myy); return true; }
        if (mxx >= listX && mxx <= listX + listW && myy >= listY && myy <= listY + listH) {
            int y = listY - (int) scrollOffset;
            for (Item it : items) {
                int h = it.height();
                if (myy >= y && myy <= y + h) {
                    if (it.mouseDragged(mxx, myy, listX, y, listW)) return true;
                    break;
                }
                y += h + Spacing.S1;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            scrollTarget = Anim.clamp(scrollTarget - (float) deltaY * 24, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    private void apply() { Theme.apply(work); flash("Theme applied"); }
    private void exportTheme() { Theme.apply(work); flash("Saved to crest-theme.json"); }
    private void importTheme() { Theme.load(); work = Theme.get().clone(); buildItems(); flash("Loaded crest-theme.json"); }
    private void flash(String m) { message = m; messageTimer = 120; }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
