package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncQuestTrackerPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientQuestTrackerDataTest {
    private static final String FIRST = "first_path 1/3 branch=neutral cost=spirit_stone:2 own=4 LOCK=0 REW=0";
    private static final String SECOND = "second_path 2/4 branch=righteous cost=-:0 own=0 LOCK=1 REW=0";
    private static final String AVAILABLE =
            "available_path 0/4 branch=neutral cost=-:0 own=0 LOCK=0 REW=0 STATE=AVAILABLE GATE=NONE";
    private static final String LOCKED =
            "locked_path 0/4 branch=neutral cost=-:0 own=0 LOCK=0 REW=0 STATE=LOCKED GATE=REGION";
    private static final String ACTIVE =
            "active_path 1/4 branch=neutral cost=-:0 own=0 LOCK=0 REW=0 STATE=ACTIVE GATE=NONE";

    @AfterEach
    void resetClientData() {
        ClientQuestTrackerData.reset();
    }

    @Test
    void selectedChainSurvivesRefreshById() {
        ClientQuestTrackerData.set(new SyncQuestTrackerPacket(List.of(FIRST, SECOND)));
        assertTrue(ClientQuestTrackerData.selectChain("second_path"));

        ClientQuestTrackerData.set(new SyncQuestTrackerPacket(List.of(
                "status header",
                "second_path 4/4 DONE branch=righteous cost=-:0 own=0 LOCK=1 REW=1",
                FIRST)));

        assertEquals("second_path", ClientQuestTrackerData.selectedChainId());
        assertEquals(4, ClientQuestTrackerData.selectedChain().orElseThrow().stage());
        assertTrue(ClientQuestTrackerData.selectedChain().orElseThrow().complete());
    }

    @Test
    void missingSelectionFallsBackToFirstIncompleteChain() {
        assertEquals("second_path", ClientQuestTrackerData.resolveSelectedChainId("removed_path", List.of(
                "finished_path 3/3 DONE branch=neutral cost=-:0 own=0 LOCK=0 REW=1",
                SECOND)));
    }

    @Test
    void onlyParseableServerRowsCanBeSelected() {
        ClientQuestTrackerData.set(new SyncQuestTrackerPacket(List.of("OK sync", FIRST)));

        assertFalse(ClientQuestTrackerData.selectChain("missing"));
        assertEquals("first_path", ClientQuestTrackerData.selectedChainId());
    }

    @Test
    void parsesOptionalStateAndGateWithConveniencePredicates() {
        var available = ClientQuestTrackerData.parseChainLine(AVAILABLE).orElseThrow();
        assertEquals(ClientQuestTrackerData.TrackerState.AVAILABLE, available.state());
        assertEquals(ClientQuestTrackerData.StartGate.NONE, available.gate());
        assertTrue(available.isAvailable());
        assertFalse(available.isLocked());

        var locked = ClientQuestTrackerData.parseChainLine(LOCKED).orElseThrow();
        assertEquals(ClientQuestTrackerData.StartGate.REGION, locked.gate());
        assertTrue(locked.isLocked());
        assertFalse(locked.complete());

        // Pre-state rows remain parseable and infer ACTIVE from their positive stage.
        assertTrue(ClientQuestTrackerData.parseChainLine(FIRST).orElseThrow().isActive());
    }

    @Test
    void defaultSelectionPrefersActiveThenAvailableThenOtherStates() {
        assertEquals("available_path", ClientQuestTrackerData.resolveSelectedChainId("", List.of(
                "done_path 4/4 DONE branch=neutral cost=-:0 own=0 LOCK=0 REW=1 STATE=DONE GATE=NONE",
                LOCKED,
                AVAILABLE)));
        assertEquals("active_path", ClientQuestTrackerData.resolveSelectedChainId("", List.of(
                AVAILABLE,
                ACTIVE,
                LOCKED)));
    }

    @Test
    void trackerViewSignaturePreservesScrollForSameVisibleRowsAndSelection() {
        var first = ClientQuestTrackerData.parseChainLine(FIRST).orElseThrow();
        var second = ClientQuestTrackerData.parseChainLine(SECOND).orElseThrow();
        var completedSecond = ClientQuestTrackerData.parseChainLine(
                "second_path 4/4 DONE branch=righteous cost=-:0 own=0 LOCK=1 REW=1").orElseThrow();

        QuestTrackerScreen.ViewSignature previous = QuestTrackerScreen.viewSignature(
                "all", List.of(first, second));
        QuestTrackerScreen.ViewSignature refreshed = QuestTrackerScreen.viewSignature(
                "all", List.of(first, completedSecond));
        QuestTrackerScreen.ViewSignature filtered = QuestTrackerScreen.viewSignature(
                "done", List.of(completedSecond));

        assertFalse(QuestTrackerScreen.viewChanged(previous, refreshed));
        assertTrue(QuestTrackerScreen.viewChanged(previous, filtered));
        assertFalse(QuestTrackerScreen.selectedChainChanged("second_path", "second_path"));
        assertTrue(QuestTrackerScreen.selectedChainChanged("second_path", "first_path"));
    }
}
