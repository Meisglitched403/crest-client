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
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntRenderer.class)
public class TntTimerMixin {

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
        Component comp = Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(textColor & 0x00FFFFFF)));

        float yOff = 1.5f + TntTimerModule.getOffset();

        poseStack.pushPose();
        poseStack.translate(0.0, yOff + 0.5, 0.0);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.025F, -0.025F, 0.025F);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        FormattedCharSequence seq = comp.getVisualOrderText();
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
