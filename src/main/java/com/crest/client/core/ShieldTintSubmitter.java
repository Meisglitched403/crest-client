package com.crest.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.crest.client.ui.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Re-submits the shield model geometry tinted with the Shield Status colour,
 * used in place of the vanilla ShieldSpecialRenderer submit. Ported from
 * Walksy/ShieldStatus ShieldSpecialSubmitter.
 */
public class ShieldTintSubmitter {

    private final ShieldModel model;
    private final SpriteGetter sprites;

    public ShieldTintSubmitter(ShieldModel model, SpriteGetter sprites) {
        this.model = model;
        this.sprites = sprites;
    }

    public void submit(ItemDisplayContext context, DataComponentMap components,
                        PoseStack poseStack, SubmitNodeCollector collector,
                        int lightCoords, int overlayCoords, boolean hasFoil,
                        net.minecraft.world.entity.player.Player owner) {
        // A shield tint must be opaque; a transparent tint makes the model
        // invisible. Force full alpha regardless of the stored color value.
        int color = ColorUtil.withAlpha(ShieldStatusModule.getColor(owner), 255);
        Identifier baseSheet = ShieldStatusModule.getTexture(owner);
        final Identifier sheet = ShieldStatusModule.isGrayscale()
                ? GrayscaleTextureCache.get(baseSheet) : baseSheet;

        RenderType renderType = RenderTypes.entityTranslucent(sheet);
        collector.submitCustomGeometry(poseStack, renderType, (pose, _) -> {
            submitShieldModel(pose, components, lightCoords, overlayCoords, hasFoil, color, sheet);
        });
    }

    private void submitShieldModel(PoseStack.Pose pose, DataComponentMap components,
                                   int lightCoords, int overlayCoords, boolean hasFoil,
                                   int tintColor, Identifier shieldSheet) {
        PoseStack poseStack = new PoseStack();
        poseStack.last().set(pose);
        poseStack.pushPose();

        BannerPatternLayers patterns = components != null
                ? components.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                : BannerPatternLayers.EMPTY;
        DyeColor baseColor = components != null ? components.get(DataComponents.BASE_COLOR) : null;
        boolean hasPatterns = !patterns.layers().isEmpty() || baseColor != null;

        RenderType renderType = RenderTypes.entityTranslucent(shieldSheet);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer glintConsumer = ItemFeatureRenderer.getFoilBuffer(bufferSource, renderType, false, hasFoil);

        model.handle().render(poseStack, glintConsumer, lightCoords, overlayCoords, tintColor);
        model.plate().render(poseStack, glintConsumer, lightCoords, overlayCoords, tintColor);

        if (hasPatterns) {
            submitPatterns(poseStack, bufferSource, lightCoords, overlayCoords,
                    baseColor != null ? baseColor : DyeColor.WHITE, patterns);
        }

        poseStack.popPose();
        bufferSource.endBatch();
    }

    private void submitPatterns(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                int lightCoords, int overlayCoords, DyeColor baseColor,
                                BannerPatternLayers patterns) {
        submitPatternLayer(net.minecraft.client.renderer.Sheets.SHIELD_PATTERN_BASE, bufferSource,
                poseStack, lightCoords, overlayCoords, baseColor);
        for (int maskIndex = 0; maskIndex < 16 && maskIndex < patterns.layers().size(); ++maskIndex) {
            BannerPatternLayers.Layer layer = patterns.layers().get(maskIndex);
            submitPatternLayer(net.minecraft.client.renderer.Sheets.getShieldSprite(layer.pattern()),
                    bufferSource, poseStack, lightCoords, overlayCoords, layer.color());
        }
    }

    private void submitPatternLayer(SpriteId sprite, MultiBufferSource.BufferSource bufferSource,
                                     PoseStack poseStack, int lightCoords, int overlayCoords, DyeColor color) {
        model.plate().render(poseStack,
                sprite.buffer(this.sprites, bufferSource, RenderTypes::entityTranslucent),
                lightCoords, overlayCoords, color.getTextureDiffuseColor());
    }
}
