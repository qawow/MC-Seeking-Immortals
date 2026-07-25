package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtbNativeQuestSyncTest {
    @Test
    void parsesMirrorAndWriteTargetsWithUnderscoreChainIds() {
        assertEquals(new FtbNativeQuestSync.Target("void_palace_campaign", 6),
                FtbNativeQuestSync.parseMirrorTag("si_native_void_palace_campaign_6").orElseThrow());
        assertEquals(new FtbNativeQuestSync.Target("blood_forbidden_campaign", 1),
                FtbNativeQuestSync.parseWriteTag("si_native_write_blood_forbidden_campaign_1").orElseThrow());
        assertEquals(new FtbNativeQuestSync.Target("qixuan_mortal_path", 3),
                FtbNativeQuestSync.parseReadyTag("si_native_ready_qixuan_mortal_path_3").orElseThrow());
    }

    @Test
    void rejectsUnknownAndOutOfRangeTargets() {
        assertTrue(FtbNativeQuestSync.parseMirrorTag("si_native_missing_chain_1").isEmpty());
        assertTrue(FtbNativeQuestSync.parseMirrorTag("si_native_qixuan_mortal_path_0").isEmpty());
        assertTrue(FtbNativeQuestSync.parseMirrorTag("si_native_qixuan_mortal_path_5").isEmpty());
        assertTrue(FtbNativeQuestSync.parseMirrorTag("si_native_qixuan_mortal_path_last").isEmpty());
    }

    @Test
    void mirrorTagNeverParsesAsWriteTagOrViceVersa() {
        assertTrue(FtbNativeQuestSync.parseMirrorTag(
                "si_native_write_qixuan_mortal_path_1").isEmpty());
        assertTrue(FtbNativeQuestSync.parseMirrorTag(
                "si_native_ready_qixuan_mortal_path_1").isEmpty());
        assertTrue(FtbNativeQuestSync.parseWriteTag(
                "si_native_qixuan_mortal_path_1").isEmpty());
    }

    @Test
    void writeTargetsDeduplicateAndIgnoreOrdinaryTags() {
        assertEquals(List.of(new FtbNativeQuestSync.Target("qixuan_mortal_path", 1)),
                FtbNativeQuestSync.writeTargets(Set.of(
                        "seeking_immortals",
                        "si_native_write_qixuan_mortal_path_1",
                        "optional")));
    }

    @Test
    void writeTargetsFailClosedForMalformedOrMultipleNativeWrites() {
        assertTrue(FtbNativeQuestSync.writeTargets(Set.of(
                "si_native_write_qixuan_mortal_path_1",
                "si_native_write_missing_chain_1")).isEmpty());
        assertTrue(FtbNativeQuestSync.writeTargets(Set.of(
                "si_native_write_qixuan_mortal_path_1",
                "si_native_write_qixuan_mortal_path_2")).isEmpty());
    }

    @Test
    void writeIntentRequiresOneMatchingNativeChainTag() {
        FtbNativeQuestSync.WriteIntentValidation valid = FtbNativeQuestSync.validateWriteIntent(Set.of(
                "seeking_immortals",
                "qixuan_mortal_path",
                "si_native_write_qixuan_mortal_path_2"));
        assertTrue(valid.valid());
        assertEquals(FtbNativeQuestSync.WriteIntentStatus.VALID, valid.status());
        assertEquals(new FtbNativeQuestSync.Target("qixuan_mortal_path", 2), valid.intent().target());

        assertEquals(FtbNativeQuestSync.WriteIntentStatus.MISSING_CHAIN_TAG,
                FtbNativeQuestSync.validateWriteIntent(Set.of(
                        "si_native_write_qixuan_mortal_path_2")).status());
        assertEquals(FtbNativeQuestSync.WriteIntentStatus.CHAIN_MISMATCH,
                FtbNativeQuestSync.validateWriteIntent(Set.of(
                        "huangfeng_cultivation_path",
                        "si_native_write_qixuan_mortal_path_2")).status());
        assertEquals(FtbNativeQuestSync.WriteIntentStatus.MULTIPLE_CHAIN_TAGS,
                FtbNativeQuestSync.validateWriteIntent(Set.of(
                        "qixuan_mortal_path",
                        "huangfeng_cultivation_path",
                        "si_native_write_qixuan_mortal_path_2")).status());
    }

    @Test
    void structuredWriteIntentRejectsMissingMalformedAndMultipleTargets() {
        assertEquals(FtbNativeQuestSync.WriteIntentStatus.NO_WRITE_TAG,
                FtbNativeQuestSync.validateWriteIntent(Set.of("qixuan_mortal_path")).status());
        assertEquals(FtbNativeQuestSync.WriteIntentStatus.MALFORMED_WRITE_TAG,
                FtbNativeQuestSync.validateWriteIntent(Set.of(
                        "qixuan_mortal_path", "si_native_write_missing_chain_1")).status());
        assertEquals(FtbNativeQuestSync.WriteIntentStatus.MULTIPLE_WRITE_TARGETS,
                FtbNativeQuestSync.validateWriteIntent(Set.of(
                        "qixuan_mortal_path",
                        "si_native_write_qixuan_mortal_path_1",
                        "si_native_write_qixuan_mortal_path_2")).status());
    }

    @Test
    void teamWritebackRequiresOneFullMemberAndTheSameSoleOnlineMember() {
        UUID solo = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        assertEquals(solo, FtbNativeQuestSync.singleAuthorityMember(
                Set.of(), solo, List.of(solo)).orElseThrow(),
                "a personal team exposes its owner through the team id, not getMembers()");
        assertEquals(solo, FtbNativeQuestSync.singleAuthorityMember(
                Set.of(solo), solo, List.of(solo)).orElseThrow());
        assertTrue(FtbNativeQuestSync.singleAuthorityMember(Set.of(solo), solo, List.of()).isEmpty());
        assertTrue(FtbNativeQuestSync.singleAuthorityMember(
                Set.of(solo), solo, List.of(other)).isEmpty());
        assertTrue(FtbNativeQuestSync.singleAuthorityMember(
                Set.of(other), solo, List.of(solo)).isEmpty(),
                "an offline second team member must still reject native writeback");
        assertTrue(FtbNativeQuestSync.singleAuthorityMember(
                Set.of(solo, other), solo, List.of(solo, other)).isEmpty());
    }

    @Test
    void pureProgressCheckRequiresMatchingChainAndStage() {
        FtbNativeQuestSync.Target target = new FtbNativeQuestSync.Target("qixuan_mortal_path", 3);

        assertTrue(FtbNativeQuestSync.isSatisfied(
                new TextQuestChainService.ChainProgress("qixuan_mortal_path", 3, 4, false), target));
        assertFalse(FtbNativeQuestSync.isSatisfied(
                new TextQuestChainService.ChainProgress("qixuan_mortal_path", 2, 4, false), target));
        assertFalse(FtbNativeQuestSync.isSatisfied(
                new TextQuestChainService.ChainProgress("huangfeng_cultivation_path", 5, 5, true), target));
    }

    @Test
    void writeActionAllowsExactlyOneOrderedNativeTransition() {
        FtbNativeQuestSync.Target first = new FtbNativeQuestSync.Target("qixuan_mortal_path", 1);
        FtbNativeQuestSync.Target third = new FtbNativeQuestSync.Target("qixuan_mortal_path", 3);

        assertEquals(FtbNativeQuestSync.WriteAction.START, FtbNativeQuestSync.writeAction(
                new TextQuestChainService.ChainProgress("qixuan_mortal_path", 0, 4, false), first));
        assertEquals(FtbNativeQuestSync.WriteAction.ADVANCE, FtbNativeQuestSync.writeAction(
                new TextQuestChainService.ChainProgress("qixuan_mortal_path", 2, 4, false), third));
        assertEquals(FtbNativeQuestSync.WriteAction.SATISFIED, FtbNativeQuestSync.writeAction(
                new TextQuestChainService.ChainProgress("qixuan_mortal_path", 3, 4, false), third));
    }

    @Test
    void writeActionRejectsSkipAheadAndCrossChainTargets() {
        FtbNativeQuestSync.Target third = new FtbNativeQuestSync.Target("qixuan_mortal_path", 3);

        assertEquals(FtbNativeQuestSync.WriteAction.REJECT, FtbNativeQuestSync.writeAction(
                new TextQuestChainService.ChainProgress("qixuan_mortal_path", 0, 4, false), third));
        assertEquals(FtbNativeQuestSync.WriteAction.REJECT, FtbNativeQuestSync.writeAction(
                new TextQuestChainService.ChainProgress("qixuan_mortal_path", 1, 4, false), third));
        assertEquals(FtbNativeQuestSync.WriteAction.REJECT, FtbNativeQuestSync.writeAction(
                new TextQuestChainService.ChainProgress("huangfeng_cultivation_path", 2, 5, false), third));
    }

    @Test
    void explicitWritePathDoesNotReplayHooksOrRequireNpcProximity() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/quest/FtbNativeQuestSync.java"));
        String nativeSource = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/quest/TextQuestChainService.java"));

        assertFalse(source.contains("QuestHookRuntime"));
        assertFalse(source.contains("TextQuestNpcHookService"));
        assertTrue(source.contains("TextQuestChainService.transitionExact("));
        assertTrue(nativeSource.contains(
                "QuestAuthorityCatalog.stageGate(player, chain.id(), targetStage)"));
        assertTrue(nativeSource.contains("!= QuestAuthorityCatalog.Gate.OPEN"));
    }
}
