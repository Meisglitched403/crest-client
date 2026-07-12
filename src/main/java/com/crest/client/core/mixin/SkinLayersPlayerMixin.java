package com.crest.client.core.mixin;

import com.crest.client.core.skinlayers3d.Mesh;
import com.crest.client.core.skinlayers3d.PlayerSettings;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Avatar.class)
public abstract class SkinLayersPlayerMixin extends LivingEntity implements PlayerSettings {
    @Unique private Mesh headMesh;
    @Unique private Mesh torsoMesh;
    @Unique private Mesh leftArmMesh;
    @Unique private Mesh rightArmMesh;
    @Unique private Mesh leftLegMesh;
    @Unique private Mesh rightLegMesh;
    @Unique private Identifier currentSkin;
    @Unique private boolean thinArms;

    protected SkinLayersPlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override public Mesh getHeadMesh() { return headMesh; }
    @Override public void setHeadMesh(Mesh m) { headMesh = m; }
    @Override public Mesh getTorsoMesh() { return torsoMesh; }
    @Override public void setTorsoMesh(Mesh m) { torsoMesh = m; }
    @Override public Mesh getLeftArmMesh() { return leftArmMesh; }
    @Override public void setLeftArmMesh(Mesh m) { leftArmMesh = m; }
    @Override public Mesh getRightArmMesh() { return rightArmMesh; }
    @Override public void setRightArmMesh(Mesh m) { rightArmMesh = m; }
    @Override public Mesh getLeftLegMesh() { return leftLegMesh; }
    @Override public void setLeftLegMesh(Mesh m) { leftLegMesh = m; }
    @Override public Mesh getRightLegMesh() { return rightLegMesh; }
    @Override public void setRightLegMesh(Mesh m) { rightLegMesh = m; }
    @Override public Identifier getCurrentSkin() { return currentSkin; }
    @Override public void setCurrentSkin(Identifier s) { currentSkin = s; }
    @Override public boolean hasThinArms() { return thinArms; }
    @Override public void setThinArms(boolean t) { thinArms = t; }
}
