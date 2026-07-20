package com.crest.client.core;

import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class SkinChangerScreen extends Screen {

    private final Screen parent;
    private String query = "";
    private boolean focused = false;
    private int cursorBlink = 0;

    private int panelW = 360;
    private int panelH = 200;
    private int panelX, panelY;

    public SkinChangerScreen(Screen parent) {
        super(Component.literal("Skin Search"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(360, width - 40);
        panelH = Math.min(200, height - 40);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        Theme.tick(delta);
        cursorBlink++;

        g.fill(0, 0, width, height, ColorUtil.withAlpha(Theme.BACKGROUND, 180));
        Panel.drawGlass(g, panelX, panelY, panelW, panelH, ColorUtil.withAlpha(Theme.GLASS_BG, 240), Theme.getAnimatedAccent());

        int cx = panelX + panelW / 2;
        int left = panelX + 16;
        int right = panelX + panelW - 16;
        int cw = right - left;

        g.centeredText(font, Component.literal("Search Skin by Username"), cx, panelY + 14, Theme.FOREGROUND);

        // Back
        String back = "< Back";
        int backW = font.width(back) + 12;
        boolean backHover = mx >= left && mx <= left + backW && my >= panelY + 6 && my <= panelY + 22;
        Panel.draw(g, left, panelY + 6, backW, 16, ColorUtil.withAlpha(backHover ? Theme.getAnimatedAccent() : 0, 120));
        g.text(font, Component.literal(back), left + 6, panelY + 8, Theme.FOREGROUND);

        // Text field
        int fieldY = panelY + 50;
        Panel.draw(g, left, fieldY, cw, 26, ColorUtil.withAlpha(focused ? Theme.getAnimatedAccent() : Theme.BACKGROUND, focused ? 40 : 120));
        Panel.drawHollowRect(g, left, fieldY, cw, 26, Theme.BORDER_LIGHT);
        String shown = query.isEmpty() ? "Type a Minecraft username..." : query;
        g.text(font, Component.literal(shown), left + 8, fieldY + 8,
                query.isEmpty() ? Theme.MUTED_FOREGROUND : Theme.FOREGROUND);
        if (focused && (cursorBlink / 10) % 2 == 0) {
            int tx = left + 8 + font.width(query);
            g.fill(tx, fieldY + 4, tx + 1, fieldY + 22, Theme.getAnimatedAccent());
        }

        // Search button
        int btnY = fieldY + 40;
        boolean btnHover = mx >= left && mx <= right && my >= btnY && my <= btnY + 32;
        Panel.draw(g, left, btnY, cw, 32, btnHover ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 50) : ColorUtil.withAlpha(Theme.BACKGROUND, 130));
        Panel.drawHollowRect(g, left, btnY, cw, 32, btnHover ? Theme.getAnimatedAccent() : Theme.BORDER_LIGHT);
        g.centeredText(font, Component.literal("Search & Apply"), cx, btnY + 10, btnHover ? Theme.getAnimatedAccent() : Theme.FOREGROUND);

        // Status
        String status = SkinChanger.status();
        if (!status.isEmpty()) {
            g.centeredText(font, Component.literal(status), cx, btnY + 48, Theme.MUTED_FOREGROUND);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int left = panelX + 16;
        int right = panelX + panelW - 16;
        int cw = right - left;

        String back = "< Back";
        int backW = font.width(back) + 12;
        if (mx >= left && mx <= left + backW && my >= panelY + 6 && my <= panelY + 22) {
            minecraft.setScreen(parent);
            return true;
        }

        int fieldY = panelY + 50;
        focused = mx >= left && mx <= right && my >= fieldY && my <= fieldY + 26;

        int btnY = fieldY + 40;
        if (mx >= left && mx <= right && my >= btnY && my <= btnY + 32) {
            submit();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (cp < 32 || cp > 126) return super.charTyped(event);
        if (focused) {
            query += event.codepointAsString();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 256) { // ESC
            minecraft.setScreen(parent);
            return true;
        }
        if (focused) {
            if (key == 259 && !query.isEmpty()) { // BACKSPACE
                query = query.substring(0, query.length() - 1);
                return true;
            }
            if (key == 257 || key == 335) { // ENTER / KP_ENTER
                submit();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void submit() {
        if (query.trim().isEmpty()) return;
        SkinChanger.loadFromUsername(query.trim());
        SkinChanger.applyToLocalPlayer();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
