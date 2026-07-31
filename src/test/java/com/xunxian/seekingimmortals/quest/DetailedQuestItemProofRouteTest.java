package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
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
 * Q-B-3 item proofs: item acquisition, crafting, alchemy and delivery events.
 *
 * <p>No Mockito is available, so the tests exercise the extracted package-private pure mapping
 * functions plus source-level contract assertions that pin the real producer call sites.</p>
 */
class DetailedQuestItemProofRouteTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Path BULK = Path.of("src", "main", "resources", "assets", "seeking_immortals",
            "catalog_bulk_items.json");

    @Test
    void itemRoutesResolveToRealProvingItems() {
        DetailedQuestProofCatalog.Snapshot catalog = DetailedQuestRuntimeService.proofCatalog();
        for (DetailedQuestProofCatalog.Route route : catalog.routes()) {
            if (!isItemDomain(route.proofType())) {
                continue;
            }
            String token = route.parameter(route.requiredParams().keySet().iterator().next());
            if ("ALCHEMY_COMPLETED".equals(route.proofType())) {
                assertNotNull(ItemCatalogService.resolveId(token), route.eventId() + " station must resolve");
                continue;
            }
            assertFalse(DetailedQuestProofService.itemsProvingToken(token).isEmpty(),
                    route.eventId() + " has no proving item set");
            for (String proving : DetailedQuestProofService.itemsProvingToken(token)) {
                assertFalse(proving.isBlank(), route.eventId());
            }
        }
    }

    @Test
    void aliasTokensCollapseToTheirCanonicalCarriers() {
        assertTrue(DetailedQuestProofService.itemsProvingToken("jiao_pearl").contains("water_pearl"));
        assertTrue(DetailedQuestProofService.itemsProvingToken("lingzhu_fruit").contains("fire_spirit_fruit"));
        assertEquals("water_pearl", ItemCatalogService.resolveId("jiao_pearl"));
        assertEquals("fire_spirit_fruit", ItemCatalogService.resolveId("lingzhu_fruit"));
    }

    @Test
    void conceptTokensMapToRealServerObservableItems() throws Exception {
        assertEquals(Set.of("blood_forbidden_herb", "spirit_herb_bundle"),
                DetailedQuestProofService.itemsProvingToken("spirit_herb"));
        assertEquals(Set.of("void_palace_map_fragment"),
                DetailedQuestProofService.itemsProvingToken("xutian_map_fragment"));
        assertEquals(Set.of("spirit_recovery_pill", "detox_minor_pill", "escape_talisman",
                        "fire_talisman", "speed_talisman"),
                DetailedQuestProofService.itemsProvingToken("survival_preparation"));
        assertEquals(Set.of("yang_flame_talisman"),
                DetailedQuestProofService.itemsProvingToken("fire_resist_ready"));
        assertEquals(Set.of("yang_flame_talisman", "detox_minor_pill"),
                DetailedQuestProofService.itemsProvingToken("fire_toad_resistance"));
        assertEquals(Set.of("spirit_realm_gate_pass", "spirit_realm_gate_voucher"),
                DetailedQuestProofService.itemsProvingToken("realm_gate_token"));

        Set<String> mapped = new HashSet<>();
        DetailedQuestProofService.PROOF_ITEM_MAPPINGS.values().forEach(mapped::addAll);
        for (String item : mapped) {
            assertTrue(bulkIds().contains(item)
                            || ModItemsSource().contains("\"" + item + "\""),
                    "mapped prover is not a real carrier: " + item);
        }
    }

    @Test
    void identityTokensStayFailClosedWithoutRealCarriers() {
        Set<String> all = new HashSet<>();
        DetailedQuestProofService.PROOF_ITEM_MAPPINGS.values().forEach(all::addAll);
        assertFalse(all.isEmpty());
        // "survival_preparation" itself is not a real item: only its mapped kit may prove it.
        assertFalse(DetailedQuestProofService.itemsProvingToken("survival_preparation").contains("survival_preparation"));
        // identity of a real new carrier is allowed
        assertTrue(DetailedQuestProofService.itemsProvingToken("tai_yang_jing_huo").contains("tai_yang_jing_huo"));
    }

    @Test
    void routeItemMatchingAcceptsOnlyProvingItems() {
        assertTrue(DetailedQuestProofService.routeItemMatches("jiao_pearl", "water_pearl"));
        assertTrue(DetailedQuestProofService.routeItemMatches("spirit_herb", "blood_forbidden_herb"));
        assertTrue(DetailedQuestProofService.routeItemMatches("lingzhu_fruit", "fire_spirit_fruit"));
        assertTrue(DetailedQuestProofService.routeItemMatches("tai_yang_jing_huo", "tai_yang_jing_huo"));
        assertTrue(DetailedQuestProofService.routeItemMatches("realm_gate_token", "spirit_realm_gate_pass"));
        assertFalse(DetailedQuestProofService.routeItemMatches("jiao_pearl", "dayan_fragment"));
        assertFalse(DetailedQuestProofService.routeItemMatches("spirit_herb", "water_pearl"));
        assertFalse(DetailedQuestProofService.routeItemMatches("fire_resist_ready", "spirit_recovery_pill"));
        assertFalse(DetailedQuestProofService.routeItemMatches("xutian_map_fragment", ""));
        assertFalse(DetailedQuestProofService.routeItemMatches("xutian_map_fragment", "dayan_fragment"));
    }

    @Test
    void itemEventFactoriesCarryExplicitTypesAndKeys() {
        DetailedQuestProofEvent acquired = DetailedQuestProofEvent.itemAcquired("WATER_PEARL");
        assertEquals(DetailedQuestProofEvent.Type.ITEM_ACQUIRED, acquired.type());
        assertEquals("item_pickup", acquired.producer());
        assertEquals("water_pearl", acquired.parameter("item"));
        assertEquals("item:water_pearl", acquired.eventKey());
        assertEquals(Map.of("item", "water_pearl"), acquired.parameters());

        DetailedQuestProofEvent crafted = DetailedQuestProofEvent.itemCrafted("huiyang_true_water");
        assertEquals(DetailedQuestProofEvent.Type.CRAFT_COMPLETED, crafted.type());
        assertEquals("crafting", crafted.producer());
        assertEquals("craft:huiyang_true_water", crafted.eventKey());

        DetailedQuestProofEvent delivered = DetailedQuestProofEvent.itemDelivered("gray_realm_clue");
        assertEquals(DetailedQuestProofEvent.Type.ITEM_DELIVERED, delivered.type());
        assertEquals("item_delivery", delivered.producer());
        assertEquals("deliver:gray_realm_clue", delivered.eventKey());

        DetailedQuestProofEvent alchemy = DetailedQuestProofEvent.alchemyCompleted("alchemy_furnace_g3");
        assertEquals(DetailedQuestProofEvent.Type.ALCHEMY_COMPLETED, alchemy.type());
        assertEquals("alchemy", alchemy.producer());
        assertEquals("alchemy:alchemy_furnace_g3", alchemy.eventKey());
        assertEquals("alchemy_furnace_g3", alchemy.parameter("station"));
    }

    @Test
    void historyRecordsKeepTheItemContextAndReplayRoundTrips() {
        DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog()
                .find("xutian_window_prepare", 1);
        assertNotNull(route);
        DetailedQuestProofEvent event = DetailedQuestProofEvent.itemAcquired("void_palace_map_fragment");
        CompoundTag entry = DetailedQuestProofService.historyEntry(route, event);
        assertEquals("ITEM_ACQUIRED", entry.getString("Type"));
        assertEquals("void_palace_map_fragment", entry.getString("Item"));
        assertEquals("item:void_palace_map_fragment", entry.getString("EventKey"));

        CompoundTag history = new CompoundTag();
        history.put(route.eventId(), entry);
        assertTrue(DetailedQuestProofService.hasHistoryEntry(history, route.eventId()));

        DetailedQuestProofEvent replayed = DetailedQuestProofService.eventFromHistory(route, history);
        assertNotNull(replayed);
        assertEquals(DetailedQuestProofEvent.Source.HISTORY, replayed.source());
        assertEquals("void_palace_map_fragment", replayed.parameter("item"));
        assertEquals("item:void_palace_map_fragment", replayed.eventKey());
        assertEquals("void_palace_map_fragment", replayed.eventKey().substring("item:".length()));
    }

    @Test
    void alchemyHistoryKeepsTheStationAndStationAliasesCollapse() {
        DetailedQuestProofCatalog.Route route = DetailedQuestRuntimeService.proofCatalog()
                .find("peiying_material_hunt", 3);
        assertNotNull(route);
        assertEquals("alchemy_furnace_g3", route.parameter("station"));
        assertEquals(ItemCatalogService.resolveId("alchemy_furnace_g3"),
                ItemCatalogService.resolveId("alchemy_furnace"));

        DetailedQuestProofEvent event = DetailedQuestProofEvent.alchemyCompleted("alchemy_furnace_g3");
        CompoundTag entry = DetailedQuestProofService.historyEntry(route, event);
        assertEquals("alchemy_furnace_g3", entry.getString("Station"));
        CompoundTag history = new CompoundTag();
        history.put(route.eventId(), entry);
        DetailedQuestProofEvent replayed = DetailedQuestProofService.eventFromHistory(route, history);
        assertNotNull(replayed);
        assertEquals("alchemy_furnace_g3", replayed.parameter("station"));
    }

    @Test
    void deliveryNpcMatchingRequiresGiverOrCurrentPlace() {
        DetailedQuestRuntimeService.Chain chain = DetailedQuestRuntimeService.find("huangfeng_blood_quota")
                .orElse(null);
        assertNotNull(chain);
        assertTrue(DetailedQuestProofService.deliveryNpcMatches(chain, 1, "npc_huangfeng_contribution"));
        assertFalse(DetailedQuestProofService.deliveryNpcMatches(chain, 1, "npc_heavenly_court_inspector"));
        assertFalse(DetailedQuestProofService.deliveryNpcMatches(chain, 1, ""));
        assertFalse(DetailedQuestProofService.deliveryNpcMatches(chain, 99, "npc_huangfeng_contribution"));
        assertFalse(DetailedQuestProofService.deliveryNpcMatches(null, 1, "npc_huangfeng_contribution"));

        DetailedQuestRuntimeService.Chain zhenyan = DetailedQuestRuntimeService.find("zhenyan_outer_lesson")
                .orElse(null);
        assertNotNull(zhenyan);
        assertTrue(DetailedQuestProofService.deliveryNpcMatches(zhenyan, 1, "npc_zhenyan_elder_template"));
    }

    @Test
    void producersAreWiredAndLegacyItemStringsAreGone() throws Exception {
        String hooks = compact(Files.readString(JAVA_ROOT.resolve("quest/QuestHookRuntime.java")));
        String furnace = compact(Files.readString(JAVA_ROOT.resolve(
                "block/entity/AlchemyFurnaceBlockEntity.java")));
        String service = compact(Files.readString(JAVA_ROOT.resolve(
                "quest/DetailedQuestProofService.java")));

        assertTrue(hooks.contains("DetailedQuestProofService.recordItemCrafted(player,itemId)"),
                "crafting must record the structured craft proof");
        assertTrue(hooks.contains("DetailedQuestProofService.recordItemAcquired(player,itemId)"),
                "pickup must record the structured acquisition proof");
        assertTrue(hooks.contains("DetailedQuestProofService.recordItemDelivered(player,npcId)"),
                "turn-in must record the structured delivery proof");
        assertTrue(furnace.contains("recordAlchemyCompleted(player,\"alchemy_furnace_g\"+Math.min(5,Math.max(1,getFurnaceTier())))"),
                "furnace output collection must record the alchemy proof");

        assertFalse(hooks.contains("recordAndAdvance(player,itemId)"),
                "Q-B-3 pickup/craft entries must not use the legacy recordAndAdvance string path");

        assertTrue(service.contains("heldProvingItem(player,route.parameter(\"item\"))"),
                "item proofs must re-check the real server inventory");
        assertTrue(service.contains("event.source()==DetailedQuestProofEvent.Source.HISTORY"),
                "replay must use the server-recorded history fact");
        assertTrue(service.contains("hasLedger(player,ledgerKey)") && service.contains("handledChains.add(route.chainId())"),
                "ledger-first ordering must exist");
        int ledger = service.indexOf("hasLedger(player,ledgerKey)");
        int handled = service.indexOf("handledChains.add(route.chainId())");
        assertTrue(ledger >= 0 && handled > ledger,
                "the ledger check must run before the one-step-per-chain slot is consumed");
    }

    @Test
    void newQB3CarriersAreRegisteredBulkItemsWithBilingualNames() throws Exception {
        Set<String> bulk = bulkIds();
        for (String id : List.of("tai_yang_jing_huo", "five_cold_flames", "silver_tadpole_script",
                "dragon_scale_fruit", "blank_letter", "gray_realm_clue")) {
            assertTrue(bulk.contains(id), "missing bulk carrier " + id);
        }
        String zh = Files.readString(Path.of("src", "main", "resources", "assets", "seeking_immortals",
                "lang", "zh_cn.json"));
        String en = Files.readString(Path.of("src", "main", "resources", "assets", "seeking_immortals",
                "lang", "en_us.json"));
        for (String id : List.of("tai_yang_jing_huo", "five_cold_flames", "silver_tadpole_script",
                "dragon_scale_fruit", "blank_letter", "gray_realm_clue")) {
            assertTrue(zh.contains("\"item.seeking_immortals." + id + "\""), "zh name " + id);
            assertTrue(en.contains("\"item.seeking_immortals." + id + "\""), "en name " + id);
            assertTrue(zh.contains("\"tooltip.seeking_immortals.material." + id + "\""), "zh tooltip " + id);
            assertTrue(en.contains("\"tooltip.seeking_immortals.material." + id + "\""), "en tooltip " + id);
        }
    }

    @Test
    void cloneCopyCarriesItemHistoryIndependently() {
        CompoundTag source = new CompoundTag();
        CompoundTag history = new CompoundTag();
        CompoundTag record = new CompoundTag();
        record.putString("Type", "ITEM_ACQUIRED");
        record.putString("Item", "water_pearl");
        record.putString("EventKey", "item:water_pearl");
        history.put("item_acquired:nangong_wan_weight_optional:step_3", record);
        source.put(DetailedQuestProofService.HISTORY_TAG, history);

        CompoundTag target = new CompoundTag();
        DetailedQuestProofService.copyPersistentData(source, target);

        CompoundTag copied = target.getCompound(DetailedQuestProofService.HISTORY_TAG)
                .getCompound("item_acquired:nangong_wan_weight_optional:step_3");
        assertEquals("water_pearl", copied.getString("Item"));
        assertNotSame(source.get(DetailedQuestProofService.HISTORY_TAG),
                target.get(DetailedQuestProofService.HISTORY_TAG));
    }

    private static boolean isItemDomain(String proofType) {
        return "ITEM_ACQUIRED".equals(proofType) || "CRAFT_COMPLETED".equals(proofType)
                || "ITEM_DELIVERED".equals(proofType) || "ALCHEMY_COMPLETED".equals(proofType);
    }

    private static Set<String> bulkIds() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(BULK)).getAsJsonObject();
        JsonArray items = root.getAsJsonArray("items");
        Set<String> ids = new HashSet<>();
        for (JsonElement element : items) {
            ids.add(element.getAsJsonObject().get("id").getAsString());
        }
        return ids;
    }

    private static String ModItemsSource() throws Exception {
        return Files.readString(JAVA_ROOT.resolve("registry/ModItems.java"));
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }
}
