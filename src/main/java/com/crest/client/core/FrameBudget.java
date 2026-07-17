package com.crest.client.core;

/**
 * ponytail: Adaptive Capture Frame-Budget (ACFB).
 *
 * The capture pipeline (GPU readback + CPU copy) is a per-frame tax on the
 * render thread. Other recorders capture unconditionally and let that tax steal
 * FPS from the game. ACFB inverts that: it measures the REAL cost of every
 * rendered frame and only allows capture to run when there is spare frame
 * budget. When the game is struggling, capture is skipped (and its interval
 * backs off) so gameplay FPS is always protected. When headroom is plentiful,
 * capture runs at the full target.
 *
 * All state is accessed only from the render thread, so no locking is needed.
 */
public final class FrameBudget {
    // Smoothed average frame time in nanoseconds (EMA).
    private static double avgFrameNs = 16_666_667.0; // seed at ~60fps
    private static long lastHeadNs = 0; // GameRenderer.render HEAD timestamp

    // Adaptive capture interval, in nanoseconds. Starts at the requested target.
    private static long captureIntervalNs = 16_666_667L;
    private static long lastCaptureNs = 0;

    // EMA smoothing factor for frame-time measurement.
    private static final double FRAME_EMA = 0.1;

    // ponytail: last capture decision, surfaced read-only to the Perf HUD module
    // so it can show whether ACFB is currently protecting gameplay FPS.
    private static volatile String lastCaptureDecision = "RUN";

    // Floor for the capture interval (never let capture drop below ~20fps worth).
    private static final long MIN_CAPTURE_INTERVAL_NS = 50_000_000L; // 20fps

    // ponytail: cached camera position (world) for distant-block LOD tiers. Written
    // on the render thread (RenderBudgetMixin) and read (volatile) from Sodium's
    // chunk-build worker threads. Slight staleness is fine for distance tiers.
    private static volatile double camX = 0, camY = 0, camZ = 0;

    public static void setCamera(double x, double y, double z) {
        camX = x; camY = y; camZ = z;
    }
    public static double cameraX() { return camX; }
    public static double cameraY() { return camY; }
    public static double cameraZ() { return camZ; }

    public static void markFrameStart(long nowNs) {
        lastHeadNs = nowNs;
    }

    public static void markFrameEnd(long nowNs) {
        if (lastHeadNs == 0) return;
        long dt = nowNs - lastHeadNs;
        if (dt <= 0 || dt > 1_000_000_000L) return; // ignore absurd deltas (tab-out, hitch)
        avgFrameNs += (dt - avgFrameNs) * FRAME_EMA;
    }

    public static double avgFrameMs() {
        return avgFrameNs / 1_000_000.0;
    }

    /**
     * Decide whether capture may run this frame.
     *
     * @param nowNs        current System.nanoTime()
     * @param targetFps    requested capture FPS
     * @param displayHz    the gameplay refresh target (e.g. monitor hz / 60)
     * @return true if capture should proceed this frame
     */
    public static boolean shouldCapture(long nowNs, int targetFps, int displayHz) {
        long targetFrameNs = 1_000_000_000L / Math.max(1, displayHz);
        long targetCaptureNs = 1_000_000_000L / Math.max(1, targetFps);

        // If the game itself is over budget, skip capture and back off.
        if (avgFrameNs > targetFrameNs) {
            // The game is already struggling — never capture now, widen interval.
            captureIntervalNs = Math.min(MIN_CAPTURE_INTERVAL_NS,
                (long) (captureIntervalNs * 1.25));
            lastCaptureNs = nowNs;
            lastCaptureDecision = "SKIP (budget)";
            return false;
        }

        // Healthy: converge capture interval back toward the requested target.
        if (captureIntervalNs > targetCaptureNs) {
            captureIntervalNs = Math.max(targetCaptureNs,
                (long) (captureIntervalNs * 0.9));
        } else {
            captureIntervalNs = targetCaptureNs;
        }

        if (nowNs - lastCaptureNs < captureIntervalNs) {
            lastCaptureDecision = "SKIP (interval)";
            return false;
        }
        lastCaptureNs = nowNs;
        lastCaptureDecision = "RUN";
        return true;
    }

    public static String lastCaptureDecision() {
        return lastCaptureDecision;
    }
}
