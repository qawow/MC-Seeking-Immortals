package com.xunxian.seekingimmortals.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCloneWiringTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void cloneTransfersAuthorityInOrderAndAlwaysInvalidatesOriginalCapabilities() throws IOException {
        String source = readSource("event", "ModEvents.java");
        String method = compact(methodSource(source, "public static void onPlayerClone("));

        int revive = method.indexOf("event.getOriginal().reviveCaps();");
        int tryStart = method.indexOf("try{", revive);
        int preservedMove = method.indexOf(
                "PlayerPersistentDataClonePolicy.moveExtremePreserved(originalData,clonedData)", tryStart);
        int capabilityCopy = method.indexOf("newData.loadNBTData(oldData.saveNBTData())", preservedMove);
        int durableCopy = method.indexOf(
                "PlayerPersistentDataClonePolicy.copyDurableData(originalData,clonedData)", capabilityCopy);
        int escortGuard = method.indexOf(
                "SectMissionGenerator.hasActiveEscortMission(originalPlayer)", durableCopy);
        int escortClear = method.indexOf(
                "EscortMissionService.clearEscort(originalPlayer,true)", escortGuard);
        int finallyStart = method.indexOf("}finally{", escortClear);
        int invalidate = method.indexOf("event.getOriginal().invalidateCaps();", finallyStart);

        assertTrue(revive >= 0, "clone must revive the original capabilities");
        assertTrue(tryStart > revive, "clone transfer must start in a try block after reviveCaps");
        assertTrue(preservedMove > tryStart, "the isolated inventory payload must move first");
        assertTrue(capabilityCopy > preservedMove, "capability data must follow the payload move");
        assertTrue(durableCopy > capabilityCopy, "durable PersistentData must follow capability copy");
        assertTrue(escortGuard > durableCopy && escortClear > escortGuard,
                "the original active escort must be cleared after persistent transfer");
        assertTrue(finallyStart > escortClear, "all clone transfers must complete before finally");
        assertTrue(invalidate > finallyStart, "original capabilities must be invalidated in finally");
    }

    @Test
    void respawnRestoresPreservedItemsThenEscortBeforeClientSync() throws IOException {
        String source = readSource("event", "ModEvents.java");
        String method = compact(methodSource(source, "public static void onPlayerRespawn("));

        int restore = method.indexOf("BreakthroughService.restorePreservedOnRespawn(player)");
        int restartEscort = method.indexOf("SectMissionGenerator.restartEscortAfterRespawn(player)");
        int sync = method.indexOf("syncClientMirrors(player,cultivation)");

        assertTrue(restore >= 0, "respawn must restore extreme preserved inventory");
        assertTrue(restartEscort > restore, "escort restart must follow preserved inventory restoration");
        assertTrue(sync > restartEscort, "client mirrors must sync after escort restart");
    }

    @Test
    void preservedInventoryUsesConsumeOncePolicyAndSharedDelivery() throws IOException {
        String source = readSource("cultivation", "BreakthroughService.java");
        String method = compact(methodSource(source, "public static void restorePreservedOnRespawn("));

        int take = method.indexOf(
                "PlayerPersistentDataClonePolicy.takeExtremePreserved(player.getPersistentData())");
        int deliver = method.indexOf("InventoryDeliveryService.giveOrDrop(player,stack)", take);

        assertTrue(take >= 0, "respawn restoration must consume the preserved payload through clone policy");
        assertTrue(deliver > take, "each consumed preserved stack must use shared inventory delivery");
        assertFalse(method.contains("player.drop("), "restoration must not directly drop stacks");
        assertFalse(method.contains("player.getInventory().add("),
                "restoration must not directly add stacks to player inventory");
    }

    @Test
    void cancelledExtremeDeathRollsBackAndCannotOverwriteStalePayload() throws IOException {
        String source = readSource("cultivation", "BreakthroughService.java");
        String effect = compact(methodSource(source, "private static void applyQiDeviationEffect("));
        String forceDeath = compact(methodSource(source, "private static void forceExtremeDeath("));
        String committed = compact(methodSource(source, "public static void markExtremeDeathCommitted("));

        int extreme = effect.indexOf("caseEXTREME->");
        int staleRollback = effect.indexOf("restorePreservedOnRespawn(player)", extreme);
        int preserve = effect.indexOf("preserveHalfInventory(player,random)", staleRollback);
        int force = effect.indexOf("forceExtremeDeath(player)", preserve);

        assertTrue(extreme >= 0 && staleRollback > extreme && preserve > staleRollback,
                "a stale extreme payload must be restored before a new half is selected");
        assertTrue(force > preserve, "the selected half must enter the tracked death transaction");
        assertTrue(forceDeath.contains("ACTIVE_EXTREME_DEATHS.add(playerId)"));
        assertTrue(forceDeath.contains("ACTIVE_EXTREME_DEATHS.remove(playerId)"));
        assertTrue(forceDeath.contains("COMMITTED_EXTREME_DEATHS.remove(playerId)"));
        assertTrue(forceDeath.contains("if(!deathCommitted&&player.getPersistentData().contains(PRESERVED_KEY))"));
        assertTrue(forceDeath.contains("player.setHealth(1.0F)"));
        assertTrue(forceDeath.contains("restorePreservedOnRespawn(player)"));
        assertTrue(committed.contains("COMMITTED_EXTREME_DEATHS.add(player.getUUID())"));
    }

    @Test
    void deathSideEffectsRunOnlyAfterCommittedDrops() throws IOException {
        String rawSource = readSource("event", "ModEvents.java");
        String source = compact(rawSource);
        String method = compact(methodSource(rawSource, "public static void onLivingDrops("));
        String effects = compact(methodSource(rawSource, "private static void handleCommittedLivingDrops("));
        String questHooks = compact(readSource("quest", "QuestHookRuntime.java"));
        int handler = source.indexOf(
                "@SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)publicstaticvoidonLivingDrops(");
        int handle = method.indexOf("handleCommittedLivingDrops(event)");
        int caught = method.indexOf("catch(RuntimeExceptionexception)", handle);
        int committed = method.indexOf("BreakthroughService.markExtremeDeathCommitted(player)", caught);

        assertTrue(handler >= 0 && handle >= 0 && caught > handle && committed > caught,
                "death side effects must be contained before the transaction commit marker");
        assertTrue(effects.contains("SectWarService.onKill(killer,victim)"));
        assertTrue(questHooks.contains(
                "@SubscribeEvent(receiveCanceled=true)publicstaticvoidonLivingDrops(LivingDropsEventevent)"));
        assertTrue(questHooks.contains("catch(RuntimeExceptionexception)"));
        assertFalse(source.contains("LivingDeathEvent"));
        assertFalse(questHooks.contains("LivingDeathEvent"));
    }

    @Test
    void extremeIsolationDoesNotDependOnMutableInventoryRules() throws IOException {
        String breakthrough = readSource("cultivation", "BreakthroughService.java");
        String policy = readSource("persistence", "PlayerPersistentDataClonePolicy.java");
        String attempt = compact(methodSource(breakthrough, "public static void attempt("));
        int spectatorGate = attempt.indexOf("if(player.isSpectator())");
        int capability = attempt.indexOf("CultivationHelper.get(player)");

        assertFalse(breakthrough.contains("GameRules.RULE_KEEPINVENTORY"));
        assertFalse(policy.contains("EXTREME_RESTORE_ON_RESPAWN"));
        assertTrue(spectatorGate >= 0 && capability > spectatorGate);
    }

    @Test
    void escortRespawnPreservesCompletionAndFailsClosedWhenRestartFails() throws IOException {
        String source = readSource("sect", "SectMissionGenerator.java");
        String method = compact(methodSource(source, "public static void restartEscortAfterRespawn("));
        String clearGenerated = compact(methodSource(source, "public static void clearGenerated("));

        int completedGuard = method.indexOf("if(root.getBoolean(\"escort\")){");
        int completedReturn = method.indexOf("return;", completedGuard);
        int activeGuard = method.indexOf("EscortMissionService.hasLoadedActiveEscort(player)", completedReturn);
        int clearEvidence = method.indexOf("root.putBoolean(\"escort\",false)", activeGuard);
        int markPending = method.indexOf("root.putBoolean(ESCORT_RESTART_PENDING,true)", clearEvidence);
        int persistEvidence = method.indexOf(
                "player.getPersistentData().put(PROGRESS_ROOT,root)", markPending);
        int retryCall = method.indexOf("retryPendingEscort(player)", persistEvidence);
        String retry = compact(methodSource(source, "public static void retryPendingEscort("));
        int restart = retry.indexOf("if(EscortMissionService.startEscort(player,false)){");
        int scheduleRetry = retry.indexOf(
                "root.putLong(ESCORT_RESTART_AT,now+ESCORT_RESTART_DELAY_TICKS)", restart);

        assertTrue(completedGuard >= 0 && completedReturn > completedGuard,
                "completed escort evidence must survive respawn for turn-in");
        assertTrue(activeGuard > completedReturn && clearEvidence > activeGuard
                        && markPending > clearEvidence && persistEvidence > markPending,
                "stale escort completion evidence must be cleared and persisted first");
        assertTrue(retryCall > persistEvidence, "respawn must immediately attempt the pending escort restart");
        assertTrue(restart >= 0 && scheduleRetry > restart,
                "failed escort restart must schedule a later retry");
        assertFalse(retry.contains("clearGenerated(player)"),
                "transient spawn failure must not delete the active generated mission");
        assertFalse(retry.contains("clearSectMission()"),
                "transient spawn failure must not delete the capability mission ledger");
        assertTrue(clearGenerated.contains("player.getPersistentData().remove(ACTIVE_ROOT)"),
                "clearGenerated must remove the active mission payload");
    }

    @Test
    void escortCleanupUsesExactDeferredDismissalAcrossDimensions() throws IOException {
        String source = compact(readSource("sect", "EscortMissionService.java"));

        assertTrue(source.contains(
                "ServitorRegistrySavedData.get(player.getServer()).dismiss(player.getUUID(),entityId)"));
        assertTrue(source.contains("for(ServerLevellevel:player.getServer().getAllLevels())"));
        assertTrue(source.contains("isOwnedEscort(player,escort)"));
        assertTrue(source.contains("registryState.dismissed()"));
        assertTrue(source.contains("publicstaticbooleantick(ServerPlayerplayer)"));
        assertTrue(source.contains("state.dimensionId()"));
    }

    private static String readSource(String directory, String fileName) throws IOException {
        return Files.readString(JAVA_ROOT.resolve(Path.of(directory, fileName)));
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        assertTrue(openingBrace >= 0, "missing method body: " + declaration);

        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("unterminated source method: " + declaration);
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
