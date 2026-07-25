package com.xunxian.seekingimmortals.item;

/** Pure eight-way bearing helper kept independent from Minecraft registries. */
public final class SpaceRiftCompassDirection {
    private SpaceRiftCompassDirection() {}

    public static String key(double dx, double dz) {
        double angle = Math.atan2(dz, dx);
        int octant = Math.floorMod((int) Math.round(angle / (Math.PI / 4.0D)), 8);
        return switch (octant) {
            case 0 -> "direction.seeking_immortals.east";
            case 1 -> "direction.seeking_immortals.southeast";
            case 2 -> "direction.seeking_immortals.south";
            case 3 -> "direction.seeking_immortals.southwest";
            case 4 -> "direction.seeking_immortals.west";
            case 5 -> "direction.seeking_immortals.northwest";
            case 6 -> "direction.seeking_immortals.north";
            default -> "direction.seeking_immortals.northeast";
        };
    }
}
