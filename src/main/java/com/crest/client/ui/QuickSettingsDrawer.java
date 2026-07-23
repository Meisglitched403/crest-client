package com.crest.client.ui;

import com.crest.client.core.CrestModules;
import com.crest.client.core.Profiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QuickSettingsDrawer implements Widget {
    private static final int DRAWER_W = 220;
    private static final int TOGGLE_H = 20;
    private static final int SECTION_GAP = 8;

    private boolean open;
    private final Animated slideAnim = new Animated(0f, 10f);
    private final List<QuickToggle> toggles = new ArrayList<>();
    private final List<QuickButton> themeButtons = new ArrayList<>();
    private final List<QuickButton> profileButtons = new ArrayList<>();
    private int x, y, h;
    private int mx, my;
    private int lastHoveredToggle = -1;
    private int lastHoveredThemeBtn = -1;
    private int lastHoveredProfileBtn = -1;

    private static class QuickToggle {
        final String moduleId;
        final String label;
        QuickToggle(String moduleId, String label) {
            this.moduleId = moduleId;
            this.label = label;
        }
    }

    private static class QuickButton {
        final String label;
        final Runnable action;
        QuickButton(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    public QuickSettingsDrawer() {
        toggles.add(new QuickToggle("fullbright", "Fullbright"));
        toggles.add(new QuickToggle("zoom", "Zoom"));
        toggles.add(new QuickToggle("toggle_sneak", "Toggle Sneak"));
        toggles.add(new QuickToggle("no_fog", "No Fog"));
        toggles.add(new QuickToggle("no_hurtcam", "No Hurtcam"));
        toggles.add(new QuickToggle("low_fire", "Low Fire"));

        themeButtons.add(new QuickButton("Dark", () -> applyTheme("Dark")));
        themeButtons.add(new QuickButton("Light", () -> applyTheme("Light")));
        themeButtons.add(new QuickButton("Amoled", () -> applyTheme("Amoled")));

        for (String name : Profiles.names()) {
            profileButtons.add(new QuickButton(name, () -> applyProfile(name)));
        }
    }

    private static void applyTheme(String name) {
        Theme.apply(ThemePresets.fromName(name));
        Theme.save();
    }

    private static void applyProfile(String name) {
        Profiles.apply(name);
        Minecraft.getInstance().player.sendSystemMessage(
            Component.literal("[Crest] Applied profile '" + name + "'"));
    }

    public void toggle() {
        open = !open;
        slideAnim.set(open ? 1f : 0f);
    }

    public boolean isOpen() { return open; }

    public void refreshProfiles() {
        profileButtons.clear();
        for (String name : Profiles.names()) {
            profileButtons.add(new QuickButton(name, () -> applyProfile(name)));
        }
    }

    @Override
    public int getHeight() { return h; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        this.x = x;
        this.y = y;
        this.h = w;
        this.mx = mx;
        this.my = my;
        slideAnim.tick(delta);

        float progress = slideAnim.get();
        if (progress < 0.01f) return;

        int drawerW = (int) (DRAWER_W * Anim.easeOutCubic(progress));
        int dx = x + w - drawerW;

        g.fillGradient(dx, y, dx + drawerW, y + h,
            ColorUtil.withAlpha(Theme.SIDEBAR_BG, 220), ColorUtil.withAlpha(Theme.SIDEBAR_BG, 180));
        g.fill(dx, y, dx + 1, y + h, Theme.BORDER_LIGHT);

        int cy = y + 14;
        g.text(font, Component.literal("Quick Settings"), dx + 16, cy, Theme.FOREGROUND);
        cy += 26;

        int sw = drawerW - 32;
        int accent = Theme.getAnimatedAccent();

        cy = renderToggleSection(g, font, "Modules", toggles, dx, cy, sw, accent, delta);
        cy += SECTION_GAP;
        cy = renderThemeSection(g, font, "Themes", themeButtons, dx, cy, sw, accent, delta);
        cy += SECTION_GAP;
        cy = renderProfileSection(g, font, "Profiles", profileButtons, dx, cy, sw, accent, delta);
    }

    private int renderToggleSection(GuiGraphicsExtractor g, Font font, String title,
                                     List<QuickToggle> items, int dx, int cy, int sw, int accent, float delta) {
        g.text(font, Component.literal(title).withStyle(s -> s.withBold(true)),
                dx + 16, cy, accent);
        cy += 18;

        int hovered = -1;
        for (int i = 0; i < items.size(); i++) {
            QuickToggle qt = items.get(i);
            boolean on = CrestModules.isEnabled(qt.moduleId);
            String label = (on ? "\u2713 " : "    ") + qt.label;

            int iy = cy + i * (TOGGLE_H + 2);
            boolean hover = mx >= dx + 16 && mx <= dx + 16 + sw
                         && my >= iy && my <= iy + TOGGLE_H;

            if (hover) hovered = i;
            g.fill(dx + 16, iy, dx + 16 + sw, iy + TOGGLE_H,
                hover ? ColorUtil.withAlpha(accent, 16) : 0);
            g.text(font, Component.literal(label), dx + 22, iy + 5,
                on ? (hover ? accent : Theme.FOREGROUND) : Theme.MUTED_FOREGROUND);
        }
        if (hovered != -1 && hovered != lastHoveredToggle) UiSounds.hover();
        lastHoveredToggle = hovered;
        return cy + items.size() * (TOGGLE_H + 2);
    }

    private int renderThemeSection(GuiGraphicsExtractor g, Font font, String title,
                                    List<QuickButton> items, int dx, int cy, int sw, int accent, float delta) {
        g.text(font, Component.literal(title).withStyle(s -> s.withBold(true)),
                dx + 16, cy, accent);
        cy += 18;
        int perRow = 2;
        int btnW = (sw - 4) / perRow;
        int hovered = -1;
        for (int i = 0; i < items.size(); i++) {
            QuickButton qb = items.get(i);
            int col = i % perRow;
            int row = i / perRow;
            int bx = dx + 16 + col * (btnW + 4);
            int by = cy + row * (TOGGLE_H + 2);
            boolean hover = mx >= bx && mx <= bx + btnW && my >= by && my <= by + TOGGLE_H;
            if (hover) hovered = i;
            g.fill(bx, by, bx + btnW, by + TOGGLE_H,
                hover ? ColorUtil.withAlpha(accent, 30) : ColorUtil.withAlpha(Theme.MUTED, 60));
            Panel.drawHollowRect(g, bx, by, btnW, TOGGLE_H, hover ? accent : Theme.BORDER_LIGHT);
            g.text(font, Component.literal(qb.label), bx + 6, by + 5, hover ? accent : Theme.FOREGROUND);
        }
        if (hovered != -1 && hovered != lastHoveredThemeBtn) UiSounds.hover();
        lastHoveredThemeBtn = hovered;
        int rows = (items.size() + perRow - 1) / perRow;
        return cy + rows * (TOGGLE_H + 2);
    }

    private int renderProfileSection(GuiGraphicsExtractor g, Font font, String title,
                                      List<QuickButton> items, int dx, int cy, int sw, int accent, float delta) {
        g.text(font, Component.literal(title).withStyle(s -> s.withBold(true)),
                dx + 16, cy, accent);
        cy += 18;
        int perRow = 2;
        int btnW = (sw - 4) / perRow;
        int hovered = -1;
        for (int i = 0; i < items.size(); i++) {
            QuickButton qb = items.get(i);
            int col = i % perRow;
            int row = i / perRow;
            int bx = dx + 16 + col * (btnW + 4);
            int by = cy + row * (TOGGLE_H + 2);
            boolean hover = mx >= bx && mx <= bx + btnW && my >= by && my <= by + TOGGLE_H;
            if (hover) hovered = i;
            g.fill(bx, by, bx + btnW, by + TOGGLE_H,
                hover ? ColorUtil.withAlpha(accent, 30) : ColorUtil.withAlpha(Theme.MUTED, 60));
            Panel.drawHollowRect(g, bx, by, btnW, TOGGLE_H, hover ? accent : Theme.BORDER_LIGHT);
            g.text(font, Component.literal(qb.label), bx + 6, by + 5, hover ? accent : Theme.FOREGROUND);
        }
        if (hovered != -1 && hovered != lastHoveredProfileBtn) UiSounds.hover();
        lastHoveredProfileBtn = hovered;
        int rows = (items.size() + perRow - 1) / perRow;
        return cy + rows * (TOGGLE_H + 2);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!open || button != 0) return false;
        float progress = slideAnim.get();
        if (progress < 0.01f) return false;
        int drawerW = (int) (DRAWER_W * Anim.easeOutCubic(progress));
        int dx = this.x + this.h - drawerW;

        if (mx < dx || mx > dx + drawerW || my < this.y || my > this.y + h) return false;

        int cy = this.y + 44;
        int sw = drawerW - 32;
        int accent = Theme.getAnimatedAccent();

        for (QuickToggle qt : toggles) {
            int iy = cy;
            if (mx >= dx + 16 && mx <= dx + 16 + sw && my >= iy && my <= iy + TOGGLE_H) {
                UiSounds.click();
                CrestModules.toggle(qt.moduleId);
                return true;
            }
            cy += TOGGLE_H + 2;
        }
        cy += SECTION_GAP + 18;

        int perRow = 2;
        int btnW = (sw - 4) / perRow;
        for (int i = 0; i < themeButtons.size(); i++) {
            QuickButton qb = themeButtons.get(i);
            int col = i % perRow;
            int row = i / perRow;
            int bx = dx + 16 + col * (btnW + 4);
            int by = cy + row * (TOGGLE_H + 2);
            if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + TOGGLE_H) {
                UiSounds.click();
                qb.action.run();
                return true;
            }
        }
        int themeRows = (themeButtons.size() + perRow - 1) / perRow;
        cy += themeRows * (TOGGLE_H + 2) + SECTION_GAP + 18;

        for (int i = 0; i < profileButtons.size(); i++) {
            QuickButton qb = profileButtons.get(i);
            int col = i % perRow;
            int row = i / perRow;
            int bx = dx + 16 + col * (btnW + 4);
            int by = cy + row * (TOGGLE_H + 2);
            if (mx >= bx && mx <= bx + btnW && my >= by && my <= by + TOGGLE_H) {
                UiSounds.click();
                qb.action.run();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my) { return false; }
    @Override
    public boolean keyPressed(int key, int scan, int mods) { return false; }
    @Override
    public boolean charTyped(int codepoint, int mods) { return false; }
}
