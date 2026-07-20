package com.crest.client.skinlayers.versionless.util.wrapper;

import com.crest.client.skinlayers.versionless.util.wrapper.SolidPixelWrapper.UV;

public interface TextureData {

    public boolean isPresent(UV onTextureUV);

    public boolean isSolid(UV onTextureUV);

    public default int getWidth() {
        return 64;
    }

    public default int getHeight() {
        return 64;
    }

}
