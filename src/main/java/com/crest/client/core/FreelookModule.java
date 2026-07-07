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
    private static float freelookYaw;
    private static float freelookPitch;
    private static boolean active;
    private static float prevPlayerYaw;
    private static float prevPlayerPitch;
    private static CameraType savedCameraType;
    public static float savedEntityYaw;
    public static float savedEntityPitch;

    private final BooleanSetting holdMode = new BooleanSetting(
        "Hold Mode", true
    );
    private final FloatSetting sensitivity = new FloatSetting(
        "Sensitivity", 0.1f, 2.0f, 1.0f
    );
    private final FloatSetting thirdPersonDistance = new FloatSetting(
        "3rd Person Distance", 1.0f, 10.0f, 4.0f
    );
    private final KeybindSetting toggleKey = new KeybindSetting(
        "Freelook Key", GLFW.GLFW_KEY_UNKNOWN
    );

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
        return List.of(holdMode, sensitivity, thirdPersonDistance, toggleKey);
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
            if (held && !active) {
                setActive(true);
            } else if (!held && active) {
                setActive(false);
            }
        }
    }

    public static boolean isActive() { return active; }

    public static float getYaw() { return freelookYaw; }
    public static float getPitch() { return freelookPitch; }

    public static float getThirdPersonDistance() {
        FreelookModule mod = getInstance();
        return mod != null ? mod.thirdPersonDistance.get() : 4.0f;
    }

    public static void setActive(boolean a) {
        if (a == active) return;
        active = a;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (a) {
            prevPlayerYaw = mc.player.getYRot();
            prevPlayerPitch = mc.player.getXRot();
            freelookYaw = prevPlayerYaw;
            freelookPitch = prevPlayerPitch;
            savedCameraType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else {
            mc.player.setYRot(prevPlayerYaw);
            mc.player.setXRot(prevPlayerPitch);
            mc.options.setCameraType(savedCameraType);
        }
    }

    public static void addAngles(float deltaX, float deltaY) {
        if (!active) return;
        FreelookModule mod = getInstance();
        float sens = mod != null ? mod.sensitivity.get() : 1.0f;
        freelookYaw += deltaX * sens;
        freelookPitch = Math.max(-90, Math.min(90, freelookPitch + deltaY * sens));
    }

    private static FreelookModule getInstance() {
        CrestModule m = CrestModules.get("freelook");
        return m instanceof FreelookModule f ? f : null;
    }

    @Override
    public void onEnable() {
        if (!holdMode.get()) {
            setActive(true);
        }
    }

    @Override
    public void onDisable() {
        if (!holdMode.get()) {
            setActive(false);
        }
    }
}
