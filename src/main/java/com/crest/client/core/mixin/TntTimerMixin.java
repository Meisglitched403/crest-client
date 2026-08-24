package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.TntTimerModule;
import com.crest.client.ui.ColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(TntRenderer.class)
public class TntTimerMixin {

    // ponytail: cache the shaped text so repeated fuse values (several TNT lit
    // together) don't re-run String.format + Component shaping per entity per frame.
    private static final Map<String, FormattedCharSequence> TEXT_CACHE =
        new LinkedHashMap<>(32, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, FormattedCharSequence> eldest) {
                return size() > 64;
            }
        };

    @Inject(method = "submit", at = @At("TAIL"))
    private void crest$renderTimer(
        TntRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci
    ) {
        if (!CrestModules.isEnabled("tnt_timer")) return;

        float fuseTicks = state.fuseRemainingInTicks;
        if (fuseTicks <= 0) return;

        float seconds = fuseTicks / 20.0f;
        String text;
        if (TntTimerModule.showDecimals()) {
            text = String.format("%.1fs", seconds);
        } else {
            text = (int) Math.ceil(seconds) + "s";
        }

        int textColor = TntTimerModule.getTextColor();
        FormattedCharSequence seq = TEXT_CACHE.computeIfAbsent(text,
            t -> Component.literal(t).getVisualOrderText());

        float yOff = 1.5f + TntTimerModule.getOffset();

        poseStack.pushPose();
        poseStack.translate(0.0, yOff + 0.5, 0.0);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.025F, -0.025F, 0.025F);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float x = -font.width(seq) / 2.0F;

        int bgColor;
        if (TntTimerModule.isBgEnabled()) {
            bgColor = ColorUtil.argb(TntTimerModule.getBgOpacity(),
                ColorUtil.getR(TntTimerModule.getBgColor()),
                ColorUtil.getG(TntTimerModule.getBgColor()),
                ColorUtil.getB(TntTimerModule.getBgColor()));
        } else {
            bgColor = 0;
        }

        submitNodeCollector.submitText(
            poseStack, x, 0, seq, false, Font.DisplayMode.SEE_THROUGH,
            state.lightCoords, textColor, bgColor, 0
        );
        poseStack.popPose();
    }
}
