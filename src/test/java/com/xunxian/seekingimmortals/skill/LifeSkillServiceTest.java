package com.xunxian.seekingimmortals.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifeSkillServiceTest {
    @Test
    void adjustedSuccessRateStacksBonusAndClamps() {
        double base = 0.50D;
        // Without player, bonus is 0.
        assertEquals(0.50D, LifeSkillService.adjustedSuccessRate(null, SkillType.ALCHEMY, base), 1e-9);
        // Clamp upper
        assertTrue(LifeSkillService.adjustedSuccessRate(null, SkillType.ALCHEMY, 1.5D) <= 0.95D);
        // Clamp lower floor at 0.03
        assertEquals(0.03D, LifeSkillService.adjustedSuccessRate(null, SkillType.ALCHEMY, 0.0D), 1e-9);
    }

    @Test
    void summaryLineHandlesMissingSkill() {
        String line = LifeSkillService.summaryLine(null, SkillType.TALISMAN_CRAFTING);
        assertTrue(line.contains("L0"));
    }

    @Test
    void bonusConstantsAreStable() {
        assertEquals(0.02D, LifeSkillService.BONUS_PER_LEVEL, 1e-9);
        assertEquals(0.20D, LifeSkillService.BONUS_MAX, 1e-9);
    }
}
