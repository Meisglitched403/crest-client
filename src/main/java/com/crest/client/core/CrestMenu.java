package com.crest.client.core;

import com.crest.client.music.MusicModule;
import com.crest.client.music.MusicScreen;
import com.crest.client.ui.*;
import com.crest.client.ui.layout.LayoutEngine;
import com.crest.client.ui.layout.LayoutNode;
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
    private static final int SIDEBAR_W_DEFAULT = 200;
    private static final int SIDEBAR_W_COMPACT = 60;
    private static final int CARD_GAP_DEFAULT = 12;
    private static final int CARD_GAP_COMPACT = 8;
    private static final int CARD_MIN_W = 180;
    private static final int SEARCH_H = 36;
    private static final int TOGGLE_W = 44;
    private static final int TOGGLE_H = 24;
    private static final int BOTTOM_ZONE = 180;
    private static final int TAB_H = 30;
    private static final int TAB_STEP = 35;

    private static final String FAV_CAT = "Favorites";

    private String selectedCategory;
    private int hoveredIndex = -1;
    private float scrollOffset = 0;
    private float scrollTarget = 0;
    private int maxScroll = 0;
    private final SearchBar searchBar = new SearchBar(q -> {
        scrollTarget = 0;
        scrollOffset = 0;
    }, "Search modules...");
    private final DropdownMenu filterMenu = new DropdownMenu()
        .add("All modules", () -> {})
        .add("Enabled only", () -> {})
        .add("Disabled only", () -> {})
        .addSeparator()
        .add("Sort: A-Z", () -> {})
        .add("Sort: Z-A", () -> {});
    private int filterBtnX, filterBtnY;
    private final QuickSettingsDrawer quickSettings = new QuickSettingsDrawer();
    private int gearBtnX;
    private boolean draggingScrollbar;
    private int scrollbarDragStartY;
    private float scrollbarDragStartOffset;

    private float sideScroll = 0;
    private float sideScrollTarget = 0;
    private int sideMaxScroll = 0;
    private int lastHoveredSideBtn = -1;
    private int lastHoveredCatIdx = -1;
    private int lastHoveredCard = -1;
    private boolean lastFilterHover;
    private boolean lastGearHover;

    private int pX, pY, pW, pH;
    private int sidebarW;
    private int contentX, contentY, contentW;
    private int gridY, gridH, cols;
    private int cardH;
    private int cardGap;

    private final Animated openAnim = new Animated(0f, 12f);
    private final Map<String, Animated> cardHoverAnims = new HashMap<>();
    private final Map<String, Animated> toggleAnims = new HashMap<>();
    private final Map<String, Animated> cardOpenAnims = new HashMap<>();
    private int mx, my;
    private Breakpoints.Size currentSize = Breakpoints.Size.MD;
    private boolean sidebarCollapsed = false;

    protected CrestMenu() { super(Component.literal("")); }

    @Override
    protected void init() {
        Theme.load();
        openAnim.setImmediate(0f);
        openAnim.set(1f);
        computeLayout();
    }

    private void computeLayout() {
        Breakpoints.Size newSize = Breakpoints.getCurrentSize(width);
        if (newSize != currentSize) {
            currentSize = newSize;
        }

        pX = MARGIN;
        pY = MARGIN;
        pW = width - MARGIN * 2;
        pH = height - MARGIN * 2;

        sidebarCollapsed = Breakpoints.isXsOrSmaller(width);
        sidebarW = sidebarCollapsed ? SIDEBAR_W_COMPACT : SIDEBAR_W_DEFAULT;

        List<String> cats = Cats();
        selectedCategory = cats.isEmpty() ? null : cats.get(0);

        contentX = pX + sidebarW;
        contentY = pY + 30;
        contentW = pW - sidebarW - 30;
        gridY = contentY + SEARCH_H + 16;
        gridH = pH - (gridY - pY) - 16;

        cols = Math.max(2, LayoutEngine.computeGridCols(contentW, CARD_MIN_W, CARD_GAP_DEFAULT, 0));
        cardH = Theme.scaled(Theme.ROW_H() + 64);
        cardGap = Theme.density == Theme.Density.COMPACT ? CARD_GAP_COMPACT : CARD_GAP_DEFAULT;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        this.mx = mx; this.my = my;
        Theme.tick(delta);
        openAnim.tick(delta);
        float open = openAnim.get();
        if (open < 0.01) return;

        if (Breakpoints.getCurrentSize(width) != currentSize) {
            computeLayout();
        }

        g.fill(0, 0, width, height, ColorUtil.withAlpha(Theme.GLASS_BG, (int) (Theme.glassOpacity * open)));

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
        g.fill(pX, pY, pX + sidebarW, pY + pH, Theme.SIDEBAR_BG);
        g.fill(pX + sidebarW, pY, pX + sidebarW + 1, pY + pH, Theme.BORDER_LIGHT);

        int accent = Theme.getAnimatedAccent();
        int titleY = pY + 30;
        String crestDot = "Crest.";
        int titleX = pX + (sidebarW - font.width(crestDot)) / 2;
        g.text(font, Component.literal("Crest"), titleX, titleY, Theme.FOREGROUND);
        int crestW = font.width("Crest");
        g.text(font, Component.literal("."), titleX + crestW, titleY, accent);

        List<String> cats = Cats();
        int tabAreaTop = pY + 56;
        int tabAreaBottom = pY + pH - BOTTOM_ZONE;

        int contentH = cats.size() * TAB_STEP;
        int areaH = Math.max(0, tabAreaBottom - tabAreaTop);
        sideMaxScroll = Math.max(0, contentH - areaH);
        sideScrollTarget = Anim.clamp(sideScrollTarget, 0, sideMaxScroll);
        sideScroll += (sideScrollTarget - sideScroll) * 0.35f;
        if (Math.abs(sideScroll - sideScrollTarget) < 0.01f) sideScroll = sideScrollTarget;
        int scroll = (int) sideScroll;

        g.enableScissor(pX, tabAreaTop, pX + sidebarW, tabAreaBottom);
        int tabY = tabAreaTop - scroll;
        int hoveredCat = -1;
        for (int i = 0; i < cats.size(); i++) {
            String cat = cats.get(i);
            boolean selected = cat.equals(selectedCategory);
            boolean hover = mx >= pX + 20 && mx <= pX + sidebarW - 20
                         && my >= tabY && my <= tabY + TAB_H;

            if (selected) {
                g.fill(pX + 20, tabY, pX + sidebarW - 20, tabY + TAB_H, ColorUtil.withAlpha(accent, 38));
                g.fill(pX + 20, tabY, pX + 23, tabY + TAB_H, accent);
            } else if (hover) {
                g.fill(pX + 20, tabY, pX + sidebarW - 20, tabY + TAB_H, Theme.hoverTint());
                hoveredCat = i;
            }

            int textColor = selected ? accent : (hover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND);
            String label = sidebarCollapsed ? cat.substring(0, 1).toUpperCase() : cat;
            g.text(font, Component.literal(label), pX + 35, tabY + 9, textColor);
            tabY += TAB_STEP;
        }
        if (hoveredCat != -1 && hoveredCat != lastHoveredCatIdx) UiSounds.hover();
        lastHoveredCatIdx = hoveredCat;
        g.disableScissor();

        if (sideMaxScroll > 0) {
            int thumbH = Math.max(20, (int) ((float) areaH / contentH * areaH));
            int thumbY = tabAreaTop + (int) ((float) scroll / sideMaxScroll * (areaH - thumbH));
            g.fill(pX + sidebarW - 4, thumbY, pX + sidebarW - 2, thumbY + thumbH,
                    ColorUtil.withAlpha(Theme.BORDER_LIGHT, 180));
        }

        int[] bY = new int[6];
        int gap = 6;
        int bh = (BOTTOM_ZONE - gap * 5) / 6;
        for (int i = 0; i < 6; i++) {
            bY[i] = pY + pH - BOTTOM_ZONE + i * (bh + gap) + 4;
        }
        int closeY = bY[0], hudY = bY[1], packsY = bY[2], musicY = bY[3], themeY = bY[4], profY = bY[5];

        int hovered = -1;
        for (int i = 0; i < 6; i++) {
            if (mx >= pX + 20 && mx <= pX + sidebarW - 20 && my >= bY[i] && my <= bY[i] + bh) hovered = i;
        }
        if (hovered != -1 && hovered != lastHoveredSideBtn) UiSounds.hover();
        lastHoveredSideBtn = hovered;

        drawSideButton(g, accent, "\u25B3 Packs", packsY, bh);
        drawSideButton(g, accent, "\u25A3 Profiles", profY, bh);
        drawSideButton(g, accent, "\u25A4 Theme", themeY, bh);
        drawSideButton(g, accent, "\u266B Music", musicY, bh);
        drawSideButton(g, accent, "\u2699 HUD", hudY, bh);
        drawSideButton(g, Theme.DESTRUCTIVE, "\u00D7", closeY, bh);
    }

    private void drawSideButton(GuiGraphicsExtractor g, int accent, String label, int y, int bh) {
        boolean hover = mx >= pX + 20 && mx <= pX + sidebarW - 20 && my >= y && my <= y + bh;
        int col = label.equals("\u00D7") ? Theme.DESTRUCTIVE : accent;
        if (hover) {
            g.fill(pX + 20, y, pX + sidebarW - 20, y + bh, ColorUtil.withAlpha(col, 30));
        }
        g.text(font, Component.literal(label), pX + 20, y + bh / 2 - 4, hover ? accent : Theme.MUTED_FOREGROUND);
    }

    private void renderContent(GuiGraphicsExtractor g, float delta) {
        renderSearchBar(g);

        if (selectedCategory == null) return;
        List<CrestModule> mods = filterBySearch(modulesForCategory());
        int total = mods.size();

        int cardW = (contentW - (cols - 1) * cardGap) / cols;
        int gridRows = Math.max(1, (total + cols - 1) / cols);
        int maxVis = Math.max(1, gridH / (cardH + cardGap));
        int frameMaxScroll = Math.max(0, gridRows - maxVis);
        if (frameMaxScroll != maxScroll) {
            maxScroll = frameMaxScroll;
            scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
            scrollOffset = Anim.clamp(scrollOffset, 0, maxScroll);
        }
        scrollOffset += (scrollTarget - scrollOffset) * 0.35f;
        if (Math.abs(scrollOffset - scrollTarget) < 0.01f) scrollOffset = scrollTarget;

        g.enableScissor(contentX, gridY, contentX + contentW, gridY + gridH);

        int baseY = gridY - (int) (scrollOffset * (cardH + cardGap));
        int firstRow = Math.max(0, (int) scrollOffset);
        int lastRow = Math.min(gridRows - 1, firstRow + maxVis + 1);
        int firstIdx = Math.min(total - 1, firstRow * cols);
        int lastIdx = Math.min(total - 1, (lastRow + 1) * cols - 1);

        hoveredIndex = -1;
        if (total == 0) { g.disableScissor(); return; }
        for (int i = firstIdx; i <= lastIdx; i++) {
            int r = i / cols;
            int c = i % cols;
            int cx = contentX + c * (cardW + cardGap);
            int cy = baseY + r * (cardH + cardGap);
            renderCard(g, mods.get(i), i, cx, cy, cardW, cardH, delta);
        }

        if (mx >= contentX && mx <= contentX + contentW && my >= gridY && my <= gridY + gridH) {
            int col = (mx - contentX) / (cardW + cardGap);
            int row = (int) ((my - gridY + scrollOffset * (cardH + cardGap)) / (cardH + cardGap));
            int idx = row * cols + col;
            if (idx >= 0 && idx < total) {
                hoveredIndex = idx;
            }
        }

        if (hoveredIndex != -1 && hoveredIndex != lastHoveredCard) UiSounds.hover();
        lastHoveredCard = hoveredIndex;

        g.disableScissor();

        if (maxScroll > 0) {
            float thumbH = (float) maxVis / gridRows * gridH;
            float thumbY = (scrollOffset / Math.max(1, maxScroll)) * (gridH - thumbH);
            int tx = contentX + contentW - 6;
            int ty = gridY + (int) thumbY;
            int th = Math.max(8, (int) thumbH);
            boolean overTrack = mx >= tx - 3 && mx <= tx + 7
                             && my >= gridY && my <= gridY + gridH;
            boolean overThumb = mx >= tx - 3 && mx <= tx + 7
                             && my >= ty && my <= ty + th;
            int trackAlpha = (overTrack || draggingScrollbar) ? 120 : 60;
            g.fill(tx - 2, gridY, tx + 6, gridY + gridH, ColorUtil.withAlpha(Theme.GLASS_BG, trackAlpha));
            int thumbColor = overThumb || draggingScrollbar ? Theme.getAnimatedAccent() : ColorUtil.withAlpha(Theme.BORDER_LIGHT, 200);
            g.fill(tx, ty, tx + 4, ty + th, thumbColor);
        }

        if (!searchBar.getText().isEmpty()) {
            g.text(font, "\"" + searchBar.getText() + "\"", contentX + 8, gridY + gridH + 6, Theme.MUTED_FOREGROUND);
        }

        quickSettings.render(g, font, pX, pY, pW, mx, my, delta);
    }

    private void renderSearchBar(GuiGraphicsExtractor g) {
        int sbW = contentW - 44;
        searchBar.render(g, font, contentX, contentY, sbW, mx, my, 0);
        filterBtnX = contentX + sbW + 4;
        filterBtnY = contentY;
        boolean filterHover = mx >= filterBtnX && mx <= filterBtnX + 36 && my >= filterBtnY && my <= filterBtnY + 36;
        int fbFill = filterHover ? ColorUtil.withAlpha(Theme.MUTED, 120) : ColorUtil.withAlpha(Theme.SIDEBAR_BG, 180);
        g.fill(filterBtnX, filterBtnY, filterBtnX + 36, filterBtnY + 36, fbFill);
        Panel.drawHollowRect(g, filterBtnX, filterBtnY, 36, 36, filterHover ? Theme.getAnimatedAccent() : Theme.BORDER_LIGHT);
        g.text(font, Component.literal("\u22EE"), filterBtnX + 10, filterBtnY + 10, filterHover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND);
        filterMenu.render(g, font, mx, my);

        gearBtnX = filterBtnX + 40;
        boolean gearHover = mx >= gearBtnX && mx <= gearBtnX + 36 && my >= filterBtnY && my <= filterBtnY + 36;
        if (gearHover != lastGearHover) { if (gearHover) UiSounds.hover(); lastGearHover = gearHover; }
        int gearFill = gearHover ? ColorUtil.withAlpha(Theme.MUTED, 120) : ColorUtil.withAlpha(Theme.SIDEBAR_BG, 180);
        g.fill(gearBtnX, filterBtnY, gearBtnX + 36, filterBtnY + 36, gearFill);
        Panel.drawHollowRect(g, gearBtnX, filterBtnY, 36, 36, gearHover ? Theme.getAnimatedAccent() : Theme.BORDER_LIGHT);
        g.text(font, Component.literal("\u2699"), gearBtnX + 8, filterBtnY + 10, gearHover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND);
        if (filterHover != lastFilterHover) { if (filterHover) UiSounds.hover(); lastFilterHover = filterHover; }
    }

    private void renderCard(GuiGraphicsExtractor g, CrestModule mod, int idx, int cx, int cy, int cw, int ch, float delta) {
        boolean hover = mx >= cx && mx <= cx + cw && my >= cy && my <= cy + ch;
        boolean enabled = CrestModules.isEnabled(mod.getId());
        if (hover) hoveredIndex = idx;

        String id = mod.getId();
        Animated ha = cardHoverAnims.computeIfAbsent(id, k -> new Animated(0f, 14f));
        ha.set(hover ? 1f : 0f);
        ha.tick(delta);
        float hoverAmt = ha.get();

        Animated ta = toggleAnims.computeIfAbsent(id, k -> new Animated(0f, 12f));
        ta.set(enabled ? 1f : 0f);
        ta.tick(delta);

        Animated oa = cardOpenAnims.computeIfAbsent(id, k -> new Animated(0f, 14f));
        oa.set(1f);
        oa.tick(delta);
        float enter = Anim.clamp(openAnim.get() * 1.4f - (idx % cols) * 0.06f, 0f, 1f) * oa.get();
        if (enter < 0.01f) return;

        g.pose().pushMatrix();
        g.pose().translate(cx + cw / 2f, cy + ch / 2f);
        g.pose().scale(0.92f + 0.08f * enter);
        g.pose().translate(-(cx + cw / 2f), -(cy + ch / 2f));
        float toggleAmt = ta.get();

        int accent = Theme.getAnimatedAccent();

        int topAlpha = (int) (Anim.lerp(21, 32, hoverAmt));
        int botAlpha = (int) (Anim.lerp(2, 5, hoverAmt));
        g.fillGradient(cx, cy, cx + cw, cy + ch,
            ColorUtil.withAlpha(0xFFFFFFFF, topAlpha),
            ColorUtil.withAlpha(0xFFFFFFFF, botAlpha));

        int borderBase = ColorUtil.withAlpha(Theme.BORDER_LIGHT, (int) Anim.lerp(32, 120, hoverAmt));
        int borderAccent = ColorUtil.withAlpha(accent, (int) Anim.lerp(80, 200, hoverAmt));
        int borderCol = ColorUtil.lerpARGB(borderBase, borderAccent, toggleAmt * 0.3f);
        Panel.drawHollowRect(g, cx, cy, cw, ch, borderCol);

        int barAlpha = (int) Anim.lerp(0, (int) Anim.lerp(160, 255, hoverAmt), toggleAmt);
        g.fill(cx + 2, cy + 8, cx + 5, cy + ch - 8, ColorUtil.withAlpha(accent, barAlpha));

        int nameMaxW = cw - 32;
        String name = font.width(mod.getName()) > nameMaxW
            ? font.plainSubstrByWidth(mod.getName(), nameMaxW - 4) + "\u2026"
            : mod.getName();
        int nameColor = ColorUtil.lerpARGB(Theme.MUTED_FOREGROUND, Theme.FOREGROUND, toggleAmt);
        g.text(font, Component.literal(name), cx + 14, cy + 14, nameColor);

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

        drawToggle(g, cx + cw - TOGGLE_W - 12, cy + 8, enabled, toggleAmt);

        int[] sr = starRect(cx, cy, cw, ch);
        boolean starHover = mx >= sr[0] && mx <= sr[0] + sr[2] && my >= sr[1] && my <= sr[1] + sr[3];
        int starCol = isFavorite(mod.getId())
                ? ColorUtil.lerpARGB(Theme.MUTED_FOREGROUND, Theme.getAnimatedAccent(), 1f)
                : (starHover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND);
        g.text(font, Component.literal(isFavorite(mod.getId()) ? "\u2605" : "\u2606"),
                sr[0], sr[1], starCol);

        g.pose().popMatrix();
    }

    private int[] starRect(int cx, int cy, int cw, int ch) {
        int s = 16;
        return new int[]{ cx + 4, cy + ch - s - 4, s, s };
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

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBar.charTyped(event.codepoint(), 0)) {
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
        if (key == GLFW.GLFW_KEY_BACKSPACE && !searchBar.getText().isEmpty()) {
            searchBar.keyPressed(key, 0, 0);
            scrollTarget = 0;
            scrollOffset = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN ||
            key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            List<CrestModule> mods = filterBySearch(modulesForCategory());
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
            else if (hrow >= scrollTarget + Math.max(1, gridH / (cardH + cardGap))) scrollTarget = hrow - Math.max(1, gridH / (cardH + cardGap)) + 1;
            scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            List<CrestModule> mods = filterBySearch(modulesForCategory());
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
                searchBar.setText("");
                scrollTarget = scrollOffset = hoveredIndex = 0;
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mxx = event.x(), myy = event.y();
        int btn = event.buttonInfo().input();
        if (btn != 0) return super.mouseClicked(event, doubleClick);

        if (filterMenu.mouseClicked(mxx, myy, btn)) return true;

        if (mxx >= filterBtnX && mxx <= filterBtnX + 36 && myy >= filterBtnY && myy <= filterBtnY + 36) {
            UiSounds.click();
            filterMenu.toggle(filterBtnX, filterBtnY + 36, 36);
            return true;
        }
        if (mxx >= gearBtnX && mxx <= gearBtnX + 36 && myy >= filterBtnY && myy <= filterBtnY + 36) {
            UiSounds.click();
            quickSettings.toggle();
            return true;
        }
        if (quickSettings.mouseClicked(mxx, myy, btn)) {
            UiSounds.click();
            return true;
        }

        int tx = contentX + contentW - 6;
        if (selectedCategory != null && maxScroll > 0 && mxx >= tx - 3 && mxx <= tx + 7) {
            if (myy >= gridY && myy <= gridY + gridH) {
                List<CrestModule> mods = filterBySearch(modulesForCategory());
                int total = mods.size();
                int gridRows = Math.max(1, (total + cols - 1) / cols);
                int mv = Math.max(1, gridH / (cardH + cardGap));
                float thumbH = (float) mv / gridRows * gridH;
                float thumbY = (scrollOffset / Math.max(1, maxScroll)) * (gridH - thumbH);
                if (myy >= gridY + thumbY && myy <= gridY + thumbY + thumbH) {
                    draggingScrollbar = true;
                    scrollbarDragStartY = (int) myy;
                    scrollbarDragStartOffset = scrollOffset;
                } else if (maxScroll > 0) {
                    scrollTarget = Anim.clamp((float) (myy - gridY) / gridH * maxScroll, 0, maxScroll);
                }
                return true;
            }
        }

        if (searchBar.mouseClicked(mxx, myy, 0)) {
            scrollTarget = scrollOffset = 0;
            return true;
        }

        int[] bY = new int[6];
        int gap = 6;
        int bh = (BOTTOM_ZONE - gap * 5) / 6;
        for (int i = 0; i < 6; i++) {
            bY[i] = pY + pH - BOTTOM_ZONE + i * (bh + gap) + 4;
        }
        int closeY = bY[0], hudY = bY[1], packsY = bY[2], musicY = bY[3], themeY = bY[4], profY = bY[5];

        if (mxx >= pX + 20 && mxx <= pX + sidebarW - 20 && myy >= closeY && myy <= closeY + bh) {
            UiSounds.click();
            onClose();
            return true;
        }
        if (mxx >= pX + 20 && mxx <= pX + sidebarW - 20 && myy >= themeY && myy <= themeY + bh) {
            UiSounds.click();
            minecraft.setScreen(new ThemeEditorScreen(this));
            return true;
        }
        if (mxx >= pX + 20 && mxx <= pX + sidebarW - 20 && myy >= profY && myy <= profY + bh) {
            UiSounds.click();
            minecraft.setScreen(new ProfileScreen(this));
            return true;
        }
        if (mxx >= pX + 20 && mxx <= pX + sidebarW - 20 && myy >= musicY && myy <= musicY + bh) {
            UiSounds.click();
            minecraft.setScreen(new MusicScreen(MusicModule.getPlayer()));
            return true;
        }
        if (mxx >= pX + 20 && mxx <= pX + sidebarW - 20 && myy >= packsY && myy <= packsY + bh) {
            UiSounds.click();
            minecraft.setScreen(new ResourcePackBrowserScreen(this));
            return true;
        }
        if (mxx >= pX + 20 && mxx <= pX + sidebarW - 20 && myy >= hudY && myy <= hudY + bh) {
            UiSounds.click();
            minecraft.setScreen(new HudEditScreen());
            return true;
        }

        List<String> cats = Cats();
        int tabAreaTop = pY + 56;
        int tabAreaBottom = pY + pH - BOTTOM_ZONE;
        int scroll = (int) sideScroll;
        int tabY = tabAreaTop - scroll;
        for (int i = 0; i < cats.size(); i++) {
            if (tabY >= tabAreaTop && tabY + TAB_H <= tabAreaBottom
                && mxx >= pX + 20 && mxx <= pX + sidebarW - 20 && myy >= tabY && myy <= tabY + TAB_H) {
                if (!cats.get(i).equals(selectedCategory)) {
                    UiSounds.click();
                    selectedCategory = cats.get(i);
                    scrollTarget = scrollOffset = hoveredIndex = 0;
                }
                return true;
            }
            tabY += TAB_STEP;
        }

        if (selectedCategory != null) {
            List<CrestModule> mods = filterBySearch(modulesForCategory());
            int cardW = (contentW - (cols - 1) * cardGap) / cols;

            int col = (int) ((mxx - contentX) / (cardW + cardGap));
            int row = (int) ((myy - gridY + scrollOffset * (cardH + cardGap)) / (cardH + cardGap));
            int idx = row * cols + col;
            if (mxx >= contentX && mxx <= contentX + contentW && myy >= gridY && myy <= gridY + gridH
                && idx >= 0 && idx < mods.size()) {
                int cx = contentX + col * (cardW + cardGap);
                int cy = gridY + (int) (row * (cardH + cardGap) - scrollOffset * (cardH + cardGap));
                int[] sr = starRect(cx, cy, cardW, cardH);
                if (mxx >= sr[0] && mxx <= sr[0] + sr[2] && myy >= sr[1] && myy <= sr[1] + sr[3]) {
                    UiSounds.click();
                    toggleFavorite(mods.get(idx).getId());
                    return true;
                }
                int toggleX = cx + cardW - TOGGLE_W - 12;
                int toggleY = cy + 8;
                if (mxx >= toggleX && mxx <= toggleX + TOGGLE_W && myy >= toggleY && myy <= toggleY + TOGGLE_H) {
                    UiSounds.click();
                    CrestModules.setEnabled(mods.get(idx).getId(), !CrestModules.isEnabled(mods.get(idx).getId()));
                } else {
                    UiSounds.click();
                    CrestModule mod = mods.get(idx);
                    Screen config = mod.createConfigScreen(this);
                    minecraft.setScreen(config != null ? config : new ModuleDetailScreen(mod, this));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar && selectedCategory != null && maxScroll > 0) {
            float dy2 = (float) (event.y() - scrollbarDragStartY);
            float dScroll = dy2 / gridH * maxScroll;
            scrollTarget = Anim.clamp(scrollbarDragStartOffset + dScroll, 0, maxScroll);
            scrollOffset = scrollTarget;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        boolean overSidebar = mouseX >= pX && mouseX <= pX + sidebarW
                           && mouseY >= pY && mouseY <= pY + pH;
        if (overSidebar) {
            sideScrollTarget = Anim.clamp(sideScrollTarget - (float) deltaY * TAB_STEP, 0, sideMaxScroll);
            return true;
        }
        if (selectedCategory == null) return false;
        List<CrestModule> mods = filterBySearch(modulesForCategory());
        int total = mods.size();
        if (total == 0) { maxScroll = 0; return true; }
        int gridRows = (total + cols - 1) / cols;
        int maxVis = Math.max(1, gridH / (cardH + cardGap));
        maxScroll = Math.max(0, gridRows - maxVis);
        if (maxScroll == 0) return true;
        scrollTarget = Anim.clamp(scrollTarget - (float) deltaY, 0, maxScroll);
        return true;
    }

    private List<String> Cats() {
        List<String> cats = new ArrayList<>(CrestModules.getCategories());
        cats.add(0, FAV_CAT);
        return cats;
    }

    private static boolean isFavorite(String id) {
        return CrestModules.getConfigManager().getBoolean("crest_client", "fav:" + id);
    }

    private static void toggleFavorite(String id) {
        boolean next = !isFavorite(id);
        CrestModules.getConfigManager().set("crest_client", "fav:" + id, next);
        CrestModules.getConfigManager().save();
    }

    private List<CrestModule> modulesForCategory() {
        if (FAV_CAT.equals(selectedCategory)) {
            return CrestModules.getAll().values().stream()
                    .filter(m -> isFavorite(m.getId()))
                    .collect(Collectors.toList());
        }
        return CrestModules.getByCategory(selectedCategory);
    }

    private List<CrestModule> filterBySearch(List<CrestModule> mods) {
        String q = searchBar.getText();
        if (q.isEmpty()) return mods;
        return mods.stream()
                .filter(m -> SearchBar.fuzzyMatch(q, m.getName()) || SearchBar.fuzzyMatch(q, m.getId()))
                .collect(Collectors.toList());
    }

    public static void open() { Minecraft.getInstance().setScreen(new CrestMenu()); }

    @Override public void onClose() { minecraft.setScreen(null); }
    @Override public boolean isPauseScreen() { return false; }
}
