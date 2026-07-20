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
        assertEquals(0.10D, LifeSkillService.PROFICIENCY_BONUS_MAX, 1e-9);
    }

    @Test
    void proficiencyBonusScalesLinearlyToCap() {
        assertEquals(0.0D, LifeSkillService.proficiencyBonus(0), 1e-9);
        assertEquals(0.05D, LifeSkillService.proficiencyBonus(5000), 1e-9);
        assertEquals(0.10D, LifeSkillService.proficiencyBonus(10000), 1e-9);
        assertEquals(0.10D, LifeSkillService.proficiencyBonus(20000), 1e-9);
        assertEquals(0.0D, LifeSkillService.proficiencyBonus(-10), 1e-9);
    }

    @Test
    void successBonusSourceConsumesProficiency() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "skill", "LifeSkillService.java"));
        String compact = source.replaceAll("\\s+", "");
        assertTrue(compact.contains("proficiencyBonus(proficiency(player,type))")
                        || compact.contains("proficiencyBonus(proficiency("),
                "successBonus must include proficiency contribution");
        assertTrue(compact.contains("PROFICIENCY_BONUS_MAX"));
        assertTrue(compact.contains("BONUS_MAX+PROFICIENCY_BONUS_MAX")
                        || compact.contains("BONUS_MAX + PROFICIENCY_BONUS_MAX"));
    }

    @Test
    void stationEfficiencyScalesSuccessRate() {
        assertEquals(0.50D, LifeSkillService.applyStationEfficiency(0.50D, 1.0D), 1e-9);
        assertEquals(0.30D, LifeSkillService.applyStationEfficiency(0.50D, 0.60D), 1e-9);
        assertEquals(0.10D, LifeSkillService.applyStationEfficiency(0.50D, 0.20D), 1e-9);
        assertEquals(0.03D, LifeSkillService.applyStationEfficiency(0.50D, 0.0D), 1e-9);
        assertTrue(LifeSkillService.applyStationEfficiency(1.5D, 1.0D) <= 0.95D);
    }
}
