package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailedQuestProofCatalogTest {
    private static final Path ROUTES = Path.of("src", "main", "resources", "data", "seeking_immortals",
            "text_material", "detailed_quest_proof_routes.json");
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern EVENT = Pattern.compile(
            "[a-z][a-z0-9_]*:[a-z][a-z0-9_]*:step_[1-9][0-9]*");
    private static final Map<String, String> PARAMETER_BY_TYPE = Map.ofEntries(
            Map.entry("REGION_ENTER", "region"), Map.entry("DIMENSION_ENTER", "dimension"),
            Map.entry("STRUCTURE_FORMED", "structure"), Map.entry("NPC_DIALOGUE", "npc"),
            Map.entry("ITEM_ACQUIRED", "item"), Map.entry("ITEM_DELIVERED", "item"),
            Map.entry("CRAFT_COMPLETED", "item"), Map.entry("ALCHEMY_COMPLETED", "station"),
            Map.entry("ENTITY_KILLED", "entity"), Map.entry("ENTITY_CAPTURED_ALIVE", "entity"),
            Map.entry("ENCOUNTER_CLEARED", "region"), Map.entry("ESCORT_COMPLETED", "region"),
            Map.entry("METHOD_LAYER_REACHED", "method"), Map.entry("REALM_REACHED", "realm"),
            Map.entry("TECHNIQUE_LEARNED", "technique"), Map.entry("SHOP_TRANSACTION", "shop"),
            Map.entry("AUCTION_TRANSACTION", "auction"), Map.entry("REPUTATION_REACHED", "faction"),
            Map.entry("CHOICE_COMMITTED", "choice"), Map.entry("INFO_ACKNOWLEDGED", "choice"),
            Map.entry("SPIRIT_ROOT_TESTED", "item"));

    @Test
    void allTwentyThreeChainsAndNinetyFiveStepsHaveExactlyOneRoute() {
        DetailedQuestRuntimeService.Snapshot chains = DetailedQuestRuntimeService.builtin();
        DetailedQuestProofCatalog.Snapshot catalog = DetailedQuestRuntimeService.proofCatalog();

        assertEquals(23, chains.chains().size());
        assertEquals(95, chains.stepCount());
        assertEquals(23, catalog.chainIds().size());
        assertEquals(95, catalog.routeCount());
        assertEquals(95, catalog.stepCount());

        Set<String> keys = new HashSet<>();
        for (DetailedQuestRuntimeService.Chain chain : chains.chains().values()) {
            assertTrue(catalog.chainIds().contains(chain.id()), chain.id());
            for (DetailedQuestRuntimeService.Step step : chain.steps()) {
                String key = chain.id() + ":" + step.number();
                assertTrue(keys.add(key), "duplicate source step " + key);
                DetailedQuestProofCatalog.Route route = catalog.find(chain.id(), step.number());
                assertNotNull(route, "missing proof route " + key);
                assertEquals(key, route.chainId() + ":" + route.step());
            }
        }
        assertEquals(95, keys.size());
    }

    @Test
    void routePoliciesTypesParametersAndProducersAreStrictlyClassified() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(ROUTES)).getAsJsonObject();
        JsonArray routes = root.getAsJsonArray("routes");
        Set<String> eventIds = new HashSet<>();
        assertEquals(1, root.get("schema_version").getAsInt());
        assertEquals("quest_chains_playable_v141.json", root.get("source").getAsString());
        assertEquals(95, routes.size());

        for (JsonElement element : routes) {
            JsonObject route = element.getAsJsonObject();
            String chain = route.get("chain_id").getAsString();
            int step = route.get("step").getAsInt();
            String type = route.get("proof_type").getAsString();
            String eventId = route.get("event_id").getAsString();
            assertTrue(ID.matcher(chain).matches(), chain);
            assertTrue(EVENT.matcher(eventId).matches(), eventId);
            assertTrue(eventIds.add(eventId), "duplicate event id " + eventId);
            assertEquals(type.toLowerCase() + ":" + chain + ":step_" + step, eventId);
            assertEquals("PLAYER", route.get("owner_policy").getAsString());
            assertEquals("SOLO_OR_PARTY", route.get("party_policy").getAsString());
            assertEquals("NONE", route.get("consume_policy").getAsString());
            assertEquals("IDEMPOTENT", route.get("repeat_policy").getAsString());
            assertFalse("ADMIN_ONLY".equals(route.get("owner_policy").getAsString()));
            assertFalse("ADMIN_ONLY".equals(route.get("party_policy").getAsString()));
            assertTrue(route.get("allow_history_replay").isJsonPrimitive()
                    && route.getAsJsonPrimitive("allow_history_replay").isBoolean());
            assertEquals(Set.of(PARAMETER_BY_TYPE.get(type)), route.getAsJsonObject("required_params").keySet());
            String parameter = PARAMETER_BY_TYPE.get(type);
            assertNotNull(parameter, type);
            assertTrue(ID.matcher(route.getAsJsonObject("required_params").get(parameter).getAsString()).matches(),
                    type + ":" + parameter);
        }
        assertEquals(95, eventIds.size());
    }

    @Test
    void runtimeUsesTheSameStrictCatalogAndDoesNotAcceptAnUnroutedStep() {
        DetailedQuestProofCatalog.Snapshot catalog = DetailedQuestRuntimeService.proofCatalog();
        assertTrue(catalog.covers("mortal_qixuan_entry", 1));
        assertTrue(catalog.covers("deity_huoyu_path", 9));
        assertFalse(catalog.covers("mortal_qixuan_entry", 0));
        assertFalse(catalog.covers("missing_chain", 1));
        assertEquals("yin_zhi_horse", catalog.find("peiying_material_hunt", 2)
                .parameter("entity"));
        DetailedQuestProofCatalog.Route mortal = catalog.find("mortal_qixuan_entry", 4);
        assertEquals("changchun_gong", mortal.parameter("method"));
        assertEquals(1, mortal.minimumLayer());
        assertEquals("qi_refining", mortal.minimumRealm());
        assertEquals("great_five_elements_world_art",
                catalog.find("wuxing_intro", 3).parameter("method"));
        assertEquals("fansheng_zhenmogong",
                catalog.find("deity_huoyu_path", 3).parameter("method"));
        assertEquals("body_integration",
                catalog.find("guanghan_endgame_path", 5).parameter("realm"));
        assertEquals("deity_transformation",
                catalog.find("deity_huoyu_path", 8).parameter("realm"));
    }
}
