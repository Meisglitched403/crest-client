package com.crest.client.core.skinlayers3d;

import com.mojang.blaze3d.platform.NativeImage;

public class NMSWrapper {
    public static class WrappedNativeImage implements TextureData {
        private final NativeImage image;

        public WrappedNativeImage(NativeImage image) {
            this.image = image;
        }

        @Override
        public boolean isPresent(SolidPixelWrapper.UV uv) {
            return image.getLuminanceOrAlpha(uv.u(), uv.v()) != 0;
        }

        @Override
        public boolean isSolid(SolidPixelWrapper.UV uv) {
            return image.getLuminanceOrAlpha(uv.u(), uv.v()) == -1;
        }

        @Override
        public int getWidth() { return image.getWidth(); }

        @Override
        public int getHeight() { return image.getHeight(); }
    }
}
