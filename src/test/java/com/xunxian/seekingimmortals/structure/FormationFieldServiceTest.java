package com.xunxian.seekingimmortals.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationFieldServiceTest {
    @Test
    void fieldKindsExposeRingConfig() {
        assertEquals(2, FormationFieldService.FieldKind.SPIRIT_GATHER.radius());
        assertTrue(FormationFieldService.FieldKind.SPIRIT_GATHER.usesSpiritGatheringRing());
        assertEquals(2, FormationFieldService.FieldKind.KILL_SWORD.radius());
        assertTrue(!FormationFieldService.FieldKind.KILL_SWORD.usesSpiritGatheringRing());
    }

    @Test
    void activeCountStartsEmptyAfterClear() {
        FormationFieldService.clearAll();
        assertEquals(0, FormationFieldService.activeCount());
    }
}
