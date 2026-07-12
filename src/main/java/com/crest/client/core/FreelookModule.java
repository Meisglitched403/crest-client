package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.setting.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

public class FreelookModule implements CrestModule {
    private static float cameraYaw;
    private static float cameraPitch;
    private static float prevPlayerYaw;
    private static float prevPlayerPitch;
    private static CameraType savedCameraType;
    private static boolean active;
    private static int activePerspective;

    private final BooleanSetting holdMode = new BooleanSetting("Hold Mode", true);
    private final ModeSetting perspective = new ModeSetting("Perspective", new String[]{"Third Person Back", "Third Person Front", "First Person"}, 0);
    private final FloatSetting maxYaw = new FloatSetting("Max Yaw", 90f, 360f, 360f);
    private final KeybindSetting toggleKey = new KeybindSetting("Freelook Key", GLFW.GLFW_KEY_UNKNOWN);

    @Override
    public String getId() { return "freelook"; }
    @Override
    public String getName() { return "Freelook"; }
    @Override
    public String getDescription() { return "Camera freelook independent of player rotation"; }
    @Override
    public String getCategory() { return "Visual"; }
    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(holdMode, perspective, maxYaw, toggleKey);
    }

    @Override
    public void onInitialize() {
        CrestModules.getEventBus().subscribe(TickEvent.class, this::onTick);
    }

    private void onTick(TickEvent event) {
        Minecraft mc = event.getClient();
        if (mc.player == null) return;

        if (holdMode.get()) {
            long window = glfwGetCurrentContext();
            if (window == 0) return;
            int key = toggleKey.get();
            boolean held = glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            if (held && !active) start(mc);
            else if (!held && active) stop(mc);
        }
    }

    public static boolean isActive() { return active; }
    public static float getYaw() { return cameraYaw; }
    public static float getPitch() { return cameraPitch; }

    public static int getActivePerspective() { return activePerspective; }

    public static void start(Minecraft mc) {
        if (active) return;
        active = true;
        prevPlayerYaw = mc.player.getYRot();
        prevPlayerPitch = mc.player.getXRot();
        cameraYaw = prevPlayerYaw;
        cameraPitch = prevPlayerPitch;
        savedCameraType = mc.options.getCameraType();
        FreelookModule mod = getInstance();
        activePerspective = mod != null ? mod.perspective.get() : 0;
        mc.options.setCameraType(switch (activePerspective) {
            case 0 -> CameraType.THIRD_PERSON_BACK;
            case 1 -> CameraType.THIRD_PERSON_FRONT;
            default -> CameraType.FIRST_PERSON;
        });
    }

    public static void stop(Minecraft mc) {
        if (!active) return;
        active = false;
        mc.player.setYRot(prevPlayerYaw);
        mc.player.setXRot(prevPlayerPitch);
        mc.options.setCameraType(savedCameraType);
    }

    public static void addAngles(float deltaYaw, float deltaPitch) {
        FreelookModule mod = getInstance();
        float yawLimit = mod != null ? mod.maxYaw.get() : 360f;
        cameraPitch = Math.max(-90, Math.min(90, cameraPitch + deltaPitch));
        if (yawLimit < 360f) {
            float diff = cameraYaw - prevPlayerYaw;
            diff = ((diff % 360) + 540) % 360 - 180;
            float maxDiff = yawLimit / 2f;
            float newDiff = diff + deltaYaw;
            if (newDiff > maxDiff) deltaYaw = maxDiff - diff;
            else if (newDiff < -maxDiff) deltaYaw = -maxDiff - diff;
        }
        cameraYaw += deltaYaw;
    }

    private static FreelookModule getInstance() {
        CrestModule m = CrestModules.get("freelook");
        return m instanceof FreelookModule f ? f : null;
    }

    @Override
    public void onEnable() { if (!holdMode.get()) start(Minecraft.getInstance()); }
    @Override
    public void onDisable() { if (!holdMode.get()) stop(Minecraft.getInstance()); }
}
