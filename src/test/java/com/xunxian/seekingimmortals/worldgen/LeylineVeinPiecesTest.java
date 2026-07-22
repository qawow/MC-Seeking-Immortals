package com.xunxian.seekingimmortals.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeylineVeinPiecesTest {
    @Test
    void shapeConstantsAreDistinct() {
        assertEquals(0, LeylineVeinPieces.SHAPE_MOUNTAIN);
        assertEquals(1, LeylineVeinPieces.SHAPE_FOREST);
        assertEquals(2, LeylineVeinPieces.SHAPE_SHORE);
        assertEquals(3, LeylineVeinPieces.SHAPE_PLAINS);
        assertTrue(LeylineVeinPieces.SHAPE_PLAINS != LeylineVeinPieces.SHAPE_MOUNTAIN);
    }

    @Test
    void elementalBiasTracksLeylineLandscape() {
        assertEquals(LeylineVeinPieces.ELEMENT_METAL,
                LeylineVeinPieces.preferredElementForShape(LeylineVeinPieces.SHAPE_MOUNTAIN, 0));
        assertEquals(LeylineVeinPieces.ELEMENT_EARTH,
                LeylineVeinPieces.preferredElementForShape(LeylineVeinPieces.SHAPE_MOUNTAIN, 45));
        assertEquals(LeylineVeinPieces.ELEMENT_WOOD,
                LeylineVeinPieces.preferredElementForShape(LeylineVeinPieces.SHAPE_FOREST, 0));
        assertEquals(LeylineVeinPieces.ELEMENT_WATER,
                LeylineVeinPieces.preferredElementForShape(LeylineVeinPieces.SHAPE_SHORE, 0));
        assertEquals(LeylineVeinPieces.ELEMENT_FIRE,
                LeylineVeinPieces.preferredElementForShape(LeylineVeinPieces.SHAPE_PLAINS, 50));
        assertEquals(LeylineVeinPieces.ELEMENT_METAL,
                LeylineVeinPieces.preferredElementForShape(LeylineVeinPieces.SHAPE_PLAINS, -1));
    }

    @Test
    void elementalBiasWeightsRemainStableAcrossEveryRoll() {
        assertArrayEquals(new int[]{45, 0, 7, 13, 35},
                elementCounts(LeylineVeinPieces.SHAPE_MOUNTAIN));
        assertArrayEquals(new int[]{7, 55, 13, 0, 25},
                elementCounts(LeylineVeinPieces.SHAPE_FOREST));
        assertArrayEquals(new int[]{8, 18, 60, 0, 14},
                elementCounts(LeylineVeinPieces.SHAPE_SHORE));
        assertArrayEquals(new int[]{11, 14, 0, 25, 50},
                elementCounts(LeylineVeinPieces.SHAPE_PLAINS));
    }

    private static int[] elementCounts(int shape) {
        int[] counts = new int[5];
        for (int roll = 0; roll < 100; roll++) {
            counts[LeylineVeinPieces.preferredElementForShape(shape, roll)]++;
        }
        return counts;
    }
}
