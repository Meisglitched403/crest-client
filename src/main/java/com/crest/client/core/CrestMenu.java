package com.crest.client.core;

import com.crest.client.music.MusicModule;
import com.crest.client.music.MusicScreen;
import com.crest.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

public class CrestMenu extends Screen {
    private static final int MARGIN = 40;
    private static final int SIDEBAR_W = 200;
    private static final int CARD_H = 90;
    private static final int CARD_GAP = 12;
    private static final int SEARCH_H = 36;
    private static final int TOGGLE_W = 44;
    private static final int TOGGLE_H = 24;

    private String selectedCategory;
    private int hoveredIndex = -1;
    private float scrollOffset = 0;
    private float scrollTarget = 0;
    private int maxScroll = 0;
    private String scrollFilter = "";

    private int pX, pY, pW, pH;
    private int contentX, contentY, contentW;
    private int gridY, gridH, cols;

    private final Animated openAnim = new Animated(0f, 12f);
    private final Map<String, Animated> cardHoverAnims = new HashMap<>();
    private final Map<String, Animated> toggleAnims = new HashMap<>();
    private int mx, my;

    protected CrestMenu() { super(Component.literal("")); }

    @Override
    protected void init() {
        Theme.load();
        pX = MARGIN;
        pY = MARGIN;
        pW = width - MARGIN * 2;
        pH = height - MARGIN * 2;

        List<String> cats = Cats();
        selectedCategory = cats.isEmpty() ? null : cats.get(0);

        contentX = pX + SIDEBAR_W;
        contentY = pY + 30;
        contentW = pW - SIDEBAR_W - 30;
        gridY = contentY + SEARCH_H + 16;
        gridH = pH - (gridY - pY) - 16;
        cols = Math.max(2, contentW / 200);

        openAnim.setImmediate(0f);
        openAnim.set(1f);
    }

    // ---- Render ----

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        this.mx = mx; this.my = my;
        Theme.tick(delta);
        openAnim.tick(delta);
        float open = openAnim.get();
        if (open < 0.01) return;

        g.fill(0, 0, width, height, ColorUtil.withAlpha(Theme.GLASS_BG, (int) (220 * open)));

        int wy = (int) ((1 - open) * -12);
        g.pose().pushMatrix();
        g.pose().translate(0, wy);

        Panel.draw(g, pX, pY, pW, pH, Theme.GLASS_BG);
        Panel.drawHollowRect(g, pX, pY, pW, pH, Theme.BORDER_LIGHT);

        renderSidebar(g, delta);
        renderContent(g, delta);

        g.pose().popMatrix();
    }

    private void renderSidebar(GuiGraphicsExtractor g, float delta) {
        g.fill(pX, pY, pX + SIDEBAR_W, pY + pH, Theme.SIDEBAR_BG);
        g.fill(pX + SIDEBAR_W, pY, pX + SIDEBAR_W + 1, pY + pH, Theme.BORDER_LIGHT);

        int accent = Theme.getAnimatedAccent();
        g.text(font, "Crest", pX + 20, pY + 30, Theme.FOREGROUND);
        int crestW = font.width("Crest");
        g.text(font, ".", pX + 20 + crestW, pY + 30, accent);

        List<String> cats = Cats();
        int tabY = pY + 70;
        for (int i = 0; i < cats.size(); i++) {
            String cat = cats.get(i);
            int th = 30;
            boolean selected = cat.equals(selectedCategory);
            boolean hover = mx >= pX + 20 && mx <= pX + SIDEBAR_W - 20
                         && my >= tabY && my <= tabY + th;

            if (selected) {
                g.fill(pX + 20, tabY, pX + SIDEBAR_W - 20, tabY + th, ColorUtil.withAlpha(accent, 38));
                g.fill(pX + 20, tabY, pX + 23, tabY + th, accent);
            } else if (hover) {
                g.fill(pX + 20, tabY, pX + SIDEBAR_W - 20, tabY + th, 0x08FFFFFF);
            }

            int textColor = selected ? accent : (hover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND);
            g.text(font, Component.literal(cat), pX + 35, tabY + 9, textColor);
            tabY += 35;
        }

        // Music button
        int musicY = pY + pH - 90;
        boolean musicHover = mx >= pX + 20 && mx <= pX + SIDEBAR_W - 20
                         && my >= musicY && my <= musicY + 26;
        if (musicHover) {
            g.fill(pX + 20, musicY, pX + SIDEBAR_W - 20, musicY + 26, ColorUtil.withAlpha(accent, 30));
        }
        g.text(font, "\u266B Music", pX + 20, musicY + 6, musicHover ? accent : Theme.MUTED_FOREGROUND);

        // HUD Edit button (draggable GUI)
        int hudY = pY + pH - 60;
        boolean hudHover = mx >= pX + 20 && mx <= pX + SIDEBAR_W - 20
                        && my >= hudY && my <= hudY + 26;
        if (hudHover) {
            g.fill(pX + 20, hudY, pX + SIDEBAR_W - 20, hudY + 26, ColorUtil.withAlpha(accent, 30));
        }
        String hudText = "\u2699 Edit HUD";
        g.text(font, hudText, pX + 20, hudY + 6, hudHover ? accent : Theme.MUTED_FOREGROUND);

        // Close button
        int closeY = pY + pH - 30;
        boolean closeH = mx >= pX + 20 && mx <= pX + SIDEBAR_W - 20
                      && my >= closeY && my <= closeY + 26;
        if (closeH) {
            g.fill(pX + 20, closeY, pX + SIDEBAR_W - 20, closeY + 26, ColorUtil.withAlpha(Theme.DESTRUCTIVE, 30));
        }
        g.text(font, "\u00D7", pX + 20, closeY + 6, closeH ? Theme.DESTRUCTIVE : Theme.MUTED_FOREGROUND);
    }

    private void renderContent(GuiGraphicsExtractor g, float delta) {
        renderSearchBar(g);

        if (selectedCategory == null) return;
        List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
        int total = mods.size();

        int cardW = (contentW - (cols - 1) * CARD_GAP) / cols;
        int gridRows = (total + cols - 1) / cols;
        int maxVis = Math.max(1, gridH / (CARD_H + CARD_GAP));
        int frameMaxScroll = Math.max(0, gridRows - maxVis);
        if (frameMaxScroll != maxScroll) {
            maxScroll = frameMaxScroll;
            scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
            scrollOffset = Anim.clamp(scrollOffset, 0, maxScroll);
        }
        scrollOffset += (scrollTarget - scrollOffset) * 0.35f;
        if (Math.abs(scrollOffset - scrollTarget) < 0.01f) scrollOffset = scrollTarget;

        g.enableScissor(contentX, gridY, contentX + contentW, gridY + gridH);

        int baseY = gridY - (int) (scrollOffset * (CARD_H + CARD_GAP));

        hoveredIndex = -1;
        for (int i = 0; i < total; i++) {
            int r = i / cols;
            int c = i % cols;
            int cx = contentX + c * (cardW + CARD_GAP);
            int cy = baseY + r * (CARD_H + CARD_GAP);
            if (cy > gridY + gridH + CARD_H) break;
            if (cy + CARD_H < gridY) continue;
            renderCard(g, mods.get(i), i, cx, cy, cardW, CARD_H, delta);
        }
        g.disableScissor();

        if (maxScroll > 0) {
            float thumbH = (float) maxVis / gridRows * gridH;
            float thumbY = (scrollOffset / Math.max(1, maxScroll)) * (gridH - thumbH);
            g.fill(contentX + contentW - 5, gridY + (int) thumbY,
                   contentX + contentW - 3, gridY + (int) (thumbY + thumbH), Theme.getAnimatedAccent());
        }

        if (!scrollFilter.isEmpty()) {
            g.text(font, "\"" + scrollFilter + "\"", contentX + 8, gridY + gridH + 6, Theme.MUTED_FOREGROUND);
        }
    }

    private void renderSearchBar(GuiGraphicsExtractor g) {
        int sbY = contentY;
        g.fillGradient(contentX, sbY, contentX + contentW, sbY + SEARCH_H, 0x4D000000, 0x26000000);
        Panel.drawHollowRect(g, contentX, sbY, contentW, SEARCH_H, Theme.BORDER_LIGHT);
        String placeholder = scrollFilter.isEmpty() ? "Search modules..." : scrollFilter;
        int fg = scrollFilter.isEmpty() ? Theme.MUTED_FOREGROUND : Theme.FOREGROUND;
        g.text(font, Component.literal(placeholder), contentX + 15, sbY + 12, fg);
    }

    private void renderCard(GuiGraphicsExtractor g, CrestModule mod, int idx, int cx, int cy, int cw, int ch, float delta) {
        boolean hover = mx >= cx && mx <= cx + cw && my >= cy && my <= cy + ch;
        boolean enabled = CrestModules.isEnabled(mod.getId());
        if (hover) hoveredIndex = idx;

        // Animated hover
        String id = mod.getId();
        Animated ha = cardHoverAnims.computeIfAbsent(id, k -> new Animated(0f, 14f));
        ha.set(hover ? 1f : 0f);
        ha.tick(delta);
        float hoverAmt = ha.get();

        // Animated toggle
        Animated ta = toggleAnims.computeIfAbsent(id, k -> new Animated(0f, 12f));
        ta.set(enabled ? 1f : 0f);
        ta.tick(delta);
        float toggleAmt = ta.get();

        int accent = Theme.getAnimatedAccent();

        // Card background: gradient fill with hover brightening
        int topAlpha = (int) (Anim.lerp(21, 32, hoverAmt));
        int botAlpha = (int) (Anim.lerp(2, 5, hoverAmt));
        g.fillGradient(cx, cy, cx + cw, cy + ch,
            ColorUtil.withAlpha(0xFFFFFFFF, topAlpha),
            ColorUtil.withAlpha(0xFFFFFFFF, botAlpha));

        // Animated border: lerps from subtle to accent on hover
        int borderBase = ColorUtil.withAlpha(Theme.BORDER_LIGHT, (int) Anim.lerp(32, 120, hoverAmt));
        int borderAccent = ColorUtil.withAlpha(accent, (int) Anim.lerp(80, 200, hoverAmt));
        int borderCol = ColorUtil.lerpARGB(borderBase, borderAccent, toggleAmt * 0.3f);
        Panel.drawHollowRect(g, cx, cy, cw, ch, borderCol);

        // Accent bar on left: fades in with toggle, brightens on hover
        int barAlpha = (int) Anim.lerp(0, (int) Anim.lerp(160, 255, hoverAmt), toggleAmt);
        g.fill(cx + 2, cy + 8, cx + 5, cy + ch - 8, ColorUtil.withAlpha(accent, barAlpha));

        // Module name
        int nameMaxW = cw - 32;
        String name = font.width(mod.getName()) > nameMaxW
            ? font.plainSubstrByWidth(mod.getName(), nameMaxW - 4) + "\u2026"
            : mod.getName();
        int nameColor = ColorUtil.lerpARGB(Theme.MUTED_FOREGROUND, Theme.FOREGROUND, toggleAmt);
        g.text(font, Component.literal(name), cx + 14, cy + 14, nameColor);

        // Description
        String desc = mod.getDescription();
        if (desc != null && !desc.isEmpty()) {
            String truncated = font.width(desc) > cw - 28
                ? font.plainSubstrByWidth(desc, cw - 32) + "\u2026"
                : desc;
            int descColor = ColorUtil.lerpARGB(
                ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 120),
                Theme.MUTED_FOREGROUND, toggleAmt);
            g.text(font, Component.literal(truncated), cx + 14, cy + 32, descColor);
        }

        // Toggle switch
        drawToggle(g, cx + cw - TOGGLE_W - 12, cy + 8, enabled, toggleAmt);
    }

    private void drawToggle(GuiGraphicsExtractor g, int x, int y, boolean on, float anim) {
        int trackOff = 0x1AFFFFFF;
        int trackOn = ColorUtil.lerpARGB(0x1AFFFFFF, Theme.getAnimatedAccent(), 0.9f);
        int trackColor = ColorUtil.lerpARGB(trackOff, trackOn, anim);
        g.fillGradient(x, y, x + TOGGLE_W, y + TOGGLE_H, trackColor, ColorUtil.withAlpha(trackColor, 80));
        Panel.drawHollowRect(g, x, y, TOGGLE_W, TOGGLE_H, Theme.BORDER_LIGHT);

        int knobMinX = x + 3;
        int knobMaxX = x + TOGGLE_W - 19;
        int knobX = (int) Anim.lerp(knobMinX, knobMaxX, anim);
        int knobColor = ColorUtil.lerpARGB(Theme.MUTED_FOREGROUND, Theme.PRIMARY, anim);
        g.fill(knobX, y + 3, knobX + 16, y + TOGGLE_H - 3, knobColor);
    }

    // ---- Input ----

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (cp >= 32 && cp < 127) {
            scrollFilter += event.codepointAsString();
            scrollTarget = 0;
            scrollOffset = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !scrollFilter.isEmpty()) {
            scrollFilter = scrollFilter.substring(0, scrollFilter.length() - 1);
            scrollTarget = 0;
            scrollOffset = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN ||
            key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            int total = mods.size();
            if (total == 0) return true;
            int row = hoveredIndex >= 0 ? hoveredIndex / cols : 0;
            int col = hoveredIndex >= 0 ? hoveredIndex % cols : 0;
            switch (key) {
                case GLFW.GLFW_KEY_UP -> { if (row > 0) row--; }
                case GLFW.GLFW_KEY_DOWN -> { if ((row + 1) * cols < total) row++; }
                case GLFW.GLFW_KEY_LEFT -> {
                    if (col > 0) col--;
                    else if (row > 0) { row--; col = cols - 1; }
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (col + 1 < cols && row * cols + col + 1 < total) col++;
                    else if ((row + 1) * cols < total) { row++; col = 0; }
                }
            }
            hoveredIndex = Math.min(row * cols + col, total - 1);
            int hrow = hoveredIndex / cols;
            if (hrow < scrollTarget) scrollTarget = hrow;
            else if (hrow >= scrollTarget + Math.max(1, gridH / (CARD_H + CARD_GAP))) scrollTarget = hrow - Math.max(1, gridH / (CARD_H + CARD_GAP)) + 1;
            scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            if (hoveredIndex >= 0 && hoveredIndex < mods.size()) {
                CrestModules.setEnabled(mods.get(hoveredIndex).getId(), !CrestModules.isEnabled(mods.get(hoveredIndex).getId()));
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            List<String> cats = Cats();
            if (!cats.isEmpty()) {
                int i = cats.indexOf(selectedCategory);
                selectedCategory = cats.get((i + 1) % cats.size());
                scrollFilter = "";
                scrollTarget = scrollOffset = hoveredIndex = 0;
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mxx = event.x(), myy = event.y();
        int btn = event.buttonInfo().input();
        if (btn != 0) return super.mouseClicked(event, doubleClick);

        // Close button
        int closeY = pY + pH - 30;
        if (mxx >= pX + 20 && mxx <= pX + SIDEBAR_W - 20 && myy >= closeY && myy <= closeY + 26) {
            onClose();
            return true;
        }

        // Music button
        int musicY = pY + pH - 90;
        if (mxx >= pX + 20 && mxx <= pX + SIDEBAR_W - 20 && myy >= musicY && myy <= musicY + 26) {
            minecraft.setScreen(new MusicScreen(MusicModule.getPlayer()));
            return true;
        }

        // HUD Edit button
        int hudY = pY + pH - 60;
        if (mxx >= pX + 20 && mxx <= pX + SIDEBAR_W - 20 && myy >= hudY && myy <= hudY + 26) {
            minecraft.setScreen(new HudEditScreen());
            return true;
        }

        // Sidebar tabs
        List<String> cats = Cats();
        int tabY = pY + 70;
        for (int i = 0; i < cats.size(); i++) {
            int th = 30;
            if (mxx >= pX + 20 && mxx <= pX + SIDEBAR_W - 20 && myy >= tabY && myy <= tabY + th) {
                if (!cats.get(i).equals(selectedCategory)) {
                    selectedCategory = cats.get(i);
                    scrollTarget = scrollOffset = hoveredIndex = 0;
                }
                return true;
            }
            tabY += 35;
        }

        // Cards
        if (selectedCategory != null) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            int cardW = (contentW - (cols - 1) * CARD_GAP) / cols;
            int baseY = gridY - (int) (scrollOffset * (CARD_H + CARD_GAP));

            for (int i = 0; i < mods.size(); i++) {
                int r = i / cols;
                int c = i % cols;
                int cx = contentX + c * (cardW + CARD_GAP);
                int cy = baseY + r * (CARD_H + CARD_GAP);

                if (mxx >= cx && mxx <= cx + cardW && myy >= cy && myy <= cy + CARD_H) {
                    int toggleX = cx + cardW - TOGGLE_W - 12;
                    int toggleY = cy + 8;
                    if (mxx >= toggleX && mxx <= toggleX + TOGGLE_W && myy >= toggleY && myy <= toggleY + TOGGLE_H) {
                        CrestModules.setEnabled(mods.get(i).getId(), !CrestModules.isEnabled(mods.get(i).getId()));
                    } else {
                        CrestModule mod = mods.get(i);
                        Screen config = mod.createConfigScreen(this);
                        minecraft.setScreen(config != null ? config : new ModuleDetailScreen(mod, this));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (selectedCategory == null) return false;
        List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
        int total = mods.size();
        if (total == 0) { maxScroll = 0; return true; }
        int gridRows = (total + cols - 1) / cols;
        int maxVis = Math.max(1, gridH / (CARD_H + CARD_GAP));
        maxScroll = Math.max(0, gridRows - maxVis);
        if (maxScroll == 0) return true;
        scrollTarget = Anim.clamp(scrollTarget - (float) deltaY, 0, maxScroll);
        return true;
    }

    private List<String> Cats() { return CrestModules.getCategories(); }

    private List<CrestModule> filterBySearch(List<CrestModule> mods) {
        if (scrollFilter.isEmpty()) return mods;
        String q = scrollFilter.toLowerCase();
        return mods.stream()
                .filter(m -> m.getName().toLowerCase().contains(q) || m.getId().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public static void open() { Minecraft.getInstance().setScreen(new CrestMenu()); }

    @Override public void onClose() { minecraft.setScreen(null); }
    @Override public boolean isPauseScreen() { return false; }
}
