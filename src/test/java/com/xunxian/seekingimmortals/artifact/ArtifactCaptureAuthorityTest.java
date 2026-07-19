package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.worldpack.BeastSpawnTableService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactCaptureAuthorityTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals",
            "artifact", "ArtifactCaptureService.java");

    @Test
    void captureTargetsOnlyTameableEcologyBeastsAndRejectsProtectedEntities() throws Exception {
        String source = Files.readString(SOURCE);
        String gate = compact(methodSource(source, "static boolean isCapturableTarget("));

        assertTrue(gate.contains("getBoolean(\"seeking_immortals_ecology_beast\")"));
        assertTrue(gate.contains("BossEncounterService.isBossMob(mob)"));
        assertTrue(gate.contains("SecretRealmTrialService.isTrialMob(mob)"));
        assertTrue(gate.contains("instanceofnet.minecraft.world.entity.player.Player"));
        assertTrue(gate.contains("instanceofnet.minecraft.world.entity.npc.AbstractVillager"));
        assertTrue(gate.contains("!servitor.isHostileTrial()"));
        assertTrue(gate.contains(".filter(BeastBestiaryService.BeastEntry::tameable)"));
        assertTrue(gate.contains("!entry.trueSpirit()&&!entry.companionOnly()"));
        assertFalse(gate.contains("instanceofMonster"),
                "ordinary monsters must never become capture candidates");
    }

    @Test
    void captureWeakensBeforeMutatingJarOrRemovingEntity() throws Exception {
        String source = Files.readString(SOURCE);
        String capture = compact(methodSource(source,
                "public static boolean releaseOrCapture(ServerPlayer player, ItemStack jar, boolean sneakSeal)"));

        int weakened = capture.indexOf("if(!isWeakened(best))");
        int writeId = capture.indexOf("putString(TAG,id)");
        int discard = capture.indexOf("best.discard()");
        assertTrue(weakened >= 0 && writeId > weakened && discard > writeId,
                "health and authority gates must pass before jar mutation and entity removal");
    }

    @Test
    void shippedEcologyTablesStillContainLegalCaptureTargets() {
        long capturable = BeastSpawnTableService.tables().stream()
                .flatMap(table -> table.weights().stream())
                .map(BeastSpawnTableService.Weight::beastId)
                .distinct()
                .map(BeastBestiaryService::find)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::orElseThrow)
                .filter(BeastBestiaryService.BeastEntry::tameable)
                .filter(entry -> !entry.trueSpirit() && !entry.companionOnly())
                .count();

        assertTrue(capturable > 0, "capture hardening must not eliminate every shipped ecology target");
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        int closingBrace = matchingDelimiter(source, openingBrace);
        return source.substring(start, closingBrace + 1);
    }

    private static int matchingDelimiter(String source, int openingBrace) {
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return index;
            }
        }
        throw new AssertionError("unterminated method body");
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
