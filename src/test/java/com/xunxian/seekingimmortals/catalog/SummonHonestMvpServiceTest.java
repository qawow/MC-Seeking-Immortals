package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonHonestMvpServiceTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals",
            "catalog", "SummonHonestMvpService.java");
    private static final Path SPELL_SOURCE = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals",
            "skill", "effect", "spell", "HonestSummonSpell.java");

    @Test
    void puppetDefinitionsIndexed() {
        assertTrue(SummonHonestMvpService.puppetDefinitionCount() >= 0);
    }

    @Test
    void shardReservationChecksTheWholeInventoryBeforeMutatingStacks() throws Exception {
        String source = Files.readString(SOURCE);
        String reserve = methodSource(source,
                "private static Optional<List<ShardReservation>> reserveShards(");

        int countAvailable = reserve.indexOf("available += stack.getCount()");
        int rejectShortfall = reserve.indexOf("if (available < required)");
        int firstMutation = reserve.indexOf("stack.shrink(take)");
        assertTrue(countAvailable >= 0);
        assertTrue(rejectShortfall > countAvailable);
        assertTrue(firstMutation > rejectShortfall,
                "inventory stacks must remain untouched until the full shard cost is available");
        assertTrue(reserve.contains("new ShardReservation(i, take)"));
        assertTrue(reserve.contains("return Optional.of(List.copyOf(reservations))"));
    }

    @Test
    void failedEntitySpawnRefundsReservationWithoutHonestMvpFallback() throws Exception {
        String source = Files.readString(SOURCE);
        String summon = methodSource(source, "public static boolean summonProxy(");
        String refund = methodSource(source,
                "private static void refundShards(ServerPlayer player");

        int reserve = summon.indexOf("reserveShards(player, shardCost)");
        int spawn = summon.indexOf("boolean spawned = spawnConfigured(");
        int failure = summon.indexOf("if (!spawned)");
        int rollback = summon.indexOf("refundShards(player, reservation)", failure);
        int failureReturn = summon.indexOf("return;", rollback);
        int successBuff = summon.indexOf("player.addEffect(", failureReturn);
        assertTrue(reserve >= 0 && spawn > reserve);
        assertTrue(failure > spawn && rollback > failure);
        assertTrue(failureReturn > rollback && successBuff > failureReturn,
                "spawn failure must refund and return before applying the success buff");
        assertFalse(summon.contains("MobEffects.DAMAGE_BOOST"));
        assertFalse(summon.contains("message.seeking_immortals.summon.honest_mvp"));

        assertTrue(refund.contains("player.getInventory().setItem(reservation.slot()"));
        assertTrue(refund.contains("stack.grow(reservation.count())"));
        assertTrue(refund.contains("player.getInventory().placeItemBackInInventory("));
    }

    @Test
    void honestSummonSpellFailsWithoutApplyingLegacyPlayerBuffFallbacks() throws Exception {
        String source = Files.readString(SPELL_SOURCE);
        String execute = methodSource(source, "public boolean execute(");

        int spawn = execute.indexOf("boolean spawned = SummonHonestMvpService.spawnConfigured(");
        int failure = execute.indexOf("if (!spawned)", spawn);
        int failureMessage = execute.indexOf("message.seeking_immortals.summon.entity_failed", failure);
        int failureReturn = execute.indexOf("return false;", failureMessage);
        int successVfx = execute.indexOf("level.sendParticles(", failureReturn);
        int successBuff = execute.indexOf("player.addEffect(", failureReturn);
        assertTrue(spawn >= 0 && failure > spawn);
        assertTrue(failureMessage > failure && failureReturn > failureMessage);
        assertTrue(successVfx > failureReturn && successBuff > failureReturn,
                "summon VFX and the brief focus buff require a real spawned entity");

        assertFalse(execute.contains("message.seeking_immortals.summon.honest_mvp"));
        assertFalse(execute.contains("message.seeking_immortals.summon.entity_pending"));
        assertFalse(execute.contains("MobEffects.DAMAGE_BOOST"));
        assertFalse(execute.contains("MobEffects.MOVEMENT_SPEED"));
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
