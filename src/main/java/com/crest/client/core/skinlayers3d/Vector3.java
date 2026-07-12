package com.crest.client.core.skinlayers3d;

public class Vector3 {
    public float x, y, z;

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void mul(float mx, float my, float mz) {
        this.x *= mx;
        this.y *= my;
        this.z *= mz;
    }
}
