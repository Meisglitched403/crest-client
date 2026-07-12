package com.crest.client.core.skinlayers3d;

import net.minecraft.resources.Identifier;

public interface PlayerSettings extends PlayerData {
    void setHeadMesh(Mesh mesh);
    void setTorsoMesh(Mesh mesh);
    void setLeftArmMesh(Mesh mesh);
    void setRightArmMesh(Mesh mesh);
    void setLeftLegMesh(Mesh mesh);
    void setRightLegMesh(Mesh mesh);
    Identifier getCurrentSkin();
    void setCurrentSkin(Identifier skin);
    boolean hasThinArms();
    void setThinArms(boolean thinArms);

    default void clearMeshes() {
        setHeadMesh(null);
        setTorsoMesh(null);
        setLeftArmMesh(null);
        setRightArmMesh(null);
        setLeftLegMesh(null);
        setRightLegMesh(null);
    }
}
