package com.xunxian.seekingimmortals.quest;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Q-B-4 combat proofs: entity kills, alive captures, encounters and escorts.
 *
 * <p>No Mockito is available, so the tests exercise the extracted package-private pure mapping
 * functions plus source-level contract assertions that pin the real producer call sites.</p>
 */
class DetailedQuestCombatProofRouteTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void encounterPhaseMappingIsExactAndUnmappedRegionsStayFailClosed() {
        assertEquals(List.of("bf_water_jiao"),
                DetailedQuestProofService.encounterRegionsForPhase("blood_forbidden", "mid"));
        assertEquals(List.of("zm_candle"),
                DetailedQuestProofService.encounterRegionsForPhase("fallen_demon_valley", "core"));
        assertEquals(List.of("qz_l2"),
                DetailedQuestProofService.encounterRegionsForPhase("thousand_bamboo_puppet_tower", "mid"));
        assertEquals(List.of("qz_l3"),
                DetailedQuestProofService.encounterRegionsForPhase("thousand_bamboo_puppet_tower", "core"));
        assertEquals(List.of("yy_yezha"),
                DetailedQuestProofService.encounterRegionsForPhase("yinyang_ku", "mid"));
        assertEquals(List.of("gh_inner"),
                DetailedQuestProofService.encounterRegionsForPhase("guanghan_realm", "core"));

        assertTrue(DetailedQuestProofService.encounterRegionsForPhase("blood_forbidden", "core").isEmpty());
        assertTrue(DetailedQuestProofService.encounterRegionsForPhase("yinyang_ku", "entry").isEmpty());
        assertTrue(DetailedQuestProofService.encounterRegionsForPhase("unknown", "mid").isEmpty());
        assertTrue(DetailedQuestProofService.encounterRegionsForPhase("blood_forbidden", "").isEmpty());

        // The ordinary encounter regions have no runtime layer yet; they must not be faked
        // into any realm-phase mapping.
        Set<String> mapped = new HashSet<>();
        for (String realm : List.of("blood_forbidden", "fallen_demon_valley",
                "thousand_bamboo_puppet_tower", "yinyang_ku", "guanghan_realm")) {
            for (String phase : List.of("entry", "mid", "core")) {
                mapped.addAll(DetailedQuestProofService.encounterRegionsForPhase(realm, phase));
            }
        }
        assertFalse(mapped.contains("wuxing_shallow_trial"));
        assertFalse(mapped.contains("gray_realm_border"));
        assertFalse(mapped.contains("heifeng_sea"));
    }

    @Test
    void entityKillRoutesResolveToRealServerBossIds() {
        DetailedQuestProofCatalog.Snapshot catalog = DetailedQuestRuntimeService.proofCatalog();
        for (DetailedQuestProofCatalog.Route route : catalog.routes()) {
            if ("ENTITY_KILLED".equals(route.proofType()) || "ENTITY_CAPTURED_ALIVE".equals(route.proofType())) {
                String entity = route.parameter("entity");
                assertFalse(DetailedQuestProofService.entitiesProvingToken(entity).isEmpty(), route.eventId());
                assertTrue(DetailedQuestProofService.entitiesProvingToken(entity).contains(entity), route.eventId());
            }
        }
        assertTrue(DetailedQuestProofService.entitiesProvingToken("qianzhu_tower_lord")
                .contains("puppet_tower_lord"));
        assertTrue(DetailedQuestProofService.entityTokenMatches("qianzhu_tower_lord", "puppet_tower_lord"));
        assertTrue(DetailedQuestProofService.entityTokenMatches("yin_zhi_horse", "yin_zhi_horse"));
        assertFalse(DetailedQuestProofService.entityTokenMatches("qianzhu_tower_lord", "yin_zhi_horse"));
        assertFalse(DetailedQuestProofService.entityTokenMatches("qianzhu_tower_lord", ""));
        assertFalse(DetailedQuestProofService.entityTokenMatches("", "puppet_tower_lord"));
    }

    @Test
    void combatEventFactoriesCarryExplicitTypesAndKeys() {
        DetailedQuestProofEvent killed = DetailedQuestProofEvent.entityKilled("PUPPET_TOWER_LORD");
        assertEquals(DetailedQuestProofEvent.Type.ENTITY_KILLED, killed.type());
        assertEquals("living_kill", killed.producer());
        assertEquals("puppet_tower_lord", killed.parameter("entity"));
        assertEquals("kill:puppet_tower_lord", killed.eventKey());
        assertEquals(Map.of("entity", "puppet_tower_lord"), killed.parameters());

        DetailedQuestProofEvent captured = DetailedQuestProofEvent.entityCapturedAlive("yin_zhi_horse");
        assertEquals(DetailedQuestProofEvent.Type.ENTITY_CAPTURED_ALIVE, captured.type());
        assertEquals("capture", captured.producer());
        assertEquals("capture:yin_zhi_horse", captured.eventKey());

        DetailedQuestProofEvent encounter = DetailedQuestProofEvent.encounterCleared("qz_l2");
        assertEquals(DetailedQuestProofEvent.Type.ENCOUNTER_CLEARED, encounter.type());
        assertEquals("encounter", encounter.producer());
        assertEquals("encounter:qz_l2", encounter.eventKey());

        DetailedQuestProofEvent realmEncounter = DetailedQuestProofEvent.secretRealmEncounterCleared(
                "zm_candle", "fallen_demon_valley", "session-1", "core");
        assertEquals("fallen_demon_valley", realmEncounter.secretRealmId());
        assertEquals("session-1", realmEncounter.sessionId());
        assertEquals("core", realmEncounter.phase());

        DetailedQuestProofEvent escort = DetailedQuestProofEvent.escortCompleted("tiannan");
        assertEquals(DetailedQuestProofEvent.Type.ESCORT_COMPLETED, escort.type());
        assertEquals("escort", escort.producer());
        assertEquals("escort:tiannan", escort.eventKey());
    }

    @Test
    void everyCombatRouteHasAnExplicitProductionClassification() {
        DetailedQuestProofCatalog.Snapshot catalog = DetailedQuestRuntimeService.proofCatalog();
        Set<String> ordinaryEncounters = Set.of("wuxing_shallow_trial", "gray_realm_border", "heifeng_sea");
        for (DetailedQuestProofCatalog.Route route : catalog.routes()) {
            switch (route.proofType()) {
                case "ENTITY_KILLED", "ENTITY_CAPTURED_ALIVE" ->
                        assertFalse(DetailedQuestProofService.entitiesProvingToken(route.parameter("entity"))
                                .isEmpty(), route.eventId());
                case "ENCOUNTER_CLEARED" -> {
                    String region = route.parameter("region");
                    boolean mapped = false;
                    for (String realm : List.of("blood_forbidden", "fallen_demon_valley",
                            "thousand_bamboo_puppet_tower", "yinyang_ku", "guanghan_realm")) {
                        for (String phase : List.of("entry", "mid", "core")) {
                            if (DetailedQuestProofService.encounterRegionsForPhase(realm, phase).contains(region)) {
                                mapped = true;
                            }
                        }
                    }
                    assertTrue(mapped || ordinaryEncounters.contains(region),
                            route.eventId() + ":" + region + " has no production classification");
                }
                case "ESCORT_COMPLETED" -> assertEquals("gray_realm", route.parameter("region"),
                        route.eventId());
                default -> { }
            }
        }
    }

    @Test
    void producersAreWiredWithServerObservedFacts() throws Exception {
        String boss = compact(Files.readString(JAVA_ROOT.resolve("worldpack/BossEncounterService.java")));
        String hooks = compact(Files.readString(JAVA_ROOT.resolve("quest/QuestHookRuntime.java")));
        String capture = compact(Files.readString(JAVA_ROOT.resolve("artifact/ArtifactCaptureService.java")));
        String escort = compact(Files.readString(JAVA_ROOT.resolve("sect/EscortMissionService.java")));

        assertTrue(boss.contains("recordEntityKilled(killer,bossId)"),
                "boss kills must record the structured kill proof");
        assertTrue(boss.contains("claimEncounter(killer,bossTag)"),
                "boss kills must validate the session owner first");
        int claim = boss.indexOf("claimEncounter(killer,bossTag)");
        int killProof = boss.indexOf("recordEntityKilled(killer,bossId)");
        assertTrue(claim >= 0 && killProof > claim,
                "the kill proof must be recorded only after the owner validation");

        assertTrue(hooks.contains("recordEntityKilled(killer,typeId)"),
                "generic player kills must record the structured kill proof");
        assertTrue(hooks.contains("recordEncounterCleared(player,realm,layerKey)"),
                "realm layer clears must record the structured encounter proof");
        assertTrue(capture.contains("recordEntityCaptured(player,id)"),
                "successful captures must record the structured capture proof");
        assertTrue(escort.contains("recordEscortCompleted(player)"),
                "completed escorts must record the structured escort proof");
    }

    @Test
    void escortCompletionRequiresTheOwnedServitorAtTheSteward() throws Exception {
        String escort = compact(Files.readString(JAVA_ROOT.resolve("sect/EscortMissionService.java")));
        int ownerCheck = escort.indexOf("player.getUUID().equals(escort.getPersistentData().getUUID(ENTITY_TAG_OWNER))");
        int distance = escort.indexOf("escort.distanceToSqr(steward)");
        int proof = escort.indexOf("recordEscortCompleted(player)");
        assertTrue(ownerCheck >= 0 && distance > ownerCheck && proof > distance,
                "escort proofs must require the owned servitor and steward proximity first");
    }

    @Test
    void encounterAndKillRecordsKeepTheirContextAndReplayRoundTrips() {
        DetailedQuestProofCatalog.Route killRoute = DetailedQuestRuntimeService.proofCatalog()
                .find("qianzhu_tower_trial", 5);
        assertNotNull(killRoute);
        DetailedQuestProofEvent killed = DetailedQuestProofEvent.entityKilled("puppet_tower_lord");
        CompoundTag killEntry = DetailedQuestProofService.historyEntry(killRoute, killed);
        assertEquals("ENTITY_KILLED", killEntry.getString("Type"));
        assertEquals("puppet_tower_lord", killEntry.getString("Entity"));

        CompoundTag history = new CompoundTag();
        history.put(killRoute.eventId(), killEntry);
        DetailedQuestProofEvent replayedKill = DetailedQuestProofService.eventFromHistory(killRoute, history);
        assertNotNull(replayedKill);
        assertEquals(DetailedQuestProofEvent.Source.HISTORY, replayedKill.source());
        assertEquals("puppet_tower_lord", replayedKill.parameter("entity"));
        assertEquals("kill:puppet_tower_lord", replayedKill.eventKey());

        DetailedQuestProofCatalog.Route encounterRoute = DetailedQuestRuntimeService.proofCatalog()
                .find("qianzhu_tower_trial", 2);
        assertNotNull(encounterRoute);
        DetailedQuestProofEvent encounter = DetailedQuestProofEvent.secretRealmEncounterCleared(
                "qz_l2", "thousand_bamboo_puppet_tower", "session-2", "mid");
        CompoundTag encounterEntry = DetailedQuestProofService.historyEntry(encounterRoute, encounter);
        assertEquals("qz_l2", encounterEntry.getString("Region"));
        assertEquals("thousand_bamboo_puppet_tower", encounterEntry.getString("SecretRealm"));
        assertEquals("mid", encounterEntry.getString("Phase"));
        history.put(encounterRoute.eventId(), encounterEntry);
        DetailedQuestProofEvent replayedEncounter = DetailedQuestProofService.eventFromHistory(encounterRoute, history);
        assertNotNull(replayedEncounter);
        assertEquals("qz_l2", replayedEncounter.parameter("region"));
        assertEquals("thousand_bamboo_puppet_tower", replayedEncounter.secretRealmId());
        assertEquals("mid", replayedEncounter.phase());
    }

    @Test
    void realmEncounterAndLayerProofsRequireTheLiveSessionInSource() throws Exception {
        String service = compact(Files.readString(JAVA_ROOT.resolve(
                "quest/DetailedQuestProofService.java")));
        assertTrue(service.contains("recordEncounterCleared(ServerPlayerplayer,StringrealmId,Stringlayer)"));
        assertTrue(service.contains("encounterRegionsForPhase(event.secretRealmId(),event.phase())"));
        assertTrue(service.contains("SecretRealmSessionService.activeSession(player,event.secretRealmId())"));
        assertTrue(service.contains("session.sessionId().equals(event.sessionId())"));
        assertTrue(service.contains("no_active_session"));
        assertTrue(service.contains("entitiesProvingToken(routeEntityToken)"));
    }

    @Test
    void cloneCopyCarriesCombatHistoryIndependently() {
        CompoundTag source = new CompoundTag();
        CompoundTag history = new CompoundTag();
        CompoundTag kill = new CompoundTag();
        kill.putString("Type", "ENTITY_KILLED");
        kill.putString("Entity", "puppet_tower_lord");
        history.put("entity_killed:qianzhu_tower_trial:step_5", kill);
        CompoundTag encounter = new CompoundTag();
        encounter.putString("Type", "ENCOUNTER_CLEARED");
        encounter.putString("Region", "qz_l2");
        encounter.putString("SecretRealm", "thousand_bamboo_puppet_tower");
        history.put("encounter_cleared:qianzhu_tower_trial:step_2", encounter);
        source.put(DetailedQuestProofService.HISTORY_TAG, history);

        CompoundTag target = new CompoundTag();
        DetailedQuestProofService.copyPersistentData(source, target);

        CompoundTag copiedKill = target.getCompound(DetailedQuestProofService.HISTORY_TAG)
                .getCompound("entity_killed:qianzhu_tower_trial:step_5");
        assertEquals("puppet_tower_lord", copiedKill.getString("Entity"));
        CompoundTag copiedEncounter = target.getCompound(DetailedQuestProofService.HISTORY_TAG)
                .getCompound("encounter_cleared:qianzhu_tower_trial:step_2");
        assertEquals("qz_l2", copiedEncounter.getString("Region"));
        assertNotSame(source.get(DetailedQuestProofService.HISTORY_TAG),
                target.get(DetailedQuestProofService.HISTORY_TAG));
    }

    @Test
    void legacyEntityStringEvidenceIsNotAssembledByCombatEntries() throws Exception {
        String hooks = Files.readString(JAVA_ROOT.resolve("quest/QuestHookRuntime.java"));
        String boss = Files.readString(JAVA_ROOT.resolve("worldpack/BossEncounterService.java"));
        String capture = Files.readString(JAVA_ROOT.resolve("artifact/ArtifactCaptureService.java"));
        String escort = Files.readString(JAVA_ROOT.resolve("sect/EscortMissionService.java"));
        assertFalse(hooks.contains("recordAndAdvance(player, typeId)"));
        assertFalse(boss.contains("recordAndAdvance("));
        assertFalse(capture.contains("recordAndAdvance("));
        assertFalse(escort.contains("recordAndAdvance("));
        assertFalse(boss.contains("quest_step_"));
        assertFalse(capture.contains("quest_step_"));
        assertFalse(escort.contains("quest_step_"));
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }
}
