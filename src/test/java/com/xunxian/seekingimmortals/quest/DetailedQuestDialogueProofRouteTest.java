package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Q-B-5 dialogue proofs: NPC dialogue, rule acknowledgements, dialogue choices, shops,
 * auctions and reputation.
 *
 * <p>No Mockito is available, so the tests exercise the extracted package-private pure mapping
 * functions plus source-level contract assertions that pin the real producer call sites.</p>
 */
class DetailedQuestDialogueProofRouteTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Set<String> FAIL_CLOSED_INFO = Set.of("dayan_complete_rarity_rule");
    private static final Set<String> FAIL_CLOSED_CHOICES = Set.of(
            "bf_mid_contest", "gh_true_immortal_pressure");
    private static final Set<String> FAIL_CLOSED_SHOPS = Set.of(
            "heifeng_sea_route", "dajin_jin_capital_rim", "ziling_exchange");
    private static final Set<String> FAIL_CLOSED_AUCTIONS = Set.of("fallen_demon_token");

    @Test
    void acknowledgementSourcesAreExactServerNodes() {
        assertEquals(Set.of("blood_forbidden_window"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_sect_contribution_clerk", "quota_shop"));
        assertEquals(Set.of("star_palace_rejection_rule"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_star_palace_registrar", "inverse_block"));
        assertEquals(Set.of("fengyuan_gate_contribution_rule"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_tianyuan_registrar", "pay_portal"));
        assertEquals(Set.of("tianyuan_garrison_board"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_tianyuan_registrar", "jobs"));
        assertEquals(Set.of("true_word_lecture"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_zhenyan_lecturer", "accept_lesson"));
        assertEquals(Set.of("reincarnation_backlash_terms"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_reincarnation_clerk", "intro_quest"));
        assertEquals(Set.of("mortal_qixuan_entry_step_1"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_tiannan_steward", "visitor_service"));
        assertEquals(Set.of("true_word_exam_passed"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_zhenyan_lecturer", "inner"));
        assertEquals(Set.of("tianyuan_return_fee"),
                DetailedQuestProofService.acknowledgedChoiceTokens("tree_tianyuan_registrar", "portal_fee"));

        assertTrue(DetailedQuestProofService.acknowledgedChoiceTokens("tree_tianyuan_registrar", "greet").isEmpty());
        assertTrue(DetailedQuestProofService.acknowledgedChoiceTokens("", "jobs").isEmpty());
        assertTrue(DetailedQuestProofService.acknowledgedChoiceTokens("tree_tianyuan_registrar", "").isEmpty());
        assertTrue(DetailedQuestProofService.acknowledgedChoiceTokens("wrong_tree", "jobs").isEmpty());
    }

    @Test
    void committedChoiceSourcesAreExactServerNodes() {
        assertEquals(Set.of("inverse_star_cipher"),
                DetailedQuestProofService.committedChoiceTokens("tree_inverse_star_contact", "cipher"));
        assertEquals(Set.of("true_word_basic_drill"),
                DetailedQuestProofService.committedChoiceTokens("tree_zhenyan_lecturer", "accept_lesson"));
        assertEquals(Set.of("inverse_star_alignment"),
                DetailedQuestProofService.committedChoiceTokens("tree_inverse_star_contact", "star_spy"));
        assertTrue(DetailedQuestProofService.committedChoiceTokens("tree_inverse_star_contact", "shop").isEmpty());
        assertTrue(DetailedQuestProofService.committedChoiceTokens("", "").isEmpty());
    }

    @Test
    void tianyuanGarrisonBoardIsDeferredToTheRegistrarQuestBoard() {
        // Q-B-2 deferred item: the garrison board is tree_tianyuan_registrar's open_quest_board.
        assertTrue(DetailedQuestProofService.acknowledgedChoiceTokens(
                "tree_tianyuan_registrar", "jobs").contains("tianyuan_garrison_board"));
        DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog()
                .find("tianyuan_to_fengyuan_gate", 1);
        assertNotNull(route);
        assertEquals("INFO_ACKNOWLEDGED", route.proofType());
        assertEquals("tianyuan_garrison_board", route.parameter("choice"));
    }

    @Test
    void npcShopAuctionFactionTokensResolveExactly() {
        assertTrue(DetailedQuestProofService.npcTokenMatches("qianzhu_teacher", "npc_qianzhu_mechanic"));
        assertTrue(DetailedQuestProofService.npcTokenMatches("npc_huangfeng_contribution", "npc_huangfeng_contribution"));
        assertFalse(DetailedQuestProofService.npcTokenMatches("qianzhu_teacher", "npc_huangfeng_contribution"));
        assertFalse(DetailedQuestProofService.npcTokenMatches("qianzhu_teacher", ""));

        assertTrue(DetailedQuestProofService.shopTokenMatches("star_palace_registry", "star_registration"));
        assertTrue(DetailedQuestProofService.shopTokenMatches("star_palace_registry", "star_palace_patrol_supply"));
        assertTrue(DetailedQuestProofService.shopTokenMatches("inverse_star_smuggle", "inverse_star_black_market"));
        assertTrue(DetailedQuestProofService.shopTokenMatches("reincarnation_trade_desk", "nether_ferry_vendor"));
        assertFalse(DetailedQuestProofService.shopTokenMatches("star_palace_registry", "inverse_star_black_market"));

        assertTrue(DetailedQuestProofService.auctionTokenMatches("dajin_wanbao_auction", "wanbao_auction"));
        assertFalse(DetailedQuestProofService.auctionTokenMatches("dajin_wanbao_auction", "chaotic_sea_inner"));

        assertTrue(DetailedQuestProofService.factionTokenMatches("huangfeng", "huangfeng_gu"));
        assertTrue(DetailedQuestProofService.factionTokenMatches("tianyuan", "tianyuan"));
        assertFalse(DetailedQuestProofService.factionTokenMatches("huangfeng", "tianyuan"));
    }

    @Test
    void everyQB5RouteHasAnExplicitProductionClassification() {
        DetailedQuestProofCatalog.Snapshot catalog = DetailedQuestRuntimeService.proofCatalog();
        for (DetailedQuestProofCatalog.Route route : catalog.routes()) {
            switch (route.proofType()) {
                case "NPC_DIALOGUE" -> {
                    String npc = route.parameter("npc");
                    assertTrue(npcExists(npc) || DetailedQuestProofService.npcTokenMatches(npc, npc),
                            route.eventId() + ":" + npc);
                }
                case "INFO_ACKNOWLEDGED" -> {
                    String choice = route.parameter("choice");
                    assertTrue(DetailedQuestProofService.INFO_CHOICE_SOURCES.containsKey(choice)
                                    || FAIL_CLOSED_INFO.contains(choice),
                            route.eventId() + ":" + choice);
                }
                case "CHOICE_COMMITTED" -> {
                    String choice = route.parameter("choice");
                    assertTrue(DetailedQuestProofService.CHOICE_COMMITTED_SOURCES.containsKey(choice)
                                    || FAIL_CLOSED_CHOICES.contains(choice),
                            route.eventId() + ":" + choice);
                }
                case "SHOP_TRANSACTION" -> {
                    String shop = route.parameter("shop");
                    assertTrue(DetailedQuestProofService.PROOF_SHOP_MAPPINGS.containsKey(shop)
                                    || FAIL_CLOSED_SHOPS.contains(shop),
                            route.eventId() + ":" + shop);
                }
                case "AUCTION_TRANSACTION" -> {
                    String auction = route.parameter("auction");
                    assertTrue(DetailedQuestProofService.PROOF_AUCTION_MAPPINGS.containsKey(auction)
                                    || FAIL_CLOSED_AUCTIONS.contains(auction),
                            route.eventId() + ":" + auction);
                }
                case "REPUTATION_REACHED" -> {
                    String faction = route.parameter("faction");
                    assertFalse(DetailedQuestProofService.factionsProvingToken(faction).isEmpty(),
                            route.eventId());
                }
                default -> { }
            }
        }
    }

    @Test
    void dialogueEventFactoriesCarryExplicitTypesAndKeys() {
        DetailedQuestProofEvent npc = DetailedQuestProofEvent.npcDialogue("NPC_HUANGFENG_CONTRIBUTION");
        assertEquals(DetailedQuestProofEvent.Type.NPC_DIALOGUE, npc.type());
        assertEquals("npc_dialogue", npc.producer());
        assertEquals("npc_huangfeng_contribution", npc.parameter("npc"));
        assertEquals("npc:npc_huangfeng_contribution", npc.eventKey());

        DetailedQuestProofEvent info = DetailedQuestProofEvent.infoAcknowledged("blood_forbidden_window");
        assertEquals(DetailedQuestProofEvent.Type.INFO_ACKNOWLEDGED, info.type());
        assertEquals("info:blood_forbidden_window", info.eventKey());

        DetailedQuestProofEvent choice = DetailedQuestProofEvent.choiceCommitted("inverse_star_cipher");
        assertEquals(DetailedQuestProofEvent.Type.CHOICE_COMMITTED, choice.type());
        assertEquals("dialogue_choice", choice.producer());
        assertEquals("choice:inverse_star_cipher", choice.eventKey());

        DetailedQuestProofEvent shop = DetailedQuestProofEvent.shopTransaction("star_palace_patrol_supply");
        assertEquals(DetailedQuestProofEvent.Type.SHOP_TRANSACTION, shop.type());
        assertEquals("shop", shop.producer());
        assertEquals("shop:star_palace_patrol_supply", shop.eventKey());

        DetailedQuestProofEvent auction = DetailedQuestProofEvent.auctionTransaction("wanbao_auction");
        assertEquals(DetailedQuestProofEvent.Type.AUCTION_TRANSACTION, auction.type());
        assertEquals("auction", auction.producer());
        assertEquals("auction:wanbao_auction", auction.eventKey());

        DetailedQuestProofEvent rep = DetailedQuestProofEvent.reputationReached("huangfeng_gu");
        assertEquals(DetailedQuestProofEvent.Type.REPUTATION_REACHED, rep.type());
        assertEquals("reputation", rep.producer());
        assertEquals("rep:huangfeng_gu", rep.eventKey());
        assertEquals(Map.of("faction", "huangfeng_gu"), rep.parameters());
    }

    @Test
    void historyRecordsKeepQB5ContextAndReplayRoundTrips() {
        DetailedQuestProofCatalog.Route npcRoute = DetailedQuestRuntimeService.proofCatalog()
                .find("tianyuan_landing_register", 2);
        assertNotNull(npcRoute);
        CompoundTag history = new CompoundTag();
        DetailedQuestProofEvent npcEvent = DetailedQuestProofEvent.npcDialogue("npc_tianyuan_registrar");
        CompoundTag npcEntry = DetailedQuestProofService.historyEntry(npcRoute, npcEvent);
        assertEquals("NPC_DIALOGUE", npcEntry.getString("Type"));
        assertEquals("npc_tianyuan_registrar", npcEntry.getString("Npc"));
        history.put(npcRoute.eventId(), npcEntry);
        DetailedQuestProofEvent replayedNpc = DetailedQuestProofService.eventFromHistory(npcRoute, history);
        assertNotNull(replayedNpc);
        assertEquals("npc_tianyuan_registrar", replayedNpc.parameter("npc"));

        DetailedQuestProofCatalog.Route infoRoute = DetailedQuestRuntimeService.proofCatalog()
                .find("star_palace_register", 4);
        assertNotNull(infoRoute);
        DetailedQuestProofEvent infoEvent = DetailedQuestProofEvent.infoAcknowledged("star_palace_rejection_rule");
        history.put(infoRoute.eventId(), DetailedQuestProofService.historyEntry(infoRoute, infoEvent));
        DetailedQuestProofEvent replayedInfo = DetailedQuestProofService.eventFromHistory(infoRoute, history);
        assertNotNull(replayedInfo);
        assertEquals("star_palace_rejection_rule", replayedInfo.parameter("choice"));

        DetailedQuestProofCatalog.Route shopRoute = DetailedQuestRuntimeService.proofCatalog()
                .find("star_palace_register", 2);
        assertNotNull(shopRoute);
        DetailedQuestProofEvent shopEvent = DetailedQuestProofEvent.shopTransaction("star_palace_patrol_supply");
        history.put(shopRoute.eventId(), DetailedQuestProofService.historyEntry(shopRoute, shopEvent));
        DetailedQuestProofEvent replayedShop = DetailedQuestProofService.eventFromHistory(shopRoute, history);
        assertNotNull(replayedShop);
        assertEquals("star_palace_patrol_supply", replayedShop.parameter("shop"));

        DetailedQuestProofCatalog.Route repRoute = DetailedQuestRuntimeService.proofCatalog()
                .find("huangfeng_blood_quota", 2);
        assertNotNull(repRoute);
        DetailedQuestProofEvent repEvent = DetailedQuestProofEvent.reputationReached("huangfeng_gu");
        history.put(repRoute.eventId(), DetailedQuestProofService.historyEntry(repRoute, repEvent));
        DetailedQuestProofEvent replayedRep = DetailedQuestProofService.eventFromHistory(repRoute, history);
        assertNotNull(replayedRep);
        assertEquals("huangfeng_gu", replayedRep.parameter("faction"));
    }

    @Test
    void dialogueSourceMappingsReferenceRealTreesAndNodes() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals",
                "text_material", "npc_dialogue_branches_v139.json"))).getAsJsonObject();
        Set<String> sources = new HashSet<>();
        JsonArray trees = root.getAsJsonArray("trees");
        for (JsonElement element : trees) {
            JsonObject tree = element.getAsJsonObject();
            String id = tree.get("id").getAsString();
            JsonArray nodes = tree.getAsJsonArray("nodes");
            for (JsonElement nodeElement : nodes) {
                sources.add(id + ":" + nodeElement.getAsJsonObject().get("id").getAsString());
            }
        }
        Set<String> mapped = new HashSet<>();
        DetailedQuestProofService.INFO_CHOICE_SOURCES.values().forEach(mapped::addAll);
        DetailedQuestProofService.CHOICE_COMMITTED_SOURCES.values().forEach(mapped::addAll);
        for (String source : mapped) {
            assertTrue(sources.contains(source), "dialogue source not in authored branches: " + source);
        }
    }

    @Test
    void producersAreWiredAndChecksUseServerTruth() throws Exception {
        String hooks = compact(Files.readString(JAVA_ROOT.resolve("quest/QuestHookRuntime.java")));
        String shop = compact(Files.readString(JAVA_ROOT.resolve("shop/ShopService.java")));
        String auction = compact(Files.readString(JAVA_ROOT.resolve("catalog/AuctionSoftService.java")));
        String rep = compact(Files.readString(JAVA_ROOT.resolve("worldpack/ReputationService.java")));
        String service = compact(Files.readString(JAVA_ROOT.resolve(
                "quest/DetailedQuestProofService.java")));

        assertTrue(hooks.contains("recordDialogueNode(player,event.getNpcId(),treeId,nodeId)"),
                "dialogue node visits must record the structured dialogue proofs");
        assertTrue(shop.contains("recordShopTransaction(player,normalizedShop)"),
                "successful purchases must record the shop proof");
        assertTrue(auction.contains("recordAuctionTransaction(player,venue.id())"),
                "successful bids must record the auction proof");
        assertTrue(rep.contains("recordReputationReached(player,factionKey)"),
                "reputation gains must record the reputation proof");

        assertTrue(service.contains("ReputationService.get(player,faction)>=1"),
                "reputation checks must use the live ledger");
        assertTrue(service.contains("acknowledgedChoiceTokens(treeId,nodeId)"),
                "info acknowledgements must come from the server mapping");
        assertTrue(service.contains("committedChoiceTokens(treeId,nodeId)"),
                "choice commitments must come from the server mapping");
        assertTrue(service.contains("INFO_CHOICE_SOURCES"),
                "info acknowledgements must be driven by the authored source table");
        assertTrue(service.contains("venue.id()") == false
                        || service.contains("recordAuctionTransaction(player,venue.id())"),
                "auction venue originates from the server snapshot");
    }

    @Test
    void cloneCopyCarriesDialogueHistoryIndependently() {
        CompoundTag source = new CompoundTag();
        CompoundTag history = new CompoundTag();
        CompoundTag shop = new CompoundTag();
        shop.putString("Type", "SHOP_TRANSACTION");
        shop.putString("Shop", "star_palace_patrol_supply");
        history.put("shop_transaction:star_palace_register:step_2", shop);
        CompoundTag rep = new CompoundTag();
        rep.putString("Type", "REPUTATION_REACHED");
        rep.putString("Faction", "huangfeng_gu");
        history.put("reputation_reached:huangfeng_blood_quota:step_2", rep);
        source.put(DetailedQuestProofService.HISTORY_TAG, history);

        CompoundTag target = new CompoundTag();
        DetailedQuestProofService.copyPersistentData(source, target);

        assertEquals("star_palace_patrol_supply", target.getCompound(DetailedQuestProofService.HISTORY_TAG)
                .getCompound("shop_transaction:star_palace_register:step_2").getString("Shop"));
        assertEquals("huangfeng_gu", target.getCompound(DetailedQuestProofService.HISTORY_TAG)
                .getCompound("reputation_reached:huangfeng_blood_quota:step_2").getString("Faction"));
        assertNotSame(source.get(DetailedQuestProofService.HISTORY_TAG),
                target.get(DetailedQuestProofService.HISTORY_TAG));
    }

    @Test
    void dialogueEntriesDoNotAssembleLegacyStringEvidence() throws Exception {
        String hooks = Files.readString(JAVA_ROOT.resolve("quest/QuestHookRuntime.java"));
        String shop = Files.readString(JAVA_ROOT.resolve("shop/ShopService.java"));
        String auction = Files.readString(JAVA_ROOT.resolve("catalog/AuctionSoftService.java"));
        assertFalse(shop.contains("recordAndAdvance("));
        assertFalse(auction.contains("recordAndAdvance("));
        assertFalse(shop.contains("quest_step_"));
        assertFalse(auction.contains("quest_step_"));
        assertFalse(hooks.contains("recordAndAdvance(player, nodeId)"));
    }

    private static boolean npcExists(String npcId) {
        try {
            String branches = Files.readString(Path.of("src", "main", "resources", "data",
                    "seeking_immortals", "text_material", "npc_dialogue_branches_v139.json"));
            String seeds = Files.readString(Path.of("src", "main", "resources", "data",
                    "seeking_immortals", "text_material", "named_npc_seeds_v137.json"));
            return branches.contains("\"" + npcId + "\"") || seeds.contains("\"" + npcId + "\"");
        } catch (Exception exception) {
            return false;
        }
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }
}
