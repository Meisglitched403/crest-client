package com.crest.client.skinlayers;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import com.mojang.blaze3d.platform.NativeImage;
import com.crest.client.skinlayers.accessor.NativeImageAccessor;
import com.crest.client.skinlayers.accessor.PlayerSettings;
import com.crest.client.skinlayers.api.SkinLayersAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Avatar;

public final class CrestSkinUtil {

    private static final Cache<AbstractTexture, NativeImage> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(60L, TimeUnit.SECONDS)
            .removalListener(new RemovalListener<AbstractTexture, NativeImage>() {
                @Override
                public void onRemoval(RemovalNotification<AbstractTexture, NativeImage> notification) {
                    try {
                        notification.getValue().close();
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }).build();

    private CrestSkinUtil() {
    }

    public static NativeImage getTexture(Identifier resourceLocation, boolean[] invalidated) {
        if (resourceLocation == null) {
            return null;
        }
        try {
            Optional<Resource> optionalRes = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);
            if (optionalRes.isPresent()) {
                Resource resource = optionalRes.get();
                NativeImage skin = NativeImage.read(resource.open());
                return skin;
            }
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(resourceLocation);
            if (texture == null) {
                return null;
            }
            NativeImage cachedImage = cache.getIfPresent(texture);
            if (cachedImage != null && (Object) cachedImage instanceof NativeImageAccessor ac
                    && ac.skinlayers$isAllocated()) {
                return cachedImage;
            } else {
                cache.invalidate(texture);
            }
            if (texture instanceof DynamicTexture) {
                try {
                    NativeImage img = ((DynamicTexture) texture).getPixels();
                    if (img != null && (Object) img instanceof NativeImageAccessor ac && ac.skinlayers$isAllocated()) {
                        return img;
                    }
                } catch (Exception ex) {
                    // not backed by an image
                }
                return null;
            }
            if (invalidated != null) {
                invalidated[0] = true;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean setup3dLayers(Avatar abstractClientPlayerEntity, PlayerSettings settings,
            boolean thinArms, Identifier skinLocation) {
        if (skinLocation == null) {
            return false;
        }
        boolean[] invalidated = new boolean[1];
        if (skinLocation.equals(settings.getCurrentSkin()) && thinArms == settings.hasThinArms()) {
            return settings.getHeadMesh() != null;
        }
        NativeImage skin = getTexture(skinLocation, invalidated);
        if (skin == null || skin.getWidth() != 64 || skin.getHeight() != 64) {
            settings.setCurrentSkin(skinLocation);
            settings.setThinArms(thinArms);
            settings.clearMeshes();
            return false;
        }
        settings.setLeftLegMesh(SkinLayersAPI.meshHelper.create3DMesh(skin, 4, 12, 4, 0, 48, true, 0f));
        settings.setRightLegMesh(SkinLayersAPI.meshHelper.create3DMesh(skin, 4, 12, 4, 0, 32, true, 0f));
        if (thinArms) {
            settings.setLeftArmMesh(SkinLayersAPI.meshHelper.create3DMesh(skin, 3, 12, 4, 48, 48, true, -2f));
            settings.setRightArmMesh(SkinLayersAPI.meshHelper.create3DMesh(skin, 3, 12, 4, 40, 32, true, -2f));
        } else {
            settings.setLeftArmMesh(SkinLayersAPI.meshHelper.create3DMesh(skin, 4, 12, 4, 48, 48, true, -2));
            settings.setRightArmMesh(SkinLayersAPI.meshHelper.create3DMesh(skin, 4, 12, 4, 40, 32, true, -2));
        }
        settings.setTorsoMesh(SkinLayersAPI.meshHelper.create3DMesh(skin, 8, 12, 4, 16, 32, true, 0));
        settings.setHeadMesh(SkinLayersAPI.meshHelper.create3DMesh(skin, 8, 8, 8, 32, 0, false, 0.6f));
        settings.setCurrentSkin(skinLocation);
        settings.setThinArms(thinArms);
        return true;
    }

}
