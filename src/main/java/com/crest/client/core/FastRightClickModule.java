package com.crest.client.core;

import com.crest.client.core.event.TickEvent;
import com.crest.client.core.mixin.RightClickDelayAccessor;
import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.KeybindSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Consumer;

public class FastRightClickModule implements CrestModule {
    private final BooleanSetting holdMode = new BooleanSetting("Hold Mode", true);
    private final FloatSetting maxCps = new FloatSetting("Max CPS", "Maximum clicks per second (20 = instant)", 1f, 20f, 20f);
    private final KeybindSetting toggleKey = new KeybindSetting("Toggle Key", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
    private boolean active;
    private Consumer<TickEvent> tickListener;
    private int tickCounter;

    @Override
    public String getId() { return "fast_right_click"; }

    @Override
    public String getName() { return "Fast Right Click"; }

    @Override
    public String getDescription() { return "Removes right-click delay for faster block placement"; }

    @Override
    public String getCategory() { return "Misc"; }

    @Override
    public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(holdMode, maxCps, toggleKey);
    }

    @Override
    public void onInitialize() {
        tickListener = this::onTick;
        CrestModules.getEventBus().subscribe(TickEvent.class, tickListener);
    }

    private void onTick(TickEvent event) {
        Minecraft mc = event.getClient();
        if (mc.player == null) return;

        if (holdMode.get()) {
            long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            if (window == 0) return;
            boolean held = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            active = held && CrestModules.isEnabled(getId());
        } else {
            active = CrestModules.isEnabled(getId());
        }

        if (active) {
            // Rate limit: only zero the delay when enough ticks have passed for the target CPS
            int interval = Math.round(20f / maxCps.get());
            if (interval <= 1 || tickCounter++ % interval == 0) {
                ((RightClickDelayAccessor) mc).setRightClickDelay(0);
            }
            if (!holdMode.get()) {
                mc.options.keyUse.setDown(true);
            }
        } else {
            tickCounter = 0;
        }
    }

    @Override
    public void onEnable() {
        if (!holdMode.get()) active = true;
    }

    @Override
    public void onDisable() {
        active = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.keyUse.setDown(false);
        }
    }
}
