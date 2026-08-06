package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The main story has exactly one {@code CRAFT_COMPLETED} route — {@code deity_huoyu_path} step 6,
 * "合成回阳真水" — and it was blocked twice over.
 *
 * <p>The route names {@code huiyang_true_water}, but that id has no recipe anywhere: the alchemy file
 * <em>called</em> {@code huiyang_true_water.json} outputs the graded ladder {@code
 * return_yang_true_water*} instead, so the token could never match what the player receives. And even
 * with a matching id, collecting a finished batch recorded only {@code recordAlchemyCompleted(station)}
 * — never the crafted item — so no item-bearing craft route could be satisfied by alchemy at all.</p>
 */
class MainStoryCraftProofTest {
    private static final Path FURNACE = Path.of("src", "main", "java", "com", "xunxian",
            "seekingimmortals", "block", "entity", "AlchemyFurnaceBlockEntity.java");

    @Test
    void theCraftedPillProvesTheAuthoredToken() {
        // The authored token is the ungraded name; the furnace can only ever deliver a graded pill.
        assertTrue(DetailedQuestProofService.routeItemMatches(
                        "huiyang_true_water", "return_yang_true_water"),
                "crafting 下品回阳真水 must prove deity_huoyu_path step 6, or the chain is uncompletable");
        for (String grade : new String[]{"_mid", "_high", "_supreme"}) {
            assertTrue(DetailedQuestProofService.routeItemMatches(
                            "huiyang_true_water", "return_yang_true_water" + grade),
                    "a higher grade must also prove the step: return_yang_true_water" + grade);
        }
        // Identity must keep working: the token itself is still a registered carrier.
        assertTrue(DetailedQuestProofService.itemsProvingToken("huiyang_true_water")
                        .contains("huiyang_true_water"),
                "the authored token must remain self-proving");
    }

    @Test
    void collectingAnAlchemyBatchRecordsTheCraftedItem() throws Exception {
        String source = Files.readString(FURNACE);
        // Without this the only CRAFT_COMPLETED route in the game can never fire from alchemy.
        assertTrue(source.contains("recordItemCrafted"),
                "collecting a finished batch must record the crafted item, not just the station");
        int station = source.indexOf("recordAlchemyCompleted");
        int crafted = source.indexOf("recordItemCrafted");
        assertTrue(station > 0 && crafted > 0,
                "both the station proof and the crafted-item proof must be recorded on collection");
    }
}
