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

    public boolean contains(Vec3i point) {

        return  point.getX() >= lower.getX() && point.getX() < lower.getX() + extent.getX() &&
                point.getY() >= lower.getY() && point.getY() < lower.getY() + extent.getY() &&
                point.getZ() >= lower.getZ() && point.getZ() < lower.getZ() + extent.getZ();
    }




}
