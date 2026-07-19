package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameplayAuthorityLedgerTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void unknownBossIdsFailClosedBeforeAnySpawnPath() throws Exception {
        assertTrue(BossEncounterService.isKnownBossId("blood_jiao_guardian"));
        assertFalse(BossEncounterService.isKnownBossId("core_blood_forbidden"));
        assertFalse(BossEncounterService.isKnownBossId("unknown_authority_probe"));
        assertFalse(BossEncounterService.isKnownBossId(null));

        String source = readSource("worldpack", "BossEncounterService.java");
        String spawn = compact(methodSource(source, "public static boolean spawnIfNeeded("));

        int unknownGuard = spawn.indexOf("if(!isKnownBossId(id))");
        int rejection = spawn.indexOf("returnfalse;", unknownGuard);
        int firstSpawn = firstIndex(spawn,
                "BeastBossService.spawnCatalogBoss(",
                "TrialCombatShellService.spawnHostile(");

        assertTrue(unknownGuard >= 0, "boss spawning must reject ids outside the known boss catalogs");
        assertTrue(rejection > unknownGuard, "unknown boss ids must return false");
        assertTrue(firstSpawn < 0 || rejection < firstSpawn,
                "unknown boss ids must be rejected before catalog or fallback spawning");
    }

    @Test
    void secretRealmTrialsUseKnownCatalogBossesWithoutSyntheticCoreIds() throws Exception {
        String source = readSource("worldpack", "SecretRealmTrialService.java");
        String compactSource = compact(source);
        String onEnter = compact(methodSource(source, "public static void onEnter("));

        assertFalse(onEnter.contains("\"core_\"+id"),
                "secret realms must not synthesize core_<realm> boss ids");
        assertTrue(onEnter.contains("BossEncounterService.spawnIfNeeded(player,"),
                "secret-realm entry must route the selected boss through BossEncounterService");

        int realmLookup = compactSource.indexOf("SecretRealmCatalogService.find(");
        int bossList = compactSource.indexOf(".bosses()", realmLookup);
        int knownBossFilter = compactSource.indexOf(
                ".filter(BossEncounterService::isKnownBossId)", bossList);
        assertTrue(realmLookup >= 0, "boss selection must start from SecretRealmCatalogService.RealmDef");
        assertTrue(bossList > realmLookup, "boss selection must read RealmDef.bosses");
        assertTrue(knownBossFilter > bossList, "RealmDef boss candidates must be filtered through the known boss catalog");
    }

    @Test
    void secretRealmSessionExistsBeforeTrialsSpawn() throws Exception {
        String gameplay = compact(readSource("worldpack", "WorldpackGameplayService.java"));
        int session = gameplay.indexOf("SecretRealmSessionService.onEnter(player,realm.id())");
        int trial = gameplay.indexOf("SecretRealmTrialService.onEnter(player,realm.id())");

        assertTrue(session >= 0 && trial > session,
                "session creation must precede trial, boss, and reward spawning");
    }

    @Test
    void secretRealmKillsClaimBoundEncountersBeforeRewardsOrQuestHooks() throws Exception {
        String trialSource = readSource("worldpack", "SecretRealmTrialService.java");
        String trialKill = compact(methodSource(trialSource, "public static boolean onTrialMobKilled("));
        int trialClaim = trialKill.indexOf("SecretRealmSessionService.claimEncounter(killer,trial)");
        int trialReward = firstIndex(trialKill, "unlockMid(", "unlockCore(");
        assertTrue(trialClaim >= 0 && trialReward > trialClaim,
                "trial rewards must remain behind the owner-session encounter claim");

        String bossSource = readSource("worldpack", "BossEncounterService.java");
        String bossKill = compact(methodSource(bossSource, "public static boolean onBossKilled("));
        int bossClaim = bossKill.indexOf("SecretRealmSessionService.claimEncounter(killer,bossTag)");
        int bossLoot = bossKill.indexOf("BossLootService.grantBossLoot(");
        assertTrue(bossClaim >= 0 && bossLoot > bossClaim,
                "boss loot must remain behind the owner-session encounter claim");

        String events = compact(readSource("event", "ModEvents.java"));
        assertTrue(events.contains("if(accepted){")
                        && events.indexOf("QuestHookRuntime.onSecretRealmClear(") > events.indexOf("if(accepted){"),
                "quest hooks must advance only after the trial authority accepts the kill");
        String hurt = compact(methodSource(readSource("event", "ModEvents.java"),
                "public static void onLivingHurt("));
        assertTrue(hurt.contains("SecretRealmSessionService.matchesEncounter(authorityPlayer,binding)"),
                "other players and their servitors must not be able to kill a bound encounter");
    }

    @Test
    void rewardChestsKeepItemsOutOfVanillaInventoryAndRequireSessionBinding() throws Exception {
        String rewards = readSource("worldpack", "SecretRealmRewardService.java");
        String initialize = compact(methodSource(rewards, "public static void initializeChest("));
        String claim = compact(methodSource(rewards, "public static ClaimResult claim("));

        assertTrue(initialize.contains("chest.clearContent()"),
                "bound reward chests must expose no hopper-readable vanilla inventory");
        assertTrue(initialize.contains("SecretRealmSessionService.bindEncounter("),
                "reward chests must store owner, session, realm, and encounter binding");
        int match = claim.indexOf("SecretRealmSessionService.matchesEncounter(player,binding)");
        int claimed = claim.indexOf("binding.putBoolean(CLAIMED_TAG,true)");
        int deliveryDrop = claim.indexOf("InventoryDeliveryService.giveOrDrop(");
        int deliveryEnqueue = claim.indexOf("InventoryDeliveryService.giveOrEnqueue(");
        int delivery = deliveryDrop >= 0 ? deliveryDrop : deliveryEnqueue;
        assertTrue(match >= 0 && claimed > match && delivery > claimed,
                "reward delivery must validate binding and close the claim before item delivery");
    }

    @Test
    void duplicateChronicleDiscoveryCannotMutateMappedQuestAgain() throws Exception {
        String source = readSource("catalog", "ChronicleTradeSoftService.java");
        String discover = compact(methodSource(source, "public static boolean discoverChronicle("));
        int firstMutation = firstIndex(discover,
                "TextQuestChainService.start(",
                "TextQuestChainService.advance(",
                "ReputationService.onQuestComplete(");

        assertTrue(firstMutation >= 0, "chronicle discovery must retain its mapped-quest integration");
        int duplicateGuard = discover.indexOf("if(!first)");
        if (duplicateGuard < 0) {
            duplicateGuard = discover.indexOf("if(hasDiscovered(player,key))");
        }

        if (duplicateGuard >= 0) {
            int duplicateReturn = discover.indexOf("return", duplicateGuard);
            assertTrue(duplicateReturn > duplicateGuard && duplicateReturn < firstMutation,
                    "repeat discovery must return before mapped quest mutation");
            assertAfter(discover, "TextQuestChainService.start(", duplicateReturn);
            assertAfter(discover, "TextQuestChainService.advance(", duplicateReturn);
            assertAfter(discover, "ReputationService.onQuestComplete(", duplicateReturn);
            int failedEffectReturn = discover.indexOf("if(!ok){returnfalse;}", firstMutation);
            int discoveredLedger = discover.indexOf("markDiscovered(player,key)");
            assertTrue(failedEffectReturn > firstMutation && discoveredLedger > failedEffectReturn,
                    "first discovery must be claimed only after its mapped quest effect succeeds");
            return;
        }

        String firstOnly = compact(blockSource(discover, "if(first)"));
        assertContainedWhenPresent(discover, firstOnly, "TextQuestChainService.start(");
        assertContainedWhenPresent(discover, firstOnly, "TextQuestChainService.advance(");
        assertContainedWhenPresent(discover, firstOnly, "ReputationService.onQuestComplete(");
    }

    @Test
    void completedSectMissionCannotBeAcceptedAgainOnTheSameWorldDay() throws Exception {
        String source = readSource("sect", "SectContributionService.java");
        String accept = compact(methodSource(source, "public static boolean acceptMission("));
        int missionDay = accept.indexOf("progress.getSectMissionDay()==day");
        String dayGuard = ifConditionContaining(accept, missionDay);
        int guardStart = accept.lastIndexOf("if(", missionDay);
        int guardReturn = accept.indexOf("return;", missionDay);
        int missionCreation = firstIndex(accept,
                "SectMissionGenerator.generate(",
                "SectContentService.missionForDay(");

        assertTrue(missionDay >= 0, "mission acceptance must compare the stored mission day");
        assertTrue(dayGuard.contains("progress.isSectMissionAccepted()")
                        || dayGuard.contains("progress.isSectMissionCompleted()"),
                "the same-day guard must cover an accepted or completed mission ledger");
        assertFalse(dayGuard.contains("!progress.isSectMissionCompleted()"),
                "completion must not reopen mission acceptance on the same world day");
        assertTrue(guardStart >= 0 && guardReturn > missionDay && guardReturn < missionCreation,
                "the same-day ledger guard must return before generating another mission");
    }

    @Test
    void generatedGatherAndFormationTurnInRequireRealCostsAndEvidence() throws Exception {
        String source = readSource("sect", "SectMissionGenerator.java");
        String accept = compact(methodSource(source, "public static boolean acceptGenerated("));
        String turnIn = compact(methodSource(source, "public static boolean turnIn("));
        String gather = compact(blockSource(turnIn, "if(\"gather\".equals(type))"));
        String formation = compact(blockSource(turnIn, "elseif(\"formation\".equals(type))"));
        String consumeGather = compact(methodSource(source, "private static void consumeGatherItems("));

        int requiredBudget = gather.indexOf("required=Math.max(1,mission.count())");
        int gatherValidation = gather.indexOf("have<required");
        int gatherConsumption = gather.indexOf("consumeGatherItems(player,mission.target(),required)");
        assertTrue(requiredBudget >= 0 && gatherValidation > requiredBudget,
                "gather turn-in must derive and verify the required mission quantity before success");
        assertTrue(gatherConsumption > gatherValidation,
                "successful gather turn-in must consume items after the quantity check");
        int remaining = consumeGather.indexOf("remaining=Math.max(0,count)");
        int consumed = consumeGather.indexOf("consumed=Math.min(remaining,stack.getCount())", remaining);
        int shrink = consumeGather.indexOf("stack.shrink(consumed)", consumed);
        int decrement = consumeGather.indexOf("remaining-=consumed", shrink);
        assertTrue(remaining >= 0 && consumed > remaining && shrink > consumed && decrement > shrink,
                "gather consumption must shrink only the remaining required quantity");

        assertTrue(accept.contains("if(\"formation\".equalsIgnoreCase(mission.type()))")
                        && accept.contains("root.putBoolean(\"formation\",false)"),
                "accepting a formation mission must clear stale deployment evidence");
        Matcher evidence = Pattern.compile(
                "root\\.get(?:Boolean|Int)\\(\\\"[^\\\"]*formation[^\\\"]*\\\"\\)")
                .matcher(formation);
        assertTrue(evidence.find(), "formation turn-in must read deployment evidence");
        int rejection = formation.indexOf("returnfalse;", evidence.start());
        int completionPractice = formation.indexOf("LifeSkillService.grantPractice(");
        assertTrue(rejection > evidence.start() && completionPractice > rejection,
                "missing formation deployment must return false before completion practice");
    }

    @Test
    void successfulFormationDeploymentNotifiesTheMissionLedger() throws Exception {
        String source = readSource("structure", "FormationFieldService.java");
        assertFormationNotificationOrder(methodSource(source,
                "public static boolean activate(ServerLevel level, BlockPos corePos, FieldKind kind, "
                        + "ServerPlayer deployer, String formationId)"));
        assertFormationNotificationOrder(methodSource(source,
                "public static boolean activateFreeField(ServerLevel level, BlockPos center, FieldKind kind, "
                        + "int durationTicks, ServerPlayer deployer, String formationId)"));

        String practice = compact(methodSource(source, "private static void grantFormationPractice("));
        assertTrue(practice.contains("SectMissionGenerator.onFormationDeployed(deployer)"),
                "successful formation practice must notify the generated-mission ledger");
    }

    private static void assertFormationNotificationOrder(String methodSource) {
        String method = compact(methodSource);
        int activation = method.indexOf("ACTIVE.put(");
        int persistence = method.indexOf("persistField(", activation);
        int notification = method.indexOf("grantFormationPractice(deployer,", persistence);
        int success = method.lastIndexOf("returntrue;");

        assertTrue(activation >= 0 && persistence > activation,
                "formation deployment must become active and persist before reporting success");
        assertTrue(notification > persistence && success > notification,
                "only a successfully persisted formation may notify SectMissionGenerator");
    }

    private static void assertAfter(String source, String marker, int boundary) {
        int index = source.indexOf(marker);
        assertTrue(index < 0 || index > boundary, marker + " must remain behind the duplicate-event guard");
    }

    private static void assertContainedWhenPresent(String source, String block, String marker) {
        if (source.contains(marker)) {
            assertTrue(block.contains(marker), marker + " must execute only for the first event discovery");
        }
    }

    private static String ifConditionContaining(String source, int markerIndex) {
        assertTrue(markerIndex >= 0, "missing condition marker");
        int ifStart = source.lastIndexOf("if(", markerIndex);
        assertTrue(ifStart >= 0, "missing if condition for marker");
        int openingParenthesis = ifStart + 2;
        int closingParenthesis = matchingDelimiter(source, openingParenthesis, '(', ')');
        return source.substring(openingParenthesis + 1, closingParenthesis);
    }

    private static int firstIndex(String source, String... markers) {
        int first = -1;
        for (String marker : markers) {
            int index = source.indexOf(marker);
            if (index >= 0 && (first < 0 || index < first)) {
                first = index;
            }
        }
        return first;
    }

    private static String readSource(String directory, String fileName) throws Exception {
        return Files.readString(JAVA_ROOT.resolve(Path.of(directory, fileName)));
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        assertTrue(openingBrace >= 0, "missing method body: " + declaration);
        int closingBrace = matchingDelimiter(source, openingBrace, '{', '}');
        return source.substring(start, closingBrace + 1);
    }

    private static String blockSource(String source, String marker) {
        int start = source.indexOf(marker);
        assertTrue(start >= 0, "missing source block: " + marker);
        int openingBrace = source.indexOf('{', start);
        assertTrue(openingBrace >= 0, "missing block body: " + marker);
        int closingBrace = matchingDelimiter(source, openingBrace, '{', '}');
        return source.substring(start, closingBrace + 1);
    }

    private static int matchingDelimiter(String source, int opening, char open, char close) {
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == open) {
                depth++;
            } else if (current == close && --depth == 0) {
                return index;
            }
        }
        throw new AssertionError("unterminated source delimiter at " + opening);
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
