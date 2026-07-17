package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class ZoomModule implements CrestModule {
    public static double prevFov;
    public static double currentFov;
    public static double originalFov;

    private static float progress;
    private static int scrollTier;
    private static boolean active;
    private static boolean initialized;

    // ponytail: cache the singleton so getSensitivity()/isScrollZoomEnabled()
    // (called on the mouse-turn hot path) don't do a Map.get + cast per call.
    private static ZoomModule instance;

    private final FloatSetting initialZoom = new FloatSetting("Zoom Amount", 1.1f, 50f, 4f);
    private final FloatSetting zoomInTime = new FloatSetting("Zoom In Time", 0f, 1f, 0.3f);
    private final FloatSetting zoomOutTime = new FloatSetting("Zoom Out Time", 0f, 1f, 0.2f);
    private final ModeSetting transition = new ModeSetting("Transition", new String[]{"Cubic", "Sine", "Quad", "Quart", "Expo", "Linear"}, 0);
    private final ModeSetting transitionMode = new ModeSetting("Transition Mode", new String[]{"Out", "In", "In/Out"}, 0);
    private final BooleanSetting holdMode = new BooleanSetting("Hold Mode", true);
    private final BooleanSetting scrollZoom = new BooleanSetting("Scroll Zoom", true);
    private final FloatSetting zoomPerStep = new FloatSetting("Zoom Per Scroll", 0.5f, 10f, 2f);
    private final FloatSetting sensitivity = new FloatSetting("Sensitivity", 0f, 100f, 50f);

    private static final int ZOOM_KEY = GLFW.GLFW_KEY_Z;
    private static final String[] STYLES = {"cubic", "sine", "quad", "quart", "expo", "linear"};

    @Override
    public String getId() { return "zoom"; }
    @Override
    public String getName() { return "Zoom"; }
    @Override
    public String getDescription() { return "Customizable zoom with smooth transitions"; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(initialZoom, zoomInTime, zoomOutTime, transition, transitionMode, holdMode, scrollZoom, zoomPerStep, sensitivity);
    }

    @Override
    public void onInitialize() {
        instance = this;
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    private void onTick(TickEvent event) {
        Minecraft mc = event.getClient();
        if (mc.player == null) return;

        if (!initialized) {
            originalFov = mc.options.fov().get();
            prevFov = originalFov;
            currentFov = originalFov;
            initialized = true;
        }

        long window = glfwGetCurrentContext();
        if (window == 0) return;

        boolean isDown = glfwGetKey(window, ZOOM_KEY) == GLFW.GLFW_PRESS;

        if (holdMode.get()) {
            if (active && !isDown) scrollTier = 0;
            active = isDown;
        }

        prevFov = currentFov;

        float targetDivider = active ? (initialZoom.get() + scrollTier * zoomPerStep.get()) : 1f;

        float speed = active ? zoomInTime.get() : zoomOutTime.get();
        if (speed > 0.001f) {
            float step = 1f / (speed * 20f);
            if (active) progress = Math.min(1, progress + step);
            else progress = Math.max(0, progress - step);
        } else {
            progress = active ? 1 : 0;
        }

        String style = STYLES[transition.get()];
        Easing.Mode mode = Easing.Mode.values()[transitionMode.get()];
        float eased = style.equals("linear") ? progress : Easing.apply(style, mode, progress);

        currentFov = originalFov / (1f + (targetDivider - 1f) * eased);
    }

    public static boolean isActive() { return active; }
    public static boolean isInitialized() { return initialized; }
    public static float getSensitivity() {
        return instance != null ? instance.sensitivity.get() / 100f : 0.5f;
    }
    public static boolean isScrollZoomEnabled() {
        return instance != null ? instance.scrollZoom.get() : true;
    }
    public static void addScrollTier(int delta) {
        scrollTier = Math.max(0, Math.min(100, scrollTier + delta));
    }

    @Override
    public void onEnable() { if (!holdMode.get()) active = true; }
    @Override
    public void onDisable() {
        if (!holdMode.get()) {
            active = false;
            scrollTier = 0;
        }
    }
}
