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
    private static final int SEARCH_H = 36;
    private static final int TOGGLE_W = 44;
    private static final int TOGGLE_H = 24;
    private static final int SECTION_HEADER_H = 40;
    private static final int MODULE_ROW_H = 44;
    private static final int SECTION_GAP = 10;
    private static final int BOTTOM_ZONE = 180;
    private static final int TAB_H = 30;
    private static final int TAB_STEP = 35;

    private static final String FAV_CAT = "Favorites";

    private final Set<String> expandedCategories = new HashSet<>();
    private int hoveredSection = -1;
    private int hoveredModInSection = -1;
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
    private int lastHoveredSection = -1;
    private int lastHoveredModIdx = -1;
    private boolean lastFilterHover;
    private boolean lastGearHover;

    private int pX, pY, pW, pH;
    private int sidebarW;
    private int contentX, contentY, contentW;
    private int gridY, gridH;

    private final Animated openAnim = new Animated(0f, 12f);
    private final Map<String, Animated> toggleAnims = new HashMap<>();
    private final Map<String, Animated> sectionHoverAnims = new HashMap<>();
    private final Map<String, Animated> rowHoverAnims = new HashMap<>();
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
        if (expandedCategories.isEmpty()) {
            cats.stream().findFirst().ifPresent(expandedCategories::add);
        }

        contentX = pX + sidebarW;
        contentY = pY + 30;
        contentW = pW - sidebarW - 30;
        gridY = contentY + SEARCH_H + 16;
        gridH = pH - (gridY - pY) - 16;
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
            boolean expanded = expandedCategories.contains(cat);
            boolean hover = mx >= pX + 20 && mx <= pX + sidebarW - 20
                         && my >= tabY && my <= tabY + TAB_H;

            if (expanded) {
                g.fill(pX + 20, tabY, pX + sidebarW - 20, tabY + TAB_H, ColorUtil.withAlpha(accent, 38));
                g.fill(pX + 20, tabY, pX + 23, tabY + TAB_H, accent);
            } else if (hover) {
                g.fill(pX + 20, tabY, pX + sidebarW - 20, tabY + TAB_H, Theme.hoverTint());
                hoveredCat = i;
            }

            int textColor = expanded ? accent : (hover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND);
            String label = sidebarCollapsed ? cat.substring(0, 1).toUpperCase() : cat;
            g.text(font, Component.literal(label), pX + 35, tabY + 9, textColor);
            String indicator = expanded ? "\u25BC" : "\u25B6";
            g.text(font, Component.literal(indicator), pX + sidebarW - 28, tabY + 9,
                expanded ? accent : ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 120));
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

        if (expandedCategories.isEmpty()) {
            String msg = "Click a category in the sidebar to show modules";
            int mw = font.width(msg);
            g.text(font, Component.literal(msg), contentX + (contentW - mw) / 2, gridY + 40, Theme.MUTED_FOREGROUND);
            return;
        }

        int contentH = computeContentHeight();
        int frameMaxScroll = Math.max(0, contentH - gridH);
        if (frameMaxScroll != maxScroll) {
            maxScroll = frameMaxScroll;
            scrollTarget = Anim.clamp(scrollTarget, 0, maxScroll);
            scrollOffset = Anim.clamp(scrollOffset, 0, maxScroll);
        }
        scrollOffset += (scrollTarget - scrollOffset) * 0.35f;
        if (Math.abs(scrollOffset - scrollTarget) < 0.01f) scrollOffset = scrollTarget;

        g.enableScissor(contentX, gridY, contentX + contentW, gridY + gridH);

        int accent = Theme.getAnimatedAccent();
        hoveredSection = -1;
        hoveredModInSection = -1;
        int sectionY = gridY - (int) scrollOffset;
        List<String> cats = Cats();

        for (int si = 0; si < cats.size(); si++) {
            String cat = cats.get(si);
            List<CrestModule> mods = getVisibleModules(cat);
            boolean expanded = expandedCategories.contains(cat);
            int bodyH = expanded ? mods.size() * MODULE_ROW_H : 0;
            int totalH = SECTION_HEADER_H + bodyH + 8;
            if (totalH <= 0) continue;

            if (sectionY + totalH > gridY && sectionY < gridY + gridH) {
                renderCategorySection(g, cat, mods, expanded, sectionY, totalH, accent, delta);
                if (mx >= contentX && mx <= contentX + contentW && my >= sectionY && my <= sectionY + totalH) {
                    hoveredSection = si;
                    if (expanded) {
                        int modY = sectionY + SECTION_HEADER_H + 4;
                        for (int mi = 0; mi < mods.size(); mi++) {
                            if (my >= modY && my <= modY + MODULE_ROW_H) {
                                hoveredModInSection = mi;
                                break;
                            }
                            modY += MODULE_ROW_H;
                        }
                    }
                }
            }
            sectionY += totalH + SECTION_GAP;
        }

        if (hoveredSection != -1 && hoveredSection != lastHoveredSection) UiSounds.hover();
        lastHoveredSection = hoveredSection;
        if (hoveredModInSection != -1 && hoveredModInSection != lastHoveredModIdx) UiSounds.hover();
        lastHoveredModIdx = hoveredModInSection;

        g.disableScissor();

        if (maxScroll > 0) {
            float thumbH = (float) gridH / contentH * gridH;
            float thumbY = (scrollOffset / Math.max(1, maxScroll)) * (gridH - thumbH);
            int tx = contentX + contentW - 6;
            int ty = gridY + (int) thumbY;
            int th = Math.max(8, (int) thumbH);
            boolean overTrack = mx >= tx - 3 && mx <= tx + 7
                             && my >= gridY && my <= gridY + gridH;
            int trackAlpha = (overTrack || draggingScrollbar) ? 120 : 60;
            g.fill(tx - 2, gridY, tx + 6, gridY + gridH, ColorUtil.withAlpha(Theme.GLASS_BG, trackAlpha));
            int thumbColor = overThumb(mx, my) || draggingScrollbar ? accent : ColorUtil.withAlpha(Theme.BORDER_LIGHT, 200);
            g.fill(tx, ty, tx + 4, ty + th, thumbColor);
        }

        if (!searchBar.getText().isEmpty()) {
            g.text(font, "\"" + searchBar.getText() + "\"", contentX + 8, gridY + gridH + 6, Theme.MUTED_FOREGROUND);
        }

        quickSettings.render(g, font, pX, pY, pW, mx, my, delta);
    }

    private boolean overThumb(int mx, int my) {
        if (maxScroll <= 0) return false;
        int tx = contentX + contentW - 6;
        int contentH = computeContentHeight();
        float thumbH = (float) gridH / contentH * gridH;
        float thumbY = (scrollOffset / Math.max(1, maxScroll)) * (gridH - thumbH);
        return mx >= tx - 3 && mx <= tx + 7 && my >= gridY + thumbY && my <= gridY + thumbY + thumbH;
    }

    private int computeContentHeight() {
        int h = 0;
        for (String cat : Cats()) {
            List<CrestModule> mods = getVisibleModules(cat);
            int bodyH = expandedCategories.contains(cat) ? mods.size() * MODULE_ROW_H : 0;
            int totalH = SECTION_HEADER_H + bodyH + 8;
            if (totalH <= 0) continue;
            h += totalH + SECTION_GAP;
        }
        return Math.max(0, h - SECTION_GAP);
    }

    private void renderCategorySection(GuiGraphicsExtractor g, String cat, List<CrestModule> mods,
                                        boolean expanded, int y, int totalH, int accent, float delta) {
        int pad = 8;
        int cardX = contentX;
        int cardY = y;
        int cardW = contentW;
        int cardH = totalH;

        Panel.drawElevated(g, cardX, cardY, cardW, cardH, ColorUtil.withAlpha(Theme.CARD, 220), Theme.ELEVATION_1);
        g.fill(cardX + 2, cardY, cardX + cardW - 2, cardY + 1, ColorUtil.withAlpha(accent, 80));

        int enabled = 0, total = 0;
        for (CrestModule m : mods) { total++; if (CrestModules.isEnabled(m.getId())) enabled++; }

        int headerY = cardY + pad;
        String indicator = expanded ? "\u25BC" : "\u25B6";
        g.text(font, Component.literal(indicator), cardX + 12, headerY + 6,
            expanded ? accent : Theme.MUTED_FOREGROUND);
        g.text(font, Component.literal(cat), cardX + 32, headerY + 6, Theme.FOREGROUND);
        String info = total + " module" + (total != 1 ? "s" : "") + "  \u2022  " + enabled + "/" + total + " on";
        int infoW = font.width(info);
        g.text(font, Component.literal(info), cardX + cardW - infoW - 12, headerY + 6,
            ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 180));

        if (!expanded || mods.isEmpty()) return;

        int rowY = cardY + SECTION_HEADER_H + 4;
        for (int mi = 0; mi < mods.size(); mi++) {
            renderModuleRow(g, mods.get(mi), rowY, cardW, accent, delta);
            rowY += MODULE_ROW_H;
        }
    }

    private void renderModuleRow(GuiGraphicsExtractor g, CrestModule mod, int y, int cardW, int accent, float delta) {
        String id = mod.getId();
        boolean enabled = CrestModules.isEnabled(id);
        boolean hover = mx >= contentX && mx <= contentX + contentW && my >= y && my <= y + MODULE_ROW_H;

        Animated ha = rowHoverAnims.computeIfAbsent(id, k -> new Animated(0f, 12f));
        ha.set(hover ? 1f : 0f);
        ha.tick(delta);
        float hoverAmt = ha.get();

        if (hoverAmt > 0.01f) {
            g.fill(contentX + 4, y, contentX + contentW - 4, y + MODULE_ROW_H,
                ColorUtil.withAlpha(Theme.MUTED, (int) (hoverAmt * 80)));
        }

        int lx = contentX + 16;
        g.text(font, Component.literal(mod.getName()), lx, y + 6,
            enabled ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND);

        String desc = mod.getDescription();
        if (desc != null && !desc.isEmpty()) {
            int descMaxW = cardW - 200;
            String truncated = font.width(desc) > descMaxW
                ? font.plainSubstrByWidth(desc, descMaxW - 4) + "\u2026"
                : desc;
            g.text(font, Component.literal(truncated), lx, y + 24,
                ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 140));
        }

        int starX = contentX + contentW - TOGGLE_W - 48;
        boolean starHover = hover && mx >= starX - 6 && mx <= starX + 18 && my >= y + 8 && my <= y + 36;
        int starCol = isFavorite(id)
            ? ColorUtil.lerpARGB(Theme.MUTED_FOREGROUND, accent, 1f)
            : (starHover ? accent : Theme.MUTED_FOREGROUND);
        g.text(font, Component.literal(isFavorite(id) ? "\u2605" : "\u2606"), starX, y + 13, starCol);
        if (starHover) {
            g.fill(starX - 4, y + 8, starX + 18, y + 36,
                ColorUtil.withAlpha(accent, 16));
        }

        Animated ta = toggleAnims.computeIfAbsent(id, k -> new Animated(0f, 12f));
        ta.set(enabled ? 1f : 0f);
        ta.tick(delta);
        int toggleX = contentX + contentW - TOGGLE_W - 12;
        drawToggle(g, toggleX, y + (MODULE_ROW_H - TOGGLE_H) / 2, enabled, ta.get());
    }

    private List<CrestModule> getVisibleModules(String cat) {
        List<CrestModule> mods;
        if (FAV_CAT.equals(cat)) {
            mods = CrestModules.getAll().values().stream()
                    .filter(m -> isFavorite(m.getId()))
                    .collect(Collectors.toList());
        } else {
            mods = CrestModules.getByCategory(cat);
        }
        return filterBySearch(mods);
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
        if (filterMenu.open && filterMenu.keyPressed(key, 0, event.modifiers())) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            List<String> cats = Cats();
            if (cats.isEmpty()) return true;
            int si = hoveredSection >= 0 ? hoveredSection : 0;
            if (key == GLFW.GLFW_KEY_UP && si > 0) si--;
            else if (key == GLFW.GLFW_KEY_DOWN && si < cats.size() - 1) si++;
            hoveredSection = si;
            String cat = cats.get(si);
            if (!expandedCategories.contains(cat)) {
                expandedCategories.add(cat);
            }
            List<CrestModule> mods = getVisibleModules(cat);
            if (key == GLFW.GLFW_KEY_UP) hoveredModInSection = mods.isEmpty() ? -1 : mods.size() - 1;
            else hoveredModInSection = 0;
            int targetSectionY = sectionY(cat);
            float targetScroll = targetSectionY - gridY + SECTION_HEADER_H + 4;
            scrollTarget = Anim.clamp(targetScroll, 0, maxScroll);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (hoveredSection >= 0 && hoveredModInSection >= 0) {
                List<String> cats = Cats();
                if (hoveredSection < cats.size()) {
                    List<CrestModule> mods = getVisibleModules(cats.get(hoveredSection));
                    if (hoveredModInSection < mods.size()) {
                        CrestModules.setEnabled(mods.get(hoveredModInSection).getId(), !CrestModules.isEnabled(mods.get(hoveredModInSection).getId()));
                    }
                }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            List<String> cats = Cats();
            if (!cats.isEmpty()) {
                String current = expandedCategories.isEmpty() ? cats.get(0) : expandedCategories.iterator().next();
                int i = cats.indexOf(current);
                String next = cats.get((i + 1) % cats.size());
                expandedCategories.clear();
                expandedCategories.add(next);
                searchBar.setText("");
                scrollTarget = scrollOffset = 0;
                hoveredSection = i;
                hoveredModInSection = -1;
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
            filterMenu.toggle(filterBtnX, filterBtnY + 36, 36, 36);
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
        if (!expandedCategories.isEmpty() && maxScroll > 0 && mxx >= tx - 3 && mxx <= tx + 7) {
            if (myy >= gridY && myy <= gridY + gridH) {
                int contentH = computeContentHeight();
                float thumbH = (float) gridH / contentH * gridH;
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
                String cat = cats.get(i);
                if (expandedCategories.contains(cat)) expandedCategories.remove(cat);
                else expandedCategories.add(cat);
                UiSounds.click();
                scrollTarget = scrollOffset = 0;
                hoveredSection = expandedCategories.contains(cat) ? i : -1;
                return true;
            }
            tabY += TAB_STEP;
        }

        if (!expandedCategories.isEmpty() && mxx >= contentX && mxx <= contentX + contentW && myy >= gridY && myy <= gridY + gridH) {
            int secY = gridY - (int) scrollOffset;
            for (int si = 0; si < cats.size(); si++) {
                String cat = cats.get(si);
                List<CrestModule> mods = getVisibleModules(cat);
                boolean expanded = expandedCategories.contains(cat);
                int bodyH = expanded ? mods.size() * MODULE_ROW_H : 0;
                int totalH = SECTION_HEADER_H + bodyH + 8;
                if (totalH <= 0) { secY += totalH + SECTION_GAP; continue; }

                if (myy >= secY && myy <= secY + totalH) {
                    if (myy < secY + SECTION_HEADER_H) {
                        if (expandedCategories.contains(cat)) expandedCategories.remove(cat);
                        else expandedCategories.add(cat);
                        UiSounds.click();
                        return true;
                    }
                    if (expanded) {
                        int modY = secY + SECTION_HEADER_H + 4;
                        for (int mi = 0; mi < mods.size(); mi++) {
                            if (myy >= modY && myy <= modY + MODULE_ROW_H) {
                                int starX = contentX + contentW - TOGGLE_W - 48;
                                int toggleX = contentX + contentW - TOGGLE_W - 12;
                                int toggleY = modY + (MODULE_ROW_H - TOGGLE_H) / 2;
                                if (mxx >= starX - 6 && mxx <= starX + 18 && myy >= modY + 8 && myy <= modY + 36) {
                                    UiSounds.click();
                                    toggleFavorite(mods.get(mi).getId());
                                    return true;
                                }
                                if (mxx >= toggleX && mxx <= toggleX + TOGGLE_W && myy >= toggleY && myy <= toggleY + TOGGLE_H) {
                                    UiSounds.click();
                                    CrestModules.setEnabled(mods.get(mi).getId(), !CrestModules.isEnabled(mods.get(mi).getId()));
                                    return true;
                                }
                                UiSounds.click();
                                CrestModule mod = mods.get(mi);
                                Screen config = mod.createConfigScreen(this);
                                minecraft.setScreen(config != null ? config : new ModuleDetailScreen(mod, this));
                                return true;
                            }
                            modY += MODULE_ROW_H;
                        }
                    }
                    return true;
                }
                secY += totalH + SECTION_GAP;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar && !expandedCategories.isEmpty() && maxScroll > 0) {
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
        if (expandedCategories.isEmpty()) return false;
        int contentH = computeContentHeight();
        maxScroll = Math.max(0, contentH - gridH);
        if (maxScroll == 0) return true;
        scrollTarget = Anim.clamp(scrollTarget - (float) deltaY, 0, maxScroll);
        return true;
    }

    private int sectionY(String cat) {
        int sy = gridY - (int) scrollOffset;
        for (String c : Cats()) {
            if (c.equals(cat)) return sy;
            List<CrestModule> mods = getVisibleModules(c);
            int bodyH = expandedCategories.contains(c) ? mods.size() * MODULE_ROW_H : 0;
            sy += SECTION_HEADER_H + bodyH + 8 + SECTION_GAP;
        }
        return sy;
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
