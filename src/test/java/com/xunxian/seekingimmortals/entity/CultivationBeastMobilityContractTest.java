package com.xunxian.seekingimmortals.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationBeastMobilityContractTest {
    private static final Path JAVA = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void naturalPlacementAcceptsGroundOrWaterWithoutGroundOnlyRegistration() throws Exception {
        String registry = Files.readString(JAVA.resolve("registry/ModEntities.java"));
        String beast = Files.readString(JAVA.resolve("entity/CultivationBeastEntity.java"));
        String registration = methodSource(registry, "private static void onSpawnPlacementRegister(");
        String placement = methodSource(beast, "public static boolean checkSpawnRules(");

        assertTrue(registration.contains("SpawnPlacements.Type.NO_RESTRICTIONS"));
        assertFalse(registration.contains("SpawnPlacements.Type.ON_GROUND"));
        assertTrue(placement.contains("Difficulty.PEACEFUL"));
        assertTrue(placement.contains("Monster.isDarkEnoughToSpawn"));
        assertTrue(placement.contains("FluidTags.WATER"));
        assertTrue(placement.contains("isValidSpawn(level, pos.below(), type)"));
    }

    @Test
    void aquaticAndAvianPlansHaveDedicatedMovementPaths() throws Exception {
        String beast = Files.readString(JAVA.resolve("entity/CultivationBeastEntity.java"));

        assertTrue(beast.contains("new TryFindWaterGoal(this)"));
        assertTrue(beast.contains("getBodyPlan() == BodyPlan.AQUATIC && !isInWaterOrBubble()"));
        assertTrue(beast.contains("new FlyingMoveControl(this, 20, true)"));
        assertTrue(beast.contains("new FlyingPathNavigation(this, level())"));
        assertTrue(beast.contains("new WaterAvoidingRandomFlyingGoal(this, 1.05D)"));
        assertTrue(beast.contains("getBodyPlan() == BodyPlan.AVIAN && super.canUse()"));
    }

    @Test
    void allBodyPlansHaveDistinctStableDimensionsAndRefreshAfterConfiguration() throws Exception {
        String beast = Files.readString(JAVA.resolve("entity/CultivationBeastEntity.java"));
        String dimensions = methodSource(beast, "public static EntityDimensions dimensionsFor(");
        for (String mapping : List.of(
                "case QUADRUPED -> EntityDimensions.scalable(1.00F, 1.25F)",
                "case SERPENT -> EntityDimensions.scalable(0.80F, 0.60F)",
                "case INSECT -> EntityDimensions.scalable(0.72F, 0.55F)",
                "case AVIAN -> EntityDimensions.scalable(0.95F, 0.80F)",
                "case AQUATIC -> EntityDimensions.scalable(1.15F, 0.65F)",
                "case HUMANOID -> EntityDimensions.scalable(0.70F, 1.85F)")) {
            assertTrue(dimensions.contains(mapping), mapping);
        }
        assertEquals(6, dimensions.lines().filter(line -> line.trim().startsWith("case ")).count());

        String configure = methodSource(beast, "private void configure(String beastId");
        assertTrue(configure.contains("entityData.set(DATA_BODY_PLAN, bodyPlan.ordinal())"));
        assertTrue(configure.contains("applyBodyPlanMobility(bodyPlan)"));
        assertTrue(configure.contains("refreshDimensions()"));
    }

    @Test
    void companionsBossesAndTrialMobsSurvivePeacefulCleanup() throws Exception {
        String beast = Files.readString(JAVA.resolve("entity/CultivationBeastEntity.java"));
        String configure = methodSource(beast, "private void configure(String beastId");
        String companion = methodSource(beast, "public void configureCompanion(");
        String peaceful = methodSource(beast, "protected boolean shouldDespawnInPeaceful()");

        assertTrue(configure.contains("entityData.set(DATA_COMPANION, false)"));
        assertTrue(companion.contains("entityData.set(DATA_COMPANION, true)"));
        assertTrue(peaceful.contains("return !isCompanion()"));
        assertTrue(peaceful.contains("&& !isCatalogBoss()"));
        assertTrue(peaceful.contains("&& !BossEncounterService.isBossMob(this)"));
        assertTrue(peaceful.contains("&& !TrialCombatShellService.isHostileShell(this)"));
        assertTrue(peaceful.contains("&& !SecretRealmTrialService.isTrialMob(this)"));
    }

    @Test
    void savedCombatStateAndNamesAreRestoredForWildBossAndTrialVariants() throws Exception {
        String beast = Files.readString(JAVA.resolve("entity/CultivationBeastEntity.java"));
        String save = methodSource(beast, "public void addAdditionalSaveData(");
        String read = methodSource(beast, "public void readAdditionalSaveData(");

        assertTrue(save.contains("tag.putDouble(TAG_CONFIGURED_HEALTH"));
        assertTrue(save.contains("tag.putDouble(TAG_CONFIGURED_DAMAGE"));
        assertFalse(save.contains("if (isCompanion()) {\n            tag.putDouble(TAG_CONFIGURED_HEALTH"));
        assertTrue(read.indexOf("if (tag.contains(TAG_CONFIGURED_HEALTH))")
                < read.indexOf("if (companion)"));
        assertTrue(read.contains("setHealth(Mth.clamp(savedHealth"));
        assertTrue(read.contains("setCustomName(savedCustomName)"));
        assertTrue(read.contains("savedCustomNameVisible || companion || boss"));
    }

    @Test
    void commandSpawnPrefersWaterForAquaticBodiesWithLandFallback() throws Exception {
        String spawn = Files.readString(JAVA.resolve("worldpack/BeastSpawnTableService.java"));
        String route = methodSource(spawn, "private static BlockPos findSpawnPosition(");
        String create = methodSource(spawn, "private static boolean spawnWildBeast(");

        assertTrue(create.indexOf("entity.configureWild(") < create.indexOf("findSpawnPosition("));
        assertTrue(route.contains("bodyPlan == CultivationBeastEntity.BodyPlan.AQUATIC"));
        assertTrue(route.contains("findWaterSpawnPosition("));
        assertTrue(route.contains("return findAmphibiousLandSpawnPosition("));
        assertTrue(spawn.contains("level.getFluidState(water).is(FluidTags.WATER)"));
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
        throw new AssertionError("unterminated method body");
    }
}
