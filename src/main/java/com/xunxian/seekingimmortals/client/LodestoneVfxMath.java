package com.xunxian.seekingimmortals.client;

import net.minecraft.world.phys.Vec3;

final class LodestoneVfxMath {
    private LodestoneVfxMath() {}

    static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-8D) {
            return point.distanceToSqr(start);
        }
        double progress = point.subtract(start).dot(segment) / lengthSqr;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return point.distanceToSqr(start.add(segment.scale(progress)));
    }

    static Vec3 beamFacingReference(Vec3 start, Vec3 end, Vec3 camera) {
        Vec3 axis = end.subtract(start);
        Vec3 view = start.subtract(camera);
        double axisLengthSqr = axis.lengthSqr();
        double viewLengthSqr = view.lengthSqr();
        if (axisLengthSqr < 1.0E-8D) {
            return camera;
        }
        if (viewLengthSqr >= 1.0E-8D
                && view.cross(axis).lengthSqr() > axisLengthSqr * viewLengthSqr * 1.0E-6D) {
            return camera;
        }
        Vec3 direction = axis.scale(1.0D / Math.sqrt(axisLengthSqr));
        Vec3 fallback = Math.abs(direction.y) < 0.9D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        return start.subtract(fallback);
    }
}
