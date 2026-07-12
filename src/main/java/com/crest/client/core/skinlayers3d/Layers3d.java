package com.crest.client.core.skinlayers3d;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.world.entity.Avatar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class Layers3d {
    public static final Logger LOGGER = LoggerFactory.getLogger("SkinLayers3D");
    public static final MeshHelper MESH_HELPER = new MeshHelperImpl();
    public static Avatar currentRenderedEntity;
    private static SkinLayersConfig config = new SkinLayersConfig();

    public static SkinLayersConfig getConfig() { return config; }
    public static void setConfig(SkinLayersConfig cfg) { config = cfg; }

    private static class MeshHelperImpl implements MeshHelper {
        @Override
        public Mesh create3DMesh(NativeImage image, int width, int height, int depth,
                                 int textureU, int textureV, boolean topPivot, float rotationOffset) {
            return create3DMesh(image, width, height, depth, textureU, textureV, topPivot, rotationOffset, false);
        }

        @Override
        public Mesh create3DMesh(NativeImage image, int width, int height, int depth,
                                 int textureU, int textureV, boolean topPivot, float rotationOffset, boolean mirror) {
            CustomizableCubeListBuilder builder = new CustomizableCubeListBuilder();
            builder.mirror(mirror);
            if (SolidPixelWrapper.wrapBox(builder, new NMSWrapper.WrappedNativeImage(image),
                    width, height, depth, textureU, textureV, topPivot, rotationOffset) != null) {
                return new CustomizableModelPart(
                        Collections.emptyList(),
                        builder.getCubes(),
                        Collections.emptyMap()
                );
            }
            return Mesh.EMPTY;
        }
    }
}
