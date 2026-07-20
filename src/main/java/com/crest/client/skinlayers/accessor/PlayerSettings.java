package com.crest.client.skinlayers.accessor;

import com.crest.client.skinlayers.api.Mesh;
import com.crest.client.skinlayers.api.PlayerData;
import net.minecraft.resources.Identifier;

public interface PlayerSettings extends PlayerData {

    public void setHeadMesh(Mesh box);

    public void setTorsoMesh(Mesh box);

    public void setLeftArmMesh(Mesh box);

    public void setRightArmMesh(Mesh box);

    public void setLeftLegMesh(Mesh box);

    public void setRightLegMesh(Mesh box);

    public net.minecraft.resources.Identifier getCurrentSkin();

    public void setCurrentSkin(net.minecraft.resources.Identifier skin);

    public boolean hasThinArms();

    public void setThinArms(boolean thin);

    public default void clearMeshes() {
        setHeadMesh(null);
        setTorsoMesh(null);
        setLeftArmMesh(null);
        setRightArmMesh(null);
        setLeftLegMesh(null);
        setRightLegMesh(null);
    }

}
