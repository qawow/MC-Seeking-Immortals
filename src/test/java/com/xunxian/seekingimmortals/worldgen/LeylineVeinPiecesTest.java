package com.xunxian.seekingimmortals.worldgen;

import org.junit.jupiter.api.Test;

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
}
