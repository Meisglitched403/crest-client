package com.crest.client.core.skinlayers3d;

import java.util.HashMap;
import java.util.Map;

public class CustomizableCube {
    private final Direction[] hidden;
    protected final Polygon[] polygons;
    protected int polygonCount;
    public final float minX, minY, minZ, maxX, maxY, maxZ;

    public CustomizableCube(int u, int v, float x, float y, float z, float sizeX, float sizeY, float sizeZ,
                            float extraX, float extraY, float extraZ, boolean mirror,
                            float textureWidth, float textureHeight, Direction[] hide, Direction[][] hideCorners) {
        this.hidden = hide;
        this.minX = x;
        this.minY = y;
        this.minZ = z;
        this.maxX = x + sizeX;
        this.maxY = y + sizeY;
        this.maxZ = z + sizeZ;
        this.polygons = new Polygon[6];

        float pX = x + sizeX;
        float pY = y + sizeY;
        float pZ = z + sizeZ;
        x -= extraX;
        y -= extraY;
        z -= extraZ;
        pX += extraX;
        pY += extraY;
        pZ += extraZ;

        if (mirror) {
            float tmp = pX;
            pX = x;
            x = tmp;
        }

        Vertex vNNN = new Vertex(x, y, z, 0, 0);
        Vertex vPNN = new Vertex(pX, y, z, 0, 8);
        Vertex vPPN = new Vertex(pX, pY, z, 8, 8);
        Vertex vNPN = new Vertex(x, pY, z, 8, 0);
        Vertex vNNP = new Vertex(x, y, pZ, 0, 0);
        Vertex vPNP = new Vertex(pX, y, pZ, 0, 8);
        Vertex vPPP = new Vertex(pX, pY, pZ, 8, 8);
        Vertex vNPP = new Vertex(x, pY, pZ, 8, 0);

        float minU = u;
        float maxU = u + 1f;
        float minV = v;
        float maxV = v + 1f;

        Map<Direction.Axis, Direction[]> axisToCorner = new HashMap<>();
        for (Direction[] corner : hideCorners) {
            outer:
            for (Direction.Axis axis : Direction.Axis.VALUES) {
                for (Direction dir : corner) {
                    if (dir.getAxis() == axis) continue outer;
                }
                axisToCorner.put(axis, corner);
                break;
            }
        }

        if (visibleFace(Direction.DOWN))
            polygons[polygonCount++] = new Polygon(removeCornerVertex(new Vertex[]{vPNP, vNNP, vNNN, vPNN}, axisToCorner.get(Direction.Axis.Y)), minU, minV, maxU, maxV, textureWidth, textureHeight, mirror, Direction.DOWN);
        if (visibleFace(Direction.UP))
            polygons[polygonCount++] = new Polygon(removeCornerVertex(new Vertex[]{vPPN, vNPN, vNPP, vPPP}, axisToCorner.get(Direction.Axis.Y)), minU, minV, maxU, maxV, textureWidth, textureHeight, mirror, Direction.UP);
        if (visibleFace(Direction.NORTH))
            polygons[polygonCount++] = new Polygon(removeCornerVertex(new Vertex[]{vPNN, vNNN, vNPN, vPPN}, axisToCorner.get(Direction.Axis.Z)), minU, minV, maxU, maxV, textureWidth, textureHeight, mirror, Direction.NORTH);
        if (visibleFace(Direction.SOUTH))
            polygons[polygonCount++] = new Polygon(removeCornerVertex(new Vertex[]{vNNP, vPNP, vPPP, vNPP}, axisToCorner.get(Direction.Axis.Z)), minU, minV, maxU, maxV, textureWidth, textureHeight, mirror, Direction.SOUTH);
        if (visibleFace(Direction.WEST))
            polygons[polygonCount++] = new Polygon(removeCornerVertex(new Vertex[]{vNNN, vNNP, vNPP, vNPN}, axisToCorner.get(Direction.Axis.X)), minU, minV, maxU, maxV, textureWidth, textureHeight, mirror, Direction.WEST);
        if (visibleFace(Direction.EAST))
            polygons[polygonCount++] = new Polygon(removeCornerVertex(new Vertex[]{vPNP, vPNN, vPPN, vPPP}, axisToCorner.get(Direction.Axis.X)), minU, minV, maxU, maxV, textureWidth, textureHeight, mirror, Direction.EAST);
    }

    private boolean visibleFace(Direction face) {
        for (Direction dir : hidden) {
            if (dir == face) return false;
        }
        return true;
    }

    private static Vertex[] removeCornerVertex(Vertex[] vertices, Direction[] corner) {
        if (corner == null) return vertices;
        Vertex except = vertices[0];
        for (int i = 1; i < 4; i++)
            except = compareVertices(except, vertices[i], corner);
        int index = 0;
        for (int i = 0; i < 4; i++) {
            if (vertices[i] != except)
                vertices[index++] = vertices[i];
        }
        vertices[3] = vertices[2];
        return vertices;
    }

    private static Vertex compareVertices(Vertex v1, Vertex v2, Direction[] corner) {
        for (Direction dir : corner) {
            double d = dir.getAxis().choose(v1.pos.x - v2.pos.x, v1.pos.y - v2.pos.y, v1.pos.z - v2.pos.z) * dir.getDirStep();
            if (d > 0) return v1;
            if (d < 0) return v2;
        }
        return v1;
    }

    protected static class Polygon {
        public final Vertex[] vertices;
        public final Vector3 normal;

        public Polygon(Vertex[] vertices, float minU, float minV, float maxU, float maxV,
                       float textureWidth, float textureHeight, boolean mirror, Direction direction) {
            this.vertices = vertices;
            float zw = 0f / textureWidth;
            float zh = 0f / textureHeight;
            vertices[0] = vertices[0].remap(maxU / textureWidth - zw, minV / textureHeight + zh);
            vertices[1] = vertices[1].remap(minU / textureWidth + zw, minV / textureHeight + zh);
            vertices[2] = vertices[2].remap(minU / textureWidth + zw, maxV / textureHeight - zh);
            vertices[3] = vertices[3].remap(maxU / textureWidth - zw, maxV / textureHeight - zh);

            if (mirror) {
                for (int i = 0; i < vertices.length / 2; i++) {
                    Vertex tmp = vertices[i];
                    vertices[i] = vertices[vertices.length - 1 - i];
                    vertices[vertices.length - 1 - i] = tmp;
                }
            }

            this.normal = new Vector3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            if (mirror) this.normal.mul(-1, 1, 1);
        }
    }

    protected static class Vertex {
        public final Vector3 pos;
        public final float u, v;
        public final float scaledX, scaledY, scaledZ;

        public Vertex(float x, float y, float z, float u, float v) {
            this(new Vector3(x, y, z), u, v);
        }

        public Vertex(Vector3 pos, float u, float v) {
            this.pos = pos;
            this.u = u;
            this.v = v;
            this.scaledX = pos.x / 16f;
            this.scaledY = pos.y / 16f;
            this.scaledZ = pos.z / 16f;
        }

        public Vertex remap(float u, float v) {
            return new Vertex(this.pos, u, v);
        }
    }
}
