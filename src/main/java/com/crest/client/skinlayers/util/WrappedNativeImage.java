package com.crest.client.skinlayers.util;

import com.mojang.blaze3d.platform.NativeImage;

import com.crest.client.skinlayers.versionless.util.wrapper.SolidPixelWrapper.UV;
import com.crest.client.skinlayers.versionless.util.wrapper.TextureData;

public class WrappedNativeImage implements TextureData {

    private final NativeImage natImage;

    public WrappedNativeImage(NativeImage natImage) {
        this.natImage = natImage;
    }

    @Override
    public boolean isPresent(UV onTextureUV) {
        if (onTextureUV.u() < 0 || onTextureUV.v() < 0 || onTextureUV.u() >= natImage.getWidth()
                || onTextureUV.v() >= natImage.getHeight())
            return false;
        return natImage.getLuminanceOrAlpha(onTextureUV.u(), onTextureUV.v()) != 0;
    }

    @Override
    public boolean isSolid(UV onTextureUV) {
        if (onTextureUV.u() < 0 || onTextureUV.v() < 0 || onTextureUV.u() >= natImage.getWidth()
                || onTextureUV.v() >= natImage.getHeight())
            return false;
        return natImage.getLuminanceOrAlpha(onTextureUV.u(), onTextureUV.v()) == -1;
    }

    @Override
    public int getWidth() {
        return natImage.getWidth();
    }

    @Override
    public int getHeight() {
        return natImage.getHeight();
    }

}
