package me.m1dnightninja.creativeplots.api.plot;

import me.m1dnightninja.midnightcore.api.math.Vec3i;

public enum PlotDirection {

    NORTH( 0,-1),
    SOUTH( 0, 1),
    EAST ( 1, 0),
    WEST (-1, 0);


    int xs;
    int zs;

    PlotDirection(int x, int z) {

        this.xs = x;
        this.zs = z;
    }

    public int getXShift() {
        return xs;
    }

    public int getZShift() {
        return zs;
    }

    public Vec3i vector() {
        return new Vec3i(xs,0,zs);
    }

    public Vec3i vectorInverted() {
        return new Vec3i(-1 * zs, 0, -1 * xs);
    }

}
