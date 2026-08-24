package com.crest.client.cosmetics.ui;

import com.crest.client.cosmetics.SkinLibrary;
import com.crest.client.ui.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public class SkinCard implements Widget {
    private static final int CARD_H = 80;
    
    private final SkinLibrary.SkinEntry skin;
    private final Consumer<String> onSelect;
    private final Consumer<String> onApply;
    private final Consumer<String> onFavorite;
    private final Consumer<String> onDelete;
    
    private int lastX, lastY, lastW;
    private boolean equipped;
    
    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }
    
    public SkinCard(SkinLibrary.SkinEntry skin, Consumer<String> onSelect, Consumer<String> onApply, Consumer<String> onFavorite, Consumer<String> onDelete) {
        this.skin = skin;
        this.onSelect = onSelect;
        this.onApply = onApply;
        this.onFavorite = onFavorite;
        this.onDelete = onDelete;
    }
    
    @Override
    public int getWidth() {
        return 0;
    }
    
    @Override
    public int getHeight() {
        return CARD_H;
    }
    
    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        lastX = x;
        lastY = y;
        lastW = w;
        
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + CARD_H;
        
        int bg = hover ? ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 120) : ColorUtil.withAlpha(Theme.BACKGROUND, 60);
        g.fill(x, y, x + w, y + CARD_H, bg);
        Panel.drawHollowRect(g, x, y, w, CARD_H, equipped ? Theme.ACCENT : ColorUtil.withAlpha(Theme.BORDER, 100));
        
        if (equipped) {
            g.fill(x, y, x + w, y + 2, Theme.ACCENT);
        }
        
        if (hover) {
            g.fill(x, y, x + 3, y + CARD_H, Theme.ACCENT);
        }
        
        // Skin preview thumbnail
        int previewSize = 64;
        int previewX = x + Spacing.S2;
        int previewY = y + (CARD_H - previewSize) / 2;
        
        Identifier texture = SkinLibrary.getTexture(skin.id);
        if (texture != null) {
            g.fill(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY + previewSize + 1, ColorUtil.withAlpha(Theme.BORDER, 150));
            g.blit(RenderPipelines.GUI_TEXTURED, texture, previewX, previewY, 8f, 8f, previewSize, previewSize, 64, 64, 0xFFFFFFFF);
        } else {
            g.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, ColorUtil.withAlpha(Theme.MUTED, 100));
            g.centeredText(font, Component.literal("?"), previewX + previewSize / 2, previewY + previewSize / 2 - font.lineHeight / 2, Theme.MUTED_FOREGROUND);
        }
        
        // Skin info
        int infoX = previewX + previewSize + Spacing.S3;
        int infoY = y + Spacing.S3;
        
        g.text(font, Component.literal(skin.name), infoX, infoY, Theme.FOREGROUND);
        
        if (equipped) {
            int tagX = infoX + font.width(skin.name) + Spacing.S2;
            g.fill(tagX, infoY, tagX + font.width("Equipped") + Spacing.S2, infoY + font.lineHeight, ColorUtil.withAlpha(Theme.ACCENT, 70));
            g.text(font, Component.literal("Equipped"), tagX + Spacing.S1, infoY, Theme.ACCENT);
        }
        
        String modelType = skin.modelType.toString().toLowerCase();
        String sourceText = skin.source != null && !skin.source.isEmpty() ? skin.source : "Custom";
        g.text(font, Component.literal(modelType + " • " + sourceText), infoX, infoY + font.lineHeight + 2, 
            ColorUtil.withAlpha(Theme.MUTED_FOREGROUND, 200));
        
        // Action buttons
        int btnY = y + CARD_H - 28;
        int btnSize = 24;
        int btnSpacing = 4;
        
        int starX = x + w - Spacing.S2 - btnSize * 3 - btnSpacing * 2;
        int deleteX = starX + btnSize + btnSpacing;
        int applyX = deleteX + btnSize + btnSpacing;
        
        boolean starHover = mx >= starX && mx <= starX + btnSize && my >= btnY && my <= btnY + btnSize;
        boolean deleteHover = mx >= deleteX && mx <= deleteX + btnSize && my >= btnY && my <= btnY + btnSize;
        boolean applyHover = mx >= applyX && mx <= applyX + btnSize && my >= btnY && my <= btnY + btnSize;
        
        // Star/favorite button
        int starBg = skin.isFavorite ? ColorUtil.withAlpha(Theme.ACCENT, starHover ? 200 : 150) 
            : ColorUtil.withAlpha(Theme.SURFACE_VARIANT, starHover ? 180 : 120);
        g.fill(starX, btnY, starX + btnSize, btnY + btnSize, starBg);
        Panel.drawHollowRect(g, starX, btnY, btnSize, btnSize, Theme.BORDER);
        g.centeredText(font, Component.literal(skin.isFavorite ? "★" : "☆"), starX + btnSize / 2, btnY + 6, 
            skin.isFavorite ? 0xFFFFD700 : Theme.MUTED_FOREGROUND);
        
        // Delete button
        int deleteBg = ColorUtil.withAlpha(Theme.DESTRUCTIVE, deleteHover ? 200 : 100);
        g.fill(deleteX, btnY, deleteX + btnSize, btnY + btnSize, deleteBg);
        Panel.drawHollowRect(g, deleteX, btnY, btnSize, btnSize, Theme.BORDER);
        g.centeredText(font, Component.literal("×"), deleteX + btnSize / 2, btnY + 4, 0xFFFFFFFF);
        
        // Apply button
        int applyBg = ColorUtil.withAlpha(Theme.ACCENT, applyHover ? 220 : 180);
        g.fill(applyX, btnY, applyX + btnSize, btnY + btnSize, applyBg);
        Panel.drawHollowRect(g, applyX, btnY, btnSize, btnSize, Theme.ACCENT);
        g.centeredText(font, Component.literal("✓"), applyX + btnSize / 2, btnY + 5, 0xFFFFFFFF);
    }
    
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (my < lastY || my > lastY + CARD_H) return false;
        
        int btnY = lastY + CARD_H - 28;
        int btnSize = 24;
        int btnSpacing = 4;
        
        int starX = lastX + lastW - Spacing.S2 - btnSize * 3 - btnSpacing * 2;
        int deleteX = starX + btnSize + btnSpacing;
        int applyX = deleteX + btnSize + btnSpacing;
        
        if (mx >= starX && mx <= starX + btnSize && my >= btnY && my <= btnY + btnSize) {
            onFavorite.accept(skin.id);
            return true;
        }
        
        if (mx >= deleteX && mx <= deleteX + btnSize && my >= btnY && my <= btnY + btnSize) {
            onDelete.accept(skin.id);
            return true;
        }
        
        if (mx >= applyX && mx <= applyX + btnSize && my >= btnY && my <= btnY + btnSize) {
            onApply.accept(skin.id);
            return true;
        }
        
        if (mx >= lastX && mx <= lastX + lastW) {
            onSelect.accept(skin.id);
            return true;
        }
        
        return false;
    }
}
