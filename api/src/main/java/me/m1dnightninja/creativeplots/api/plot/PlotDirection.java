package me.m1dnightninja.creativeplots.api.plot;

import me.m1dnightninja.midnightcore.api.math.Vec3i;

public enum PlotDirection {

    NORTH("north", 0,-1),
    SOUTH("south", 0, 1),
    EAST ("east", 1, 0),
    WEST ("west",-1, 0);


    String name;
    int xs;
    int zs;

    PlotDirection(String name, int x, int z) {

        this.name = name;
        this.xs = x;
        this.zs = z;
    }

    public String getName() {
        return name;
    }

    public static PlotDirection byName(String name) {
        for(PlotDirection dir : values()) {
            if(dir.name.equals(name)) {
                return dir;
            }
        }
        return null;
    }

    public boolean isPerpendicular(PlotDirection other) {
        return (xs == 0 && other.xs != 0) || (zs == 0 && other.zs != 0);
    }

    public PlotDirection[] getPerpendicular() {
        PlotDirection[] out = new PlotDirection[2];
        int i = 0;
        for(PlotDirection d : values()) {
            if(d.isPerpendicular(this)) {
                out[i] = d;
                i++;
            }
        }
        return out;
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
