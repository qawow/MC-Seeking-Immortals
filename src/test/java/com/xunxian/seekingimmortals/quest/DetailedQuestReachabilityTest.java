package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.RealmStage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailedQuestReachabilityTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void mapsRuntimeRegionsAndDedicatedRealmsToExactPlayableChains() {
        assertEquals(List.of("mortal_qixuan_entry"),
                QuestHookRuntime.detailedChainsForRegion("qinglan_mountains"));
        assertEquals(List.of("kunwu_clue_assemble", "yinyang_ku_intel"),
                QuestHookRuntime.detailedChainsForRegion("dajin"));
        assertEquals(List.of("tianyuan_landing_register"),
                QuestHookRuntime.detailedChainsForRegion("tianyuan"));
        assertEquals(List.of("blood_forbidden_run", "nangong_wan_weight_optional"),
                QuestHookRuntime.detailedChainsForSecretRealm("blood_forbidden"));
        assertEquals(List.of("xutian_window_prepare"),
                QuestHookRuntime.detailedChainsForSecretRealm("void_palace"));
        assertEquals(List.of("kunwu_clue_assemble"),
                QuestHookRuntime.detailedChainsForSecretRealm("kunwu_mountain"));
        assertEquals(List.of("zhuimo_token", "lingzhu_fruit_run"),
                QuestHookRuntime.detailedChainsForSecretRealm("fallen_demon_valley"));
        assertEquals(List.of("yinyang_ku_intel", "peiying_material_hunt"),
                QuestHookRuntime.detailedChainsForSecretRealm("yinyang_ku"));
        assertEquals(List.of("qianzhu_tower_trial"),
                QuestHookRuntime.detailedChainsForSecretRealm("thousand_bamboo_puppet_tower"));
        assertEquals(List.of("guanghan_endgame_path"),
                QuestHookRuntime.detailedChainsForSecretRealm("guanghan_realm"));
    }

    @Test
    void startsLateMortalEndgameOnlyAtAuthoredRealmBand() {
        assertFalse(QuestHookRuntime.isHighRealmPathEligible(Realm.NASCENT_SOUL, RealmStage.MIDDLE));
        assertTrue(QuestHookRuntime.isHighRealmPathEligible(Realm.NASCENT_SOUL, RealmStage.LATE));
        assertTrue(QuestHookRuntime.isHighRealmPathEligible(Realm.NASCENT_SOUL, RealmStage.PEAK));
        assertTrue(QuestHookRuntime.isHighRealmPathEligible(Realm.SOUL_TRANSFORMATION, RealmStage.EARLY));
        assertFalse(QuestHookRuntime.isHighRealmPathEligible(Realm.VOID_REFINEMENT, RealmStage.EARLY));
    }

    @Test
    void entryHooksRunOnlyAfterAuthoritativeTransitions() throws Exception {
        String session = compact(Files.readString(JAVA_ROOT.resolve(
                "worldpack/SecretRealmSessionService.java")));
        assertTrue(session.contains("\"yinyang_ku\".equals(realmId.trim().toLowerCase(Locale.ROOT))")
                && session.contains("NpcDialogueFlags.hasFlag(player,\"yinyang_ku_entry\")"));
        int sessionStarted = session.indexOf("startSession(player,realmId,timeLimit,party)");
        int realmHook = session.indexOf("QuestHookRuntime.onSecretRealmEnter(player,realmId)");
        assertTrue(sessionStarted >= 0 && realmHook > sessionStarted);

        String gameplay = compact(Files.readString(JAVA_ROOT.resolve(
                "worldpack/WorldpackGameplayService.java")));
        int teleport = gameplay.indexOf("if(!teleportToAnchor(player,anchor.get()))");
        int regionHook = gameplay.indexOf("QuestHookRuntime.onRegionReached(player,region.id())", teleport);
        assertTrue(teleport >= 0 && regionHook > teleport);

        String ascension = compact(Files.readString(JAVA_ROOT.resolve(
                "worldpack/AscensionService.java")));
        int ascended = ascension.indexOf("NpcDialogueFlags.setFlag(player,FLAG_ASCENDED,true)");
        int tianyuanHook = ascension.indexOf("QuestHookRuntime.onRegionReached(player,\"tianyuan\")", ascended);
        assertTrue(ascended >= 0 && tianyuanHook > ascended);
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }
}
