package com.xunxian.seekingimmortals.visual;

import java.util.List;
import java.util.Locale;

/** One executable, source-traceable layer in an authored visual program. */
public record VisualProgramLayer(
        int layerIndex,
        int eventOrdinal,
        VisualPrimitive primitive,
        Anchor anchor,
        Path path,
        Motion motion,
        int copies,
        double radiusScale,
        double lengthScale,
        double heightScale,
        double speed,
        double spreadDegrees,
        double rotationDegrees,
        double verticalOffset,
        double jitter,
        long primaryArgb,
        long secondaryArgb,
        List<String> evidenceTerms,
        String sourceQuote,
        boolean inferred) {

    public VisualProgramLayer {
        if (layerIndex < 0 || eventOrdinal < 0 || primitive == null || anchor == null
                || path == null || motion == null) {
            throw new IllegalArgumentException("invalid visual program identity");
        }
        copies = Math.max(1, Math.min(24, copies));
        radiusScale = bound(radiusScale, 0.1D, 8.0D);
        lengthScale = bound(lengthScale, 0.1D, 8.0D);
        heightScale = bound(heightScale, 0.1D, 8.0D);
        speed = bound(speed, 0.0D, 4.0D);
        spreadDegrees = bound(spreadDegrees, 0.0D, 360.0D);
        rotationDegrees = finite(rotationDegrees) ? rotationDegrees : 0.0D;
        verticalOffset = bound(verticalOffset, -8.0D, 16.0D);
        jitter = bound(jitter, 0.0D, 1.0D);
        if (primaryArgb < 0L || primaryArgb > 0xffff_ffffL
                || secondaryArgb < 0L || secondaryArgb > 0xffff_ffffL) {
            throw new IllegalArgumentException("visual program color outside unsigned 32-bit range");
        }
        evidenceTerms = evidenceTerms == null ? List.of() : evidenceTerms.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct().limit(12).toList();
        sourceQuote = sourceQuote == null ? "" : sourceQuote.trim();
        if (sourceQuote.isBlank() && !inferred) {
            throw new IllegalArgumentException("authored visual layer requires source quote");
        }
    }

    private static double bound(double value, double min, double max) {
        if (!finite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static boolean finite(double value) {
        return Double.isFinite(value);
    }

    public enum Anchor {
        CASTER, TARGET, PATH, MIDPOINT, SCREEN;

        public static Anchor parse(String value) {
            return valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        }
    }

    public enum Path {
        STATIC, DIRECT, CONVERGE, EXPAND, RISE, FALL, ORBIT, SPIRAL, SCATTER, WAVE, TRACK;

        public static Path parse(String value) {
            return valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        }
    }

    public enum Motion {
        STEADY, ACCELERATE, DECELERATE, PULSE, FLICKER, MATERIALIZE, DISSOLVE;

        public static Motion parse(String value) {
            return valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        }
    }
}
