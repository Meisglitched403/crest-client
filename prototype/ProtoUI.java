import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ProtoUI {

    static class Component {
        final String text;
        Component(String t) { text = t; }
        static Component literal(String t) { return new Component(t); }
    }

    static class FontStub {
        final java.awt.Font awt;
        final int lineHeight;
        FontStub(java.awt.Font f) { awt = f; lineHeight = f.getSize() + 4; }
        int width(String s) {
            BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            FontRenderContext frc = g.getFontRenderContext();
            int w = (int) awt.getStringBounds(s, frc).getWidth();
            g.dispose();
            return w;
        }
    }

    static class G2D {
        Graphics2D g;
        void fill(int x1, int y1, int x2, int y2, int color) {
            g.setColor(new Color(color, true));
            g.fillRect(x1, y1, x2 - x1, y2 - y1);
        }
        void text(FontStub font, Component comp, int x, int y, int color) {
            g.setColor(new Color(color, true));
            g.setFont(font.awt);
            g.drawString(comp.text, x, y + font.awt.getSize());
        }
        void centeredText(FontStub font, Component comp, int x, int y, int color) {
            int w = font.width(comp.text);
            text(font, comp, x - w / 2, y, color);
        }
    }

    static class Theme {
        static final int SURFACE = 0xFF1A1A2E;
        static final int SURFACE_VARIANT = 0xFF25253D;
        static final int ON_SURFACE = 0xFFE0E0FF;
        static final int ON_SURFACE_VARIANT = 0xFFA0A0C0;
        static final int PRIMARY = 0xFF7C7CFF;
        static final int PRIMARY_CONTAINER = 0xFF2A2A50;
        static final int BG_BASE = 0xFF1A1A2E;
        static final int BG_HOVER = 0xFF40406A;
        static final int BG_SELECT = 0xFF33335A;
        static final int BG_PANEL = 0xFF1E1E38;
        static final int TEXT = 0xFFE0E0FF;
        static final int TEXT_DIM = 0xFFA0A0C0;
        static final int TEXT_FAINT = 0xFF606080;
        static final int OUTLINE = 0xFF555577;
        static final int OVERLAY = 0x80101020;
        static int accent = 0xFF7C7CFF;
        static int getAnimatedAccent() { return accent; }
        static void tick(float delta) {}
    }

    static class ColorUtil {
        static int withAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
        static int lerpARGB(int a, int b, float t) {
            int aa = (a >> 24) & 0xFF, ab = (b >> 24) & 0xFF;
            int ra = (a >> 16) & 0xFF, rb = (b >> 16) & 0xFF;
            int ga = (a >> 8) & 0xFF, gb = (b >> 8) & 0xFF;
            int ba = a & 0xFF, bb = b & 0xFF;
            return ((int)(aa + (ab - aa) * t) << 24)
                 | ((int)(ra + (rb - ra) * t) << 16)
                 | ((int)(ga + (gb - ga) * t) << 8)
                 | (int)(ba + (bb - ba) * t);
        }
    }

    static class Anim {
        static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    }

    static class Panel {
        static void draw(G2D g, int x, int y, int w, int h, int color) {
            g.g.setColor(new Color(color, true));
            g.g.fill(new RoundRectangle2D.Float(x, y, w, h, 5, 5));
        }
        static void drawGlass(G2D g, int x, int y, int w, int h, int bg, int accent) { draw(g, x, y, w, h, bg); }
        static void drawElevated(G2D g, int x, int y, int w, int h, int bg, int elevation) { draw(g, x, y, w, h, bg); }
        static void drawGlassElevated(G2D g, int x, int y, int w, int h, int bg, int a, int e) { draw(g, x, y, w, h, bg); }
    }

    interface Widget {
        int H = 20;
        int getHeight();
        void render(G2D g, FontStub font, int x, int y, int w, int mx, int my, float delta);
        default boolean mouseClicked(double mx, double my, int button) { return false; }
        default boolean mouseDragged(double mx, double my) { return false; }
        default boolean keyPressed(int key, int scan, int mods) { return false; }
        default boolean charTyped(int codepoint, int mods) { return false; }
    }

    // ─────────────────────── BUTTON ───────────────────────

    static class Button implements Widget {
        final String text;
        final Runnable onClick;
        int lx, ly, lw;
        boolean pressed;

        Button(String text, Runnable onClick) { this.text = text; this.onClick = onClick; }
        public int getHeight() { return H; }

        public void render(G2D g, FontStub font, int x, int y, int w, int mx, int my, float delta) {
            lx = x; ly = y; lw = w;
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + H;
            int bg;
            if (pressed)      bg = ColorUtil.withAlpha(Theme.PRIMARY, 120);
            else if (hover)   bg = ColorUtil.withAlpha(Theme.PRIMARY, 60);
            else              bg = ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 100);
            Panel.draw(g, x, y, w, H, bg);
            int tc = (hover || pressed) ? Theme.PRIMARY : Theme.ON_SURFACE;
            g.centeredText(font, Component.literal(text), x + w / 2, y + (H - font.lineHeight) / 2, tc);
        }

        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0) { pressed = true; onClick.run(); return true; }
            return false;
        }
    }

    // ─────────────────── TOGGLE SWITCH ───────────────────

    static class ToggleSwitch {
        static final int W = 32, H = 14;

        static void render(G2D g, int x, int y, boolean on, float anim) {
            int bgColor = on ? ColorUtil.lerpARGB(0xFF333355, Theme.getAnimatedAccent(), anim)
                             : ColorUtil.lerpARGB(0xFF333355, 0xFF555577, anim);
            int bw = (int) (W * 0.8f + W * 0.2f * anim);
            int bx = x + (W - bw) / 2;
            Panel.draw(g, bx, y, bw, H, bgColor);
            int knobSize = H - 4;
            int knobX = on ? bx + bw - knobSize - 2 : bx + 2;
            int knobColor = ColorUtil.lerpARGB(0xFF888899, 0xFFFFFFFF, anim);
            Panel.draw(g, knobX, y + 2, knobSize, knobSize, knobColor);
        }

        static boolean hit(double mx, double my, int x, int y) {
            return mx >= x && mx <= x + W && my >= y && my <= y + H;
        }
    }

    // ────────────────────── SLIDER ──────────────────────

    static class Slider implements Widget {
        private final float min, max;
        private float value;
        private final Consumer<Float> onChange;
        private int lx, lw;
        boolean dragging;

        Slider(float min, float max, float initial, Consumer<Float> onChange) {
            this.min = min; this.max = max; this.value = initial; this.onChange = onChange;
        }

        public int getHeight() { return H; }

        public void render(G2D g, FontStub font, int x, int y, int w, int mx, int my, float delta) {
            lx = x; lw = w;
            float frac = (max > min) ? (value - min) / (max - min) : 0;
            frac = Anim.clamp(frac, 0, 1);
            int barY = y + 7;
            g.fill(x, barY, x + w, barY + 4, ColorUtil.withAlpha(Theme.BG_BASE, 220));
            int fillW = (int) (frac * w);
            g.fill(x, barY, x + fillW, barY + 4, Theme.getAnimatedAccent());
            if (dragging)
                g.fill(x + fillW - 2, barY - 2, x + fillW + 2, barY + 6, 0xFFFFFFFF);
        }

        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0) return false;
            dragging = true;
            update((int) mx);
            return true;
        }

        public boolean mouseDragged(double mx, double my) {
            if (!dragging) return false;
            update((int) mx);
            return true;
        }

        void stopDrag() { dragging = false; }

        private void update(int mx) {
            if (max <= min || lw <= 0) return;
            float rel = Anim.clamp((float) (mx - lx) / lw, 0, 1);
            value = min + rel * (max - min);
            if (onChange != null) onChange.accept(value);
        }
    }

    // ──────────────────── TEXT INPUT ────────────────────

    static class TextInput implements Widget {
        private String text;
        private final Consumer<String> onChange;
        boolean focused;
        private String editing;
        private float cursorTimer;

        TextInput(String initial, Consumer<String> onChange) {
            this.text = initial; this.onChange = onChange;
        }

        public int getHeight() { return H; }

        public void render(G2D g, FontStub font, int x, int y, int w, int mx, int my, float delta) {
            cursorTimer += delta;
            int bg = focused ? ColorUtil.withAlpha(Theme.PRIMARY_CONTAINER, 100)
                             : ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 80);
            Panel.draw(g, x, y, w, H, bg);
            if (focused) g.fill(x, y + H - 2, x + w, y + H, Theme.getAnimatedAccent());
            String display = focused ? editing : text;
            int tx = x + 4, ty = y + (H - font.lineHeight) / 2;
            g.text(font, Component.literal(display), tx, ty, Theme.ON_SURFACE);
            if (focused && (int) (cursorTimer * 2) % 2 == 0) {
                int cx = tx + font.width(editing);
                g.fill(cx, ty, cx + 1, ty + font.lineHeight, Theme.PRIMARY);
            }
            if (!focused && text.isEmpty())
                g.text(font, Component.literal("type here..."), tx, ty, Theme.OUTLINE);
        }

        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0) { focused = true; editing = text; cursorTimer = 0; return true; }
            return false;
        }

        public boolean keyPressed(int key, int scan, int mods) {
            if (!focused) return false;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) { commit(); return true; }
            if (key == KeyEvent.VK_BACK_SPACE && !editing.isEmpty()) { editing = editing.substring(0, editing.length() - 1); return true; }
            return false;
        }

        public boolean charTyped(int codepoint, int mods) {
            if (!focused) return false;
            editing += (char) codepoint;
            cursorTimer = 0;
            return true;
        }

        void blur() { if (focused) commit(); }
        private void commit() { text = editing; focused = false; if (onChange != null) onChange.accept(text); }
    }

    // ──────────────────── TOGGLE ROW ────────────────────

    static class ToggleRow implements Widget {
        final String label;
        boolean on;
        final Runnable onToggle;

        ToggleRow(String label, boolean initial, Runnable onToggle) {
            this.label = label; this.on = initial; this.onToggle = onToggle;
        }

        public int getHeight() { return H; }

        public void render(G2D g, FontStub font, int x, int y, int w, int mx, int my, float delta) {
            int lw = font.width(label) + 4;
            int cx = x + lw + 4;
            int cw = w - lw - 8;
            if (cw < ToggleSwitch.W) cw = ToggleSwitch.W;
            g.text(font, Component.literal(label), x + 2, y + 4, Theme.ON_SURFACE_VARIANT);
            ToggleSwitch.render(g, cx + cw - ToggleSwitch.W, y + 2, on, on ? 1f : 0f);
        }

        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0) { on = !on; onToggle.run(); return true; }
            return false;
        }
    }

    // ────────────────── SCROLL CONTAINER ──────────────────

    static class ScrollContainer {
        float scrollOffset, scrollTarget;
        int x, y, w, h, rowH, contentH;
        List<? extends Widget> children;
        int hoverColor;
        int contentTop;

        ScrollContainer set(int x, int y, int w, int h, int rowH, List<? extends Widget> children) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.rowH = rowH;
            setChildren(children);
            return this;
        }

        void setChildren(List<? extends Widget> children) {
            this.children = children;
            contentH = children.size() * rowH + 8;
        }

        void render(G2D g, FontStub font, int mx, int my, float delta) {
            int maxH = Math.max(0, contentH - h);
            scrollTarget = Anim.clamp(scrollTarget, 0, maxH);
            scrollOffset += (scrollTarget - scrollOffset) * 0.12f;

            contentTop = y - (int) scrollOffset;
            for (int i = 0; i < children.size(); i++) {
                int cy = contentTop + i * rowH;
                if (cy + rowH < y) continue;
                if (cy > y + h) break;
                boolean hover = hoverColor != 0 && mx >= x && mx <= x + w && my >= cy && my <= cy + rowH - 2;
                if (hover) g.fill(x, cy, x + w, cy + rowH - 2, hoverColor);
                children.get(i).render(g, font, x + 4, cy, w - 8, mx, my, delta);
            }

            if (maxH > 0) {
                int tx = x + w - 4;
                float th = (float) h / contentH * h;
                float ty = scrollOffset / contentH * h;
                g.fill(tx, y, tx + 2, y + h, ColorUtil.withAlpha(Theme.BG_BASE, 200));
                g.fill(tx, y + (int) ty, tx + 2, y + (int) (ty + th), Theme.getAnimatedAccent());
            }
        }

        Widget childAt(double my) {
            int idx = (int) ((my - y + scrollOffset) / rowH);
            if (idx >= 0 && idx < children.size()) return children.get(idx);
            return null;
        }

        boolean mouseClicked(double mx, double my, int button) {
            if (!(mx >= x && mx <= x + w && my >= y && my <= y + h)) return false;
            Widget c = childAt(my);
            if (c == null) return false;
            return c.mouseClicked(mx, my, button);
        }

        boolean mouseDragged(double mx, double my) {
            Widget c = childAt(my);
            return c != null && c.mouseDragged(mx, my);
        }

        void mouseScrolled(double dy) {
            int maxH = Math.max(0, contentH - h);
            scrollTarget = Anim.clamp(scrollTarget - (float) dy * 3, 0, maxH);
        }
    }

    // ────────────────────── DEMO ──────────────────────

    static class DemoPanel extends JPanel {
        private final FontStub font = new FontStub(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        final List<Widget> widgets = new ArrayList<>();
        final ScrollContainer scroll = new ScrollContainer();
        Widget activeWidget;
        int mx, my;

        DemoPanel() {
            setPreferredSize(new Dimension(440, 480));
            setBackground(new Color(0x80101020, true));
            setFocusable(true);

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    int x = e.getX(), y = e.getY(), btn = e.getButton();

                    // header toggle
                    int tw = getWidth() - 12 - 12 - ToggleSwitch.W;
                    int ty = (48 - ToggleSwitch.H) / 2;
                    if (y < 48 && ToggleSwitch.hit(x, y, tw, ty)) {
                        System.out.println("Toggle clicked");
                        return;
                    }

                    // scroll area
                    if (scroll.mouseClicked(x, y, btn)) {
                        activeWidget = scroll.childAt(y);
                        return;
                    }

                    // clicked elsewhere — blur text input
                    if (activeWidget instanceof TextInput) { ((TextInput) activeWidget).blur(); activeWidget = null; }
                }

                public void mouseReleased(MouseEvent e) {
                    for (Widget w : widgets)
                        if (w instanceof Slider) ((Slider) w).stopDrag();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); }
                public void mouseDragged(MouseEvent e) {
                    mx = e.getX(); my = e.getY();
                    if (activeWidget instanceof Slider)
                        ((Slider) activeWidget).mouseDragged(mx, my);
                    else
                        scroll.mouseDragged(mx, my);
                }
            });

            addMouseWheelListener(e -> scroll.mouseScrolled(e.getPreciseWheelRotation()));

            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (activeWidget != null) activeWidget.keyPressed(e.getKeyCode(), 0, 0);
                }
                public void keyTyped(KeyEvent e) {
                    if (activeWidget != null) activeWidget.charTyped(e.getKeyChar(), 0);
                }
            });

            widgets.add(new ToggleRow("Enabled", true, () -> System.out.println("Toggled")));
            widgets.add(new Slider(0, 100, 50, v -> System.out.println("Slider: " + v)));
            widgets.add(new Slider(0.1f, 10, 2.5f, v -> {}));
            widgets.add(new TextInput("hello", v -> {}));
            widgets.add(new Button("Reset Settings", () -> System.out.println("Reset clicked")));
            widgets.add(new ToggleRow("Feature A", false, () -> {}));
            widgets.add(new ToggleRow("Feature B", true, () -> {}));
            widgets.add(new Slider(1, 20, 5, v -> {}));
            widgets.add(new TextInput("world", v -> {}));
            widgets.add(new Button("Apply", () -> System.out.println("Apply clicked")));

            scroll.set(16, 56, getPreferredSize().width - 32, getPreferredSize().height - 80, 22, widgets);
            scroll.hoverColor = ColorUtil.withAlpha(Theme.BG_HOVER, 100);
        }

        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            G2D g = new G2D();
            g.g = (Graphics2D) gg;
            g.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int pH = Math.min(H - 60, widgets.size() * 22 + 16);

            Panel.drawGlass(g, 0, 0, W, 48,
                ColorUtil.withAlpha(Theme.BG_PANEL, 235), Theme.getAnimatedAccent());
            g.text(font, Component.literal("\u2190  Fast Right Click"), 12, 14, Theme.TEXT);
            ToggleSwitch.render(g, W - 12 - 12 - ToggleSwitch.W, (48 - ToggleSwitch.H) / 2, true, 1f);

            Panel.drawGlassElevated(g, 16, 56, W - 32, pH,
                ColorUtil.withAlpha(Theme.SURFACE, 235), Theme.getAnimatedAccent(), 0);

            scroll.set(16, 56, W - 32, pH, 22, widgets);
            scroll.render(g, font, mx, my, 0.016f);
        }

        void tick() { repaint(); }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Crest UI Prototype");
        DemoPanel panel = new DemoPanel();
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new javax.swing.Timer(16, e -> panel.tick()).start();
    }
}
