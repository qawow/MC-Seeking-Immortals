package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.item.pill.CatalogPillType;
import com.xunxian.seekingimmortals.item.pill.PillQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreakthroughAidLogicTest {
    @Test
    void pillQualityBreakthroughBonusesUseUnifiedTable() {
        assertEquals(0.05D, PillQuality.LOW.getBreakthroughBonus(), 0.0001D);
        assertEquals(0.10D, PillQuality.MEDIUM.getBreakthroughBonus(), 0.0001D);
        assertEquals(0.15D, PillQuality.HIGH.getBreakthroughBonus(), 0.0001D);
        assertEquals(0.20D, PillQuality.SUPREME.getBreakthroughBonus(), 0.0001D);
    }

    @Test
    void breakthroughPillBonusClampsAtTwentyPercent() {
        PlayerCultivation cultivation = new PlayerCultivation();

        cultivation.setBreakthroughPillBonus(0.35D);

        assertEquals(0.20D, cultivation.getBreakthroughPillBonus(), 0.0001D);
        assertTrue(cultivation.isBreakthroughAssisted());

        cultivation.setBreakthroughPillBonus(-0.25D);

        assertEquals(0.0D, cultivation.getBreakthroughPillBonus(), 0.0001D);
        assertFalse(cultivation.isBreakthroughAssisted());
    }

    @Test
    void breakthroughChanceBreakdownUsesPassedPillBonus() {
        PlayerCultivation cultivation = new PlayerCultivation();
        PlayerCultivation.BreakthroughChanceBreakdown base =
                cultivation.getBreakthroughChanceBreakdown(PlayerCultivation.BreakthroughChanceModifiers.NONE);

        PlayerCultivation.BreakthroughChanceBreakdown boosted =
                cultivation.getBreakthroughChanceBreakdown(new PlayerCultivation.BreakthroughChanceModifiers(0.15D, 0.0D, 0.0D));

        assertEquals(0.15D, boosted.pillBonus(), 0.0001D);
        assertEquals(base.chance() + 0.15D, boosted.chance(), 0.0001D);
    }

    @Test
    void futureSystemCatalogPillsAreExplicitlyDisabled() {
        assertTrue(CatalogPillType.CLEAR_VOID.futureSystemDisabled());
        assertTrue(CatalogPillType.FORGET_DUST.futureSystemDisabled());
        assertTrue(CatalogPillType.APPEARANCE_FIXING.futureSystemDisabled());
        assertFalse(CatalogPillType.SPIRIT_GATHERING.futureSystemDisabled());
        assertFalse(CatalogPillType.BODY_TEMPERING.futureSystemDisabled());
        assertFalse(CatalogPillType.PRESSURE_RESIST.futureSystemDisabled());
        assertFalse(CatalogPillType.RETURN_YANG_TRUE_WATER.futureSystemDisabled());
    }

    @Test
    void pressureResistPillUsesDiyuanRealmGate() {
        assertEquals("pressure_resist_pill", CatalogPillType.PRESSURE_RESIST.id());
        assertEquals(Realm.VOID_REFINEMENT, CatalogPillType.PRESSURE_RESIST.minRealm());
    }
}
