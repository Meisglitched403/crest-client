package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAnimationModule implements CrestModule {
    private final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    private final BooleanSetting messageAnimation = new BooleanSetting("Message Animation", true);
    private final IntegerSetting fadeTime = new IntegerSetting("Fade Time (ms)", 100, 2000, 500);
    private final BooleanSetting opacityAnimation = new BooleanSetting("Opacity Animation", true);
    private final ModeSetting easing = new ModeSetting("Easing", new String[]{"Sine", "Quad", "Cubic", "Quart", "Expo"}, 0);
    private final ModeSetting easingMode = new ModeSetting("Easing Mode", new String[]{"In", "Out", "InOut"}, 1);

    static ChatAnimationModule INSTANCE;
    
    // Cache for completed animations to avoid redundant calculations
    private static final Map<GuiMessage.Line, Float> progressCache = new HashMap<>();
    private static DeltaTracker cachedTracker = null;
    private static long lastCacheTime = 0;

    @Override public String getId() { return "chat_animation"; }
    @Override public String getName() { return "Chat Animation"; }
    @Override public String getDescription() { return "Animated chat messages: slide-up and fade-in effects"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public void onInitialize() { INSTANCE = this; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(enabled, messageAnimation, fadeTime, opacityAnimation, easing, easingMode);
    }

    public static boolean isOn() {
        var cam = INSTANCE;
        if (cam == null) return false;
        return CrestModules.isEnabled("chat_animation") && cam.enabled.get();
    }

    public static boolean messagesAnimated() {
        var cam = INSTANCE;
        return cam != null && cam.messageAnimation.get();
    }

    public static int getFadeTime() {
        var cam = INSTANCE;
        return cam != null ? cam.fadeTime.get() : 500;
    }

    public static boolean opacityAnimated() {
        var cam = INSTANCE;
        return cam != null && cam.opacityAnimation.get();
    }

    public static String getEasing() {
        var cam = INSTANCE;
        return cam != null ? cam.easing.getMode() : "Sine";
    }

    public static Easing.Mode getEasingMode() {
        var cam = INSTANCE;
        if (cam == null) return Easing.Mode.OUT;
        return switch (cam.easingMode.get()) {
            case 0 -> Easing.Mode.IN;
            case 1 -> Easing.Mode.OUT;
            case 2 -> Easing.Mode.IN_OUT;
            default -> Easing.Mode.OUT;
        };
    }

    public static float lineProgress(GuiMessage.Line line) {
        if (!isOn()) return 1f;
        
        // Check cache first for completed animations
        Float cached = progressCache.get(line);
        if (cached != null && cached >= 1f) return 1f;
        
        // Cache delta tracker per frame to avoid repeated calls
        long currentTime = System.currentTimeMillis();
        if (cachedTracker == null || currentTime != lastCacheTime) {
            cachedTracker = Minecraft.getInstance().getDeltaTracker();
            lastCacheTime = currentTime;
        }
        
        // Calculate elapsed time in milliseconds using proper delta
        float partialTick = cachedTracker.getGameTimeDeltaPartialTick(false);
        float elapsedTicks = (Minecraft.getInstance().gui.getGuiTicks() - line.addedTime()) + partialTick;
        
        if (elapsedTicks <= 0f) return 0f;
        
        // Convert ticks to milliseconds (50ms per tick at 20 TPS)
        // Use float throughout to preserve sub-frame precision
        float elapsedMs = elapsedTicks * 50f;
        float progress = Mth.clamp(elapsedMs / getFadeTime(), 0f, 1f);
        
        // Cache completed animations
        if (progress >= 1f) {
            progressCache.put(line, 1f);
        }
        
        return progress;
    }
    
    // Clear cache when settings change or on world unload
    public static void clearCache() {
        progressCache.clear();
        cachedTracker = null;
    }
}
