package com.crest.client.core;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

public class CrestClickGui extends Screen {
    private static final int CAT_W = 130;
    private static final int SIDEBAR = 10;
    private static final int HEADER_H = 24;
    private static final int SEARCH_H = 18;
    private static final int ROW_H = 20;
    private static final int TITLE_H = 14;
    private static final int ACCENT = 0xFF5555FF;
    private static final int BG_MAIN = 0xCC0A0A1A;
    private static final int BG_PANEL = 0xCC111122;
    private static final int BG_HOVER = 0x44333366;
    private static final int BG_SELECT = 0x66222255;
    private static final int TEXT_DIM = 0xFF888888;
    private static final int TEXT_ON = 0xFF55FF55;
    private static final int TEXT_OFF = 0xFFAA5555;

    private String selectedCategory;
    private int hoveredModule = -1;
    private int scrollOffset = 0;
    private String searchQuery = "";
    private boolean searchFocused = false;

    private int modAreaX;
    private int modAreaW;
    private int maxVisibleRows;
    private int editBtnX;
    private int editBtnW;

    protected CrestClickGui() {
        super(Component.literal("Crest Client"));
    }

    @Override
    protected void init() {
        List<String> cats = CrestModules.getCategories();
        selectedCategory = cats.isEmpty() ? null : cats.get(0);
        modAreaX = SIDEBAR + CAT_W + 4;
        modAreaW = Math.max(width - modAreaX - SIDEBAR, 120);
        maxVisibleRows = (height - HEADER_H - SEARCH_H - 20) / ROW_H;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, BG_MAIN);

        renderHeader(g);
        renderSearchBar(g, mx, my);
        renderCategories(g, mx, my);
        renderModules(g, mx, my);
    }

    private void renderHeader(GuiGraphicsExtractor g) {
        g.fill(0, 0, width, HEADER_H, ACCENT);
        int y = (HEADER_H - TITLE_H) / 2;
        g.centeredText(font, getTitle(), width / 2, y, 0xFFFFFFFF);

        String editText = "Edit HUD";
        int ew = font.width(editText) + 8;
        editBtnX = width - ew - 6;
        editBtnW = ew;
        g.fill(editBtnX, 3, editBtnX + ew, HEADER_H - 3, 0x44333366);
        g.centeredText(font, Component.literal(editText), editBtnX + ew / 2, y, 0xFFFFFFFF);
    }

    private void renderSearchBar(GuiGraphicsExtractor g, int mx, int my) {
        int sy = HEADER_H + 4;
        int sh = SEARCH_H;

        boolean hover = mx >= SIDEBAR && mx <= SIDEBAR + CAT_W && my >= sy && my <= sy + sh;
        g.fill(SIDEBAR, sy, SIDEBAR + CAT_W, sy + sh, searchFocused ? 0xAA333355 : 0xAA222244);

        String display = searchQuery.isEmpty() ? "Search..." : searchQuery;
        g.text(font, Component.literal(display), SIDEBAR + 3, sy + 3,
            searchQuery.isEmpty() ? TEXT_DIM : 0xFFFFFFFF);

        if (searchFocused && searchQuery.isEmpty() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = SIDEBAR + 3 + font.width(searchQuery);
            g.fill(cx, sy + 2, cx + 1, sy + sh - 2, 0xFFFFFFFF);
        }
    }

    private void renderCategories(GuiGraphicsExtractor g, int mx, int my) {
        List<String> cats = CrestModules.getCategories();
        int sy = HEADER_H + SEARCH_H + 8;

        for (int i = 0; i < cats.size(); i++) {
            int cy = sy + i * ROW_H;
            String cat = cats.get(i);
            boolean hover = mx >= SIDEBAR && mx <= SIDEBAR + CAT_W && my >= cy && my <= cy + ROW_H - 2;
            boolean selected = cat.equals(selectedCategory);

            int bg = selected ? BG_SELECT : hover ? BG_HOVER : 0x00000000;
            if (bg != 0) g.fill(SIDEBAR, cy, SIDEBAR + CAT_W, cy + ROW_H - 2, bg);

            int border = selected ? ACCENT : 0x00000000;
            if (selected) g.fill(SIDEBAR, cy, SIDEBAR + 2, cy + ROW_H - 2, border);

            int txtColor = selected ? 0xFFFFFFFF : hover ? 0xFFCCCCFF : TEXT_DIM;
            g.text(font, Component.literal(cat), SIDEBAR + 8, cy + 4, txtColor);

            int count = CrestModules.getByCategory(cat).size();
            String badge = String.valueOf(count);
            int bw = font.width(badge) + 6;
            int bx = SIDEBAR + CAT_W - bw - 4;
            g.fill(bx, cy + 2, bx + bw, cy + ROW_H - 4, 0x44333366);
            g.centeredText(font, Component.literal(badge), bx + bw / 2, cy + 3, TEXT_DIM);
        }
    }

    private void renderModules(GuiGraphicsExtractor g, int mx, int my) {
        if (selectedCategory == null) return;

        List<CrestModule> allMods = CrestModules.getByCategory(selectedCategory);
        List<CrestModule> mods = filterBySearch(allMods);
        int sy = HEADER_H + SEARCH_H + 8;

        hoveredModule = -1;
        int visibleCount = Math.min(mods.size() - scrollOffset, maxVisibleRows);

        for (int i = 0; i < visibleCount; i++) {
            int idx = i + scrollOffset;
            if (idx >= mods.size()) break;

            CrestModule mod = mods.get(idx);
            int cy = sy + i * ROW_H;
            boolean hover = mx >= modAreaX && mx <= modAreaX + modAreaW && my >= cy && my <= cy + ROW_H - 2;
            if (hover) hoveredModule = idx;

            boolean enabled = CrestModules.isEnabled(mod.getId());
            int bg = hover ? BG_HOVER : (i % 2 == 0 ? 0x11000000 : 0x00000000);
            if (bg != 0) g.fill(modAreaX, cy, modAreaX + modAreaW, cy + ROW_H - 2, bg);

            if (enabled)
                g.fill(modAreaX, cy, modAreaX + 3, cy + ROW_H - 2, TEXT_ON);
            else
                g.fill(modAreaX, cy, modAreaX + 3, cy + ROW_H - 2, TEXT_OFF);

            String toggle = enabled ? "[ON]" : "[OFF]";
            int toggleColor = enabled ? TEXT_ON : TEXT_OFF;

            String modeSuffix = mod instanceof ArmorHudModule a ? " [" + a.getModeLabel() + "]" : "";
            String name = mod.getName();
            int toggleW = font.width(toggle) + 4;
            int modeW = modeSuffix.isEmpty() ? 0 : font.width(modeSuffix);
            int nameMaxW = modAreaW - toggleW - modeW - 10;
            String displayName = font.width(name) > nameMaxW
                ? font.plainSubstrByWidth(name, nameMaxW - 4) + "..."
                : name;

            g.text(font, Component.literal(displayName), modAreaX + 8, cy + 4, 0xFFFFFFFF);
            if (!modeSuffix.isEmpty())
                g.text(font, Component.literal(modeSuffix), modAreaX + 8 + font.width(displayName), cy + 4, TEXT_DIM);
            g.text(font, Component.literal(toggle), modAreaX + modAreaW - toggleW, cy + 4, toggleColor);

            if (hover && !mod.getDescription().isEmpty()) {
                renderDescription(g, mod.getDescription(), mx, cy);
            }
        }

        int totalMods = mods.size();
        if (totalMods > maxVisibleRows) {
            String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + maxVisibleRows, totalMods) + "/" + totalMods;
            g.text(font, Component.literal(scrollInfo), modAreaX + modAreaW - font.width(scrollInfo),
                sy + visibleCount * ROW_H + 2, TEXT_DIM);
        }
    }

    private void renderDescription(GuiGraphicsExtractor g, String desc, int mx, int my) {
        int dw = Math.min(font.width(desc) + 8, 300);
        int dh = font.lineHeight + 4;
        int dx = Math.min(mx + 8, width - dw - 4);
        int dy = my - dh - 4;
        if (dy < 0) dy = my + 20;

        g.fill(dx, dy, dx + dw, dy + dh, 0xEE222244);
        g.fill(dx, dy, dx + dw, dy + 1, ACCENT);
        g.text(font, Component.literal(desc), dx + 4, dy + 2, 0xFFCCCCCC);
    }

    private List<CrestModule> filterBySearch(List<CrestModule> mods) {
        if (searchQuery.isEmpty()) return mods;
        String q = searchQuery.toLowerCase();
        return mods.stream()
            .filter(m -> m.getName().toLowerCase().contains(q) || m.getId().toLowerCase().contains(q))
            .collect(Collectors.toList());
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!searchFocused) return super.charTyped(event);
        int cp = event.codepoint();
        if (cp >= 32 && cp < 127) {
            searchQuery += event.codepointAsString();
            scrollOffset = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    scrollOffset = 0;
                }
                return true;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_UP && hoveredModule >= 0) {
            hoveredModule = Math.max(0, hoveredModule - 1);
            if (hoveredModule < scrollOffset) scrollOffset = hoveredModule;
            return true;
        }

        if (key == GLFW.GLFW_KEY_DOWN && hoveredModule >= 0) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            hoveredModule = Math.min(mods.size() - 1, hoveredModule + 1);
            if (hoveredModule >= scrollOffset + maxVisibleRows)
                scrollOffset = Math.max(0, hoveredModule - maxVisibleRows + 1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (hoveredModule >= 0 && selectedCategory != null) {
                List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
                if (hoveredModule < mods.size()) {
                    CrestModule mod = mods.get(hoveredModule);
                    boolean newState = !CrestModules.isEnabled(mod.getId());
                    CrestModules.setEnabled(mod.getId(), newState);
                    return true;
                }
            }
        }

        if (key == GLFW.GLFW_KEY_SLASH && searchQuery.isEmpty()) {
            searchFocused = true;
            return true;
        }

        if (key == GLFW.GLFW_KEY_TAB) {
            List<String> cats = CrestModules.getCategories();
            if (!cats.isEmpty()) {
                int idx = cats.indexOf(selectedCategory);
                selectedCategory = cats.get((idx + 1) % cats.size());
                scrollOffset = 0;
                hoveredModule = -1;
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
            if (my >= 3 && my <= HEADER_H - 3 && mx >= editBtnX && mx <= editBtnX + editBtnW) {
                minecraft.setScreen(new HudEditScreen());
                return true;
            }

            if (my >= HEADER_H && my <= HEADER_H + SEARCH_H && mx >= SIDEBAR && mx <= SIDEBAR + CAT_W) {
                searchFocused = true;
                return true;
            }
        }

        searchFocused = false;

        List<String> cats = CrestModules.getCategories();
        int sy = HEADER_H + SEARCH_H + 8;
        for (int i = 0; i < cats.size(); i++) {
            int cy = sy + i * ROW_H;
            if (mx >= SIDEBAR && mx <= SIDEBAR + CAT_W && my >= cy && my <= cy + ROW_H - 2) {
                selectedCategory = cats.get(i);
                scrollOffset = 0;
                hoveredModule = -1;
                return true;
            }
        }

        if (selectedCategory != null) {
            List<CrestModule> allMods = CrestModules.getByCategory(selectedCategory);
            List<CrestModule> mods = filterBySearch(allMods);
            int visibleCount = Math.min(mods.size() - scrollOffset, maxVisibleRows);

            for (int i = 0; i < visibleCount; i++) {
                int idx = i + scrollOffset;
                if (idx >= mods.size()) break;
                int cy = sy + i * ROW_H;
                if (mx >= modAreaX && mx <= modAreaX + modAreaW && my >= cy && my <= cy + ROW_H - 2) {
                    CrestModule mod = mods.get(idx);
                    if (btn == 1 && mod instanceof ArmorHudModule armor) {
                        armor.cycleDurabilityMode();
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(
                                Component.literal("Armor HUD: " + armor.getModeLabel()));
                        }
                        return true;
                    }
                    if (btn == 0) {
                        boolean newState = !CrestModules.isEnabled(mod.getId());
                        CrestModules.setEnabled(mod.getId(), newState);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
