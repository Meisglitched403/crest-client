package com.crest.client.core;

import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HudEditScreen extends Screen {
    private static final int OVERLAY = 0x60000000;
    private static final int BORDER = 0x88FFFFFF;
    private static final int SELECTED = 0xCC5555FF;
    private static final int LABEL_BG = 0xCC222244;

    private String selectedId;
    private boolean dragging;
    private int dragOffX;
    private int dragOffY;
    private boolean changed;

    protected HudEditScreen() {
        super(Component.literal("Edit HUD"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        int accent = Theme.getAnimatedAccent();
        g.fill(0, 0, width, height, OVERLAY);

        Minecraft mc = Minecraft.getInstance();
        List<HudModule> modules = getHudModules();
        boolean hasEnabled = false;

        for (HudModule mod : modules) {
            if (!CrestModules.isEnabled(mod.getId())) continue;
            hasEnabled = true;
            int rx = mod.getX() < 0 ? width - mod.getWidth() : mod.getX();
            int ry = mod.getY();
            boolean selected = mod.getId().equals(selectedId);
            int borderCol = selected ? accent : BORDER;

            g.fill(rx - 2, ry - 2, rx + mod.getWidth() + 2, ry - 1, borderCol);
            g.fill(rx - 2, ry + mod.getHeight() + 1, rx + mod.getWidth() + 2, ry + mod.getHeight() + 2, borderCol);
            g.fill(rx - 2, ry - 2, rx - 1, ry + mod.getHeight() + 2, borderCol);
            g.fill(rx + mod.getWidth() + 1, ry - 2, rx + mod.getWidth() + 2, ry + mod.getHeight() + 2, borderCol);

            String modeSuffix = mod instanceof ArmorHudModule a ? " [" + a.getModeLabel() + "]" : "";
            String modLabel = "[" + mod.getName() + "]" + modeSuffix;
            Component label = Component.literal(modLabel);
            int lw = font.width(label);
            int labelY = Math.max(0, ry - 11);
            g.fill(rx, labelY, rx + lw + 4, labelY + 10, selected ? ColorUtil.withAlpha(accent, 200) : LABEL_BG);
            g.text(font, label, rx + 2, labelY + 1, 0xFFFFFFFF);
        }

        if (!hasEnabled) {
            Component msg = Component.literal("No HUD modules enabled — enable them in the ClickGUI");
            g.centeredText(font, msg, width / 2, height / 2 - 10, 0xFF888888);
        }

        Component hint = Component.literal("Click + drag to move  |  ESC to save & close");
        g.text(font, hint, (width - font.width(hint)) / 2, height - 16, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.buttonInfo().input();
        if (btn != 0) return super.mouseClicked(event, doubleClick);

        for (HudModule mod : getHudModules()) {
            if (!CrestModules.isEnabled(mod.getId())) continue;
            int rx = mod.getX() < 0 ? width - mod.getWidth() : mod.getX();
            int ry = mod.getY();
            if (mx >= rx && mx <= rx + mod.getWidth() && my >= ry && my <= ry + mod.getHeight()) {
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
