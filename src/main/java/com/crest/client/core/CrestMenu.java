package com.crest.client.core;

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

import java.util.*;
import java.util.stream.Collectors;

public class CrestMenu extends Screen {
    private static final int CAT_W = 140;
    private static final int SIDEBAR = 12;
    private static final int HEADER_H = 26;
    private static final int SEARCH_H = 20;
    private static final int ROW_H = 22;
    private static final int TITLE_H = 14;
    private static final int CARD_INSET = 3;
    private static final int CARD_GAP = 4;
    private static final int CARD_H = 32;

    private String selectedCategory;
    private int hoveredRow = -1;
    private float scrollOffset = 0;
    private float scrollTarget = 0;
    private String searchQuery = "";
    private boolean searchFocused = false;

    private int modAreaX;
    private int modAreaW;
    private int maxVisibleRows;

    // animations
    private final Animated openAnim = new Animated(0f, 10f);
    private final Animated accentPulse = new Animated(0f, 4f);
    private final Map<String, Animated> toggleAnim = new HashMap<>();
    private final Map<String, Animated> hoverAnim = new HashMap<>();

    protected CrestMenu() {
        super(Component.literal("Crest Client"));
    }

    private Animated toggleFor(String id) {
        return toggleAnim.computeIfAbsent(id, k -> new Animated(0f, 12f));
    }

    private Animated hoverFor(String id) {
        return hoverAnim.computeIfAbsent(id, k -> new Animated(0f, 16f));
    }

    @Override
    protected void init() {
        List<String> cats = CrestModules.getCategories();
        selectedCategory = cats.isEmpty() ? null : cats.get(0);
        modAreaX = SIDEBAR + CAT_W + 6;
        modAreaW = Math.max(width - modAreaX - SIDEBAR, 220);
        maxVisibleRows = (height - HEADER_H - SEARCH_H - 24) / ROW_H;
        openAnim.setImmediate(0f);
        openAnim.set(1f);
        Theme.load();
    }

    // --- Render ---

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        openAnim.tick(delta);
        accentPulse.tick(delta);
        float open = openAnim.get();

        g.nextStratum();
        int bgAlpha = (int) (Math.round(Theme.OVERLAY * 1.0) & 0xFF);
        g.fill(0, 0, width, height, ColorUtil.withAlpha(Theme.OVERLAY, (int) (bgAlpha * open)));

        int wy = (int) ((1 - open) * 24);
        g.pose().pushMatrix();
        g.pose().translate(0, wy);

        renderHeader(g, mx, my);
        renderSearchBar(g, mx, my);
        renderCategories(g, mx, my);
        renderModules(g, mx, my, delta);

        g.pose().popMatrix();
    }

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my) {
        Panel.drawGlass(g, 0, 0, width, HEADER_H, ColorUtil.withAlpha(Theme.BG_PANEL, 235), Theme.getAnimatedAccent());
        int y = (HEADER_H - TITLE_H) / 2;
        g.centeredText(font, getTitle(), width / 2, y, Theme.TEXT);

        String editText = "Edit HUD";
        int ew = font.width(editText) + 14;
        int editBtnX = width - ew - 8;
        boolean hover = mx >= editBtnX && mx <= editBtnX + ew && my >= 3 && my <= HEADER_H - 3;
        Panel.draw(g, editBtnX, 3, ew, HEADER_H - 6, ColorUtil.withAlpha(hover ? Theme.getAccentHover() : Theme.BG_HOVER, 220));
        g.centeredText(font, Component.literal(editText), editBtnX + ew / 2, y, Theme.TEXT);
    }

    private void renderSearchBar(GuiGraphicsExtractor g, int mx, int my) {
        int sy = HEADER_H + 5;
        int sh = SEARCH_H;
        boolean hover = mx >= SIDEBAR && mx <= SIDEBAR + CAT_W && my >= sy && my <= sy + sh;
        Panel.draw(g, SIDEBAR, sy, CAT_W, sh, ColorUtil.withAlpha(searchFocused || hover ? Theme.BG_HOVER : Theme.BG_SURFACE, 220));

        String display = searchQuery.isEmpty() ? "Search modules..." : searchQuery;
        g.text(font, Component.literal(display), SIDEBAR + 6, sy + 5,
            searchQuery.isEmpty() ? Theme.TEXT_FAINT : Theme.TEXT);

        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = SIDEBAR + 6 + font.width(searchQuery);
            g.fill(cx, sy + 4, cx + 1, sy + sh - 4, Theme.getAnimatedAccent());
        }
    }

    private void renderCategories(GuiGraphicsExtractor g, int mx, int my) {
        List<String> cats = CrestModules.getCategories();
        String[] icons = {"\uD83D\uDDBC", "\uD83D\uDCCA", "\uD83C\uDFAE", "\uD83D\uDD27", "\uD83C\uDFB5", "\uD83C\uDFA8"};
        int sy = HEADER_H + SEARCH_H + 12;

        Panel.draw(g, SIDEBAR, sy - 4, CAT_W, cats.size() * ROW_H + 8, ColorUtil.withAlpha(Theme.BG_PANEL, 200));

        for (int i = 0; i < cats.size(); i++) {
            int cy = sy + i * ROW_H;
            String cat = cats.get(i);
            boolean hover = mx >= SIDEBAR && mx <= SIDEBAR + CAT_W && my >= cy && my <= cy + ROW_H - 2;
            boolean selected = cat.equals(selectedCategory);

            if (selected) {
                g.fill(SIDEBAR + 4, cy, SIDEBAR + 6, cy + ROW_H - 2, Theme.getAnimatedAccent());
                g.fill(SIDEBAR + 6, cy, SIDEBAR + CAT_W - 4, cy + ROW_H - 2, ColorUtil.withAlpha(Theme.BG_SELECT, 180));
            } else if (hover) {
                g.fill(SIDEBAR + 6, cy, SIDEBAR + CAT_W - 4, cy + ROW_H - 2, ColorUtil.withAlpha(Theme.BG_HOVER, 160));
            }

            String icon = i < icons.length ? icons[i] : "\u2022";
            g.text(font, Component.literal(icon), SIDEBAR + 10, cy + 2, selected ? Theme.TEXT : Theme.TEXT_DIM);
            g.text(font, Component.literal(cat), SIDEBAR + 28, cy + 4, selected ? Theme.TEXT : hover ? 0xFFD8DCFF : Theme.TEXT_DIM);

            int count = CrestModules.getByCategory(cat).size();
            String badge = String.valueOf(count);
            int bw = font.width(badge) + 8;
            int bx = SIDEBAR + CAT_W - bw - 6;
            g.fill(bx, cy + 4, bx + bw, cy + ROW_H - 6, ColorUtil.withAlpha(Theme.BG_BASE, 200));
            g.centeredText(font, Component.literal(badge), bx + bw / 2, cy + 5, Theme.TEXT_FAINT);
        }
    }

    private void renderModules(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (selectedCategory == null) return;

        List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
        int total = mods.size();

        maxVisibleRows = (height - HEADER_H - SEARCH_H - 24) / ROW_H;

        scrollTarget = Anim.clamp(scrollTarget, 0, Math.max(0, total - maxVisibleRows));
        scrollOffset += (scrollTarget - scrollOffset) * Anim.smooth(delta, 18f);

        int listTop = HEADER_H + SEARCH_H + 12;
        int listH = maxVisibleRows * ROW_H;

        Panel.draw(g, modAreaX, listTop - 4, modAreaW, listH + 8, ColorUtil.withAlpha(Theme.BG_PANEL, 200));

        g.enableScissor(modAreaX, listTop, modAreaX + modAreaW, listTop + listH);

        hoveredRow = -1;
        int startRow = (int) Math.floor(scrollOffset);
        float frac = scrollOffset - startRow;
        int sy = listTop - (int) (frac * ROW_H);

        for (int i = startRow; i < mods.size(); i++) {
            CrestModule mod = mods.get(i);
            int cy = sy + (i - startRow) * ROW_H;
            if (cy > listTop + listH + CARD_H) break;
            if (cy + ROW_H < listTop) continue;
            renderModuleCard(g, mx, my, mod, cy, i, delta);
        }

        g.disableScissor();

        if (total > maxVisibleRows) {
            int trackX = modAreaX + modAreaW - 4;
            g.fill(trackX, listTop, trackX + 3, listTop + listH, ColorUtil.withAlpha(Theme.BG_BASE, 200));
            float thumbH = (float) maxVisibleRows / total * listH;
            float thumbY = scrollOffset / total * listH;
            g.fill(trackX, listTop + (int) thumbY, trackX + 3, listTop + (int) (thumbY + thumbH), Theme.getAnimatedAccent());
        }
    }

    private void renderModuleCard(GuiGraphicsExtractor g, int mx, int my, CrestModule mod, int cy, int displayIdx, float delta) {
        boolean hover = mx >= modAreaX && mx <= modAreaX + modAreaW && my >= cy && my <= cy + CARD_H - 2;
        if (hover) hoveredRow = displayIdx;

        Animated ha = hoverFor(mod.getId());
        ha.set(hover ? 1f : 0f);
        ha.tick(delta);
        boolean enabled = CrestModules.isEnabled(mod.getId());

        Animated ta = toggleFor(mod.getId());
        ta.set(enabled ? 1f : 0f);
        ta.tick(delta);

        int tint = ColorUtil.withAlpha(enabled ? Theme.BG_PANEL : Theme.BG_SURFACE, 220);
        int cardAlpha = (int) (200 + 55 * ha.get());
        tint = ColorUtil.withAlpha(tint, Math.min(cardAlpha, 255));
        Panel.drawGlass(g, modAreaX + CARD_INSET, cy, modAreaW - CARD_INSET * 2, CARD_H, tint, Theme.getAnimatedAccent());

        // toggle on right
        int toggleX = modAreaX + modAreaW - CARD_INSET - 8 - ToggleSwitch.W;
        ToggleSwitch.render(g, toggleX, cy + (CARD_H - ToggleSwitch.H) / 2, enabled, ta.get());

        // name on left
        int nameX = modAreaX + CARD_INSET + 8;
        int nameMaxW = modAreaW - CARD_INSET * 2 - 16 - ToggleSwitch.W - 8;
        String name = font.width(mod.getName()) > nameMaxW
            ? font.plainSubstrByWidth(mod.getName(), nameMaxW - 4) + "..."
            : mod.getName();
        g.text(font, Component.literal(name), nameX, cy + (CARD_H - font.lineHeight) / 2,
            enabled ? Theme.TEXT : Theme.TEXT_DIM);

        // "> settings" hint on right before toggle
        String hint = "\u25B6 settings";
        int hintW = font.width(hint);
        int hintX = toggleX - hintW - 6;
        g.text(font, Component.literal(hint), hintX, cy + (CARD_H - font.lineHeight) / 2,
            hover ? Theme.TEXT_DIM : ColorUtil.withAlpha(Theme.TEXT_DIM, 120));

        if (hover && !mod.getDescription().isEmpty()) {
            renderDescription(g, mod.getDescription(), mx, cy);
        }
    }

    private void renderDescription(GuiGraphicsExtractor g, String desc, int mx, int my) {
        int dw = Math.min(font.width(desc) + 12, 320);
        int dh = font.lineHeight + 6;
        int dx = Math.min(mx + 10, width - dw - 4);
        int dy = my - dh - 6;
        if (dy < HEADER_H) dy = my + 22;
        Panel.draw(g, dx, dy, dw, dh, ColorUtil.withAlpha(Theme.BG_PANEL, 245));
        g.fill(dx + 3, dy + 3, dx + 5, dy + dh - 3, Theme.getAnimatedAccent());
        g.text(font, Component.literal(desc), dx + 10, dy + 4, 0xFFD8DCFF);
    }

    // --- Input ---

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (!searchFocused) return super.charTyped(event);
        if (cp >= 32 && cp < 127) {
            searchQuery += event.codepointAsString();
            scrollTarget = 0; scrollOffset = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { searchFocused = false; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); scrollTarget = 0; scrollOffset = 0; }
                return true;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_UP) {
            if (hoveredRow > 0) hoveredRow--;
            scrollTarget = Math.max(0, Math.min(scrollTarget, hoveredRow - maxVisibleRows + 1));
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            if (hoveredRow < mods.size() - 1) hoveredRow++;
            if (hoveredRow >= scrollTarget + maxVisibleRows) scrollTarget = Math.max(0, hoveredRow - maxVisibleRows + 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            if (hoveredRow >= 0 && hoveredRow < mods.size()) {
                minecraft.setScreen(new ModuleDetailScreen(mods.get(hoveredRow), this));
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_SLASH && searchQuery.isEmpty()) { searchFocused = true; return true; }
        if (key == GLFW.GLFW_KEY_TAB) {
            List<String> cats = CrestModules.getCategories();
            if (!cats.isEmpty()) {
                int idx = cats.indexOf(selectedCategory);
                selectedCategory = cats.get((idx + 1) % cats.size());
                scrollTarget = 0; scrollOffset = 0; hoveredRow = -1;
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.buttonInfo().input();

        if (btn == 0) {
            int editBtnX = width - font.width("Edit HUD") - 22;
            if (my >= 3 && my <= HEADER_H - 3 && mx >= editBtnX && mx <= editBtnX + font.width("Edit HUD") + 14) {
                minecraft.setScreen(new HudEditScreen());
                return true;
            }
            if (my >= HEADER_H && my <= HEADER_H + SEARCH_H + 5 && mx >= SIDEBAR && mx <= SIDEBAR + CAT_W) {
                searchFocused = true;
                return true;
            }
        }
        searchFocused = false;

        // category sidebar
        List<String> cats = CrestModules.getCategories();
        int sy = HEADER_H + SEARCH_H + 12;
        for (int i = 0; i < cats.size(); i++) {
            int cy = sy + i * ROW_H;
            if (mx >= SIDEBAR && mx <= SIDEBAR + CAT_W && my >= cy && my <= cy + ROW_H - 2) {
                selectedCategory = cats.get(i);
                scrollTarget = 0; scrollOffset = 0; hoveredRow = -1;
                return true;
            }
        }

        // module cards
        if (selectedCategory != null && mx >= modAreaX && mx <= modAreaX + modAreaW) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            int startRow = (int) Math.floor(scrollOffset);
            int listTop = HEADER_H + SEARCH_H + 12;
            float frac = scrollOffset - startRow;
            int baseY = listTop - (int) (frac * ROW_H);

            for (int i = startRow; i < mods.size(); i++) {
                int cy = baseY + (i - startRow) * ROW_H;
                if (cy > listTop + maxVisibleRows * ROW_H) break;
                if (my < cy || my > cy + CARD_H - 2) continue;
                CrestModule mod = mods.get(i);

                // toggle switch on right side of card
                int toggleX = modAreaX + modAreaW - CARD_INSET - 8 - ToggleSwitch.W;
                if (mx >= toggleX && mx <= toggleX + ToggleSwitch.W) {
                    CrestModules.setEnabled(mod.getId(), !CrestModules.isEnabled(mod.getId()));
                } else {
                    // open detail screen
                    minecraft.setScreen(new ModuleDetailScreen(mod, this));
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (selectedCategory != null) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            scrollTarget = Anim.clamp(scrollTarget - (float) deltaY * 3, 0, Math.max(0, mods.size() - maxVisibleRows));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private List<CrestModule> filterBySearch(List<CrestModule> mods) {
        if (searchQuery.isEmpty()) return mods;
        String q = searchQuery.toLowerCase();
        return mods.stream()
            .filter(m -> m.getName().toLowerCase().contains(q) || m.getId().toLowerCase().contains(q))
            .collect(Collectors.toList());
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new CrestMenu());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
