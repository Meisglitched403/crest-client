package com.crest.client.core.skinlayers3d;

import java.util.List;

public abstract class CustomModelPart {
    protected float x, y, z, xRot, yRot, zRot;
    protected boolean visible = true;
    protected float[] polygonData;
    protected int polygonAmount;

    public CustomModelPart(List<CustomizableCube> customCubes) {
        compactCubes(customCubes);
    }

    private void compactCubes(List<CustomizableCube> customCubes) {
        for (CustomizableCube cube : customCubes)
            polygonAmount += cube.polygonCount;

        polygonData = new float[polygonAmount * 23];
        int offset = 0;
        for (CustomizableCube cube : customCubes) {
            for (int id = 0; id < cube.polygonCount; id++) {
                CustomizableCube.Polygon polygon = cube.polygons[id];
                Vector3 normal = polygon.normal;
                polygonData[offset] = normal.x;
                polygonData[offset + 1] = normal.y;
                polygonData[offset + 2] = normal.z;
                for (int i = 0; i < 4; i++) {
                    CustomizableCube.Vertex vertex = polygon.vertices[i];
                    polygonData[offset + 3 + i * 5] = vertex.scaledX;
                    polygonData[offset + 3 + i * 5 + 1] = vertex.scaledY;
                    polygonData[offset + 3 + i * 5 + 2] = vertex.scaledZ;
                    polygonData[offset + 3 + i * 5 + 3] = vertex.u;
                    polygonData[offset + 3 + i * 5 + 4] = vertex.v;
                }
                offset += 23;
            }
        }
    }

    public void setPosition(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public void setRotation(float xRot, float yRot, float zRot) { this.xRot = xRot; this.yRot = yRot; this.zRot = zRot; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible() { return visible; }
}
