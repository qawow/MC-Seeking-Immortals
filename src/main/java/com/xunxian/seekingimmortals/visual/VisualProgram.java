package com.xunxian.seekingimmortals.visual;

import java.util.Comparator;
import java.util.List;

/** Immutable executable visual program compiled from source visual prose. */
public record VisualProgram(
        String compiler,
        int sourceQuoteCount,
        int coveredQuoteCount,
        boolean inferredFallback,
        List<VisualProgramLayer> layers) {

    public VisualProgram {
        compiler = compiler == null ? "" : compiler.trim();
        if (sourceQuoteCount < 0 || coveredQuoteCount < 0 || coveredQuoteCount > sourceQuoteCount) {
            throw new IllegalArgumentException("invalid visual program source counts");
        }
        List<VisualProgramLayer> ordered = layers == null ? List.of() : layers.stream()
                .sorted(Comparator.comparingInt(VisualProgramLayer::eventOrdinal)
                        .thenComparingInt(VisualProgramLayer::layerIndex))
                .toList();
        if (sourceQuoteCount > 0 && coveredQuoteCount != sourceQuoteCount) {
            throw new IllegalArgumentException("visual program does not cover every source quote");
        }
        if (sourceQuoteCount > 0 && ordered.isEmpty()) {
            throw new IllegalArgumentException("visual program has no executable layers");
        }
        layers = ordered;
    }

    public static VisualProgram empty() {
        return new VisualProgram("", 0, 0, true, List.of());
    }

    public boolean executable() {
        return !layers.isEmpty();
    }

    public List<VisualProgramLayer> forEvent(int eventOrdinal) {
        if (eventOrdinal < 0) {
            return layers;
        }
        return layers.stream().filter(layer -> layer.eventOrdinal() == eventOrdinal).toList();
    }
}
