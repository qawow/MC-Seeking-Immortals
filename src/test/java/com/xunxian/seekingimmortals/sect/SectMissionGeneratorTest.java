package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void escortServiceHasNoGeneratorCallbackAndReturnsArrivalResult() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "sect", "EscortMissionService.java");
        String source = Files.readString(sourcePath);
        assertTrue(source.contains("public static boolean onStewardContact"));
        assertTrue(source.contains("escort.distanceToSqr(steward)"));
        assertFalse(source.contains("SectMissionGenerator."));
    }

    @Test
    void generatorMarksEscortOnlyAfterAuthoritySuccess() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "sect", "SectMissionGenerator.java");
        String source = Files.readString(sourcePath);
        int start = source.indexOf("public static void onStewardEscortMark");
        int end = source.indexOf("public static boolean turnIn", start);
        String method = source.substring(start, end);
        int authority = method.indexOf("EscortMissionService.onStewardContact(player, steward)");
        int complete = method.indexOf("root.putBoolean(\"escort\", true)");
        assertTrue(authority >= 0);
        assertTrue(complete > authority);
        assertFalse(method.contains("EscortMissionService.isActive(player)"));
    }

    @Test
    void generatedEscortIsPersistedOnlyAfterSpawnSuccess() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "sect", "SectMissionGenerator.java");
        String source = Files.readString(sourcePath);
        int start = source.indexOf("public static boolean acceptGenerated");
        int end = source.indexOf("public static Mission activeGenerated", start);
        String method = source.substring(start, end);
        int spawn = method.indexOf("EscortMissionService.startEscort(player)");
        int persist = method.indexOf("player.getPersistentData().put(ACTIVE_ROOT, tag)");
        assertTrue(spawn >= 0);
        assertTrue(persist > spawn);
    }
}
