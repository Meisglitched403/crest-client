package com.crest.client.core;

import com.crest.client.core.setting.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CrestClickGui extends Screen {
    private static final int CAT_W = 130;
    private static final int SIDEBAR = 10;
    private static final int HEADER_H = 24;
    private static final int SEARCH_H = 18;
    private static final int ROW_H = 20;
    private static final int SETTING_ROW_H = 18;
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
    private int hoveredRow = -1;
    private int hoveredSettingCol = -1;
    private int scrollOffset = 0;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private final Set<String> expandedModules = new HashSet<>();

    private int modAreaX;
    private int modAreaW;
    private int maxVisibleRows;
    private int editBtnX;
    private int editBtnW;

    private boolean keybindCapture;
    private String keybindCaptureModule;

    protected CrestClickGui() {
        super(Component.literal("Crest Client"));
    }

    @Override
    protected void init() {
        List<String> cats = CrestModules.getCategories();
        selectedCategory = cats.isEmpty() ? null : cats.get(0);
        modAreaX = SIDEBAR + CAT_W + 4;
        modAreaW = Math.max(width - modAreaX - SIDEBAR, 200);
        maxVisibleRows = (height - HEADER_H - SEARCH_H - 20) / ROW_H;
    }

    // --- Row counting ---

    private record DisplayRow(CrestModule module, boolean isHeader, int settingIndex, Setting<?> setting, int totalHeight) {}
    private static final int HEADER = 0;
    private static final int SETTING = 1;

    private int countDisplayRows(List<CrestModule> mods) {
        int rows = 0;
        for (CrestModule mod : mods) {
            rows++;
            if (expandedModules.contains(mod.getId())) {
                rows += mod.getSettings().size();
            }
        }
        return rows;
    }

    private CrestModule getModuleAtDisplayRow(List<CrestModule> mods, int displayRow, int[] outType, int[] outSettingIdx) {
        int row = 0;
        for (CrestModule mod : mods) {
            if (row == displayRow) {
                outType[0] = HEADER;
                return mod;
            }
            row++;
            if (expandedModules.contains(mod.getId())) {
                List<Setting<?>> settings = mod.getSettings();
                for (int si = 0; si < settings.size(); si++) {
                    if (row == displayRow) {
                        outType[0] = SETTING;
                        outSettingIdx[0] = si;
                        return mod;
                    }
                    row++;
                }
            }
        }
        return null;
    }

    // --- Render ---

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
        int totalRows = countDisplayRows(mods);
        int sy = HEADER_H + SEARCH_H + 8;

        hoveredRow = -1;
        hoveredSettingCol = -1;

        int displayIdx = 0;
        int rowOffset = 0;

        for (CrestModule mod : mods) {
            if (displayIdx < scrollOffset) {
                displayIdx++;
                if (expandedModules.contains(mod.getId())) displayIdx += mod.getSettings().size();
                continue;
            }

            int rowY = sy + rowOffset * ROW_H;
            if (rowY > height) break;

            renderModuleRow(g, mx, my, mod, rowY, displayIdx == scrollOffset ? 0 : displayIdx - scrollOffset);
            rowOffset++;
            displayIdx++;

            if (expandedModules.contains(mod.getId())) {
                List<Setting<?>> settings = mod.getSettings();
                for (int si = 0; si < settings.size(); si++) {
                    if (displayIdx < scrollOffset) {
                        displayIdx++;
                        continue;
                    }
                    int settingY = sy + rowOffset * ROW_H;
                    if (settingY > height) break;
                    renderSettingRow(g, mx, my, mod, settings.get(si), si, settingY, rowOffset);
                    rowOffset++;
                    displayIdx++;
                }
            }
        }

        if (totalRows > maxVisibleRows) {
            int visibleEnd = Math.min(scrollOffset + maxVisibleRows, totalRows);
            String scrollInfo = (scrollOffset + 1) + "-" + visibleEnd + "/" + totalRows;
            g.text(font, Component.literal(scrollInfo), modAreaX + modAreaW - font.width(scrollInfo),
                sy + Math.min(maxVisibleRows, totalRows) * ROW_H + 2, TEXT_DIM);
        }
    }

    private void renderModuleRow(GuiGraphicsExtractor g, int mx, int my, CrestModule mod, int cy, int rowOffset) {
        boolean hover = mx >= modAreaX && mx <= modAreaX + modAreaW && my >= cy && my <= cy + ROW_H - 2;
        if (hover) hoveredRow = rowOffset;

        boolean enabled = CrestModules.isEnabled(mod.getId());
        int bg = hover ? BG_HOVER : (rowOffset % 2 == 0 ? 0x11000000 : 0x00000000);
        if (bg != 0) g.fill(modAreaX, cy, modAreaX + modAreaW, cy + ROW_H - 2, bg);

        if (enabled)
            g.fill(modAreaX, cy, modAreaX + 3, cy + ROW_H - 2, TEXT_ON);
        else
            g.fill(modAreaX, cy, modAreaX + 3, cy + ROW_H - 2, TEXT_OFF);

        String toggle = enabled ? "[ON]" : "[OFF]";
        int toggleColor = enabled ? TEXT_ON : TEXT_OFF;

        List<Setting<?>> settings = mod.getSettings();
        boolean hasSettings = !settings.isEmpty();
        String gear = hasSettings ? (expandedModules.contains(mod.getId()) ? "▼" : "▶") + " " : "";
        String name = gear + mod.getName();
        int toggleW = font.width(toggle) + 4;

        int nameMaxW = modAreaW - toggleW - 10;
        String displayName = font.width(name) > nameMaxW
            ? font.plainSubstrByWidth(name, nameMaxW - 4) + "..."
            : name;

        g.text(font, Component.literal(displayName), modAreaX + 8, cy + 4, 0xFFFFFFFF);
        g.text(font, Component.literal(toggle), modAreaX + modAreaW - toggleW, cy + 4, toggleColor);

        if (hover && !mod.getDescription().isEmpty()) {
            renderDescription(g, mod.getDescription(), mx, cy);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderSettingRow(GuiGraphicsExtractor g, int mx, int my, CrestModule mod, Setting<?> setting, int si, int cy, int rowOffset) {
        if (!setting.isVisible()) return;

        boolean hover = mx >= modAreaX && mx <= modAreaX + modAreaW && my >= cy && my <= cy + SETTING_ROW_H - 2;
        if (hover) {
            hoveredRow = rowOffset;
            hoveredSettingCol = si;
        }

        int indent = 16;
        int labelW = font.width(setting.getName()) + 4;
        int controlX = modAreaX + indent + labelW + 4;
        int controlW = modAreaW - indent - labelW - 8;

        g.fill(modAreaX + indent, cy, modAreaX + modAreaW, cy + SETTING_ROW_H - 2, hover ? 0x22224455 : 0x11000000);
        g.text(font, Component.literal(setting.getName()), modAreaX + indent + 2, cy + 3, TEXT_DIM);

        if (setting instanceof BooleanSetting bs) {
            String txt = bs.get() ? "[ON]" : "[OFF]";
            int col = bs.get() ? TEXT_ON : TEXT_OFF;
            g.text(font, Component.literal(txt), controlX, cy + 3, col);
        } else if (setting instanceof IntegerSetting is) {
            int val = is.get();
            int min = is.getMin();
            int max = is.getMax();
            int barW = Math.min(controlW - 30, 100);
            int barX = controlX;
            int barY = cy + 5;
            float frac = (max > min) ? (float)(val - min) / (max - min) : 0f;
            int fillW = (int)(frac * barW);

            g.fill(barX, barY, barX + barW, barY + 4, 0x44333366);
            if (fillW > 0) g.fill(barX, barY, barX + fillW, barY + 4, ACCENT);
            g.fill(barX + fillW - 1, barY - 1, barX + fillW + 2, barY + 5, 0xFFFFFFFF);

            String valStr = String.valueOf(val);
            g.text(font, Component.literal(valStr), barX + barW + 4, cy + 2, 0xFFFFFFFF);
        } else if (setting instanceof FloatSetting fs) {
            float val = fs.get();
            float min = fs.getMin();
            float max = fs.getMax();
            int barW = Math.min(controlW - 40, 100);
            int barX = controlX;
            int barY = cy + 5;
            float frac = (max > min) ? (val - min) / (max - min) : 0f;
            int fillW = (int)(frac * barW);

            g.fill(barX, barY, barX + barW, barY + 4, 0x44333366);
            if (fillW > 0) g.fill(barX, barY, barX + fillW, barY + 4, ACCENT);
            g.fill(barX + fillW - 1, barY - 1, barX + fillW + 2, barY + 5, 0xFFFFFFFF);

            String valStr = String.format("%.2f", val);
            g.text(font, Component.literal(valStr), barX + barW + 4, cy + 2, 0xFFFFFFFF);
        } else if (setting instanceof ModeSetting ms) {
            String modeStr = "[" + ms.getMode() + "]";
            int col = hover && hoveredSettingCol == si ? 0xFFCCCCFF : TEXT_DIM;
            g.text(font, Component.literal(modeStr), controlX, cy + 3, col);
        } else if (setting instanceof ColorSetting cs) {
            int previewX = controlX;
            int previewY = cy + 2;
            int ps = 12;
            g.fill(previewX, previewY, previewX + ps, previewY + ps, 0xFF000000 | cs.getRGB());
            String hex = String.format("#%06X", cs.getRGB());
            g.text(font, Component.literal(hex), previewX + ps + 4, cy + 3, TEXT_DIM);
        } else if (setting instanceof KeybindSetting ks) {
            String keyName = ks.getKeyName();
            if (keybindCapture && mod.getId().equals(keybindCaptureModule)) {
                keyName = "...";
            }
            int col = hover && hoveredSettingCol == si ? 0xFFCCCCFF : TEXT_DIM;
            g.text(font, Component.literal("[" + keyName + "]"), controlX, cy + 3, col);
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

    // --- Input ---

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

        if (keybindCapture) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                keybindCapture = false;
                keybindCaptureModule = null;
            } else if (keybindCaptureModule != null) {
                CrestModule mod = CrestModules.get(keybindCaptureModule);
                if (mod != null) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (s instanceof KeybindSetting ks) {
                            ks.set(key);
                            CrestModules.getConfigManager().markDirty();
                            break;
                        }
                    }
                }
                keybindCapture = false;
                keybindCaptureModule = null;
            }
            return true;
        }

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

        if (key == GLFW.GLFW_KEY_UP) {
            if (hoveredRow > 0) hoveredRow--;
            scrollOffset = Math.max(0, Math.min(scrollOffset, hoveredRow - maxVisibleRows + 1));
            return true;
        }

        if (key == GLFW.GLFW_KEY_DOWN) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            int total = countDisplayRows(mods);
            if (hoveredRow < total - 1) hoveredRow++;
            if (hoveredRow >= scrollOffset + maxVisibleRows)
                scrollOffset = Math.max(0, hoveredRow - maxVisibleRows + 1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            handleToggleCurrentRow();
            return true;
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
                hoveredRow = -1;
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

        // Category clicks
        List<String> cats = CrestModules.getCategories();
        int sy = HEADER_H + SEARCH_H + 8;
        for (int i = 0; i < cats.size(); i++) {
            int cy = sy + i * ROW_H;
            if (mx >= SIDEBAR && mx <= SIDEBAR + CAT_W && my >= cy && my <= cy + ROW_H - 2) {
                selectedCategory = cats.get(i);
                scrollOffset = 0;
                hoveredRow = -1;
                return true;
            }
        }

        // Module area clicks
        if (selectedCategory != null && mx >= modAreaX && mx <= modAreaX + modAreaW) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            int clickedRow = ((int) my - sy) / ROW_H;
            if (clickedRow < 0) return super.mouseClicked(event, doubleClick);

            int displayIdx = 0;
            for (CrestModule mod : mods) {
                if (clickedRow == displayIdx) {
                    handleModuleClick(mod, mx, my, btn);
                    return true;
                }
                displayIdx++;

                if (expandedModules.contains(mod.getId())) {
                    List<Setting<?>> settings = mod.getSettings();
                    for (int si = 0; si < settings.size(); si++) {
                        if (clickedRow == displayIdx) {
                            handleSettingClick(mod, si, mx, btn);
                            return true;
                        }
                        displayIdx++;
                    }
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void handleModuleClick(CrestModule mod, double mx, double my, int btn) {
        if (btn == 0) {
            // Check if gear area clicked
            List<Setting<?>> settings = mod.getSettings();
            if (!settings.isEmpty()) {
                int gearX = modAreaX + 8;
                if (mx >= gearX && mx <= gearX + font.width("▶") + 4) {
                    String id = mod.getId();
                    if (expandedModules.contains(id)) expandedModules.remove(id);
                    else expandedModules.add(id);
                    return;
                }
            }
            // Toggle module
            boolean newState = !CrestModules.isEnabled(mod.getId());
            CrestModules.setEnabled(mod.getId(), newState);
        } else if (btn == 1) {
            // Right-click expands/collapses settings
            String id = mod.getId();
            if (!mod.getSettings().isEmpty()) {
                if (expandedModules.contains(id)) expandedModules.remove(id);
                else expandedModules.add(id);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSettingClick(CrestModule mod, int si, double mx, int btn) {
        if (btn != 0) return;
        List<Setting<?>> settings = mod.getSettings();
        if (si >= settings.size()) return;
        Setting<?> setting = settings.get(si);
        int indent = 16;
        int labelW = font.width(setting.getName()) + 4;
        double controlX = modAreaX + indent + labelW + 4;

        if (setting instanceof BooleanSetting bs) {
            bs.toggle();
            CrestModules.getConfigManager().markDirty();
        } else if (setting instanceof ModeSetting ms) {
            ms.cycle();
            CrestModules.getConfigManager().markDirty();
        } else if (setting instanceof ColorSetting) {
            // Simple cycle through a few colors for now
            int[] colors = {0xFFFFFFFF, 0xFFFF5555, 0xFF55FF55, 0xFF5555FF, 0xFFFFFF55, 0xFFFF55FF, 0xFF55FFFF, 0xFF888888};
            ColorSetting cs = (ColorSetting) setting;
            int cur = cs.get();
            int nextIdx = 0;
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] == cur) { nextIdx = (i + 1) % colors.length; break; }
            }
            cs.set(colors[nextIdx]);
            CrestModules.getConfigManager().markDirty();
        } else if (setting instanceof KeybindSetting ks) {
            keybindCapture = true;
            keybindCaptureModule = mod.getId();
        } else if (setting instanceof IntegerSetting is) {
            int barW = Math.min(120 - 30, 100);
            double relX = (mx - controlX) / barW;
            int min = is.getMin();
            int max = is.getMax();
            int val = (int) Math.round(min + relX * (max - min));
            is.set(val);
            CrestModules.getConfigManager().markDirty();
        } else if (setting instanceof FloatSetting fs) {
            int barW = Math.min(120 - 40, 100);
            double relX = (mx - controlX) / barW;
            float min = fs.getMin();
            float max = fs.getMax();
            float val = min + (float)(relX * (max - min));
            fs.set(val);
            CrestModules.getConfigManager().markDirty();
        }
    }

    private void handleToggleCurrentRow() {
        if (selectedCategory == null || hoveredRow < 0) return;
        List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
        int[] type = new int[1];
        int[] si = new int[1];
        CrestModule mod = getModuleAtDisplayRow(mods, hoveredRow, type, si);
        if (mod == null) return;
        if (type[0] == HEADER) {
            boolean newState = !CrestModules.isEnabled(mod.getId());
            CrestModules.setEnabled(mod.getId(), newState);
        } else if (type[0] == SETTING) {
            List<Setting<?>> settings = mod.getSettings();
            if (si[0] < settings.size()) {
                Setting<?> s = settings.get(si[0]);
                if (s instanceof BooleanSetting bs) { bs.toggle(); CrestModules.getConfigManager().markDirty(); }
                else if (s instanceof ModeSetting ms) { ms.cycle(); CrestModules.getConfigManager().markDirty(); }
                else if (s instanceof KeybindSetting) { keybindCapture = true; keybindCaptureModule = mod.getId(); }
            }
        }
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
