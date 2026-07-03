package com.crest.client.bongocat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class BongoCatEditScreen extends Screen {
    private static final int OVERLAY_COLOR = 0x80000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final Component KB_TOGGLE_ON = Component.literal("Show Keyboard on HUD");
    private static final Component KB_TOGGLE_OFF = Component.literal("Show Keyboard on HUD: OFF");

    private final Screen parent;
    private final BongoCatConfig config;
    private final InputTracker input;

    private boolean dragging;
    private int dragOffX;
    private int dragOffY;
    private boolean kbToggle;

    protected BongoCatEditScreen(Screen parent) {
        super(Component.literal("Bongo Cat Editor"));
        this.parent = parent;
        this.config = BongoCatConfig.getInstance();
        this.input = InputTracker.getInstance();
    }

    @Override
    protected void init() {
        kbToggle = config.keyboardVisible;

        addRenderableWidget(Button.builder(kbToggle ? KB_TOGGLE_ON : KB_TOGGLE_OFF, btn -> {
            kbToggle = !kbToggle;
            btn.setMessage(kbToggle ? KB_TOGGLE_ON : KB_TOGGLE_OFF);
        }).bounds(width / 2 - 60, 20, 120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, OVERLAY_COLOR);

        config.keyboardVisible = kbToggle;
        BongoCatOverlay.render(g, Minecraft.getInstance(), input, config);
        config.keyboardVisible = kbToggle;

        float scale = config.scale;
        int catW = (int) (40 * (397.0f / 201.0f) * scale);
        int catH = (int) (40 * scale);
        int kbW = VirtualKeyboard.getWidth(scale);
        int totalW = catW + 4 + kbW;
        int totalH = Math.max(catH, VirtualKeyboard.getHeight(scale));

        g.fill(config.x - 1, config.y - 1, config.x + totalW + 2, config.y + totalH + 2, BORDER_COLOR);
        g.fill(config.x, config.y, config.x + totalW + 1, config.y + totalH + 1, 0x00000000);

        Component hint = Component.literal("Drag to move | Scroll to resize | Esc to save & close");
        g.text(font, hint, (width - font.width(hint)) / 2, height - 20, 0xFFAAAAAA);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.buttonInfo().input();

        if (btn == 0) {
            float scale = config.scale;
            int catW = (int) (40 * (397.0f / 201.0f) * scale);
            int catH = (int) (40 * scale);
            int kbW = VirtualKeyboard.getWidth(scale);
            int totalW = catW + 4 + kbW;
            int totalH = Math.max(catH, VirtualKeyboard.getHeight(scale));

            if (mx >= config.x && mx <= config.x + totalW && my >= config.y && my <= config.y + totalH) {
                dragging = true;
                dragOffX = (int) (mx - config.x);
                dragOffY = (int) (my - config.y);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            config.x = (int) Math.max(0, event.x() - dragOffX);
            config.y = (int) Math.max(0, event.y() - dragOffY);
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
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY != 0) {
            config.scale = Math.max(0.5f, Math.min(3.0f, config.scale + (float) (scrollY * 0.15)));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 || event.key() == 342) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        config.keyboardVisible = kbToggle;
        config.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
