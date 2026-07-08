package com.crest.client.core;

import com.crest.client.core.setting.*;
import com.crest.client.ui.Anim;
import com.crest.client.ui.Animated;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CrestClickGui extends Screen {
    private static final int CAT_W = 140;
    private static final int SIDEBAR = 12;
    private static final int HEADER_H = 26;
    private static final int SEARCH_H = 20;
    private static final int ROW_H = 22;
    private static final int SETTING_ROW_H = 20;
    private static final int TITLE_H = 14;

    private String selectedCategory;
    private int hoveredRow = -1;
    private int hoveredSettingCol = -1;
    private float scrollOffset = 0;
    private float scrollTarget = 0;
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

    // Animations
    private final Animated openAnim = new Animated(0f, 10f);
    private final Animated accentPulse = new Animated(0f, 4f);
    private int lastMaxRows = 0;
    private final java.util.Map<String, Animated> toggleAnim = new java.util.HashMap<>();
    private final java.util.Map<String, Animated> hoverAnim = new java.util.HashMap<>();
    private final Animated colorPickerAnim = new Animated(0f, 12f);
    private ColorPickerState colorPicker;

    protected CrestClickGui() {
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
        lastMaxRows = maxVisibleRows;
        openAnim.setImmediate(0f);
        openAnim.set(1f);
        Theme.load();
    }

    // --- Row counting ---

    private record DisplayRow(CrestModule module, int type, int settingIndex, Setting<?> setting) {}
    private static final int HEADER = 0;
    private static final int SETTING = 1;

    private int countDisplayRows(List<CrestModule> mods) {
        int rows = 0;
        for (CrestModule mod : mods) {
            rows++;
            if (expandedModules.contains(mod.getId())) rows += mod.getSettings().size();
        }
        return rows;
    }

    private CrestModule getModuleAtDisplayRow(List<CrestModule> mods, int displayRow, int[] outType, int[] outSettingIdx) {
        int row = 0;
        for (CrestModule mod : mods) {
            if (row == displayRow) { outType[0] = HEADER; return mod; }
            row++;
            if (expandedModules.contains(mod.getId())) {
                List<Setting<?>> settings = mod.getSettings();
                for (int si = 0; si < settings.size(); si++) {
                    if (row == displayRow) { outType[0] = SETTING; outSettingIdx[0] = si; return mod; }
                    row++;
                }
            }
        }
        return null;
    }

    private List<DisplayRow> buildRows(List<CrestModule> mods) {
        List<DisplayRow> rows = new ArrayList<>();
        for (CrestModule mod : mods) {
            rows.add(new DisplayRow(mod, HEADER, -1, null));
            if (expandedModules.contains(mod.getId())) {
                for (int si = 0; si < mod.getSettings().size(); si++) {
                    rows.add(new DisplayRow(mod, SETTING, si, mod.getSettings().get(si)));
                }
            }
        }
        return rows;
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
        editBtnX = width - ew - 8;
        editBtnW = ew;
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

            int txtColor = selected ? Theme.TEXT : hover ? 0xFFD8DCFF : Theme.TEXT_DIM;
            g.text(font, Component.literal(cat), SIDEBAR + 12, cy + 5, txtColor);

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

        List<CrestModule> allMods = CrestModules.getByCategory(selectedCategory);
        List<CrestModule> mods = filterBySearch(allMods);
        int totalRows = countDisplayRows(mods);

        maxVisibleRows = (height - HEADER_H - SEARCH_H - 24) / ROW_H;
        if (maxVisibleRows != lastMaxRows) lastMaxRows = maxVisibleRows;

        scrollTarget = Anim.clamp(scrollTarget, 0, Math.max(0, totalRows - maxVisibleRows));
        scrollOffset += (scrollTarget - scrollOffset) * Anim.smooth(delta, 18f);

        int listTop = HEADER_H + SEARCH_H + 12;
        int listH = maxVisibleRows * ROW_H;

        Panel.draw(g, modAreaX, listTop - 4, modAreaW, listH + 8, ColorUtil.withAlpha(Theme.BG_PANEL, 200));

        g.enableScissor(modAreaX, listTop, modAreaX + modAreaW, listTop + listH);

        hoveredRow = -1;
        hoveredSettingCol = -1;
        int startRow = (int) Math.floor(scrollOffset);
        float frac = scrollOffset - startRow;

        List<DisplayRow> rows = buildRows(mods);
        int sy = listTop - (int) (frac * ROW_H);

        for (int i = startRow; i < rows.size(); i++) {
            DisplayRow row = rows.get(i);
            int rowY = sy + (i - startRow) * ROW_H;
            if (rowY > listTop + listH) break;
            if (rowY + ROW_H < listTop) continue;

            if (row.type == HEADER) {
                renderModuleRow(g, mx, my, row.module, rowY, i);
            } else {
                renderSettingRow(g, mx, my, row.module, row.setting, row.settingIndex, rowY, i);
            }
        }

        g.disableScissor();

        if (totalRows > maxVisibleRows) {
            int trackX = modAreaX + modAreaW - 4;
            int trackY = listTop;
            int trackH = listH;
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, ColorUtil.withAlpha(Theme.BG_BASE, 200));
            float thumbH = (float) maxVisibleRows / totalRows * trackH;
            float thumbY = scrollOffset / totalRows * trackH;
            g.fill(trackX, trackY + (int) thumbY, trackX + 3, trackY + (int) (thumbY + thumbH), Theme.getAnimatedAccent());
        }

        // color picker popout
        colorPickerAnim.tick(delta);
        if (colorPicker != null) {
            colorPickerAnim.set(1f);
            renderColorPicker(g, mx, my);
        } else {
            colorPickerAnim.set(0f);
        }
    }

    private void renderModuleRow(GuiGraphicsExtractor g, int mx, int my, CrestModule mod, int cy, int displayIdx) {
        boolean hover = mx >= modAreaX && mx <= modAreaX + modAreaW && my >= cy && my <= cy + ROW_H - 2;
        if (hover) hoveredRow = displayIdx;

        Animated ha = hoverFor(mod.getId());
        ha.set(hover ? 1f : 0f);
        ha.tick(0.016f);
        boolean enabled = CrestModules.isEnabled(mod.getId());

        if (hover) g.fill(modAreaX + 4, cy, modAreaX + modAreaW - 4, cy + ROW_H - 2, ColorUtil.withAlpha(Theme.BG_HOVER, (int) (150 * ha.get())));

        // animated enable indicator
        Animated ta = toggleFor(mod.getId());
        ta.set(enabled ? 1f : 0f);
        ta.tick(0.016f);
        int indX = modAreaX + 8;
        int indW = 22;
        int indY = cy + 5;
        int indH = ROW_H - 12;
        g.fill(indX, indY, indX + indW, indY + indH, ColorUtil.withAlpha(enabled ? Theme.TEXT_ON : 0x55333344, 255));
        int knob = indX + 2 + (int) ((indW - 10) * Anim.easeOutCubic(ta.get()));
        int onCol = ColorUtil.lerpARGB(0x55555566, Theme.getAnimatedAccent(), ta.get());
        g.fill(indX + 2 + (int) ((indW - 10) * ta.get()), indY + 1, indX + 2 + (int) ((indW - 10) * ta.get()) + 8, indY + indH - 1, onCol);

        List<Setting<?>> settings = mod.getSettings();
        boolean hasSettings = !settings.isEmpty();
        String gear = hasSettings ? (expandedModules.contains(mod.getId()) ? "▾" : "▸") + " " : "";
        String name = gear + mod.getName();
        int toggleW = font.width("[ON]") + 4;

        int nameMaxW = modAreaW - indW - 18 - toggleW;
        String displayName = font.width(name) > nameMaxW
            ? font.plainSubstrByWidth(name, nameMaxW - 4) + "..."
            : name;

        g.text(font, Component.literal(displayName), modAreaX + indW + 14, cy + 5, Theme.TEXT);
        g.text(font, Component.literal("[ON]"), modAreaX + modAreaW - toggleW, cy + 5, enabled ? Theme.TEXT_ON : Theme.TEXT_OFF);

        if (hover && !mod.getDescription().isEmpty()) {
            renderDescription(g, mod.getDescription(), mx, cy);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderSettingRow(GuiGraphicsExtractor g, int mx, int my, CrestModule mod, Setting<?> setting, int si, int cy, int displayIdx) {
        if (!setting.isVisible()) return;

        boolean hover = mx >= modAreaX && mx <= modAreaX + modAreaW && my >= cy && my <= cy + SETTING_ROW_H - 2;
        if (hover) { hoveredRow = displayIdx; hoveredSettingCol = si; }

        int indent = 20;
        int labelW = font.width(setting.getName()) + 4;
        int controlX = modAreaX + indent + labelW + 4;
        int controlW = modAreaW - indent - labelW - 8;

        if (hover) g.fill(modAreaX + indent, cy, modAreaX + modAreaW - 4, cy + SETTING_ROW_H - 2, ColorUtil.withAlpha(Theme.BG_HOVER, 120));
        g.text(font, Component.literal(setting.getName()), modAreaX + indent + 2, cy + 4, Theme.TEXT_DIM);

        if (setting instanceof BooleanSetting bs) {
            String txt = bs.get() ? "[ON]" : "[OFF]";
            int col = bs.get() ? Theme.TEXT_ON : Theme.TEXT_OFF;
            g.text(font, Component.literal(txt), controlX, cy + 4, col);
        } else if (setting instanceof IntegerSetting is) {
            drawSlider(g, controlX, cy, controlW, is.get(), is.getMin(), is.getMax());
            g.text(font, Component.literal(String.valueOf(is.get())), controlX + Math.min(controlW - 30, 110) + 6, cy + 3, Theme.TEXT);
        } else if (setting instanceof FloatSetting fs) {
            drawSlider(g, controlX, cy, controlW, fs.get(), fs.getMin(), fs.getMax());
            g.text(font, Component.literal(String.format("%.2f", fs.get())), controlX + Math.min(controlW - 40, 110) + 6, cy + 3, Theme.TEXT);
        } else if (setting instanceof ModeSetting ms) {
            String modeStr = "[" + ms.getMode() + "]";
            int col = hover ? 0xFFD8DCFF : Theme.TEXT_DIM;
            g.text(font, Component.literal(modeStr), controlX, cy + 4, col);
        } else if (setting instanceof ColorSetting cs) {
            int previewX = controlX;
            int previewY = cy + 2;
            int ps = 13;
            g.fill(previewX - 1, previewY - 1, previewX + ps + 1, previewY + ps + 1, 0xFF000000);
            g.fill(previewX, previewY, previewX + ps, previewY + ps, 0xFF000000 | cs.getRGB());
            g.text(font, Component.literal(String.format("#%06X", cs.getRGB())), previewX + ps + 5, cy + 4, Theme.TEXT_DIM);
        } else if (setting instanceof KeybindSetting ks) {
            String keyName = ks.getKeyName();
            if (keybindCapture && mod.getId().equals(keybindCaptureModule)) keyName = "...";
            int col = hover ? 0xFFD8DCFF : Theme.TEXT_DIM;
            g.text(font, Component.literal("[" + keyName + "]"), controlX, cy + 4, col);
        }
    }

    private void drawSlider(GuiGraphicsExtractor g, int x, int y, int maxW, float val, float min, float max) {
        int barW = Math.min(maxW - 34, 110);
        int barX = x;
        int barY = y + 6;
        float frac = (max > min) ? (val - min) / (max - min) : 0f;
        frac = Anim.clamp(frac, 0, 1);
        g.fill(barX, barY, barX + barW, barY + 4, ColorUtil.withAlpha(Theme.BG_BASE, 220));
        int fillW = (int) (frac * barW);
        g.fill(barX, barY, barX + fillW, barY + 4, Theme.getAnimatedAccent());
        g.fill(barX + fillW - 2, barY - 2, barX + fillW + 2, barY + 6, 0xFFFFFFFF);
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

    // --- Color picker ---

    private static final class ColorPickerState {
        final ColorSetting setting;
        final CrestModule module;
        int h = 0, s = 100, v = 100;
        int _sx, _sy, _sq, _hx, _hy, _hh;
        ColorPickerState(ColorSetting cs, CrestModule mod) {
            this.setting = cs; this.module = mod;
            int rgb = cs.getRGB();
            float[] hsv = rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            h = (int) (hsv[0] * 360); s = (int) (hsv[1] * 100); v = (int) (hsv[2] * 100);
        }
    }

    private void renderColorPicker(GuiGraphicsExtractor g, int mx, int my) {
        int pw = 200, ph = 200;
        int px = Math.min(modAreaX + modAreaW - pw, width - pw - 8);
        int py = HEADER_H + 10;
        int a = (int) (255 * colorPickerAnim.get());
        Panel.drawGlass(g, px, py, pw, ph, ColorUtil.withAlpha(Theme.BG_PANEL, a), Theme.getAnimatedAccent());

        int sq = 130;
        int sx = px + 12, sy = py + 30;
        int hueBarX = sx + sq + 12, hueBarW = 14, hueBarH = sq;
        // saturation/value square
        for (int yy = 0; yy < sq; yy += 2) {
            for (int xx = 0; xx < sq; xx += 2) {
                float sat = xx / (float) sq;
                float val = 1 - yy / (float) sq;
                int col = hsvToInt(colorPicker.h / 360f, sat, val, 255);
                g.fill(sx + xx, sy + yy, sx + xx + 2, sy + yy + 2, col);
            }
        }
        // hue bar
        for (int yy = 0; yy < hueBarH; yy += 2) {
            float hh = 1 - yy / (float) hueBarH;
            int col = hsvToInt(hh, 1, 1, 255);
            g.fill(hueBarX, sy + yy, hueBarX + hueBarW, sy + yy + 2, col);
        }
        // selection markers
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

    private static int hsvToInt(float h, float s, float v, int a) {
        return ColorUtil.hsvToInt(h, s, v, a / 255f);
    }

    // --- Input ---

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!searchFocused) return super.charTyped(event);
        int cp = event.codepoint();
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

        if (colorPicker != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { closeColorPicker(); return true; }
            return super.keyPressed(event);
        }

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
            int total = countDisplayRows(mods);
            if (hoveredRow < total - 1) hoveredRow++;
            if (hoveredRow >= scrollTarget + maxVisibleRows) scrollTarget = Math.max(0, hoveredRow - maxVisibleRows + 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { handleToggleCurrentRow(); return true; }
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

        if (btn == 0 && colorPicker != null) {
            if (handleColorPickerClick(mx, my)) return true;
            return super.mouseClicked(event, doubleClick);
        }

        if (btn == 0) {
            if (my >= 3 && my <= HEADER_H - 3 && mx >= editBtnX && mx <= editBtnX + editBtnW) {
                minecraft.setScreen(new HudEditScreen());
                return true;
            }
            if (my >= HEADER_H && my <= HEADER_H + SEARCH_H + 5 && mx >= SIDEBAR && mx <= SIDEBAR + CAT_W) {
                searchFocused = true;
                return true;
            }
        }
        searchFocused = false;

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

        if (selectedCategory != null && mx >= modAreaX && mx <= modAreaX + modAreaW) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            List<DisplayRow> rows = buildRows(mods);
            int startRow = (int) Math.floor(scrollOffset);
            int listTop = HEADER_H + SEARCH_H + 12;
            float frac = scrollOffset - startRow;
            int baseY = listTop - (int) (frac * ROW_H);
            for (int i = startRow; i < rows.size(); i++) {
                int rowY = baseY + (i - startRow) * ROW_H;
                if (rowY > listTop + maxVisibleRows * ROW_H) break;
                if (my < rowY || my > rowY + ROW_H - 2) continue;
                DisplayRow row = rows.get(i);
                if (row.type == HEADER) handleModuleClick(row.module, mx, my, btn);
                else handleSettingClick(row.module, row.settingIndex, mx, btn);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void handleModuleClick(CrestModule mod, double mx, double my, int btn) {
        List<Setting<?>> settings = mod.getSettings();
        boolean hasSettings = !settings.isEmpty();
        int indW = 22;
        int gearX = modAreaX + indW + 14;
        int gearW = font.width("▸ ");

        if (btn == 0) {
            if (hasSettings && mx >= gearX && mx <= gearX + gearW) {
                String id = mod.getId();
                if (expandedModules.contains(id)) expandedModules.remove(id); else expandedModules.add(id);
                return;
            }
            CrestModules.setEnabled(mod.getId(), !CrestModules.isEnabled(mod.getId()));
        } else if (btn == 1) {
            if (hasSettings) {
                String id = mod.getId();
                if (expandedModules.contains(id)) expandedModules.remove(id); else expandedModules.add(id);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSettingClick(CrestModule mod, int si, double mx, int btn) {
        if (btn != 0) return;
        List<Setting<?>> settings = mod.getSettings();
        if (si >= settings.size()) return;
        Setting<?> setting = settings.get(si);
        int indent = 20;
        int labelW = font.width(setting.getName()) + 4;
        double controlX = modAreaX + indent + labelW + 4;

        if (setting instanceof BooleanSetting bs) { bs.toggle(); CrestModules.getConfigManager().markDirty(); }
        else if (setting instanceof ModeSetting ms) { ms.cycle(); CrestModules.getConfigManager().markDirty(); }
        else if (setting instanceof ColorSetting cs) { colorPicker = new ColorPickerState(cs, mod); }
        else if (setting instanceof KeybindSetting ks) { keybindCapture = true; keybindCaptureModule = mod.getId(); }
        else if (setting instanceof IntegerSetting is) { setSliderInt(is, mx, controlX, modAreaW - indent - labelW - 8); }
        else if (setting instanceof FloatSetting fs) { setSliderFloat(fs, mx, controlX, modAreaW - indent - labelW - 8); }
    }

    private void setSliderInt(IntegerSetting is, double mx, double controlX, int controlW) {
        int barW = Math.min(controlW - 34, 110);
        double relX = (mx - controlX) / barW;
        relX = Anim.clamp((float) relX, 0, 1);
        int val = (int) Math.round(is.getMin() + relX * (is.getMax() - is.getMin()));
        is.set(val);
        CrestModules.getConfigManager().markDirty();
    }
    private void setSliderFloat(FloatSetting fs, double mx, double controlX, int controlW) {
        int barW = Math.min(controlW - 40, 110);
        double relX = (mx - controlX) / barW;
        relX = Anim.clamp((float) relX, 0, 1);
        float val = fs.getMin() + (float) relX * (fs.getMax() - fs.getMin());
        fs.set(val);
        CrestModules.getConfigManager().markDirty();
    }

    private boolean handleColorPickerClick(double mx, double my) {
        ColorPickerState st = colorPicker;
        int px = Math.min(modAreaX + modAreaW - 200, width - 200 - 8);
        int py = HEADER_H + 10;
        int pw = 200, ph = 200;
        if (mx >= px + pw - 44 && mx <= px + pw - 6 && my >= py + ph - 18 && my <= py + ph - 6) {
            closeColorPicker();
            return true;
        }
        if (mx >= st._hx && mx <= st._hx + 14 && my >= st._hy && my <= st._hy + st._hh) {
            st.h = (int) ((1 - (my - st._hy) / (float) st._hh) * 360);
            applyColor();
            return true;
        }
        if (mx >= st._sx && mx <= st._sx + st._sq && my >= st._sy && my <= st._sy + st._sq) {
            st.s = (int) ((mx - st._sx) / (float) st._sq * 100);
            st.v = (int) ((1 - (my - st._sy) / (float) st._sq) * 100);
            applyColor();
            return true;
        }
        return false;
    }

    private void applyColor() {
        ColorPickerState st = colorPicker;
        int rgb = hsvToInt(st.h / 360f, st.s / 100f, st.v / 100f, 255);
        st.setting.set(rgb);
        CrestModules.getConfigManager().markDirty();
    }

    private void closeColorPicker() {
        colorPicker = null;
        CrestModules.getConfigManager().save();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        if (colorPicker != null) { handleColorPickerClick(mx, my); return true; }

        if (selectedCategory != null && mx >= modAreaX && mx <= modAreaX + modAreaW) {
            List<DisplayRow> rows = buildRows(filterBySearch(CrestModules.getByCategory(selectedCategory)));
            int startRow = (int) Math.floor(scrollOffset);
            int listTop = HEADER_H + SEARCH_H + 12;
            float frac = scrollOffset - startRow;
            int baseY = listTop - (int) (frac * ROW_H);
            for (int i = startRow; i < rows.size(); i++) {
                int rowY = baseY + (i - startRow) * ROW_H;
                if (rowY > listTop + maxVisibleRows * ROW_H) break;
                if (my < rowY || my > rowY + ROW_H - 2) continue;
                DisplayRow row = rows.get(i);
                if (row.type != SETTING) break;
                Setting<?> s = row.setting;
                int indent = 20, labelW = font.width(s.getName()) + 4;
                double controlX = modAreaX + indent + labelW + 4;
                int controlW = modAreaW - indent - labelW - 8;
                if (s instanceof IntegerSetting is) { setSliderInt(is, mx, controlX, controlW); return true; }
                if (s instanceof FloatSetting fs) { setSliderFloat(fs, mx, controlX, controlW); return true; }
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double y = deltaY;
        if (selectedCategory != null) {
            List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
            int total = countDisplayRows(mods);
            scrollTarget = Anim.clamp(scrollTarget - (float) y * 3, 0, Math.max(0, total - maxVisibleRows));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void handleToggleCurrentRow() {
        if (selectedCategory == null || hoveredRow < 0) return;
        List<CrestModule> mods = filterBySearch(CrestModules.getByCategory(selectedCategory));
        int[] type = new int[1], si = new int[1];
        CrestModule mod = getModuleAtDisplayRow(mods, hoveredRow, type, si);
        if (mod == null) return;
        if (type[0] == HEADER) {
            CrestModules.setEnabled(mod.getId(), !CrestModules.isEnabled(mod.getId()));
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

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf));
        float h, s, v = max;
        float d = max - min;
        s = max == 0 ? 0 : d / max;
        if (d == 0) h = 0;
        else if (max == rf) h = ((gf - bf) / d) % 6;
        else if (max == gf) h = (bf - rf) / d + 2;
        else h = (rf - gf) / d + 4;
        h /= 6;
        if (h < 0) h += 1;
        return new float[]{h, s, v};
    }

    private List<CrestModule> filterBySearch(List<CrestModule> mods) {
        if (searchQuery.isEmpty()) return mods;
        String q = searchQuery.toLowerCase();
        return mods.stream()
            .filter(m -> m.getName().toLowerCase().contains(q) || m.getId().toLowerCase().contains(q))
            .collect(Collectors.toList());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
