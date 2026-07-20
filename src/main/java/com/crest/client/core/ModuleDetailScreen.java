package com.crest.client.core;

import com.crest.client.core.setting.*;
import com.crest.client.ui.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ModuleDetailScreen extends Screen {
    private static final int HEADER_H = 48;
    private static final int ROW_H = 26;
    private static final int PANEL_MIN_W = 300;
    private static final int PANEL_MAX_W = 480;

    private final CrestModule module;
    private final Screen parent;
    private final List<Setting<?>> settings;

    private int panelX, panelY, panelW, panelH;

    private final Map<Setting<?>, Widget> widgetCache = new HashMap<>();
    private final Map<String, Animated> toggleAnim = new HashMap<>();
    private Widget activeWidget;
    private final ScrollContainer scroll = new ScrollContainer();

    // keybind capture
    private boolean keybindCapture;
    private String keybindCaptureModule;

    // color picker
    private ColorPicker colorPicker;
    private final Animated colorPickerAnim = new Animated(0f, 12f);

    // animations
    private final Animated openAnim = new Animated(0f, 10f);
    private int hoveredRow = -1;

    public ModuleDetailScreen(CrestModule module, Screen parent) {
        super(Component.literal(module.getName()));
        this.module = module;
        this.parent = parent;
        this.settings = module.getSettings();
        openAnim.setImmediate(0f);
        openAnim.set(1f);
    }

    @Override
    protected void init() {
        panelW = Math.max(PANEL_MIN_W, Math.min(width - Spacing.S10, PANEL_MAX_W));
        panelX = (width - panelW) / 2;
        panelY = HEADER_H + Spacing.S2;
        panelH = height - panelY - Spacing.S2;
    }

    private Animated toggleFor(String id) {
        return toggleAnim.computeIfAbsent(id, k -> new Animated(0f, 12f));
    }

    private Widget getWidget(Setting<?> setting) {
        return widgetCache.computeIfAbsent(setting, s -> {
            Widget w;
            if (s instanceof BooleanSetting bs) w = new ToggleRow(bs);
            else if (s instanceof IntegerSetting || s instanceof FloatSetting) w = new SliderRow(s);
            else if (s instanceof StringSetting ss) w = new TextRow(ss);
            else if (s instanceof ModeSetting ms) w = new ModeRow(ms);
            else if (s instanceof ColorSetting cs) {
                ColorRow cr = new ColorRow(cs);
                cr.setOnPick(cs2 -> colorPicker = new ColorPicker().setValue(cs2.get()).setOnPick(rgb -> { cs2.set(rgb); CrestModules.getConfigManager().markDirty(); }));
                w = cr;
            } else if (s instanceof KeybindSetting ks) {
                KeybindRow kr = new KeybindRow(ks);
                kr.setOnCapture((modId, ks2) -> { keybindCapture = true; keybindCaptureModule = ks2.getModuleId(); });
                w = kr;
            } else w = null;
            return w;
        });
    }

    private List<Widget> getVisibleWidgets() {
        List<Widget> list = new ArrayList<>();
        for (Setting<?> s : settings) {
            if (s.isVisible()) list.add(getWidget(s));
        }
        return list;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        openAnim.tick(delta);
        colorPickerAnim.tick(delta);
        float open = openAnim.get();

        g.fill(0, 0, width, height, ColorUtil.withAlpha(Theme.GLASS_BG, (int) (Theme.glassOpacity * open)));

        int wy = (int) ((1 - open) * 24);
        g.pose().pushMatrix();
        g.pose().translate(0, wy);

        renderHeader(g, mx, my);
        renderBody(g, mx, my, delta);

        if (colorPicker != null) {
            colorPickerAnim.set(1f);
            renderColorPicker(g, mx, my);
        } else {
            colorPickerAnim.set(0f);
        }

        g.pose().popMatrix();
    }

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my) {
        boolean enabled = CrestModules.isEnabled(module.getId());
        Animated ta = toggleFor(module.getId() + "_toggle");
        ta.set(enabled ? 1f : 0f);
        ta.tick(0.016f);

        g.fillGradient(0, 0, width, HEADER_H, Theme.GLASS_BG, ColorUtil.withAlpha(Theme.GLASS_BG, 110));
        int accent = Theme.getAnimatedAccent();

        // Back button
        String back = "\u2190";
        int backW = font.width(back) + Spacing.S2;
        boolean backHover = mx >= Spacing.S3 && mx <= Spacing.S3 + backW && my >= Spacing.S2 && my <= HEADER_H - Spacing.S2;
        if (backHover) {
            g.fill(Spacing.S3, Spacing.S2, Spacing.S3 + backW, HEADER_H - Spacing.S2, ColorUtil.withAlpha(accent, 20));
        }
        g.text(font, Component.literal(back), Spacing.S3, (HEADER_H - font.lineHeight) / 2,
            backHover ? accent : Theme.FOREGROUND);

        // Module name
        int nameX = Spacing.S3 + backW + Spacing.S3;
        g.text(font, Component.literal(module.getName()), nameX, (HEADER_H - font.lineHeight) / 2, Theme.FOREGROUND);

        // Description
        String desc = module.getDescription();
        if (!desc.isEmpty()) {
            g.text(font, Component.literal(desc), nameX, (HEADER_H - font.lineHeight) / 2 + font.lineHeight + 1,
                ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 180));
        }

        // Settings count
        int count = settings.size();
        String countStr = count + " setting" + (count != 1 ? "s" : "");
        int countW = font.width(countStr);
        int countX = width - Spacing.S3 - countW - 44 - Spacing.S3;
        g.text(font, Component.literal(countStr), countX, HEADER_H - font.lineHeight - Spacing.S1,
            ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 100));

        // Toggle
        int toggleX = width - Spacing.S3 - 44;
        drawToggle(g, toggleX, (HEADER_H - 24) / 2, enabled, ta.get());
    }

    private void drawToggle(GuiGraphicsExtractor g, int x, int y, boolean on, float anim) {
        int w = 44, h = 24;
        int trackColor = on
            ? ColorUtil.lerpARGB(0x1AFFFFFF, Theme.getAnimatedAccent(), anim)
            : ColorUtil.lerpARGB(0x1AFFFFFF, 0x2AFFFFFF, anim);
        g.fillGradient(x, y, x + w, y + h, trackColor, ColorUtil.withAlpha(trackColor, 80));
        Panel.drawHollowRect(g, x, y, w, h, Theme.BORDER_LIGHT);
        int knobMinX = x + 3;
        int knobMaxX = x + w - 19;
        int knobX = (int) Anim.lerp(knobMinX, knobMaxX, anim);
        int knobColor = ColorUtil.lerpARGB(Theme.MUTED_FOREGROUND, Theme.PRIMARY, anim);
        g.fill(knobX, y + 3, knobX + 16, y + h - 3, knobColor);
    }

    private void renderBody(GuiGraphicsExtractor g, int mx, int my, float delta) {
        List<Widget> widgets = getVisibleWidgets();
        int contentH = widgets.size() * ROW_H + Spacing.S4;

        int bh = Math.min(panelH, contentH + Spacing.S2);
        Panel.drawElevated(g, panelX, panelY, panelW, bh, ColorUtil.withAlpha(Theme.CARD, 200), Theme.ELEVATION_1);
        g.fill(panelX + 2, panelY, panelX + panelW - 2, panelY + 1, ColorUtil.withAlpha(Theme.getAnimatedAccent(), 80));

        scroll.set(panelX + Spacing.S3, panelY + Spacing.S2, panelW - Spacing.S3 * 2, bh - Spacing.S4, ROW_H, widgets);
        scroll.hoverColor = ColorUtil.withAlpha(Theme.MUTED, 100);

        // Draw dividers between rows
        for (int i = 1; i < widgets.size(); i++) {
            int dy = scroll.y + i * ROW_H - (int) scroll.scrollOffset;
            if (dy >= scroll.y && dy <= scroll.y + scroll.h) {
                g.fill(scroll.x, dy - 1, scroll.x + scroll.w, dy, ColorUtil.withAlpha(Theme.BORDER_LIGHT, 40));
            }
        }

        scroll.render(g, font, mx, my, delta);
        hoveredRow = -1;
        for (int i = 0; i < widgets.size(); i++) {
            int cy = scroll.y + i * ROW_H - (int) scroll.scrollOffset;
            if (mx >= scroll.x && mx <= scroll.x + scroll.w && my >= cy && my <= cy + ROW_H) {
                hoveredRow = i;
                break;
            }
        }

        // Hover tooltip for setting description
        if (hoveredRow >= 0 && hoveredRow < settings.size()) {
            Setting<?> s = settings.get(hoveredRow);
            String sdesc = s.getDescription();
            if (sdesc != null && !sdesc.isEmpty()) {
                int tx = mx + 12;
                int ty = my - 20;
                if (tx + 150 > width) tx = mx - 160;
                if (ty < 0) ty = my + 12;
                int tw = font.width(sdesc) + 8;
                int th = font.lineHeight + 4;
                Panel.draw(g, tx, ty, tw, th, ColorUtil.withAlpha(Theme.POPOVER, 230));
                g.text(font, Component.literal(sdesc), tx + 4, ty + 2, Theme.POPOVER_FOREGROUND);
            }
        }
    }

    // --- Input ---

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (activeWidget != null && activeWidget.charTyped(cp, 0)) return true;
        return super.charTyped(event);
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

        if (activeWidget != null && activeWidget.keyPressed(key, 0, 0)) return true;

        if (colorPicker != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { closeColorPicker(); return true; }
            return super.keyPressed(event);
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int btn = event.buttonInfo().input();

        if (btn == 0 && colorPicker != null) {
            if (handleColorPickerClick(mx, my)) return true;
            return super.mouseClicked(event, doubleClick);
        }

        // back button
        String back = "\u2190";
        int backW = font.width(back) + Spacing.S2;
        if (btn == 0 && mx >= Spacing.S3 && mx <= Spacing.S3 + backW && my >= Spacing.S2 && my <= HEADER_H - Spacing.S2) {
            onClose();
            return true;
        }

        // toggle in header
        if (btn == 0) {
            int toggleX = width - Spacing.S3 - 44;
            if (mx >= toggleX && mx <= toggleX + 44 && my >= (HEADER_H - 24) / 2 && my <= (HEADER_H + 24) / 2) {
                CrestModules.setEnabled(module.getId(), !CrestModules.isEnabled(module.getId()));
                return true;
            }
        }

        // settings inside panel
        if (scroll.mouseClicked(mx, my, btn)) {
            Widget child = scroll.childAt(my);
            if (child != null) {
                activeWidget = child;
                if (child instanceof KeybindRow kr) {
                    kr.setCapturing(keybindCapture && keybindCaptureModule != null);
                }
            }
            return true;
        }

        if (activeWidget instanceof TextRow && btn == 0) activeWidget = null;

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        if (colorPicker != null) { handleColorPickerClick(mx, my); return true; }

        if (activeWidget instanceof SliderRow sr) {
            sr.mouseDragged(mx, my);
            return true;
        }

        if (scroll.mouseDragged(mx, my)) return true;

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll.mouseScrolled(deltaY);
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void renderColorPicker(GuiGraphicsExtractor g, int mx, int my) {
        int pw = 220, ph = 260;
        int px = Math.min(width / 2 - pw / 2, width - pw - Spacing.S2);
        int py = HEADER_H + Spacing.S3;
        int a = (int) (255 * colorPickerAnim.get());
        int accent = Theme.getAnimatedAccent();

        Panel.drawElevated(g, px, py, pw, ph, ColorUtil.withAlpha(Theme.CARD, a), Theme.ELEVATION_3);
        g.fill(px + 2, py, px + pw - 2, py + 1, ColorUtil.withAlpha(accent, Theme.topStripAlpha));

        colorPicker.layout(px, py, pw, ph);
        colorPicker.render(g, font, mx, my, a);
    }

    private boolean handleColorPickerClick(double mx, double my) {
        if (colorPicker.handleClick(mx, my)) {
            CrestModules.getConfigManager().markDirty();
            return true;
        }
        return false;
    }

    private void closeColorPicker() { colorPicker = null; CrestModules.getConfigManager().save(); }

    @Override
    public boolean isPauseScreen() { return false; }
}
