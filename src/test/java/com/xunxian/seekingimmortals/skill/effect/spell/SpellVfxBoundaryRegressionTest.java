package com.xunxian.seekingimmortals.skill.effect.spell;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellVfxBoundaryRegressionTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void areaDebuffVisualUsesAuthoredRadiusWithoutReplacingAabbTargeting() throws Exception {
        String source = read("skill", "effect", "spell", "AreaDebuffSpell.java");
        int targetArea = source.indexOf("AABB area = new AABB(");
        int targetQuery = source.indexOf("level.getEntitiesOfClass(LivingEntity.class, area,");
        int capture = source.indexOf("TechniqueLifecycleVfxService.captureGeometry(");
        int captureEnd = source.indexOf(");", capture);

        assertTrue(targetArea >= 0 && targetQuery > targetArea && capture > targetQuery
                && captureEnd > capture);
        String captureCall = source.substring(capture, captureEnd);
        assertTrue(captureCall.contains("center,\n                center,\n                radius,"));
        assertFalse(source.contains("withinHorizontalRadius"));
    }

    @Test
    void failedFormationDoesNotRenderACompleteArea() throws Exception {
        String source = read("skill", "effect", "spell", "FormationSpell.java");
        int failure = source.indexOf("if (targets.isEmpty())");
        int failureReturn = source.indexOf("return false;", failure);
        assertTrue(failure >= 0 && failureReturn > failure);
        assertFalse(source.substring(failure, failureReturn).contains("form.spawnArea("));
    }

    @Test
    void servitorTerminalVisualWaitsForCommittedRemoval() throws Exception {
        String source = read("entity", "SummonedServitorEntity.java");
        int die = source.indexOf("public void die(DamageSource source)");
        int remove = source.indexOf("public void remove(RemovalReason reason)", die);
        assertTrue(die >= 0 && remove > die);
        assertFalse(source.substring(die, remove).contains("sendDissipate();"));
        assertTrue(source.contains("isAlive() && !terminalVfxSent"));
        assertTrue(source.contains("reason.shouldDestroy()"));
    }

    @Test
    void swordArraySilentCleanupCannotDissipateInTheDestinationDimension() throws Exception {
        String spell = read("skill", "effect", "spell", "MultiSwordArraySpell.java");
        String events = read("event", "ModEvents.java");
        assertTrue(spell.contains("clear(ServerPlayer player, boolean emitDissipate)"));
        assertTrue(spell.contains("active && emitDissipate"));
        assertTrue(occurrences(events, "MultiSwordArraySpell.clear(player, false)") >= 3);
        assertTrue(events.contains("MultiSwordArraySpell.clear(serverPlayer, false)"));
    }

    private static String read(String... relative) throws Exception {
        Path path = JAVA_ROOT;
        for (String part : relative) {
            path = path.resolve(part);
        }
        return Files.readString(path);
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
