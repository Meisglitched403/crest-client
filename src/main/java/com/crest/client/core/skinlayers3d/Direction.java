package com.crest.client.core.skinlayers3d;

public enum Direction {
    DOWN(Axis.Y, 0, -1, 0),
    UP(Axis.Y, 0, 1, 0),
    NORTH(Axis.Z, 0, 0, -1),
    SOUTH(Axis.Z, 0, 0, 1),
    WEST(Axis.X, -1, 0, 0),
    EAST(Axis.X, 1, 0, 0);

    private static final Direction[] OPPOSITE = {UP, DOWN, SOUTH, NORTH, EAST, WEST};
    private final Axis axis;
    private final int x, y, z;

    Direction(Axis axis, int x, int y, int z) {
        this.axis = axis;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Direction getOpposite() { return OPPOSITE[ordinal()]; }
    public Axis getAxis() { return axis; }
    public int getStepX() { return x; }
    public int getStepY() { return y; }
    public int getStepZ() { return z; }
    public int getDirStep() { return x + y + z; }

    public enum Axis {
        X, Y, Z;

        public static final Axis[] VALUES = values();

        public double choose(double x, double y, double z) {
            return switch (this) {
                case X -> x;
                case Y -> y;
                case Z -> z;
            };
        }
    }
}
