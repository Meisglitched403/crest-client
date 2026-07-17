package com.crest.client.core;

import com.crest.client.core.mixin.GameRendererAccess;
import com.crest.client.core.setting.FloatSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: ColorSaturation. Full-screen desaturation/saturation post effect.
 * ShaderManagerMixin.getPostChain is intercepted to return a PostChain built
 * from the live saturation setting, so the slider updates in real time. The
 * effect is toggled via GameRenderer.setPostEffect (invoked through GameRendererAccess).
 */
public class ColorSaturationModule implements CrestModule {
    public static final Identifier SATURATION_ID = Identifier.fromNamespaceAndPath("crest-client", "saturation");

    public static final ColorSaturationModule INSTANCE = new ColorSaturationModule();

    private final FloatSetting saturation = new FloatSetting("Saturation", 0.0F, 3.0F, 1.0F);

    @Override public String getId() { return "color_saturation"; }
    @Override public String getName() { return "Color Saturation"; }
    @Override public String getDescription() { return "Globally desaturates or saturates the screen (0 = gray, 1 = normal)."; }
    @Override public String getCategory() { return "Render"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return new ArrayList<>(List.of(saturation));
    }

    @Override
    public void onEnable() {
        GameRendererAccess invoker = mixinInvoker();
        if (invoker != null) invoker.crest$setPostEffect(SATURATION_ID);
    }

    @Override
    public void onDisable() {
        GameRendererAccess invoker = mixinInvoker();
        if (invoker != null) invoker.crest$clearPostEffect();
    }

    private static GameRendererAccess mixinInvoker() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer instanceof GameRendererAccess a) return a;
        return null;
    }

    public static float currentSaturation() {
        return INSTANCE.saturation.get();
    }

    public static boolean shouldApply() {
        return CrestModules.isEnabled("color_saturation");
    }
}
