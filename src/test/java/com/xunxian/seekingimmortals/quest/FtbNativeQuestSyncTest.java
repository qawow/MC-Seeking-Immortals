package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    void teamWritebackRequiresExactlyOneOnlineMember() {
        assertEquals("solo", FtbNativeQuestSync.singleOnlineMember(List.of("solo")).orElseThrow());
        assertTrue(FtbNativeQuestSync.singleOnlineMember(List.<String>of()).isEmpty());
        assertTrue(FtbNativeQuestSync.singleOnlineMember(List.of("one", "two")).isEmpty());
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
    void pureGateDecisionKeepsNativeAuthorityRequirements() {
        assertEquals(FtbNativeQuestSync.GateRequirement.REJECT,
                FtbNativeQuestSync.gateRequirement(FtbNativeQuestSync.WriteAction.REJECT, Optional.empty()));
        assertEquals(FtbNativeQuestSync.GateRequirement.NONE,
                FtbNativeQuestSync.gateRequirement(FtbNativeQuestSync.WriteAction.SATISFIED, Optional.empty()));
        assertEquals(FtbNativeQuestSync.GateRequirement.BOUND_NPC,
                FtbNativeQuestSync.gateRequirement(FtbNativeQuestSync.WriteAction.START, Optional.of("ignored")));
        assertEquals(FtbNativeQuestSync.GateRequirement.REJECT,
                FtbNativeQuestSync.gateRequirement(FtbNativeQuestSync.WriteAction.ADVANCE,
                        Optional.of("kunwu_map_fragment_turnin")));
        assertEquals(FtbNativeQuestSync.GateRequirement.BOUND_NPC,
                FtbNativeQuestSync.gateRequirement(FtbNativeQuestSync.WriteAction.ADVANCE, Optional.empty()));
    }
}
