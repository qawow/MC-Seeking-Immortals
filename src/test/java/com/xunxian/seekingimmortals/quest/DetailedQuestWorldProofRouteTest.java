package com.xunxian.seekingimmortals.quest;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Q-B-2 structured world proofs: region, dimension, secret-realm and structure events.
 *
 * <p>No Mockito is available, so the tests exercise the extracted package-private pure mapping
 * functions plus source-level contract assertions that pin the real producer call sites.</p>
 */
class DetailedQuestWorldProofRouteTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Path ROUTES = Path.of("src", "main", "resources", "data", "seeking_immortals",
            "text_material", "detailed_quest_proof_routes.json");

    @Test
    void secretRealmPhaseMappingIsExactAndDeepIdsNeverComeFromOrdinaryTravel() {
        assertEquals(List.of("blood_forbidden", "bf_outer_mist"),
                DetailedQuestProofService.regionIdsForPhase("blood_forbidden", "entry"));
        assertEquals(List.of("bf_water_jiao"),
                DetailedQuestProofService.regionIdsForPhase("blood_forbidden", "mid"));
        assertEquals(List.of("blood_forbidden_exit_array"),
                DetailedQuestProofService.regionIdsForPhase("blood_forbidden", "voluntary_exit"));
        assertEquals(List.of("island_xutian_window"),
                DetailedQuestProofService.regionIdsForPhase("void_palace", "entry"));
        assertEquals(List.of("dajin_kunwu_approach"),
                DetailedQuestProofService.regionIdsForPhase("kunwu_mountain", "entry"));
        assertEquals(List.of("fallen_demon_rift"),
                DetailedQuestProofService.regionIdsForPhase("fallen_demon_valley", "entry"));
        assertEquals(List.of("zm_inner"),
                DetailedQuestProofService.regionIdsForPhase("fallen_demon_valley", "mid"));
        assertEquals(List.of("zm_candle"),
                DetailedQuestProofService.regionIdsForPhase("fallen_demon_valley", "core"));
        assertEquals(List.of("yinyang_cave_gate"),
                DetailedQuestProofService.regionIdsForPhase("yinyang_ku", "entry"));
        assertEquals(List.of("gh_approach"),
                DetailedQuestProofService.regionIdsForPhase("guanghan_realm", "entry"));

        assertTrue(DetailedQuestProofService.regionIdsForPhase("blood_forbidden", "core").isEmpty());
        assertTrue(DetailedQuestProofService.regionIdsForPhase("blood_forbidden", "voluntary_exit_during_timeout").isEmpty());
        assertTrue(DetailedQuestProofService.regionIdsForPhase("yinyang_ku", "mid").isEmpty());
        assertTrue(DetailedQuestProofService.regionIdsForPhase("unknown_realm", "entry").isEmpty());
        assertTrue(DetailedQuestProofService.regionIdsForPhase("blood_forbidden", "").isEmpty());

        for (String realm : List.of("blood_forbidden", "void_palace", "kunwu_mountain",
                "fallen_demon_valley", "yinyang_ku", "guanghan_realm")) {
            for (String phase : List.of("entry", "mid", "core", "voluntary_exit")) {
                for (String region : DetailedQuestProofService.regionIdsForPhase(realm, phase)) {
                    assertTrue(DetailedQuestProofService.isSecretRealmRegion(region), realm + ":" + phase + ":" + region);
                }
            }
        }
    }

    @Test
    void ordinaryRegionAliasesNeverGrantDeepIdsAndHuangfengIsTiannanBound() {
        assertEquals(Set.of("chaotic_sea"),
                DetailedQuestProofService.trustedRegionAliases("chaotic_sea"));
        assertEquals(Set.of("tiannan", "huangfeng_outer"),
                DetailedQuestProofService.trustedRegionAliases("tiannan"));
        assertEquals(Set.of("huangfeng_outer"),
                DetailedQuestProofService.trustedRegionAliases("huangfeng_outer"));
        assertEquals(Set.of("tianyuan"),
                DetailedQuestProofService.trustedRegionAliases("tianyuan"));

        for (String region : DetailedQuestProofService.trustedRegionAliases("tiannan")) {
            assertFalse(DetailedQuestProofService.isSecretRealmRegion(region));
        }
        assertFalse(DetailedQuestProofService.trustedRegionAliases("chaotic_sea")
                .stream().anyMatch(DetailedQuestProofService::isSecretRealmRegion));
        assertFalse(DetailedQuestProofService.trustedRegionAliases("qinglan_mountains")
                .stream().anyMatch(DetailedQuestProofService::isSecretRealmRegion));
    }

    @Test
    void deepRealmIdsRejectOrdinaryContextAndAcceptOnlyBoundSessionContext() {
        DetailedQuestProofEvent forged = DetailedQuestProofEvent.regionEntered("bf_water_jiao");
        assertFalse(DetailedQuestProofService.regionProofMatchesContext("bf_water_jiao", forged));

        DetailedQuestProofEvent forgedExit = DetailedQuestProofEvent.regionEntered("blood_forbidden_exit_array");
        assertFalse(DetailedQuestProofService.regionProofMatchesContext("blood_forbidden_exit_array", forgedExit));

        DetailedQuestProofEvent bound = DetailedQuestProofEvent.secretRealmLayerEntered(
                "zm_candle", "fallen_demon_valley", "session-1", "core");
        assertTrue(DetailedQuestProofService.regionProofMatchesContext("zm_candle", bound));
        assertFalse(DetailedQuestProofService.regionProofMatchesContext("zm_inner", bound));

        DetailedQuestProofEvent wrongRealm = DetailedQuestProofEvent.secretRealmLayerEntered(
                "zm_candle", "blood_forbidden", "session-1", "core");
        assertFalse(DetailedQuestProofService.regionProofMatchesContext("zm_candle", wrongRealm));

        DetailedQuestProofEvent wrongPhase = DetailedQuestProofEvent.secretRealmLayerEntered(
                "zm_candle", "fallen_demon_valley", "session-1", "entry");
        assertFalse(DetailedQuestProofService.regionProofMatchesContext("zm_candle", wrongPhase));

        DetailedQuestProofEvent ordinary = DetailedQuestProofEvent.regionEntered("chaotic_sea");
        assertTrue(DetailedQuestProofService.regionProofMatchesContext("chaotic_sea", ordinary));
        assertTrue(DetailedQuestProofService.regionProofMatchesContext("huangfeng_outer",
                DetailedQuestProofEvent.regionEntered("tiannan")));
        assertFalse(DetailedQuestProofService.regionProofMatchesContext("blood_forbidden",
                DetailedQuestProofEvent.regionEntered("tiannan")));
    }

    @Test
    void dimensionAliasMappingIsStrict() {
        assertEquals(Set.of("tianyuan", "spirit_realm"),
                DetailedQuestProofService.trustedDimensionAliases("tianyuan"));
        assertEquals(Set.of("fengyuan", "spirit_realm"),
                DetailedQuestProofService.trustedDimensionAliases("spirit_fengyuan"));
        assertEquals(Set.of("qianzhu_tower"),
                DetailedQuestProofService.trustedDimensionAliases("secret_realm_thousand_bamboo_puppet_tower"));
        assertEquals(Set.of("seeking_immortals:mortal_world"),
                DetailedQuestProofService.trustedDimensionAliases("seeking_immortals:mortal_world"));
        assertTrue(DetailedQuestProofService.trustedDimensionAliases("").isEmpty());
        assertFalse(DetailedQuestProofService.trustedDimensionAliases("spirit_fengyuan")
                .contains("tianyuan"));
        assertFalse(DetailedQuestProofService.trustedDimensionAliases("tianyuan")
                .contains("qianzhu_tower"));
    }

    @Test
    void reclassifiedRoutesMatchTheQB2Classification() {
        DetailedQuestProofCatalog.Snapshot catalog = DetailedQuestRuntimeService.proofCatalog();

        DetailedQuestProofCatalog.Route root = catalog.find("mortal_qixuan_entry", 2);
        assertEquals("SPIRIT_ROOT_TESTED", root.proofType());
        assertEquals("spirit_root", root.producer());
        assertEquals("spirit_root_test", root.parameter("item"));

        DetailedQuestProofCatalog.Route board = catalog.find("tianyuan_to_fengyuan_gate", 1);
        assertEquals("INFO_ACKNOWLEDGED", board.proofType());
        assertEquals("tianyuan_garrison_board", board.parameter("choice"));

        DetailedQuestProofCatalog.Route candle = catalog.find("lingzhu_fruit_run", 2);
        assertEquals("REGION_ENTER", candle.proofType());
        assertEquals("zm_candle", candle.parameter("region"));

        DetailedQuestProofCatalog.Route hyCore = catalog.find("deity_huoyu_path", 5);
        assertEquals("ITEM_ACQUIRED", hyCore.proofType());
        assertEquals("tai_yang_jing_huo", hyCore.parameter("item"));

        for (DetailedQuestProofCatalog.Route route : catalog.routes()) {
            if ("STRUCTURE_FORMED".equals(route.proofType())) {
                assertTrue(realStructureIds().contains(route.parameter("structure")),
                        route.eventId() + " -> " + route.parameter("structure"));
            }
            if ("REGION_ENTER".equals(route.proofType())) {
                String region = route.parameter("region");
                if (DetailedQuestProofService.isSecretRealmRegion(region)) {
                    assertTrue(deepRegionHasPhaseSource(region), route.eventId() + ":" + region);
                }
            }
        }
    }

    private static boolean deepRegionHasPhaseSource(String region) {
        for (String realm : List.of("blood_forbidden", "void_palace", "kunwu_mountain",
                "fallen_demon_valley", "yinyang_ku", "guanghan_realm")) {
            for (String phase : List.of("entry", "mid", "core", "voluntary_exit")) {
                if (DetailedQuestProofService.regionIdsForPhase(realm, phase).contains(region)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void everyQB2DomainRouteHasAnExplicitTrustedProductionMapping() throws Exception {
        String runtime = compact(Files.readString(JAVA_ROOT.resolve("quest/QuestHookRuntime.java")));
        String gameplay = compact(Files.readString(JAVA_ROOT.resolve("worldpack/WorldpackGameplayService.java")));
        String travel = compact(Files.readString(JAVA_ROOT.resolve("worldpack/DimensionTravelService.java")));
        String ascension = compact(Files.readString(JAVA_ROOT.resolve("worldpack/AscensionService.java")));
        String structure = compact(Files.readString(JAVA_ROOT.resolve("structure/MultiblockOperationalService.java")));
        String dialogue = compact(Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java")));
        String stone = compact(Files.readString(JAVA_ROOT.resolve("item/LingGenTestStoneItem.java")));
        String session = compact(Files.readString(JAVA_ROOT.resolve("worldpack/SecretRealmSessionService.java")));

        assertTrue(runtime.contains("recordSecretRealmEntry(player,realm)"),
                "secret-realm entry proof must be recorded by the realm session hook");
        assertTrue(runtime.contains("recordSecretRealmLayer(player,realm,layerKey)"),
                "secret-realm mid/core proofs must be recorded by the clear hook");
        assertTrue(gameplay.contains("recordRegionReached(player,region.id())"),
                "region travel success must record the structured region proof");
        assertTrue(gameplay.contains("recordDimensionEntered(player)"),
                "dedicated secret-realm entry must record the structured dimension proof");
        assertTrue(gameplay.contains("recordVoluntaryExit(player,activeRealm,activeSession.get().sessionId())"),
                "only voluntary exits may record the exit proof");
        assertTrue(travel.contains("recordDimensionEntered(player)"),
                "dimension travel success must record the structured dimension proof");
        assertTrue(ascension.contains("recordDimensionEntered(player)"),
                "ascension success must record the tianyuan dimension proof");
        assertTrue(structure.contains("recordFormedProof(player,stationId,origin)"),
                "form() must record the structured structure proof after forceIntact");
        assertTrue(structure.contains("recordStructureFormed(player,stationId,player.level().dimension().location().toString(),origin.asLong())"),
                "recordFormedProof must record with the exact dimension and origin");
        assertTrue(structure.contains("isCommissioned("),
                "structure proof validation must use the read-only commissioned check");
        assertTrue(dialogue.contains("recordStructureFormed(player,id,located.dimension(),located.pos().asLong())"),
                "markStructure must record the structured structure proof for formed+commissioned structures");
        assertTrue(dialogue.contains("MultiblockOperationalService.isCommissioned(level,id,located.pos())"),
                "markStructure must require the commissioned state");
        assertTrue(stone.contains("recordSpiritualRootTested(targetServerPlayer)"),
                "the appraisal stone must be the spirit-root proof producer");
        assertTrue(session.contains("returnFromSecretRealm(player,false)"),
                "timeout and death repatriation must be non-voluntary exits");
    }

    @Test
    void qb2BusinessEntriesNoLongerAssembleLegacyRecordAndAdvanceStrings() throws Exception {
        String runtime = Files.readString(JAVA_ROOT.resolve("quest/QuestHookRuntime.java"));
        String gameplay = Files.readString(JAVA_ROOT.resolve("worldpack/WorldpackGameplayService.java"));
        String travel = Files.readString(JAVA_ROOT.resolve("worldpack/DimensionTravelService.java"));
        String ascension = Files.readString(JAVA_ROOT.resolve("worldpack/AscensionService.java"));
        String structure = Files.readString(JAVA_ROOT.resolve("structure/MultiblockOperationalService.java"));
        String dialogue = Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java"));

        assertFalse(runtime.contains("recordAndAdvance(player, region)"));
        assertFalse(runtime.contains("recordAndAdvance(player, realm)"));
        assertFalse(runtime.contains("recordAndAdvance(player, layerKey)"));
        assertFalse(gameplay.contains("recordAndAdvance("));
        assertFalse(travel.contains("recordAndAdvance("));
        assertFalse(ascension.contains("recordAndAdvance("));
        assertFalse(structure.contains("recordAndAdvance("));
        assertFalse(dialogue.contains("recordAndAdvance("));
        assertFalse(runtime.contains("quest_step_"), "Q-B-2 entries must not concatenate quest_step_ tokens");
    }

    @Test
    void historyRecordsKeepTypeKeyLayerAndWorldContextAndCloneIndependently() {
        DetailedQuestProofEvent event = DetailedQuestProofEvent.secretRealmLayerEntered(
                "bf_water_jiao", "blood_forbidden", "session-9", "mid");
        DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog()
                .find("nangong_wan_weight_optional", 1);
        assertNotNull(route);

        CompoundTag entry = DetailedQuestProofService.historyEntry(route, event);
        assertEquals("REGION_ENTER", entry.getString("Type"));
        assertEquals("region:bf_water_jiao", entry.getString("EventKey"));
        assertEquals("blood_forbidden", entry.getString("SecretRealm"));
        assertEquals("session-9", entry.getString("Session"));
        assertEquals("mid", entry.getString("Phase"));

        CompoundTag history = new CompoundTag();
        history.put(route.eventId(), entry);
        assertTrue(DetailedQuestProofService.hasHistoryEntry(history, route.eventId()));
        assertFalse(DetailedQuestProofService.hasHistoryEntry(history, "missing"));
        assertFalse(DetailedQuestProofService.hasHistoryEntry(new CompoundTag(), route.eventId()));

        DetailedQuestProofEvent replayed = DetailedQuestProofService.eventFromHistory(route, history);
        assertNotNull(replayed);
        assertEquals(DetailedQuestProofEvent.Source.HISTORY, replayed.source());
        assertEquals("bf_water_jiao", replayed.parameter("region"));
        assertEquals("blood_forbidden", replayed.secretRealmId());
        assertEquals("session-9", replayed.sessionId());
        assertEquals("mid", replayed.phase());
        assertEquals("region:bf_water_jiao", replayed.eventKey());
    }

    @Test
    void structureHistoryKeepsTheOriginalDimensionAndOriginForRevalidation() {
        DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog()
                .find("qianzhu_tower_trial", 4);
        assertNotNull(route);
        DetailedQuestProofEvent event = DetailedQuestProofEvent.structureFormed(
                "qianzhu_control_console", "seeking_immortals:secret_realm_thousand_bamboo_puppet_tower", 42L);
        CompoundTag entry = DetailedQuestProofService.historyEntry(route, event);
        assertEquals("STRUCTURE_FORMED", entry.getString("Type"));
        assertEquals(42L, entry.getLong("Pos"));
        assertTrue(entry.contains("Pos"));

        CompoundTag history = new CompoundTag();
        history.put(route.eventId(), entry);
        DetailedQuestProofEvent replayed = DetailedQuestProofService.eventFromHistory(route, history);
        assertNotNull(replayed);
        assertTrue(replayed.hasPosition());
        assertEquals(42L, replayed.packedPosition());
        assertEquals("seeking_immortals:secret_realm_thousand_bamboo_puppet_tower", replayed.dimensionId());
        assertEquals("qianzhu_control_console", replayed.parameter("structure"));
    }

    @Test
    void ledgerKeysAreStableForDuplicateEventsAndHistoryContexts() {
        DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog()
                .find("mortal_qixuan_entry", 3);
        assertNotNull(route);
        DetailedQuestProofEvent first = DetailedQuestProofEvent.regionEntered("tiannan");
        DetailedQuestProofEvent second = DetailedQuestProofEvent.regionEntered("TIANNAN");
        assertEquals(DetailedQuestProofService.ledgerKey(route, first),
                DetailedQuestProofService.ledgerKey(route, second));

        DetailedQuestProofCatalog.Route structureRoute = DetailedQuestRuntimeService.proofCatalog()
                .find("wuxing_intro", 2);
        assertNotNull(structureRoute);
        DetailedQuestProofEvent formed = DetailedQuestProofEvent.structureFormed(
                "wuxing_world_seed_block", "seeking_immortals:mortal_world", 7L);
        assertEquals(DetailedQuestProofService.ledgerKey(structureRoute, formed),
                DetailedQuestProofService.ledgerKey(structureRoute,
                        DetailedQuestProofEvent.structureFormed(
                                "wuxing_world_seed_block", "seeking_immortals:mortal_world", 7L)));
    }

    @Test
    void cloneCopyCarriesCompoundHistoryRecordsIndependently() {
        CompoundTag source = new CompoundTag();
        CompoundTag ledger = new CompoundTag();
        ledger.putBoolean("region_enter:test|region:chaotic_sea", true);
        CompoundTag history = new CompoundTag();
        CompoundTag record = new CompoundTag();
        record.putString("Type", "REGION_ENTER");
        record.putString("EventKey", "region:chaotic_sea");
        record.putString("Region", "chaotic_sea");
        history.put("region_enter:star_palace_register:step_1", record);
        source.put(DetailedQuestProofService.LEDGER_TAG, ledger);
        source.put(DetailedQuestProofService.HISTORY_TAG, history);

        CompoundTag target = new CompoundTag();
        DetailedQuestProofService.copyPersistentData(source, target);

        assertTrue(target.getCompound(DetailedQuestProofService.LEDGER_TAG)
                .getBoolean("region_enter:test|region:chaotic_sea"));
        CompoundTag copied = target.getCompound(DetailedQuestProofService.HISTORY_TAG)
                .getCompound("region_enter:star_palace_register:step_1");
        assertEquals("REGION_ENTER", copied.getString("Type"));
        assertEquals("region:chaotic_sea", copied.getString("EventKey"));
        assertNotSame(source.get(DetailedQuestProofService.HISTORY_TAG),
                target.get(DetailedQuestProofService.HISTORY_TAG));
        assertNotSame(source.get(DetailedQuestProofService.LEDGER_TAG),
                target.get(DetailedQuestProofService.LEDGER_TAG));
    }

    @Test
    void structureValidationRequiresCatalogFormedAndCommissionedStateInSource() throws Exception {
        String service = compact(Files.readString(JAVA_ROOT.resolve(
                "quest/DetailedQuestProofService.java")));
        assertTrue(service.contains("MultiblockStructureCatalog.builtin().find(route.parameter(\"structure\"))"));
        assertTrue(service.contains("MultiblockStationService.isStationFormed(level,route.parameter(\"structure\"),origin)"));
        assertTrue(service.contains("MultiblockOperationalService.isCommissioned(level,route.parameter(\"structure\"),origin)"));
        assertTrue(service.contains("event.hasPosition()"));
        assertTrue(service.contains("event.source()!=DetailedQuestProofEvent.Source.HISTORY"));
        assertTrue(service.contains("player.level().dimension().location().toString()"),
                "dimension proof must compare the post-teleport dimension");

        String operational = compact(Files.readString(JAVA_ROOT.resolve(
                "structure/MultiblockOperationalService.java")));
        assertTrue(operational.contains("publicstaticbooleanisCommissioned(ServerLevellevel,StringstationId,BlockPosorigin)"));
        assertTrue(operational.contains("state.state()!=MultiblockOperationalSavedData.OpState.DISABLED&&state.hp()>0"));
        assertFalse(operational.contains("isCommissioned(ServerLevellevel,StringstationId,BlockPosorigin){returnensureState"),
                "isCommissioned must never create state through ensureState");
    }

    @Test
    void dimensionProofsVerifyTheActualPostTeleportDimensionInSource() throws Exception {
        String service = compact(Files.readString(JAVA_ROOT.resolve(
                "quest/DetailedQuestProofService.java")));
        assertTrue(service.contains("Stringactual=normalize(player.level().dimension().location().toString())"));
        assertTrue(service.contains("!actual.equals(event.dimensionId())"));
        assertTrue(service.contains("trustedDimensionAliases(dimensionPath(event.dimensionId()))"));
    }

    @Test
    void regionProofsRequireLiveRegionOrBoundSessionInSource() throws Exception {
        String service = compact(Files.readString(JAVA_ROOT.resolve(
                "quest/DetailedQuestProofService.java")));
        assertTrue(service.contains("SecretRealmSessionService.activeSession(player,event.secretRealmId())"));
        assertTrue(service.contains("session.sessionId().equals(event.sessionId())"));
        assertTrue(service.contains("event.currentRegionId().equals(live)"));
        assertTrue(service.contains("trustedRegionAliases(event.currentRegionId()).contains(routeRegion)"));
        assertTrue(service.contains("regionIdsForPhase(event.secretRealmId(),event.phase()).contains(routeRegion)"));
    }

    @Test
    void routeResourceCarriesTheFourReclassifications() throws Exception {
        String resource = Files.readString(ROUTES);
        assertTrue(resource.contains("\"proof_type\":\"SPIRIT_ROOT_TESTED\""));
        assertTrue(resource.contains("\"item\":\"spirit_root_test\""));
        assertTrue(resource.contains("\"choice\":\"tianyuan_garrison_board\""));
        assertTrue(resource.contains("\"region\":\"zm_candle\""));
        assertTrue(resource.contains("\"item\":\"tai_yang_jing_huo\""));
        assertFalse(resource.contains("\"structure\":\"spirit_root_test\""));
        assertFalse(resource.contains("\"structure\":\"zm_candle\""));
        assertFalse(resource.contains("\"structure\":\"tianyuan_garrison_board\""));
        assertFalse(resource.contains("\"region\":\"hy_core\""));
    }

    @Test
    void structureRouteIdsAreTheEightRealFormableStructures() throws Exception {
        String resource = Files.readString(ROUTES);
        for (String structureId : realStructureIds()) {
            assertTrue(resource.contains("\"structure\":\"" + structureId + "\""),
                    "missing STRUCTURE_FORMED route for " + structureId);
        }
    }

    private static Set<String> realStructureIds() {
        return Set.of("contribution_stele", "inner_sect_task_board", "star_palace_teleport_gate",
                "inverse_star_smuggle_dock", "meditation_chamber", "wuxing_world_seed_block",
                "xutian_palace_gate_fragment", "qianzhu_control_console");
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }
}
