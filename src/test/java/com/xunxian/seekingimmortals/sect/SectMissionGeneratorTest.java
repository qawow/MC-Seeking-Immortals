package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectMissionGeneratorTest {
    @Test
    void generateProducesKnownTypes() {
        boolean saw = false;
        for (int i = 0; i < 40; i++) {
            SectMissionGenerator.Mission mission = SectMissionGenerator.generate("qinglan");
            assertNotNull(mission);
            assertNotNull(mission.type());
            assertTrue(mission.rewardContribution() > 0);
            String t = mission.type();
            assertTrue(t.equals("gather") || t.equals("kill") || t.equals("escort")
                    || t.equals("beast") || t.equals("formation"));
            saw = true;
        }
        assertTrue(saw);
    }

    @Test
    void activeGeneratedIsEmptyWithoutPlayerPayload() {
        assertTrue(SectMissionGenerator.activeGenerated(null) == null);
        assertTrue(!SectMissionGenerator.tryTurnInActive(null));
    }
}
