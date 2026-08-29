package com.crest.client.core.mixin.gui;

import com.crest.client.core.CrestModule;
import com.crest.client.core.CrestModules;
import com.crest.client.core.MouseTracesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MouseTracesScreenMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crest$renderMouseTrace(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;
        if (!CrestModules.isEnabled("mouse_traces")) return;
        CrestModule mod = CrestModules.get("mouse_traces");
        if (mod instanceof MouseTracesModule mt) {
            try {
                mt.renderTrace(g, mc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
