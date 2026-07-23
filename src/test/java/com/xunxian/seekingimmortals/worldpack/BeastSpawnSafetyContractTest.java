package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeastSpawnSafetyContractTest {
    private static final Path JAVA = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void hostileEntryPointsRejectPeacefulBeforeSpawningOrLatching() throws Exception {
        String bossService = Files.readString(JAVA.resolve("beast/BeastBossService.java"));
        String trialService = Files.readString(JAVA.resolve("worldpack/TrialCombatShellService.java"));
        String encounterService = Files.readString(JAVA.resolve("worldpack/BossEncounterService.java"));
        String ecologyService = Files.readString(JAVA.resolve("worldpack/BeastSpawnTableService.java"));

        String boss = methodSource(bossService, "public static Mob spawnBoss(");
        assertBefore(boss, "level.getDifficulty() == Difficulty.PEACEFUL",
                "ModEntities.CULTIVATION_BEAST.get().create(level)");

        String trial = methodSource(trialService, "public static Mob spawnHostile(");
        assertBefore(trial, "level.getDifficulty() == Difficulty.PEACEFUL",
                "spawnDedicatedBeast(");

        String encounter = methodSource(encounterService, "public static boolean spawnIfNeeded(");
        assertBefore(encounter, "level.getDifficulty() == Difficulty.PEACEFUL",
                "player.getPersistentData().putBoolean(key, true)");

        String ecology = methodSource(ecologyService, "private static int spawnNearPlayer(");
        assertBefore(ecology, "level.getDifficulty() == Difficulty.PEACEFUL",
                "Optional<Weight> roll = roll(");
    }

    @Test
    void bossAndTrialEntitiesRequireCollisionFreeGroundOrWaterBeforeWorldInsertion() throws Exception {
        String bossService = Files.readString(JAVA.resolve("beast/BeastBossService.java"));
        String trialService = Files.readString(JAVA.resolve("worldpack/TrialCombatShellService.java"));

        String bossSpawn = methodSource(bossService, "public static Mob spawnBoss(");
        assertBefore(bossSpawn, "boss.configureBoss(", "positionForSpawn(");
        assertBefore(bossSpawn, "positionForSpawn(", "level.addFreshEntity(boss)");
        assertTrue(bossSpawn.contains("return level.addFreshEntity(boss) ? boss : null"));
        assertSafePositionSearch(methodSource(bossService, "private static boolean positionForSpawn("));

        String trialSpawn = methodSource(trialService, "public static Mob spawnHostile(");
        assertBefore(trialSpawn, "shell.configureHostileTrial(", "positionForSpawn(");
        assertBefore(trialSpawn, "positionForSpawn(", "level.addFreshEntity(shell)");
        assertTrue(trialSpawn.contains("return level.addFreshEntity(shell) ? shell : null"));

        String dedicated = methodSource(trialService,
                "private static CultivationBeastEntity spawnDedicatedBeast(");
        assertBefore(dedicated, "beast.configureWild(", "positionForSpawn(");
        assertBefore(dedicated, "positionForSpawn(", "level.addFreshEntity(beast)");
        assertTrue(dedicated.contains("return level.addFreshEntity(beast) ? beast : null"));
        assertSafePositionSearch(methodSource(trialService, "private static boolean positionForSpawn("));
    }

    @Test
    void failedTrialSpawnsDoNotConsumeOneTimeEncounterLatches() throws Exception {
        String trials = Files.readString(JAVA.resolve("worldpack/SecretRealmTrialService.java"));
        String patrol = methodSource(trials, "private static void spawnMidPatrol(");
        String guardian = methodSource(trials, "private static void spawnCoreEncounter(");

        assertBefore(patrol, "if (spawned <= 0)", "root.putBoolean(sessionKey, true)");
        assertBefore(guardian, "if (guardian == null)", "root.putBoolean(sessionKey, true)");
    }

    @Test
    void failedDailySpawnsRemainRetryableAndPartialSuccessesLatch() throws Exception {
        String source = Files.readString(JAVA.resolve("worldpack/DailyEventEncounterService.java"));
        String spawn = methodSource(source, "public static void maybeSpawn(");

        String exactCall = "BeastSpawnTableService.spawnNearPlayerExact(";
        int spawned = spawn.indexOf("int spawned = " + exactCall);
        int beastStart = spawn.indexOf("if (plan.kind() == Kind.BEAST)");
        int shellStart = spawn.indexOf("} else if (plan.kind() == Kind.SHELL)", beastStart);
        int vanillaStart = spawn.indexOf("} else {", shellStart);
        String beastBranch = spawn.substring(beastStart, shellStart);
        String shellBranch = spawn.substring(shellStart, vanillaStart);
        String vanillaBranch = spawn.substring(vanillaStart);
        assertTrue(spawned >= 0 && beastStart >= 0 && shellStart > beastStart && vanillaStart > shellStart);
        assertEquals(spawn.indexOf(exactCall), spawn.lastIndexOf(exactCall),
                "daily beast encounters must use one exact-size ecology request");
        assertFalse(spawn.contains("int remaining = plan.count() - spawned"));
        assertFalse(spawn.contains("if (spawned < plan.count())"),
                "a partial successful encounter must latch so retries cannot exceed the authored total");
        assertZeroFailureBeforeLatch(beastBranch);
        assertZeroFailureBeforeLatch(shellBranch);
        assertZeroFailureBeforeLatch(vanillaBranch);
        assertFalse(spawn.contains("EntityType.WOLF"));
        assertFalse(spawn.contains("EntityType.FOX"));
    }

    private static void assertZeroFailureBeforeLatch(String branch) {
        int failure = branch.indexOf("if (spawned <= 0)");
        int failureReturn = branch.indexOf("return;", failure);
        int latch = branch.indexOf("latch(player,", failureReturn);
        assertTrue(failure >= 0 && failureReturn > failure && latch > failureReturn,
                "zero successful spawns must remain retryable while partial success latches");
        assertTrue(branch.indexOf("displayName(id), spawned)", latch) > latch);
    }

    private static void assertSafePositionSearch(String source) {
        assertTrue(source.contains("radius <= 4"));
        assertTrue(source.contains("level.isInWorldBounds(feet)"));
        assertTrue(source.contains("level.getFluidState(feet)"));
        assertTrue(source.contains("level.getBlockState(feet.below()).isFaceSturdy("));
        assertTrue(source.contains("level.noCollision("));
    }

    private static void assertBefore(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0, "missing source fragment: " + first);
        assertTrue(secondIndex > firstIndex,
                "expected '" + first + "' before '" + second + "'");
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("unterminated source method: " + declaration);
    }
}
