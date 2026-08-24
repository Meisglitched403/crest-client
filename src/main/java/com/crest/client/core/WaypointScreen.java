package com.crest.client.core;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;

import java.util.List;

public class WaypointScreen extends Screen {
    private final CrestModule module;
    private final Screen parent;

    public WaypointScreen(CrestModule module, Screen parent) {
        super(Component.literal("Waypoints"));
        this.module = module;
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
            Component.literal("Add Current Position"),
            btn -> minecraft.setScreen(new WaypointAddScreen(this))
        ).bounds(width / 2 - 75, 10, 150, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Clear Deaths"),
            btn -> WaypointManager.clearDeaths(minecraft)
        ).bounds(width / 2 + 80, 10, 100, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Settings"),
            btn -> minecraft.setScreen(new ModuleDetailScreen(module, this))
        ).bounds(width / 2 - 75, height - 60, 70, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> onClose()
        ).bounds(width / 2 + 5, height - 60, 70, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);

        Theme.tick(delta);
        int accent = Theme.getAnimatedAccent();
        Panel.drawGlass(g, 4, 40, width - 8, height - 84, ColorUtil.withAlpha(Theme.BG_PANEL, 200), accent);

        String title = "Waypoints (" + WaypointManager.currentWorldId(minecraft) + ")";
        g.text(font, Component.literal(title), width / 2 - font.width(title) / 2, 35, 0xFFFFFF);

        List<Waypoint> list = WaypointManager.listForCurrent(minecraft);
        int y = 55;
        for (int i = 0; i < list.size() && y < height - 72; i++) {
            Waypoint wp = list.get(i);
            renderWaypointRow(g, wp, y, mx, my);
            y += 22;
        }
    }

    private void renderWaypointRow(GuiGraphicsExtractor g, Waypoint wp, int y, int mx, int my) {
        int dotColor = 0xFF000000 | (wp.color & 0xFFFFFF);

        int bx = width - 22;
        boolean delHover = mx >= bx && mx <= bx + 14 && my >= y + 4 && my <= y + 16;
        boolean rowHover = !delHover && mx >= 8 && mx <= width - 8 && my >= y && my <= y + 20;

        g.fill(8, y, width - 8, y + 20,
            rowHover ? ColorUtil.withAlpha(Theme.BG_BASE, 130) : ColorUtil.withAlpha(Theme.BG_BASE, 200));
        g.fill(10, y + 5, 16, y + 15, dotColor);
        String label = wp.name + ("death".equals(wp.kind) ? " (death)" : "");
        g.text(font, Component.literal(label), 20, y + 3, 0xFFFFFF);
        g.text(font, Component.literal(wp.x + ", " + wp.y + ", " + wp.z), 20, y + 12, 0x888888);
        if (rowHover) {
            g.text(font, Component.literal("\u270E Edit"), width - 70, y + 3, 0xFF55AAFF);
        }

        g.fill(bx, y + 4, bx + 14, y + 16, delHover ? 0xFFFF5555 : 0x88FF5555);
        g.text(font, Component.literal("x"), bx + 4, y + 4, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        super.mouseClicked(event, doubleClick);

        double mx = event.x();
        double my = event.y();

        List<Waypoint> list = WaypointManager.listForCurrent(minecraft);
        int y = 55;
        for (int i = 0; i < list.size() && y < height - 72; i++) {
            Waypoint wp = list.get(i);
            int bx = width - 22;
            boolean delHit = mx >= bx && mx <= bx + 14 && my >= y + 4 && my <= y + 16;
            boolean rowHit = !delHit && mx >= 8 && mx <= width - 8 && my >= y && my <= y + 20;
            if (delHit) {
                WaypointManager.remove(minecraft, wp.name);
                return true;
            }
            if (rowHit) {
                minecraft.setScreen(new WaypointAddScreen(wp, this));
                return true;
            }
            y += 22;
        }
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
