package com.crest.client.core.mixin.gui;

import com.crest.client.core.CrestModule;
import com.crest.client.core.CrestModules;
import com.crest.client.core.ScoreboardModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.Gui.class)
public class ScoreboardMixin {

    // Cancel the vanilla sidebar when either the Scoreboard module (custom draw)
    // or the Hide Scoreboard module is enabled, so they don't double-draw.
    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void crest$hideScoreboard(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker d, CallbackInfo ci) {
        if (CrestModules.isEnabled("hide_scoreboard")) {
            ci.cancel();
            return;
        }
        CrestModule mod = CrestModules.get("scoreboard");
        if (mod instanceof ScoreboardModule && CrestModules.isEnabled("scoreboard")) {
            ci.cancel();
        }
    }
}
