package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionBeastMigrationTest {
    private static final Path JAVA = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void knownBeastsResolveToTheDedicatedEntityButUnknownSummonsDoNot() {
        String known = BeastBestiaryService.all().keySet().stream().findFirst().orElseThrow();

        assertEquals(known, SummonHonestMvpService.resolveRealBeastId(
                null, "beast_" + known, SummonedServitorEntity.Archetype.BEAST).orElseThrow());
        assertTrue(SummonHonestMvpService.resolveRealBeastId(
                null, "beast___not_a_real_beast__", SummonedServitorEntity.Archetype.BEAST).isEmpty());
        assertTrue(SummonHonestMvpService.resolveRealBeastId(
                null, known, SummonedServitorEntity.Archetype.GENERIC).isEmpty());
    }

    @Test
    void contractAndCaptureReleaseBypassTheLegacyBeastShell() throws Exception {
        String contract = Files.readString(JAVA.resolve("cultivation/BeastContractService.java"));
        String capture = Files.readString(JAVA.resolve("artifact/ArtifactCaptureService.java"));
        String summon = Files.readString(JAVA.resolve("catalog/SummonHonestMvpService.java"));

        String contractSummon = methodSource(contract, "public static boolean summon(ServerPlayer player, String beastId)");
        String captureRelease = methodSource(capture, "private static boolean releaseBeast(");
        String dedicatedSpawn = methodSource(summon, "private static boolean spawnCompanionBeast(");
        assertTrue(contractSummon.contains("spawnBeastConfigured("));
        assertFalse(contractSummon.contains("spawnConfigured("));
        assertTrue(captureRelease.contains("spawnBeastConfigured("));
        assertFalse(captureRelease.contains("spawnConfigured("));
        assertTrue(dedicatedSpawn.contains("ModEntities.CULTIVATION_BEAST.get().create(level)"));
        assertFalse(dedicatedSpawn.contains("ModEntities.SUMMONED_SERVITOR"));
    }

    @Test
    void dedicatedCompanionRetainsAuthorityAndLifecycleHooks() throws Exception {
        String beast = Files.readString(JAVA.resolve("entity/CultivationBeastEntity.java"));

        assertTrue(beast.contains("public void configureCompanion(ServerPlayer owner"));
        assertTrue(beast.contains("private UUID ownerUUID"));
        assertTrue(beast.contains("ServitorRegistrySavedData.get(serverLevel).register("));
        assertTrue(beast.contains("public void setStance(SummonedServitorEntity.Stance next)"));
        assertTrue(beast.contains("isFriendlyEntity(source.getEntity())"));
        assertTrue(beast.contains("BeastContractService.recordCombatCredit(serverPlayer, getBeastId(), kind)"));
        assertTrue(beast.contains("tag.putInt(TAG_LIFE, lifeTicks)"));
        assertTrue(beast.contains("public boolean removeWhenFarAway"));
    }

    @Test
    void dedicatedCompanionsReceiveCraftGateReinforcement() throws Exception {
        String craftGate = Files.readString(JAVA.resolve(
                "skill/effect/spell/CraftGateTechniqueSpell.java"));

        assertTrue(craftGate.contains("getEntitiesOfClass(CultivationBeastEntity.class"));
        assertTrue(craftGate.contains("entity.isCompanion()"));
        assertTrue(craftGate.contains("MobEffects.DAMAGE_RESISTANCE"));
        assertTrue(craftGate.contains("MobEffects.REGENERATION"));
    }

    @Test
    void aquaticCompanionsPreferNearbyWaterWithCollisionCheckedLandFallback() throws Exception {
        String summon = Files.readString(JAVA.resolve("catalog/SummonHonestMvpService.java"));
        String spawn = methodSource(summon, "private static boolean spawnCompanionBeast(");
        String servitorSpawn = methodSource(summon, "private static boolean spawnServitor(");
        String preferred = methodSource(summon, "private static Vec3 preferredCompanionBeastSpawn(");

        assertTrue(spawn.indexOf("configureCompanion(") < spawn.indexOf("getBodyPlan()"));
        assertTrue(spawn.contains("level.noCollision(beast)"));
        assertTrue(spawn.indexOf("level.noCollision(beast)") < spawn.indexOf("enforceConcurrentCap(player)"));
        assertTrue(spawn.indexOf("level.addFreshEntity(beast)") < spawn.indexOf("enforceConcurrentCap(player)"));
        assertTrue(spawn.indexOf("if (added)") < spawn.indexOf("enforceConcurrentCap(player)"));
        assertTrue(servitorSpawn.indexOf("level.addFreshEntity(servitor)")
                < servitorSpawn.indexOf("enforceConcurrentCap(player)"));
        assertTrue(servitorSpawn.indexOf("if (added)")
                < servitorSpawn.indexOf("enforceConcurrentCap(player)"));
        assertTrue(preferred.contains("bodyPlan == CultivationBeastEntity.BodyPlan.AQUATIC"));
        assertTrue(preferred.contains("FluidTags.WATER"));
        assertTrue(preferred.contains("return forwardSpawn(player)"));
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
