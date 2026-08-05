package com.xunxian.seekingimmortals.alchemy;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y-C: 窟外协作炼丹 closes the peiying_material_hunt chain. Authored rules under test —
 * 「活的才是丹」(a live carrier must always out-brew degraded material), the authored band
 * `peiying_dan_craft_chance: [0.15, 0.35]`, and 「协作炼丹是富成气质的可玩化」(on-site partners
 * raise the odds and share the credit, without inflating the pill economy).
 */
class PeiyingCoopAlchemyServiceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Path ALCHEMY_DATA = Path.of(
            "src", "main", "resources", "data", "seeking_immortals", "alchemy");

    @Test
    void authoredCraftChanceBandHoldsForEveryInput() {
        // The author gives one numeric contract: peiying_dan_craft_chance [0.15, 0.35].
        assertEquals(0.15D, PeiyingCoopAlchemyService.AUTHORED_MIN_CHANCE);
        assertEquals(0.35D, PeiyingCoopAlchemyService.AUTHORED_MAX_CHANCE);

        for (double base = 0.0D; base <= 1.0D; base += 0.05D) {
            for (int partners = 0; partners <= 8; partners++) {
                double live = PeiyingCoopAlchemyService.resolveSuccessRate(base, true, partners);
                double dead = PeiyingCoopAlchemyService.resolveSuccessRate(base, false, partners);

                assertTrue(live >= PeiyingCoopAlchemyService.AUTHORED_MIN_CHANCE
                                && live <= PeiyingCoopAlchemyService.AUTHORED_MAX_CHANCE,
                        "live rate escaped the authored band: " + live);
                assertTrue(dead >= PeiyingCoopAlchemyService.AUTHORED_MIN_CHANCE
                                && dead <= PeiyingCoopAlchemyService.AUTHORED_MAX_CHANCE,
                        "degraded rate escaped the authored band: " + dead);
                // 「活的才是丹。死的，只是一堆打折的肉」— degraded may never match live material.
                assertTrue(live > dead,
                        "degraded material must never brew as well as a live carrier"
                                + " (base=" + base + ", partners=" + partners + ")");
                assertTrue(dead <= PeiyingCoopAlchemyService.DEGRADED_MAX_CHANCE,
                        "degraded material must stay under its own ceiling");
            }
        }
    }

    @Test
    void coopPartnersHelpButAreBoundedAndDeterministic() {
        double solo = PeiyingCoopAlchemyService.resolveSuccessRate(0.15D, true, 0);
        double pair = PeiyingCoopAlchemyService.resolveSuccessRate(0.15D, true, 1);
        double capped = PeiyingCoopAlchemyService.resolveSuccessRate(0.15D, true,
                PeiyingCoopAlchemyService.MAX_COOP_PARTNERS);
        double overCapped = PeiyingCoopAlchemyService.resolveSuccessRate(0.15D, true,
                PeiyingCoopAlchemyService.MAX_COOP_PARTNERS + 5);

        assertTrue(pair > solo, "an on-site partner must actually help");
        assertTrue(capped > pair);
        assertEquals(capped, overCapped, "extra bodies past the cap must not stack");
        // Negative/garbage participant counts must not be able to lower the floor.
        assertEquals(solo, PeiyingCoopAlchemyService.resolveSuccessRate(0.15D, true, -4));
        // Pure function: same inputs, same output.
        assertEquals(pair, PeiyingCoopAlchemyService.resolveSuccessRate(0.15D, true, 1));
    }

    @Test
    void recipeIsReachableAtTheAuthoredStationTier() throws Exception {
        String recipe = Files.readString(ALCHEMY_DATA.resolve("recipes/peiying_dan.json"));

        // Route step 3 proves station "alchemy_furnace_g3"; only g1/g2/g3 alias onto the real
        // furnace block, so a tier-5 requirement would make the authored step unreachable.
        assertTrue(recipe.contains("\"required_furnace_tier\": 3"),
                "the coop route must be brewable at the authored alchemy_furnace_g3 station");
        String aliases = Files.readString(JAVA_ROOT.resolve("catalog/ItemCatalogService.java"));
        assertTrue(aliases.contains("putAlias(aliases, \"alchemy_furnace_g3\", \"alchemy_furnace\")"),
                "station proof resolution must still collapse g3 onto the real furnace");

        // 培婴丹 is the shipped nascent_soul_pill carrier (authored display 培婴丹); the coop
        // route must not mint a second item that renders under the same name.
        assertTrue(recipe.contains("seeking_immortals:nascent_soul_pill"),
                "the coop route must output the shipped 培婴丹 carrier");
        assertFalse(recipe.contains("seeking_immortals:peiying_dan"),
                "a duplicate 培婴丹 item must not be introduced");

        // The live carrier is the main ingredient, and the authored floor is the recipe baseline.
        assertTrue(recipe.contains("seeking_immortals:live_beast_carrier"),
                "the 阴芝马 carrier must be the main ingredient");
        assertTrue(recipe.contains("\"success_rate\": 0.15"),
                "the recipe baseline must be the authored floor, not a hand-picked number");

        // It has to actually ship: the packaged manifest is the loader's source of truth.
        String manifest = Files.readString(ALCHEMY_DATA.resolve("recipe_manifest.json"));
        assertTrue(manifest.contains("\"peiying_dan\""),
                "an unlisted recipe file is never loaded");
    }

    @Test
    void carrierStateIsReadFromTheStackThatWillActuallyBeConsumed() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("alchemy/PeiyingCoopAlchemyService.java"));

        // AlchemyRecipeService.removeItems consumes the first matching stack in
        // inventory.items; the state snapshot must mirror that order or the applied bonus
        // could describe a different carrier than the one burned.
        assertTrue(service.contains("player.getInventory().items"),
                "the snapshot must scan the same compartment the consume path scans");
        assertTrue(service.contains("break"),
                "the snapshot must stop at the first matching carrier, like removeItems does");
        assertTrue(service.contains("LiveCaptureCarrierService.isCarrier(")
                        && service.contains("LiveCaptureCarrierService.isLive("),
                "live vs degraded must come from the carrier NBT, not the item id");

        // The override is recipe-scoped; every other recipe keeps its existing success maths.
        String furnace = Files.readString(JAVA_ROOT.resolve("block/entity/AlchemyFurnaceBlockEntity.java"));
        assertTrue(furnace.contains("PeiyingCoopAlchemyService.isCoopRecipe(recipe)"),
                "the coop band must apply only to the authored coop recipe");
        int snapshot = furnace.indexOf("PeiyingCoopAlchemyService.snapshotCarrierLive(");
        int consume = furnace.indexOf("AlchemyRecipeService.consumeInputs(player, recipe)");
        assertTrue(snapshot >= 0 && consume >= 0 && snapshot < consume,
                "the carrier state must be read before the ingredients are consumed");
    }

    @Test
    void coopCreditRequiresPresenceAtBothStartAndSettlement() throws Exception {
        String furnace = Files.readString(JAVA_ROOT.resolve("block/entity/AlchemyFurnaceBlockEntity.java"));

        // Participants are snapshot when the batch starts (they invested the time) and
        // re-confirmed on collection, so a passer-by cannot harvest the proof.
        assertTrue(furnace.contains("coopParticipants"),
                "the on-site participant snapshot must be furnace state");
        assertTrue(furnace.contains("tag.put(\"CoopParticipants\"")
                        && furnace.contains("tag.getList(\"CoopParticipants\""),
                "the snapshot must survive relog/chunk unload with the batch");
        assertTrue(furnace.contains("PeiyingCoopAlchemyService.creditCoopParticipants("),
                "settlement must credit the confirmed participants");

        String service = Files.readString(JAVA_ROOT.resolve("alchemy/PeiyingCoopAlchemyService.java"));
        // The proof is the authored SOLO_OR_PARTY step-3 credit, recorded server-side per player.
        assertTrue(service.contains("recordAlchemyCompleted("),
                "partners must receive the same structured station proof");
        assertTrue(service.contains("giveOrEnqueue("),
                "any partner payout must go through the recoverable outbox");
        // Economy guard: the pill itself is not duplicated per partner.
        assertFalse(service.contains("recipe.outputCount() * "),
                "coop must not multiply the 培婴丹 output");
    }

    @Test
    void liveCarriersAreTickedInEveryCompartmentTheyCanRestIn() throws Exception {
        String events = Files.readString(JAVA_ROOT.resolve("event/ModEvents.java"));

        // Y-B only ticked the main inventory, so a carrier parked in the offhand or an
        // armour slot never timed out. Transit pressure must not be dodgeable.
        assertTrue(compact(events).contains("LiveCaptureCarrierService.tickCarriedTransit(serverPlayer"),
                "the transit tick must cover every compartment through one helper");

        String carrier = Files.readString(JAVA_ROOT.resolve("beast/LiveCaptureCarrierService.java"));
        assertTrue(carrier.contains("player.getInventory().items")
                        && carrier.contains("player.getInventory().offhand")
                        && carrier.contains("player.getInventory().armor"),
                "all three player inventory compartments must be swept");
        assertTrue(carrier.contains("static int tickCarriedTransit(")
                        && carrier.contains("static int degradeAllCarried("),
                "timeout and death paths must share the same compartment sweep");
    }

    /** Line breaks in a wrapped call must not decide whether a contract assertion holds. */
    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
