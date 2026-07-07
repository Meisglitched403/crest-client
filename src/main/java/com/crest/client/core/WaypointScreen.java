package com.crest.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WaypointScreen extends Screen {
    private final Screen parent;

    public WaypointScreen(Screen parent) {
        super(Component.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
            Component.literal("Add Current Position"),
            btn -> addCurrentPosition()
        ).bounds(width / 2 - 75, 10, 150, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> onClose()
        ).bounds(width / 2 - 25, height - 30, 50, 20).build());
    }

    private void addCurrentPosition() {
        if (minecraft.player == null) return;
        Vec3 pos = minecraft.player.position();
        int count = WaypointManager.getAll().size();
        String name = "Waypoint " + (count + 1);
        String dim = minecraft.level != null ? minecraft.level.dimension().identifier().toString() : "minecraft:overworld";
        WaypointManager.add(new Waypoint(name, pos.x, pos.y, pos.z, dim, 0xFFFF5555));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        extractBackground(g, mx, my, delta);
        super.extractRenderState(g, mx, my, delta);

        g.text(font, Component.literal("Waypoints"), width / 2 - font.width("Waypoints") / 2, 35, 0xFFFFFF);

        List<Waypoint> list = WaypointManager.getAll();
        int y = 55;
        for (int i = 0; i < list.size() && y < height - 40; i++) {
            Waypoint wp = list.get(i);
            renderWaypointRow(g, wp, y, mx, my);
            y += 22;
        }
    }

    private void renderWaypointRow(GuiGraphicsExtractor g, Waypoint wp, int y, int mx, int my) {
        int dotColor = wp.getColor() | 0xFF000000;

        g.fill(4, y, width - 4, y + 20, 0x44000000);
        g.fill(6, y + 5, 12, y + 15, dotColor);
        g.text(font, Component.literal(wp.getName()), 16, y + 3, 0xFFFFFF);
        g.text(font, Component.literal(String.format("%.0f, %.0f, %.0f", wp.getX(), wp.getY(), wp.getZ())), 16, y + 12, 0x888888);

        int bx = width - 22;
        boolean hovered = mx >= bx && mx <= bx + 14 && my >= y + 4 && my <= y + 16;
        g.fill(bx, y + 4, bx + 14, y + 16, hovered ? 0xFFFF5555 : 0x88FF5555);
        g.text(font, Component.literal("x"), bx + 4, y + 4, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        super.mouseClicked(event, doubleClick);

        double mx = minecraft.mouseHandler.xpos();
        double my = minecraft.mouseHandler.ypos();

        List<Waypoint> list = WaypointManager.getAll();
        int y = 55;
        for (int i = 0; i < list.size() && y < height - 40; i++) {
            Waypoint wp = list.get(i);
            int bx = width - 22;
            if (mx >= bx && mx <= bx + 14 && my >= y + 4 && my <= y + 16) {
                WaypointManager.remove(wp.getName());
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
