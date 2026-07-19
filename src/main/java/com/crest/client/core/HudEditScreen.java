package com.crest.client.core;

import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HudEditScreen extends Screen {

    private String selectedId;
    private boolean dragging;
    private boolean resizing;
    private int dragOffX;
    private int dragOffY;
    private boolean changed;

    private static final int HANDLE = 10;
    private static final int MIN_SIZE = 24;

    protected HudEditScreen() {
        super(Component.literal("Edit HUD"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new HudEditScreen());
    }

    private int rx(HudModule mod) {
        return mod.getX() < 0 ? width - mod.getRenderWidth() : mod.getX();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        int accent = Theme.getAnimatedAccent();
        g.fill(0, 0, width, height, Theme.GLASS_BG);

        List<HudModule> modules = getHudModules();
        boolean hasEnabled = false;

        for (HudModule mod : modules) {
            if (!CrestModules.isEnabled(mod.getId())) continue;
            hasEnabled = true;
            int rx = rx(mod);
            int ry = mod.getY();
            int rw = mod.getRenderWidth();
            int rh = mod.getRenderHeight();
            boolean selected = mod.getId().equals(selectedId);
            int borderCol = selected ? accent : Theme.BORDER_LIGHT;

            Panel.drawHollowRect(g, rx - 2, ry - 2, rw + 4, rh + 4, borderCol);

            if (selected) {
                // Resize handle (bottom-right corner)
                int hx = rx + rw + 4 - HANDLE;
                int hy = ry + rh + 4 - HANDLE;
                g.fill(hx, hy, hx + HANDLE, hy + HANDLE, ColorUtil.withAlpha(accent, 220));
                g.fill(hx + 2, hy + 2, hx + HANDLE - 2, hy + 2 + 1, 0xFFFFFFFF);
                g.fill(hx + 2, hy + 5, hx + HANDLE - 2, hy + 5 + 1, 0xFFFFFFFF);
                g.fill(hx + 2, hy + 2, hx + 2 + 1, hy + HANDLE - 2, 0xFFFFFFFF);
                g.fill(hx + 5, hy + 2, hx + 5 + 1, hy + HANDLE - 2, 0xFFFFFFFF);
            }

            String modeSuffix = mod instanceof ArmorHudModule a ? " [" + a.getModeLabel() + "]" : "";
            String modLabel = "[" + mod.getName() + "]" + modeSuffix;
            Component label = Component.literal(modLabel);
            int lw = font.width(label);
            int labelY = Math.max(0, ry - 11);
            int labelBg = selected ? ColorUtil.withAlpha(accent, 200) : ColorUtil.withAlpha(Theme.GLASS_BG, 220);
            g.fillGradient(rx, labelY, rx + lw + 4, labelY + 10, labelBg, ColorUtil.withAlpha(labelBg, 120));
            Panel.drawHollowRect(g, rx, labelY, lw + 4, 10, Theme.BORDER_LIGHT);
            g.text(font, label, rx + 2, labelY + 1, Theme.FOREGROUND);
        }

        if (!hasEnabled) {
            Component msg = Component.literal("No HUD modules enabled — enable them in the ClickGUI");
            g.centeredText(font, msg, width / 2, height / 2 - 10, Theme.ON_SURFACE_VARIANT);
        }

        Component hint = Component.literal("Click + drag to move  |  drag corner to resize  |  ESC to save & close");
        g.text(font, hint, (width - font.width(hint)) / 2, height - 16, Theme.TEXT_FAINT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.buttonInfo().input();
        if (btn != 0) return super.mouseClicked(event, doubleClick);

        // Selected module's resize handle takes priority
        HudModule sel = selectedId != null ? findModule(selectedId) : null;
        if (sel != null && CrestModules.isEnabled(sel.getId())) {
            int rx = rx(sel);
            int ry = sel.getY();
            int rw = sel.getRenderWidth();
            int rh = sel.getRenderHeight();
            int hx = rx + rw + 4 - HANDLE;
            int hy = ry + rh + 4 - HANDLE;
            if (mx >= hx && mx <= hx + HANDLE && my >= hy && my <= hy + HANDLE) {
                resizing = true;
                return true;
            }
        }

        for (HudModule mod : getHudModules()) {
            if (!CrestModules.isEnabled(mod.getId())) continue;
            int rx = rx(mod);
            int ry = mod.getY();
            int rw = mod.getRenderWidth();
            int rh = mod.getRenderHeight();
            if (mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh) {
                selectedId = mod.getId();
                dragging = true;
                dragOffX = (int) (mx - rx);
                dragOffY = (int) (my - ry);
                return true;
            }
        }
        selectedId = null;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (resizing && selectedId != null) {
            HudModule mod = findModule(selectedId);
            if (mod != null) {
                int rx = rx(mod);
                int ry = mod.getY();
                int newW = (int) Math.max(MIN_SIZE, Math.min(width - rx, event.x() - rx));
                int newH = (int) Math.max(MIN_SIZE, Math.min(height - ry, event.y() - ry));
                mod.setSize(newW, newH);
                HudSettings.setSize(selectedId, newW, newH);
                changed = true;
            }
            return true;
        }
        if (dragging && selectedId != null) {
            HudModule mod = findModule(selectedId);
            if (mod != null) {
                int newX = (int) Math.max(0, event.x() - dragOffX);
                int newY = (int) Math.max(0, event.y() - dragOffY);
                mod.setX(newX);
                mod.setY(newY);
                HudSettings.setPosition(selectedId, newX, newY);
                changed = true;
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        resizing = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 || event.key() == 342) {
            if (changed) HudSettings.save();
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<HudModule> getHudModules() {
        List<HudModule> list = new ArrayList<>();
        for (CrestModule m : CrestModules.getAll().values()) {
            if (m instanceof HudModule h) list.add(h);
        }
        return list;
    }

    private HudModule findModule(String id) {
        CrestModule m = CrestModules.get(id);
        return m instanceof HudModule h ? h : null;
    }
}
