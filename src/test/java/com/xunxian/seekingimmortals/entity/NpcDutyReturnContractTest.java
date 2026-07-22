package com.xunxian.seekingimmortals.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcDutyReturnContractTest {
    private static final Path ENTITY_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals", "entity");

    @Test
    void dutyNpcsThrottleReturnPathsAndRespectActiveGoals() throws Exception {
        String trader = assertReturnPathContract("MarketTraderEntity.java", "returnDistanceSqr");
        String steward = assertReturnPathContract("SectStewardEntity.java", "returnDistanceSqr");
        assertReturnPathContract("QuestNpcEntity.java", "100.0D");
        assertReturnPathContract("SpiritStoneBankerEntity.java", "36.0D");

        assertTrue(trader.contains("doublereturnDistanceSqr=isTradingHours()?36.0D:4.0D"));
        assertTrue(steward.contains("doublereturnDistanceSqr=isOnDuty()?64.0D:4.0D"));
    }

    private static String assertReturnPathContract(String file, String distanceGate) throws Exception {
        String source = compact(methodSource(Files.readString(ENTITY_ROOT.resolve(file)), "public void tick()"));
        assertTrue(source.contains("dist>" + distanceGate + "&&tickCount%20==0&&getNavigation().isDone()"));
        assertTrue(source.indexOf("getNavigation().isDone()") < source.indexOf("getNavigation().moveTo("));
        return source;
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
        throw new AssertionError("unterminated method body: " + declaration);
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
