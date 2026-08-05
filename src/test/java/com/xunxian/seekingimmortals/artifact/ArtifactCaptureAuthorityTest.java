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
        // Y-B: capturable() keeps the true-spirit/companion exclusions while also honouring the
        // authored capture_only right (see BeastBestiaryService.BeastEntry.capturable).
        assertTrue(gate.contains(".filter(BeastBestiaryService.BeastEntry::capturable)"));
        assertFalse(gate.contains("instanceofMonster"),
                "ordinary monsters must never become capture candidates");
    }

    @Test
    void captureOnlyBeastsStayCapturableButNeverBecomeCompanions() throws Exception {
        // 阴芝马 is authored `tameable: "capture_only"`: legal to take alive, never a pet.
        BeastBestiaryService.BeastEntry horse =
                BeastBestiaryService.find("yinyang_yinzhima").orElseThrow();
        assertTrue(horse.captureOnly(), "authored capture_only must be parsed");
        assertTrue(horse.capturable(), "capture_only beasts must be legal capture targets");
        assertFalse(horse.tameable(), "capture_only must never grant pet/contract rights");
        assertTrue(BeastBestiaryService.isCapturable("yinyang_yinzhima"));
        assertTrue(BeastBestiaryService.isCaptureOnlyBeast("yinyang_yinzhima"));

        // The exclusions still hold for protected beasts.
        BeastBestiaryService.all().values().stream()
                .filter(entry -> entry.trueSpirit() || entry.companionOnly())
                .forEach(entry -> assertFalse(entry.capturable(),
                        "true spirits/companions must never be capturable: " + entry.id()));
    }

    @Test
    void sessionBoundLayerBeastsStayCapturableOnlyWhenAuthoredCaptureOnly() throws Exception {
        // Y-A-2 tags layer-roster mobs as trial mobs, which would otherwise make the capture
        // objective unreachable. The exemption must be narrow: authored capture_only only.
        String gate = compact(methodSource(Files.readString(SOURCE), "static boolean isCapturableTarget("));
        assertTrue(gate.contains("SecretRealmTrialService.isTrialMob(mob)")
                        && gate.contains("!BeastBestiaryService.isCaptureOnlyBeast(beastIdOf(living))"),
                "trial mobs stay excluded unless the author marked them capture_only");
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
                .filter(BeastBestiaryService.BeastEntry::capturable)
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
