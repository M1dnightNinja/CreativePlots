package me.m1dnightninja.creativeplots.api.plot;

import me.m1dnightninja.creativeplots.api.math.Region;
import me.m1dnightninja.midnightcore.api.math.Vec3i;

public class PlotPos implements Cloneable {

    // The coordinates of the plot
    private final int x, z;


    /**
     * Constructs a new PlotPos object at given coordinates
     *
     * @param x The X coordinate
     * @param z The Y coordinate
     */
    public PlotPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    /**
     * Returns the X coordinate of a plot
     *
     * @return X coordinate as an int
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the Z coordinate of a plot
     *
     * @return Z coordinate as an int
     */
    public int getZ() {
        return z;
    }


    /**
     * Converts the PlotPos into a Region object
     *
     * @param world The PlotWorld object to query
     * @return      A Region derived from the plot's location and world
     */
    public Region toRegion(IPlotWorld world) {

        int size = world.getPlotSize();

        Vec3i loc = world.toLocation(this);

        Vec3i p1 = new Vec3i(loc.getX(), world.getWorldFloor(), loc.getZ());
        Vec3i p2 = new Vec3i(size, world.getWorldHeight() - world.getWorldFloor(), size);

        return new Region(p1, p2);
    }


    /**
     * Returns the position of an adjacent plot
     *
     * @param direction The direction of the adjacent plot relative to this position
     * @return          The adjacent plot's position as a PlotPos
     */
    public PlotPos getAdjacent(PlotDirection direction) {
        return new PlotPos(x + direction.getXShift(), z + direction.getZShift());
    }


    /**
     * Checks whether another PlotPos is adjacent to this one
     *
     * @param pos The position of the other plot
     * @return    Whether or not the other plot is adjacent, as a boolean
     */
    public boolean isAdjacent(PlotPos pos) {
        return Math.abs(pos.x - x) == 1 || Math.abs(pos.z -z) == 1;
    }


    // Override Methods

    @Override
    public boolean equals(Object obj) {

        if(!(obj instanceof PlotPos)) return false;

        PlotPos pos = (PlotPos) obj;
        return pos.x == x && pos.z == z;
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.z + "]";
    }


    /**
     * Generates a PlotPos from world coordinates
     *
     * @param x        The X coordinate in the world
     * @param z        The Y coordinate in the world
     * @param plotSize The size of each plot in the world
     * @param roadSize The size of the roads in the world
     * @return         A new PlotPos object
     */
    public static PlotPos fromCoords(int x, int z, int plotSize, int roadSize) {

        int offset = roadSize / 2;
        if(roadSize % 2 == 1) offset += 1;

        x -= offset;
        z -= offset;

        int size = plotSize + roadSize;

        int ox = x % size;
        int oz = z % size;

        if(ox < 0) ox += size;
        if(oz < 0) oz += size;

        if(ox < plotSize && oz < plotSize) {

            int px = x < 0 ? (x / size - 1) : x / size;
            int pz = z < 0 ? (z / size - 1) : z / size;

            return new PlotPos(px, pz);
        }

        return null;
    }

    /**
     * Generates a list of surrounding PlotPos objects from world coordinates
     *
     * @param x        The X coordinate in the world
     * @param z        The Y coordinate in the world
     * @param plotSize The size of each plot in the world
     * @param roadSize The size of the roads in the world
     * @return         An array of PlotPos objects
     */
    public static PlotPos[] surroundingFromCoords(int x, int z, int plotSize, int roadSize) {

        int offset = roadSize / 2;
        if(roadSize % 2 == 1) offset += 1;

        x = x - offset;
        z = z - offset;

        int size = plotSize + roadSize;

        int hx = x < 0 ? (x / size - 1) : x / size;
        int hz = z < 0 ? (z / size - 1) : z / size;
        int lx = hx - 1;
        int lz = hz - 1;

        PlotPos[] poss = new PlotPos[4];
        poss[0] = new PlotPos(lx, lz);
        poss[1] = new PlotPos(hx, lz);
        poss[2] = new PlotPos(lx, hz);
        poss[3] = new PlotPos(hx, hz);

        return poss;
    }

}
