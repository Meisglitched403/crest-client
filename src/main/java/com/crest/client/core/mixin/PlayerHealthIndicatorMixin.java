package com.crest.client.core.mixin;

import com.crest.client.core.CrestModules;
import com.crest.client.core.PlayerHealthIndicatorModule;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class PlayerHealthIndicatorMixin {

    @Inject(method = "submit", at = @At("TAIL"))
    private <S extends LivingEntityRenderState> void crest$renderHealth(
        S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci
    ) {
        if (!CrestModules.isEnabled("player_health_indicator")) return;
        if (!(state instanceof AvatarRenderState avatarState)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(avatarState.id);
        if (!(entity instanceof Player player)) return;
        if (player == mc.player) return;

        if (entity.isInvisible()) {
            boolean hasArmor = !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                || !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                || !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
            if (!hasArmor) return;
        }

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorptionAmount();

        int heartsRed = (int) Math.ceil(health / 2.0f);
        int heartsNormal = (int) Math.ceil(maxHealth / 2.0f);
        int heartsYellow = (int) Math.ceil(absorption / 2.0f);
        int heartsTotal = heartsNormal + heartsYellow;

        boolean lastRedHalf = ((int) health & 1) == 1;
        boolean lastYellowHalf = ((int) absorption & 1) == 1;

        int heartsPerRow = PlayerHealthIndicatorModule.isHeartStackingEnabled() ? 10 : heartsTotal;

        List<Component> rows = new ArrayList<>();
        MutableComponent currentRow = Component.literal("");
        for (int i = 0; i < heartsTotal; i++) {
            int col = i % heartsPerRow;
            if (col == 0 && i > 0) {
                rows.add(currentRow);
                currentRow = Component.literal("");
            }

            ChatFormatting color;
            char symbol;

            if (i < heartsRed) {
                color = ChatFormatting.RED;
                symbol = (i == heartsRed - 1 && lastRedHalf) ? '\u2661' : '\u2764';
            } else if (i < heartsNormal) {
                color = ChatFormatting.DARK_GRAY;
                symbol = '\u2661';
            } else {
                if (i == heartsTotal - 1 && lastYellowHalf) {
                    color = ChatFormatting.GOLD;
                    symbol = '\u2661';
                } else {
                    color = ChatFormatting.YELLOW;
                    symbol = '\u2764';
                }
            }

            currentRow.append(Component.literal(String.valueOf(symbol)).withStyle(color));
        }
        if (currentRow.getString().length() > 0) {
            rows.add(currentRow);
        }

        Vec3 attach = new Vec3(0.0, player.getBbHeight() + 0.5, 0.0);
        if (state.scoreText != null) {
            attach = attach.add(0.0, 9.0F * 1.15F * 0.025F, 0.0);
        }
        if (state.nameTag != null) {
            attach = attach.add(0.0, 9.0F * 1.15F * 0.025F, 0.0);
        }
        attach = attach.add(0.0, PlayerHealthIndicatorModule.getHeartOffset() * 0.025F, 0.0);

        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            submitNodeCollector.submitNameTag(
                poseStack, attach, rowIdx * mc.font.lineHeight, rows.get(rowIdx),
                true, state.lightCoords, state.distanceToCameraSq, camera
            );
        }
    }
}
