package com.crest.client.skinlayers.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.model.geom.ModelPart;

public interface MeshTransformerProvider {

    public MeshTransformer prepareTransformer(@Nullable ModelPart vanillaModel);

    public static final MeshTransformerProvider EMPTY_PROVIDER = (cube) -> MeshTransformer.EMPTY_TRANSFORMER;

}
