package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.combat.status.StatusRegistryTestSupport;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechniqueGateServiceTest {

    @Test
    void sealNascentStatusRejectsCentralCastGate() {
        boolean statusBlocked = StatusRegistryTestSupport.blocksTechnique("seal_nascent");

        TechniqueGateService.GateResult result = TechniqueGateService.canCast(
                null, new PlayerCultivation(), plainTechnique(), statusBlocked);

        assertFalse(result.allowed());
        assertEquals("message.seeking_immortals.technique_gate.status_blocked", result.messageKey());
    }

    @Test
    void unblockedStatusContinuesThroughNormalCastChecks() {
        TechniqueGateService.GateResult result = TechniqueGateService.canCast(
                null, new PlayerCultivation(), plainTechnique(), false);

        assertTrue(result.allowed());
    }

    private static TechniqueDataManager.TechniqueEntry plainTechnique() {
        return new TechniqueDataManager.TechniqueEntry(
                "status_gate_test",
                "Status Gate Test",
                "",
                "",
                1,
                0,
                Realm.MORTAL);
    }
}
