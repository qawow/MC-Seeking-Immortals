package com.xunxian.seekingimmortals.region;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyEventSchedulerTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals",
            "region", "DailyEventScheduler.java");

    @Test
    void disablingEventsForcesTheNextEnabledTickToRevisitTheDay() throws Exception {
        String method = methodSource(Files.readString(SOURCE),
                "public static void serverTick(MinecraftServer server)");
        int disabledBranch = method.indexOf("if (!RegionEventConfig.isDailyEventsEnabled())");
        assertTrue(disabledBranch >= 0, "missing disabled scheduler branch");
        int branchEnd = blockEnd(method, method.indexOf('{', disabledBranch));
        String branch = method.substring(disabledBranch, branchEnd + 1);
        assertTrue(branch.contains("lastServerDay = Long.MIN_VALUE"),
                "disabled scheduling must invalidate the day gate");
    }

    @Test
    void playerRefreshIsNotLimitedToAChangedRoll() throws Exception {
        String method = methodSource(Files.readString(SOURCE),
                "public static void rollAllRegions(ServerLevel overworld, boolean notifyPlayers)");
        int changed = method.indexOf("if (changed)");
        int changedEnd = blockEnd(method, method.indexOf('{', changed));
        int notify = method.indexOf("if (notifyPlayers)", changedEnd);
        assertTrue(changed >= 0 && notify > changedEnd,
                "player notification must run after the change hook block");
        String changedBlock = method.substring(changed, changedEnd + 1);
        String notifyBlock = method.substring(notify, blockEnd(method, method.indexOf('{', notify)) + 1);
        assertTrue(changedBlock.contains("onDailyEvent(region.id(), roll.eventId())"),
                "event hooks must remain tied to a changed roll");
        assertFalse(changedBlock.contains("if (notifyPlayers)"),
                "player refresh must not be nested under changed");
        assertTrue(notifyBlock.contains("DailyEventEffectExecutor.apply(")
                        && notifyBlock.contains("roll.untilTick(), changed"),
                "refresh must reapply authored effects");
        assertTrue(notifyBlock.contains("DailyEventEncounterService.maybeSpawn"),
                "refresh must retain encounter synchronization");
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        int end = blockEnd(source, openingBrace);
        return source.substring(start, end + 1);
    }

    private static int blockEnd(String source, int openingBrace) {
        assertTrue(openingBrace >= 0, "missing opening brace");
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return index;
            }
        }
        throw new AssertionError("unterminated source block");
    }
}
