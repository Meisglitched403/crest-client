package com.crest.client.core;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CrestClickGui extends Screen {
    private static final int CAT_WIDTH = 120;
    private static final int LEFT = 10;

    private String selectedCategory;
    private int hoveredModule = -1;

    protected CrestClickGui() {
        super(Component.literal("Crest Client"));
    }

    @Override
    protected void init() {
        List<String> cats = CrestModules.getCategories();
        selectedCategory = cats.isEmpty() ? null : cats.get(0);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        System.err.println("[Crest] extractRenderState called, selectedCategory=" + selectedCategory + " cats=" + CrestModules.getCategories() + " mods=" + (selectedCategory != null ? CrestModules.getByCategory(selectedCategory).size() : -1));

        g.fill(0, 0, width, height, 0xCC000000);

        g.centeredText(font, getTitle(), width / 2, 8, 0xFFFFFFFF);

        List<String> cats = CrestModules.getCategories();

        for (int i = 0; i < cats.size(); i++) {
            String cat = cats.get(i);
            int y = 30 + i * 22;
            boolean hovered = mx >= LEFT && mx <= LEFT + CAT_WIDTH && my >= y && my <= y + 18;
            boolean selected = cat.equals(selectedCategory);
            int bg = selected ? 0xFF5555FF : (hovered ? 0x44444444 : 0x33000000);
            g.fill(LEFT, y, LEFT + CAT_WIDTH, y + 18, bg);
            g.text(font, Component.literal(cat), LEFT + 4, y + 4, selected ? 0xFFFFFFFF : 0xFFAAAAAA);
        }

        if (selectedCategory != null) {
            List<CrestModule> mods = CrestModules.getByCategory(selectedCategory);
            int x = LEFT + CAT_WIDTH + 4;
            int modWidth = Math.max(width - x - 4, 100);

            System.err.println("[Crest] rendering " + mods.size() + " modules at x=" + x + " w=" + modWidth);

            hoveredModule = -1;
            for (int i = 0; i < mods.size(); i++) {
                CrestModule mod = mods.get(i);
                int y = 30 + i * 22;
                boolean hovered = mx >= x && mx <= x + modWidth && my >= y && my <= y + 18;
                if (hovered) hoveredModule = i;

                boolean enabled = CrestModules.isEnabled(mod.getId());
                String label = (enabled ? "[ON]  " : "[OFF] ") + mod.getName();

                System.err.println("[Crest] module " + i + ": '" + label + "' at y=" + y);

                int bg = hovered ? 0x44444444 : 0x33000000;
                g.fill(x, y, x + modWidth, y + 18, bg);
                g.text(font, Component.literal(label), x + 4, y + 4, enabled ? 0x55FF55 : 0xFF5555);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.buttonInfo().input();

        if (btn != 0) return super.mouseClicked(event, doubleClick);

        List<String> cats = CrestModules.getCategories();
        for (int i = 0; i < cats.size(); i++) {
            int y = 30 + i * 22;
            if (mx >= LEFT && mx <= LEFT + CAT_WIDTH && my >= y && my <= y + 18) {
                selectedCategory = cats.get(i);
                return true;
            }
        }

        if (selectedCategory != null) {
            List<CrestModule> mods = CrestModules.getByCategory(selectedCategory);
            int x = LEFT + CAT_WIDTH + 4;
            int modWidth = Math.max(width - x - 4, 100);
            for (int i = 0; i < mods.size(); i++) {
                int y = 30 + i * 22;
                if (mx >= x && mx <= x + modWidth && my >= y && my <= y + 18) {
                    CrestModule mod = mods.get(i);
                    boolean newState = !CrestModules.isEnabled(mod.getId());
                    CrestModules.setEnabled(mod.getId(), newState);
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == 256 || event.key() == 344) {
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
}
