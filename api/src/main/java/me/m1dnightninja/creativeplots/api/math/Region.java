package me.m1dnightninja.creativeplots.api.math;

import me.m1dnightninja.midnightcore.api.math.Vec3i;

public class Region {

    Vec3i lower;
    Vec3i extent;

    public Region(Vec3i lower, Vec3i extent) {

        if(extent.getZ() < 0 || extent.getY() < 0 || extent.getZ() < 0) {
            throw new IllegalArgumentException("Cannot create region with negative extent!");
        }

        this.lower = lower;
        this.extent = extent;
    }

    public Vec3i getLowerBound() {
        return lower;
    }

    public Vec3i getUpperBound() {
        return new Vec3i(lower.getX() + extent.getX(), lower.getY() + extent.getY(), lower.getZ() + extent.getZ());
    }

    public Vec3i getExtent() {
        return extent;
    }

    public boolean contains(Vec3i point) {

        return  point.getX() >= lower.getX() && point.getX() < lower.getX() + extent.getX() &&
                point.getY() >= lower.getY() && point.getY() < lower.getY() + extent.getY() &&
                point.getZ() >= lower.getZ() && point.getZ() < lower.getZ() + extent.getZ();
    }

    public static Region fromPoints(Vec3i p1, Vec3i p2) {

        Vec3i lower = new Vec3i(
                Math.min(p1.getX(), p2.getX()),
                Math.min(p1.getY(), p2.getY()),
                Math.min(p1.getY(), p2.getY()));
        Vec3i extent = new Vec3i(
                Math.max(p1.getX(), p2.getX()) - lower.getX(),
                Math.max(p1.getY(), p2.getY()) - lower.getY(),
                Math.max(p1.getZ(), p2.getZ()) - lower.getZ());

        return new Region(lower, extent);

    }

    public static Region normalized(Vec3i p1, Vec3i p2) {

        Vec3i lower = new Vec3i(
                Math.min(p1.getX(), p2.getX()),
                Math.min(p1.getY(), p2.getY()),
                Math.min(p1.getZ(), p2.getZ()));
        Vec3i extent = new Vec3i(
                Math.max(p1.getX(), p2.getX()),
                Math.max(p1.getY(), p2.getY()),
                Math.max(p1.getZ(), p2.getZ()));

        return new Region(lower, extent);

    }

    public Region outset(int i) {
        return new Region(lower.subtract(i), extent.add(i));
    }

    public Region shift(Vec3i dist) {
        return new Region(lower.add(dist), extent);
    }

}
