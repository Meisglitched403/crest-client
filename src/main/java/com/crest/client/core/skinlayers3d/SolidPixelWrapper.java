package com.crest.client.core.skinlayers3d;

import java.util.HashSet;

public class SolidPixelWrapper {
    private static final float PIXEL_SIZE = 1f;

    public static ModelBuilder wrapBox(ModelBuilder builder, TextureData tex, int width, int height, int depth,
                                        int textureU, int textureV, boolean topPivot, float rotationOffset) {
        builder.textureSize(tex.getWidth(), tex.getHeight());
        float staticXOffset = (float)(-width) / 2f;
        float staticYOffset = topPivot ? rotationOffset : (float)(-height) + rotationOffset;
        float staticZOffset = (float)(-depth) / 2f;
        Position staticOffset = new Position(staticXOffset, staticYOffset, staticZOffset);
        Dimensions dims = new Dimensions(width, height, depth);
        UV textureUV = new UV(textureU, textureV);

        try {
            for (Direction face : Direction.values()) {
                UV sizeUV = getSizeUV(dims, face);
                for (int u = 0; u < sizeUV.u; u++) {
                    for (int v = 0; v < sizeUV.v; v++) {
                        addPixel(tex, builder, staticOffset, face, dims, new UV(u, v), textureUV, sizeUV);
                    }
                }
            }
        } catch (Exception e) {
            Layers3d.LOGGER.error("Error creating 3D skin model", e);
            return null;
        }

        if (Layers3d.getConfig().fastRender) {
            builder.uv(textureU, textureV).addVanillaBox(staticXOffset, staticYOffset, staticZOffset, width, height, depth);
        }
        return builder;
    }

    private static UV getSizeUV(Dimensions dims, Direction face) {
        return switch (face) {
            case DOWN, UP -> new UV(dims.width, dims.depth);
            case NORTH, SOUTH -> new UV(dims.width, dims.height);
            default -> new UV(dims.depth, dims.height);
        };
    }

    private static UV getOnTextureUV(UV textureUV, UV onFaceUV, Dimensions dims, Direction face) {
        return switch (face) {
            case DOWN -> new UV(textureUV.u + dims.depth + onFaceUV.u, textureUV.v + onFaceUV.v);
            case UP -> new UV(textureUV.u + dims.width + dims.depth + onFaceUV.u, textureUV.v + onFaceUV.v);
            case NORTH -> new UV(textureUV.u + dims.depth + onFaceUV.u, textureUV.v + dims.depth + onFaceUV.v);
            case SOUTH -> new UV(textureUV.u + dims.depth + dims.width + dims.depth + onFaceUV.u, textureUV.v + dims.depth + onFaceUV.v);
            case WEST -> new UV(textureUV.u + onFaceUV.u, textureUV.v + dims.depth + onFaceUV.v);
            case EAST -> new UV(textureUV.u + dims.depth + dims.width + onFaceUV.u, textureUV.v + dims.depth + onFaceUV.v);
        };
    }

    private static VoxelPosition UVtoXYZ(UV onFaceUV, Dimensions dims, Direction face) {
        return switch (face) {
            case DOWN -> new VoxelPosition(onFaceUV.u, 0, dims.depth - 1 - onFaceUV.v);
            case UP -> new VoxelPosition(onFaceUV.u, dims.height - 1, dims.depth - 1 - onFaceUV.v);
            case NORTH -> new VoxelPosition(onFaceUV.u, onFaceUV.v, 0);
            case SOUTH -> new VoxelPosition(dims.width - 1 - onFaceUV.u, onFaceUV.v, dims.depth - 1);
            case WEST -> new VoxelPosition(0, onFaceUV.v, dims.depth - 1 - onFaceUV.u);
            case EAST -> new VoxelPosition(dims.width - 1, onFaceUV.v, onFaceUV.u);
        };
    }

    private static UV XYZtoUV(VoxelPosition pos, Dimensions dims, Direction face) {
        return switch (face) {
            case DOWN, UP -> new UV(pos.x, dims.depth - 1 - pos.z);
            case NORTH -> new UV(pos.x, pos.y);
            case SOUTH -> new UV(dims.width - 1 - pos.x, pos.y);
            case WEST -> new UV(dims.depth - 1 - pos.z, pos.y);
            case EAST -> new UV(pos.z, pos.y);
        };
    }

    private static void addPixel(TextureData tex, ModelBuilder cubes, Position staticOffset, Direction face,
                                  Dimensions dims, UV onFaceUV, UV textureUV, UV sizeUV) {
        UV onTextureUV = getOnTextureUV(textureUV, onFaceUV, dims, face);
        if (!tex.isPresent(onTextureUV)) return;

        VoxelPosition voxelPos = UVtoXYZ(onFaceUV, dims, face);
        Position pos = new Position(staticOffset.x + voxelPos.x, staticOffset.y + voxelPos.y, staticOffset.z + voxelPos.z);
        boolean solidPixel = tex.isSolid(onTextureUV);

        HashSet<Direction> hide = new HashSet<>();
        HashSet<Direction[]> corners = new HashSet<>();
        boolean isOnBorder = false;
        boolean backsideOverlaps = false;

        for (Direction neighbour : Direction.values()) {
            if (neighbour.getAxis() == face.getAxis()) continue;
            VoxelPosition neighbourVoxel = new VoxelPosition(
                    voxelPos.x + neighbour.getStepX(), voxelPos.y + neighbour.getStepY(), voxelPos.z + neighbour.getStepZ());
            UV neighbourOnFaceUV = XYZtoUV(neighbourVoxel, dims, face);

            if (isOnFace(neighbourOnFaceUV, sizeUV)) {
                if (tex.isPresent(getOnTextureUV(textureUV, neighbourOnFaceUV, dims, face))) {
                    if (solidPixel && !tex.isSolid(getOnTextureUV(textureUV, neighbourOnFaceUV, dims, face))) continue;
                    hide.add(neighbour);
                    continue;
                }
                VoxelPosition farNeighbour = new VoxelPosition(
                        neighbourVoxel.x + neighbour.getStepX(), neighbourVoxel.y + neighbour.getStepY(), neighbourVoxel.z + neighbour.getStepZ());
                UV farUV = XYZtoUV(farNeighbour, dims, face);
                if (isOnFace(farUV, sizeUV) || !tex.isPresent(getOnTextureUV(textureUV, farUV, dims, neighbour))) continue;
                if (solidPixel && !tex.isSolid(getOnTextureUV(textureUV, farUV, dims, neighbour))) continue;
                hide.add(neighbour);
                continue;
            }

            isOnBorder = true;
            neighbourOnFaceUV = XYZtoUV(voxelPos, dims, neighbour);
            if (tex.isPresent(getOnTextureUV(textureUV, neighbourOnFaceUV, dims, neighbour))) {
                backsideOverlaps = true;
                hide.add(neighbour);
                corners.add(new Direction[]{face.getOpposite(), neighbour});
            } else {
                UV downNeighbour = XYZtoUV(new VoxelPosition(
                        voxelPos.x - face.getStepX(), voxelPos.y - face.getStepY(), voxelPos.z - face.getStepZ()), dims, neighbour);
                if (tex.isPresent(getOnTextureUV(textureUV, downNeighbour, dims, neighbour)))
                    backsideOverlaps = true;
            }
        }

        if (!isOnBorder || backsideOverlaps)
            hide.add(face.getOpposite());

        if (Layers3d.getConfig().fastRender)
            hide.add(face);

        cubes.uv(onTextureUV.u, onTextureUV.v)
                .addBox(pos.x, pos.y, pos.z, 1f,
                        hide.toArray(new Direction[0]),
                        corners.toArray(new Direction[0][0]));
    }

    private static boolean isOnFace(UV uv, UV sizeUV) {
        return uv.u >= 0 && uv.u < sizeUV.u && uv.v >= 0 && uv.v < sizeUV.v;
    }

    public record Position(float x, float y, float z) {}
    public record Dimensions(int width, int height, int depth) {}
    public record UV(int u, int v) {}
    public record VoxelPosition(int x, int y, int z) {}
}
