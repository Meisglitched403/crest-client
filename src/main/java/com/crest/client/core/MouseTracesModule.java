package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import com.crest.client.ui.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a colorful trail behind the mouse cursor, but only while the cursor is
 * visible (inventory / chest / menu screens) — never during normal gameplay.
 *
 * Rendering is driven by MouseTracesScreenMixin (on top of screens), not the
 * HUD pass, so the trail is not painted over by screen backgrounds. The path is
 * smoothed with a centripetal Catmull-Rom spline and tapered toward the tail.
 */
public class MouseTracesModule implements CrestModule {

    private static final class Pt {
        final double x, y;
        Pt(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final double CR_ALPHA = 0.5; // centripetal parameterization
    private static final int MAX_GENERATED = 1200;

    private final ArrayDeque<Pt> trail = new ArrayDeque<>();
    private Pt lastSampled = null;
    private Screen lastScreen = null;

    private final ModeSetting colorMode = new ModeSetting("Color Mode",
            new String[]{"Single", "Rainbow", "Multi"}, 0);
    private final ColorSetting color = new ColorSetting("Color", 0xFFFF55FF);
    private final ColorSetting secondaryColor = new ColorSetting("Secondary Color", 0xFF66FF66);
    private final ColorSetting tertiaryColor = new ColorSetting("Tertiary Color", 0xFF66AAFF);
    private final IntegerSetting trailLength = new IntegerSetting("Trail Length", 5, 200, 48);
    private final IntegerSetting thickness = new IntegerSetting("Thickness", 1, 10, 3);
    private final IntegerSetting smoothing = new IntegerSetting("Smoothing", 4, 24, 12);
    private final BooleanSetting taper = new BooleanSetting("Taper", true);
    private final BooleanSetting fade = new BooleanSetting("Fade", true);

    @Override public String getId() { return "mouse_traces"; }
    @Override public String getName() { return "Mouse Traces"; }
    @Override public String getDescription() {
        return "Draws a colorful trail behind the mouse cursor on inventory, chest and menu screens.";
    }
    @Override public String getCategory() { return "Render"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(colorMode, color, secondaryColor, tertiaryColor,
                trailLength, thickness, smoothing, taper, fade);
    }

    private void clear() {
        trail.clear();
        lastSampled = null;
    }

    public void renderTrace(GuiGraphicsExtractor g, Minecraft mc) {
        if (mc.screen == null || mc.mouseHandler.isMouseGrabbed()) {
            clear();
            lastScreen = null;
            return;
        }
        if (mc.screen != lastScreen) {
            clear();
            lastScreen = mc.screen;
        }

        double x = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double y = mc.mouseHandler.getScaledYPos(mc.getWindow());

        if (lastSampled == null
                || Math.abs(x - lastSampled.x) > 0.01
                || Math.abs(y - lastSampled.y) > 0.01) {
            trail.addLast(new Pt(x, y));
            lastSampled = trail.getLast();
            while (trail.size() > trailLength.get()) {
                trail.removeFirst();
            }
        }

        int n = trail.size();
        if (n < 2) return;

        Pt[] pts = trail.toArray(new Pt[0]);

        int steps = smoothing.get();
        int segCount = n - 1;
        if (segCount * steps > MAX_GENERATED) {
            steps = Math.max(2, MAX_GENERATED / segCount);
        }

        List<double[]> curve = new ArrayList<>();
        buildCurve(pts, steps, curve);
        int m = curve.size();
        if (m < 2) return;

        double[] arc = new double[m];
        double total = 0;
        for (int j = 1; j < m; j++) {
            double dx = curve.get(j)[0] - curve.get(j - 1)[0];
            double dy = curve.get(j)[1] - curve.get(j - 1)[1];
            total += Math.hypot(dx, dy);
            arc[j] = total;
        }
        if (total < 0.01) total = 0.01;

        int thick = Math.max(1, thickness.get());
        int mode = colorMode.get();
        boolean useFade = fade.get();
        boolean useTaper = taper.get();

        for (int j = 0; j < m - 1; j++) {
            double[] a = curve.get(j);
            double[] b = curve.get(j + 1);
            double dx = b[0] - a[0];
            double dy = b[1] - a[1];
            double len = Math.hypot(dx, dy);
            if (len < 0.01) continue;

            double prog = (arc[j] + arc[j + 1]) * 0.5 / total;
            int col = colorFor(mode, (float) prog);
            if (useFade) col = ColorUtil.withAlpha(col, (int) (0xFF * prog));

            double w = useTaper ? (1 + (thick - 1) * prog * prog) : thick;
            int half = Math.max(1, (int) Math.round(w * 0.5));

            double midx = (a[0] + b[0]) * 0.5;
            double midy = (a[1] + b[1]) * 0.5;
            double ang = Math.atan2(dy, dx);

            g.pose().pushMatrix();
            g.pose().translate((float) midx, (float) midy);
            g.pose().rotate((float) ang);
            g.fill((int) (-len * 0.5 - 1), -half, (int) (len * 0.5 + 1), half + 1, col);
            g.pose().popMatrix();
        }

        double[] head = curve.get(m - 1);
        int headCol = colorFor(mode, 1f);
        if (useFade) headCol = ColorUtil.withAlpha(headCol, 0xFF);
        int hr = (int) Math.round(thick * 0.5) + 1;
        g.fill((int) (head[0] - hr), (int) (head[1] - hr),
               (int) (head[0] + hr), (int) (head[1] + hr), headCol);
    }

    private static double dist(Pt a, Pt b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static void buildCurve(Pt[] pts, int steps, List<double[]> out) {
        int n = pts.length;
        for (int i = 0; i < n - 1; i++) {
            Pt p0 = pts[Math.max(0, i - 1)];
            Pt p1 = pts[i];
            Pt p2 = pts[i + 1];
            Pt p3 = pts[Math.min(n - 1, i + 2)];

            double d1 = Math.pow(dist(p0, p1), CR_ALPHA);
            double d2 = Math.pow(dist(p1, p2), CR_ALPHA);
            double d3 = Math.pow(dist(p2, p3), CR_ALPHA);
            if (d1 < 1e-6) d1 = 1e-6;
            if (d2 < 1e-6) d2 = 1e-6;
            if (d3 < 1e-6) d3 = 1e-6;

            double t0 = 0;
            double t1 = t0 + d1;
            double t2 = t1 + d2;
            double t3 = t2 + d3;

            for (int s = 0; s < steps; s++) {
                double t = t1 + (t2 - t1) * (s / (double) steps);
                out.add(crPoint(p0, p1, p2, p3, t0, t1, t2, t3, t));
            }
        }
        out.add(new double[]{pts[n - 1].x, pts[n - 1].y});
    }

    private static double[] crPoint(Pt p0, Pt p1, Pt p2, Pt p3,
                                    double t0, double t1, double t2, double t3, double t) {
        return new double[]{
                crAxis(p0.x, p1.x, p2.x, p3.x, t0, t1, t2, t3, t),
                crAxis(p0.y, p1.y, p2.y, p3.y, t0, t1, t2, t3, t)
        };
    }

    private static double crAxis(double a0, double a1, double a2, double a3,
                                 double t0, double t1, double t2, double t3, double t) {
        double A1 = (t1 - t) / (t1 - t0) * a0 + (t - t0) / (t1 - t0) * a1;
        double A2 = (t2 - t) / (t2 - t1) * a1 + (t - t1) / (t2 - t1) * a2;
        double A3 = (t3 - t) / (t3 - t2) * a2 + (t - t2) / (t3 - t2) * a3;
        double B1 = (t2 - t) / (t2 - t0) * A1 + (t - t0) / (t2 - t0) * A2;
        double B2 = (t3 - t) / (t3 - t1) * A2 + (t - t1) / (t3 - t1) * A3;
        double C = (t2 - t) / (t2 - t1) * B1 + (t - t1) / (t2 - t1) * B2;
        return C;
    }

    private int colorFor(int mode, float prog) {
        if (mode == 1) {
            float scroll = (System.currentTimeMillis() % 4000L) / 4000f;
            float h = (prog * 0.8f + scroll) % 1f;
            if (h < 0) h += 1f;
            return ColorUtil.hsv(h, 0.9f, 1.0f);
        }
        if (mode == 2) {
            if (prog > 0.66f) return color.get();
            if (prog > 0.33f) return secondaryColor.get();
            return tertiaryColor.get();
        }
        return color.get();
    }
}
