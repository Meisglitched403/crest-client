package com.crest.client.core.mixin.gui;

import com.crest.client.core.CornerTextModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class ContainerCornerTextMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crest$drawCornerText(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CornerTextModule.draw(g);
    }
}

@Mixin(AbstractRecipeBookScreen.class)
class RecipeBookCornerTextMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crest$drawCornerText(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CornerTextModule.draw(g);
    }
}
