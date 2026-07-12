package com.crest.client.core.skinlayers3d;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Avatar;

import java.io.InputStream;
import java.util.Optional;

public class SkinUtil {
    public static boolean setup3dLayers(Avatar avatar, PlayerSettings settings, boolean thinArms) {
        if (!(avatar instanceof AbstractClientPlayer player)) return false;
        Identifier skinLocation = player.getSkin().body().texturePath();
        if (skinLocation == null) return false;

        if (skinLocation.equals(settings.getCurrentSkin()) && thinArms == settings.hasThinArms()) {
            return settings.getHeadMesh() != null;
        }

        NativeImage skin = getTexture(skinLocation, null);
        if (skin == null || skin.getWidth() != 64 || skin.getHeight() != 64) {
            settings.setCurrentSkin(skinLocation);
            settings.setThinArms(thinArms);
            settings.clearMeshes();
            return false;
        }

        settings.setLeftLegMesh(Layers3d.MESH_HELPER.create3DMesh(skin, 4, 12, 4, 0, 48, true, 0));
        settings.setRightLegMesh(Layers3d.MESH_HELPER.create3DMesh(skin, 4, 12, 4, 0, 32, true, 0));
        if (thinArms) {
            settings.setLeftArmMesh(Layers3d.MESH_HELPER.create3DMesh(skin, 3, 12, 4, 48, 48, true, -2));
            settings.setRightArmMesh(Layers3d.MESH_HELPER.create3DMesh(skin, 3, 12, 4, 40, 32, true, -2));
        } else {
            settings.setLeftArmMesh(Layers3d.MESH_HELPER.create3DMesh(skin, 4, 12, 4, 48, 48, true, -2));
            settings.setRightArmMesh(Layers3d.MESH_HELPER.create3DMesh(skin, 4, 12, 4, 40, 32, true, -2));
        }
        settings.setTorsoMesh(Layers3d.MESH_HELPER.create3DMesh(skin, 8, 12, 4, 16, 32, true, 0));
        settings.setHeadMesh(Layers3d.MESH_HELPER.create3DMesh(skin, 8, 8, 8, 32, 0, false, 0.6f));
        settings.setCurrentSkin(skinLocation);
        settings.setThinArms(thinArms);
        return true;
    }

    public static NativeImage getTexture(Identifier location, Object settings) {
        if (location == null) return null;
        try {
            Optional<Resource> optRes = Minecraft.getInstance().getResourceManager().getResource(location);
            if (optRes.isPresent()) {
                return NativeImage.read(optRes.get().open());
            }
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
            if (texture == null) return null;

            if (texture instanceof DynamicTexture dt) {
                return dt.getPixels();
            }
            return null;
        } catch (Exception e) {
            Layers3d.LOGGER.error("Error resolving skin texture", e);
            return null;
        }
    }
}
