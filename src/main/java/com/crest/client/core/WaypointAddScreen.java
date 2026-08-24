package com.crest.client.core;

import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.TextInput;
import com.crest.client.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Dialog used whenever a waypoint is added (hotkey or the WaypointScreen button)
 * or edited (clicking a row in WaypointScreen): a name field, a 12-colour palette,
 * a live preview/hex readout and Add/Save + Cancel. Adding captures the player's
 * current position via WaypointManager.addHere; editing renames/recolors in place.
 */
public class WaypointAddScreen extends Screen {
    private static final int[] PALETTE = {
        0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x00AA00, 0x00AAAA,
        0x55AAFF, 0x5555FF, 0xAA00AA, 0xFF55FF, 0xAA5533, 0xFFFFFF
    };

    private static final int DIALOG_W = 260;
    private static final int SWATCH = 18;
    private static final int SWATCH_GAP = 4;

    private final Screen parent;
    private final Waypoint target;
    private final TextInput nameField;

    private int color = 0x66AAFF;
    private int selected = -1;

    public WaypointAddScreen(Screen parent) {
        this(null, parent);
    }

    public WaypointAddScreen(Waypoint target, Screen parent) {
        super(Component.literal(target == null ? "Add Waypoint" : "Edit Waypoint"));
        this.parent = parent;
        this.target = target;
        if (target != null) {
            this.nameField = new TextInput(target.name, s -> {});
            this.color = target.color & 0xFFFFFF;
            this.selected = paletteIndexOf(this.color);
        } else {
            int count = WaypointManager.getAll().size();
            this.nameField = new TextInput("Waypoint " + (count + 1), s -> {});
        }
        nameField.focus();
    }

    private static int paletteIndexOf(int color) {
        int rgb = color & 0xFFFFFF;
        for (int i = 0; i < PALETTE.length; i++) {
            if ((PALETTE[i] & 0xFFFFFF) == rgb) return i;
        }
        return -1;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        addRenderableWidget(Button.builder(Component.literal(target == null ? "Add" : "Save"),
            btn -> confirm()).bounds(cx - DIALOG_W / 2, height / 2 + 46, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),
            btn -> onClose()).bounds(cx + 20, height / 2 + 46, 120, 20).build());
    }

    private void confirm() {
        if (target != null) {
            String name = nameField.getText().trim();
            WaypointManager.edit(minecraft, target.name, name, color);
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("[Crest] Waypoint updated"));
            }
            onClose();
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            int count = WaypointManager.getAll().size();
            name = "Waypoint " + (count + 1);
        }
        WaypointManager.addHere(minecraft, name, color);
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("[Crest] Waypoint \"" + name + "\" added"));
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);

        int cx = width / 2;
        int top = height / 2 - 96;
        int panelH = 142;
        int accent = Theme.getAnimatedAccent();

        Panel.drawGlass(g, cx - DIALOG_W / 2, top, DIALOG_W, panelH,
            ColorUtil.withAlpha(Theme.BG_PANEL, 220), accent);

        g.text(font, Component.literal("Waypoint Name"), cx - DIALOG_W / 2 + 12, top + 10, 0xFFFFFF);

        int fx = cx - DIALOG_W / 2 + 12;
        int fw = DIALOG_W - 24;
        nameField.render(g, font, fx, top + 28, fw, mx, my, delta);

        if (target != null) {
            g.text(font, Component.literal("Position: " + target.x + ", " + target.y + ", " + target.z),
                fx, top + 56, 0xAAAAAA);
        } else if (minecraft.player != null) {
            int x = (int) Math.floor(minecraft.player.getX());
            int y = (int) Math.floor(minecraft.player.getY());
            int z = (int) Math.floor(minecraft.player.getZ());
            g.text(font, Component.literal("Position: " + x + ", " + y + ", " + z),
                fx, top + 56, 0xAAAAAA);
        }

        int px = cx - paletteWidth() / 2;
        int py = top + 78;
        for (int i = 0; i < PALETTE.length; i++) {
            int colx = px + (i % 6) * (SWATCH + SWATCH_GAP);
            int coly = py + (i / 6) * (SWATCH + SWATCH_GAP);
            if (selected == i) {
                g.fill(colx - 2, coly - 2, colx + SWATCH + 2, coly + SWATCH + 2, 0xFFFFFFFF);
            }
            g.fill(colx, coly, colx + SWATCH, coly + SWATCH, 0xFF000000 | PALETTE[i]);
        }

        g.fill(fx, top + 106, fx + 18, top + 122, 0xFF000000 | color);
        String hex = String.format("#%06X", color & 0xFFFFFF);
        g.text(font, Component.literal(hex), fx + 24, top + 108, 0xFFFFFF);
    }

    private int paletteWidth() {
        return SWATCH * 6 + SWATCH_GAP * 5;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.buttonInfo().input() == 0) {
            int fx = width / 2 - DIALOG_W / 2 + 12;
            int fy = height / 2 - 96 + 28;
            int fw = DIALOG_W - 24;
            int fh = nameField.getHeight();
            if (mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh) {
                nameField.mouseClicked(mx, my, 0);
                return true;
            }
            nameField.blur();

            int px = width / 2 - paletteWidth() / 2;
            int py = height / 2 - 96 + 78;
            for (int i = 0; i < PALETTE.length; i++) {
                int colx = px + (i % 6) * (SWATCH + SWATCH_GAP);
                int coly = py + (i / 6) * (SWATCH + SWATCH_GAP);
                if (mx >= colx && mx <= colx + SWATCH && my >= coly && my <= coly + SWATCH) {
                    selected = i;
                    color = PALETTE[i];
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
            || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            nameField.blur();
            confirm();
            return true;
        }
        if (nameField.keyPressed(key, event.scancode(), event.modifiers())) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (nameField.charTyped(event.codepoint(), 0)) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
