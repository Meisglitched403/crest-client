package com.crest.client.skinlayers.api;

import java.util.Collections;

import com.mojang.blaze3d.platform.NativeImage;

import com.crest.client.skinlayers.render.CustomizableCubeListBuilder;
import com.crest.client.skinlayers.render.CustomizableModelPart;
import com.crest.client.skinlayers.util.WrappedNativeImage;
import com.crest.client.skinlayers.versionless.util.wrapper.SolidPixelWrapper;
import net.minecraft.world.entity.player.Player;

public final class SkinLayersAPI {

    private SkinLayersAPI() {
    }

    public static final MeshHelper meshHelper = new MeshHelperImplementation();
    public static final MeshProvider meshProvider = new MeshProviderImplementation();
    private static MeshTransformerProvider meshTransformerProvider = MeshTransformerProvider.EMPTY_PROVIDER;
    public static BoxBuilder boxBuilder = BoxBuilder.DEFAULT;

    public static void setupBoxBuilder(BoxBuilder builder) {
        SkinLayersAPI.boxBuilder = builder;
    }

    public static void setupMeshTransformerProvider(MeshTransformerProvider provider) {
        SkinLayersAPI.meshTransformerProvider = provider;
    }

    public static MeshTransformerProvider getMeshTransformerProvider() {
        return meshTransformerProvider;
    }

    private static class MeshHelperImplementation implements MeshHelper {

        @Override
        public Mesh create3DMesh(NativeImage natImage, int width, int height, int depth, int textureU, int textureV,
                boolean topPivot, float rotationOffset, boolean mirror) {
            CustomizableCubeListBuilder builder = new CustomizableCubeListBuilder();
            builder.mirror(mirror);
            if (SolidPixelWrapper.wrapBox(builder, new WrappedNativeImage(natImage), width, height, depth, textureU,
                    textureV, topPivot, rotationOffset) != null) {
                return new CustomizableModelPart(builder.getVanillaCubes(), builder.getCubes(), Collections.emptyMap());
            }
            return Mesh.EMPTY;
        }

        @Override
        public Mesh create3DMesh(NativeImage natImage, int width, int height, int depth, int textureU, int textureV,
                boolean topPivot, float rotationOffset) {
            return create3DMesh(natImage, width, height, depth, textureU, textureV, topPivot, rotationOffset, false);
        }

    }

    private static class MeshProviderImplementation implements MeshProvider {

        @Override
        public PlayerData getPlayerMesh(Player player) {
            if (player instanceof PlayerData) {
                return (PlayerData) player;
            }
            return null;
        }

    }

}
