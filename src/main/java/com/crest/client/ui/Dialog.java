package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Modal dialog with title, body, and action buttons. */
public class Dialog {
    public final List<Runnable> closeHandlers = new ArrayList<>();
    public String title;
    public String message;
    public final List<Button> actions = new ArrayList<>();
    public int px, py, pw = 280, ph;

    private final Animated fadeAnim = new Animated(0f, 14f);
    public boolean closing;

    public Dialog(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public void addAction(String label, Runnable onClick) {
        actions.add(new Button(label, onClick));
    }

    public void open() {
        closing = false;
        fadeAnim.setImmediate(0f);
        fadeAnim.set(1f);
    }

    public void close() {
        closing = true;
        fadeAnim.set(0f);
    }

    public void tick(float dt) {
        fadeAnim.tick(dt);
    }

    public boolean isOpen() {
        return fadeAnim.get() > 0.01f;
    }

    public int getFadeAlpha() {
        return (int) (200 * fadeAnim.get());
    }

    public void render(GuiGraphicsExtractor g, Font font, int screenW, int screenH) {
        if (!isOpen()) return;
        int alpha = getFadeAlpha();
        if (alpha <= 0) return;

        px = (screenW - pw) / 2;
        py = (screenH - 80) / 2;
        int titleH = font.lineHeight + Spacing.S4;
        int msgH = message != null ? font.wordWrapHeight(Component.literal(message), pw - Spacing.S6) + Spacing.S2 : 0;
        int actionH = actions.isEmpty() ? 0 : Spacing.S10;
        ph = titleH + msgH + actionH + Spacing.S4;

        // Overlay
        g.fill(0, 0, screenW, screenH, ColorUtil.withAlpha(0x000000, (int) (80 * fadeAnim.get())));

        Panel.drawGlass(g, px, py, pw, ph, ColorUtil.withAlpha(Theme.POPOVER, alpha), Theme.getAnimatedAccent());

        int y = py + Spacing.S3;
        g.text(font, Component.literal(title), px + Spacing.S4, y, ColorUtil.withAlpha(Theme.FOREGROUND, alpha));
        y += font.lineHeight + Spacing.S2;

        if (message != null) {
            g.text(font, Component.literal(message), px + Spacing.S4, y, ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, alpha));
            y += msgH;
        }

        if (!actions.isEmpty()) {
            int btnY = py + ph - Spacing.S2 - 32;
            int btnGap = Spacing.S2;
            int totalBtnW = actions.size() * 80 + (actions.size() - 1) * btnGap;
            int btnX = px + (pw - totalBtnW) / 2;
            for (Button btn : actions) {
                btn.render(g, font, btnX, btnY, 80, 0, 0, 0);
                btnX += 80 + btnGap;
            }
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!isOpen() || closing) return false;
        for (Button btn : actions) {
            if (btn.mouseClicked(mx, my, button)) return true;
        }
        // Close on click outside
        if (mx < px || mx > px + pw || my < py || my > py + ph) {
            close();
            for (Runnable h : closeHandlers) h.run();
            return true;
        }
        return false;
    }
}
