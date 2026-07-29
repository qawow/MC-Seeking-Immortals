package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectHallInteractionTest {
    @Test
    void focusedCandidateViewDrivesButtonsRenderingAndScrollingFromOneList() throws Exception {
        List<ClientSectData.Candidate> candidates = List.of(
                new ClientSectData.Candidate("qinglan", "青岚", "Qinglan", "", "", true),
                new ClientSectData.Candidate("huangfeng", "黄枫", "Huangfeng", "", "", true),
                new ClientSectData.Candidate("luoyun", "落云", "Luoyun", "", "", true));
        ClientSectData.Snapshot snapshot = snapshot(false, 0, candidates, ClientSectData.Mission.empty());

        assertEquals(List.of("huangfeng"), SectHallScreen.visibleCandidates(snapshot, "huangfeng")
                .stream().map(ClientSectData.Candidate::id).toList());
        assertEquals(3, SectHallScreen.visibleCandidates(snapshot, "").size());
        assertFalse(SectHallScreen.snapshotChanged(snapshot, snapshot));
        assertTrue(SectHallScreen.snapshotChanged(snapshot,
                snapshot(true, 0, candidates, ClientSectData.Mission.empty())), "joining must rebuild actions");
        assertTrue(SectHallScreen.snapshotChanged(snapshot,
                snapshot(false, 25, candidates, ClientSectData.Mission.empty())), "contribution changes must rebuild");
        assertTrue(SectHallScreen.snapshotChanged(snapshot,
                snapshot(true, 0, candidates, new ClientSectData.Mission(
                        "mission", "title", "objective", "item", 1, 5, true, false, false))),
                "mission state changes must rebuild");

        String source = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "client", "SectHallScreen.java")).replaceAll("\\s+", "");
        assertEquals(4, occurrences(source, "visibleCandidates(data,menu.focusSectId())"));
        assertTrue(source.contains("protectedvoidcontainerTick()"));
        assertTrue(source.contains("if(snapshotChanged(observedSnapshot,snapshot)){observedSnapshot=snapshot;rebuildActionWidgets();}"));
    }

    private static ClientSectData.Snapshot snapshot(boolean member, int contribution,
                                                    List<ClientSectData.Candidate> candidates,
                                                    ClientSectData.Mission mission) {
        return new ClientSectData.Snapshot(
                member ? "qinglan" : "", "青岚", member ? "青岚" : "-", member ? "outer" : "",
                contribution, false, false, member, true, 0,
                "screen.seeking_immortals.sect.stage.locked",
                "screen.seeking_immortals.sect.objective.waiting",
                candidates, ClientSectData.DialogueNode.empty(), mission, List.of(), true);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
