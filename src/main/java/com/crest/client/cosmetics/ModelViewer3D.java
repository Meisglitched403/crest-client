package com.crest.client.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public class ModelViewer3D {
    private static final float ROT_SENS = 0.3f;
    private static final float ROT_X_LIMIT = 50f;
    private static final float AUTO_ROT_SPEED = 0.4f;
    
    private float rotX = 10f;
    private float rotY = 20f;
    private float targetRotY = 20f;
    private float scale = 30f;
    private boolean autoRotate = true;
    private boolean dragging = false;
    private int lastDragX = -1, lastDragY = -1;
    
    private int x, y, width, height;
    private PlayerSkin currentSkin;
    private Identifier capeTexture;
    
    public ModelViewer3D() {}
    
    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public void setSkin(PlayerSkin skin) {
        this.currentSkin = skin;
    }
    
    public void setCape(Identifier capeTexture) {
        this.capeTexture = capeTexture;
    }
    
    public void setRotation(float rotX, float rotY) {
        this.rotX = rotX;
        this.rotY = rotY;
        this.targetRotY = rotY;
    }
    
    public void setScale(float scale) {
        this.scale = scale;
    }
    
    public void setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
    }
    
    public void resetView() {
        rotX = 10f;
        rotY = 20f;
        targetRotY = 20f;
        scale = 30f;
        autoRotate = true;
    }
    
    public void tick(int mx, int my, float delta) {
        if (dragging) {
            if (lastDragX >= 0) {
                rotX = Mth.clamp(rotX - (my - lastDragY) * ROT_SENS, -ROT_X_LIMIT, ROT_X_LIMIT);
                targetRotY += (mx - lastDragX) * ROT_SENS;
                autoRotate = false;
            }
            lastDragX = mx;
            lastDragY = my;
        } else {
            lastDragX = -1;
            lastDragY = -1;
            if (autoRotate) {
                targetRotY += AUTO_ROT_SPEED * delta;
            }
        }
        
        // Smooth rotation interpolation
        float diff = targetRotY - rotY;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        rotY += diff * 0.15f * delta;
    }
    
    public void render(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (currentSkin == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getGameProfile() != null) {
                currentSkin = DefaultPlayerSkin.get(mc.getGameProfile());
            } else {
                currentSkin = DefaultPlayerSkin.getDefaultSkin();
            }
        }
        
        tick(mx, my, delta);
        
        g.skin(
            getPlayerModel(currentSkin.model()),
            currentSkin.body().texturePath(),
            scale,
            rotX,
            rotY,
            -1.0625f,
            x,
            y,
            x + width,
            y + height
        );
    }
    
    public boolean mouseClicked(int mx, int my, int button) {
        if (button == 0 && isHovering(mx, my)) {
            dragging = true;
            lastDragX = mx;
            lastDragY = my;
            return true;
        }
        return false;
    }
    
    public boolean mouseReleased(int mx, int my, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            lastDragX = -1;
            lastDragY = -1;
            return true;
        }
        return false;
    }
    
    public boolean mouseScrolled(int mx, int my, double deltaY) {
        if (isHovering(mx, my)) {
            scale = Mth.clamp(scale + (float)deltaY * 2f, 15f, 60f);
            return true;
        }
        return false;
    }
    
    public boolean isHovering(int mx, int my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }
    
    private PlayerModel getPlayerModel(PlayerModelType modelType) {
        return modelType == PlayerModelType.SLIM 
            ? new PlayerModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true)
            : new PlayerModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
    }
}
