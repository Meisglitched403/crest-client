package com.crest.client.core.mixin.gui;

import com.crest.client.core.SkinChanger;
import com.crest.client.core.SkinChangerScreen;
import com.crest.client.ui.ColorUtil;
import com.crest.client.ui.Panel;
import com.crest.client.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.FileDialog;
import java.io.File;

@Mixin(TitleScreen.class)
public class TitleScreenSkinMixin {

    @Unique private static final int PANEL_W = 200;
    @Unique private static final int PANEL_H = 260;
    @Unique private static final float MODEL_H = 2.125F;
    @Unique private static final float FIT = 0.97F;
    @Unique private static final float ROT_SENS = 2.5F;
    @Unique private static final float ROT_X_LIMIT = 50.0F;

    @Unique private float rotX = -5.0F;
    @Unique private float rotY = 30.0F;
    @Unique private boolean dragging = false;
    @Unique private int lastDragX = -1, lastDragY = -1;

    @Unique private String searchText = "";
    @Unique private boolean searchFocused = false;

    @Unique private PlayerModel wideModel, slimModel;

    @Unique private int panelX, panelY;
    @Unique private int crest$mx, crest$my;

    private void crest$ensureModels() {
        if (wideModel == null) {
            Minecraft mc = Minecraft.getInstance();
            EntityModelSet models = mc.getEntityModels();
            wideModel = new PlayerModel(models.bakeLayer(ModelLayers.PLAYER), false);
            slimModel = new PlayerModel(models.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
    }

    private int crest$btn(int i) {
        // button y inside panel: Upload, Search, Apply (stacked at bottom)
        int bx = panelX + 12;
        int bw = PANEL_W - 24;
        int bh = 26;
        int gap = 8;
        int baseY = panelY + PANEL_H - 12 - bh;
        return baseY - i * (bh + gap);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crest$draw(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        crest$mx = mx;
        crest$my = my;
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();
        panelX = w - PANEL_W - 16;
        panelY = h - PANEL_H - 16;

        // 3D model area rect (computed first so the scrim can use it)
        int modelX = panelX + 10;
        int modelY = panelY + 44;
        int modelW = PANEL_W - 20;
        int modelH = PANEL_H - 150;
        if (modelH < 40) modelH = 40;

        // Panel: just a border (+ subtle scrim behind the model for contrast)
        Panel.drawHollowRect(g, panelX, panelY, PANEL_W, PANEL_H, ColorUtil.withAlpha(Theme.BORDER_LIGHT, 200));
        g.fill(modelX, modelY, modelX + modelW, modelY + modelH, ColorUtil.withAlpha(Theme.BACKGROUND, 90));

        // Title
        g.text(mc.font, Component.literal("Skin Changer"), panelX + 12, panelY + 10, Theme.FOREGROUND);
        g.text(mc.font, Component.literal(SkinChanger.status().isEmpty() ? "" : SkinChanger.status()),
                panelX + 12, panelY + 26, Theme.MUTED_FOREGROUND);

        crest$ensureModels();
        PlayerSkin skin = SkinChanger.currentSkin();
        PlayerModel model = skin.model() == PlayerModelType.SLIM ? slimModel : wideModel;
        float scale = FIT * modelH / MODEL_H;
        // release drag when the mouse button is up
        if (dragging && !crest$leftDown()) {
            dragging = false;
            lastDragX = -1;
        }
        // apply drag delta, then auto-rotate when not dragging
        if (dragging) {
            if (lastDragX >= 0) {
                rotX = net.minecraft.util.Mth.clamp(rotX - (my - lastDragY) * ROT_SENS, -ROT_X_LIMIT, ROT_X_LIMIT);
                rotY += (mx - lastDragX) * ROT_SENS;
            }
            lastDragX = mx;
            lastDragY = my;
        } else {
            rotY += delta * 12.0F;
        }
        rotY %= 360.0F;
        g.skin(model, skin.body().texturePath(), scale, rotX, rotY, -1.0625F,
                modelX, modelY, modelX + modelW, modelY + modelH);

        // Buttons
        crest$drawButton(g, "Upload Skin", crest$btn(2));
        crest$drawButton(g, "Search User", crest$btn(1));
        crest$drawButton(g, "Apply to me", crest$btn(0));
    }

    @Unique
    private void crest$drawButton(GuiGraphicsExtractor g, String label, int by) {
        Minecraft mc = Minecraft.getInstance();
        int bx = panelX + 12;
        int bw = PANEL_W - 24;
        int bh = 26;
        boolean h = isHover(bx, by, bw, bh);
        Panel.draw(g, bx, by, bw, bh, h ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 40) : ColorUtil.withAlpha(Theme.BACKGROUND, 120));
        Panel.drawHollowRect(g, bx, by, bw, bh, h ? Theme.getAnimatedAccent() : Theme.BORDER_LIGHT);
        g.centeredText(mc.font, Component.literal(label), bx + bw / 2, by + (bh - 8) / 2,
                h ? Theme.getAnimatedAccent() : Theme.FOREGROUND);
    }

    @Unique
    private boolean isHover(int x, int y, int w, int h) {
        int mx = crest$mx;
        int my = crest$my;
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Unique
    private boolean crest$leftDown() {
        try {
            long handle = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Throwable t) {
            return false;
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void crest$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> ci) {
        if (event.buttonInfo().input() != 0) return;
        double mx = event.x(), my = event.y();
        if (mx < panelX || mx > panelX + PANEL_W || my < panelY || my > panelY + PANEL_H) return;
        dragging = false;

        // Model drag area
        int modelX = panelX + 10, modelY = panelY + 44;
        int modelW = PANEL_W - 20, modelH = PANEL_H - 150;
        if (modelH < 40) modelH = 40;
        if (mx >= modelX && mx <= modelX + modelW && my >= modelY && my <= modelY + modelH) {
            dragging = true;
            lastDragX = (int) mx;
            lastDragY = (int) my;
            ci.cancel();
            ci.setReturnValue(true);
            return;
        }

        // Search User button -> opens search screen
        if (isHover(panelX + 12, crest$btn(1), PANEL_W - 24, 26)) {
            Minecraft.getInstance().setScreen(new SkinChangerScreen((TitleScreen) (Object) this));
            ci.cancel();
            ci.setReturnValue(true);
            return;
        }
        // Upload button
        if (isHover(panelX + 12, crest$btn(2), PANEL_W - 24, 26)) {
            crest$openFilePicker();
            ci.cancel();
            ci.setReturnValue(true);
            return;
        }
        // Apply button
        if (isHover(panelX + 12, crest$btn(0), PANEL_W - 24, 26)) {
            SkinChanger.applyToLocalPlayer();
            ci.cancel();
            ci.setReturnValue(true);
            return;
        }

        ci.cancel();
        ci.setReturnValue(true);
    }

    @Unique
    private void crest$openFilePicker() {
        FileDialog fd = new FileDialog((java.awt.Frame) null, "Select skin PNG");
        fd.setMode(FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".png"));
        fd.setVisible(true);
        String file = fd.getFile();
        if (file != null) {
            File chosen = new File(fd.getDirectory(), file);
            SkinChanger.loadFromFile(chosen);
        }
    }
}
