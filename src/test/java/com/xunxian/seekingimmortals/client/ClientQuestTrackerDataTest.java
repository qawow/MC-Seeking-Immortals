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
}
