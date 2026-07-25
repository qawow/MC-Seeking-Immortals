package com.xunxian.seekingimmortals.shop;

import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void clearsPersistedStockCacheWhenServerStops() throws IOException, ReflectiveOperationException {
        String events = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/event/ModEvents.java"));
        String shop = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/shop/ShopService.java"));
        Field field = ShopService.class.getDeclaredField("STOCK_CACHE");
        field.setAccessible(true);
        Map<String, Object> stockCache = (Map<String, Object>) field.get(null);
        stockCache.put("world-a/test-shop/test-entry", new Object());

        assertTrue(events.contains("ShopService.clearRuntimeStockCache();"));
        assertTrue(shop.contains("releaseStock(player, shopId, entry);"));
        assertTrue(shop.contains("persistStock(player.serverLevel(), context.key(), state);"));
        ShopService.clearRuntimeStockCache();
        assertTrue(stockCache.isEmpty());
    }

    @Test
    void marketShopCountCoversAllMerchantShops() {
        // Text merchant_shops has 46 ids; 2 are contribution halls and stay out of MARKET_SHOPS.
        assertTrue(ShopService.marketShopCount() >= 44,
                "expected >=44 market shops, got " + ShopService.marketShopCount());
        assertTrue(ShopService.isMarketShop("tiannan_talisman_lane"));
        assertTrue(ShopService.isMarketShop("dajin_spirit_stone_bank"));
        assertTrue(ShopService.isMarketShop("spirit_clan_tianhu_exchange"));
        assertFalse(ShopService.isMarketShop("qinglan_contribution_hall"));
    }

    @Test
    void parsesContributionShopEntries() {
        String json = """
                {
                  "id": "test_shop",
                  "currency": "sect_contribution",
                  "stock": 5,
                  "refresh_ticks": 1200,
                  "rank_min": "outer_disciple",
                  "entries": [
                    { "id": "a", "item": "spirit_grass", "count": 2, "cost": 30 },
                    {
                      "id": "b",
                      "item": "minecraft:stone",
                      "count": 1,
                      "cost": 1,
                      "currency": "item",
                      "currency_item": "immortal_jade",
                      "stock": 0,
                      "refresh_ticks": 2400,
                      "rank_min": null
                    }
                  ]
                }
                """;

        ShopService.Shop shop = ShopService.parseShopForTest(new StringReader(json));

        assertEquals("test_shop", shop.id());
        assertEquals(2, shop.entries().size());
        assertEquals("a", shop.entries().get(0).id());
        assertEquals("seeking_immortals:spirit_grass", shop.entries().get(0).itemId());
        assertEquals(2, shop.entries().get(0).count());
        assertEquals(30, shop.entries().get(0).cost());
        assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, shop.entries().get(0).currency());
        assertEquals(5, shop.entries().get(0).stock());
        assertEquals(1200, shop.entries().get(0).refreshTicks());
        assertEquals(ShopService.RANK_OUTER_DISCIPLE, shop.entries().get(0).rankMin());
        assertEquals("minecraft:stone", shop.entries().get(1).itemId());
        assertEquals(ShopService.CURRENCY_ITEM, shop.entries().get(1).currency());
        assertEquals("seeking_immortals:immortal_jade", shop.entries().get(1).currencyItemId());
        assertEquals(0, shop.entries().get(1).stock());
        assertEquals(2400, shop.entries().get(1).refreshTicks());
        assertEquals("", shop.entries().get(1).rankMin());
    }

    @Test
    void normalizesInvalidShopNumbersConservatively() {
        String json = """
                {
                  "id": "test_shop",
                  "currency": "item",
                  "currency_item": "minecraft:emerald",
                  "entries": [
                    { "id": "a", "item": "minecraft:stone", "count": 0, "cost": 0, "stock": -5, "refresh_ticks": -20 }
                  ]
                }
                """;

        ShopService.Entry entry = ShopService.parseShopForTest(new StringReader(json)).entries().get(0);

        assertEquals(1, entry.count());
        assertEquals(1, entry.cost());
        assertEquals(ShopService.UNLIMITED_STOCK, entry.stock());
        assertEquals(0, entry.refreshTicks());
    }

    @Test
    void checksSectShopRankRequirementsAgainstQuestStage() {
        ShopService.Entry ungated = new ShopService.Entry("grass", "seeking_immortals:spirit_grass", 1, 1,
                ShopService.CURRENCY_SECT_CONTRIBUTION, "", ShopService.UNLIMITED_STOCK, 0);
        ShopService.Entry inner = new ShopService.Entry("formula", "seeking_immortals:spirit_grass", 1, 1,
                ShopService.CURRENCY_SECT_CONTRIBUTION, "", ShopService.UNLIMITED_STOCK, 0, "inner_disciple");
        ShopService.Entry core = new ShopService.Entry("secret", "seeking_immortals:spirit_grass", 1, 1,
                ShopService.CURRENCY_SECT_CONTRIBUTION, "", ShopService.UNLIMITED_STOCK, 0, "core_disciple");

        assertTrue(ShopService.meetsRankRequirement(ungated, SectContributionService.STAGE_LOCKED));
        assertFalse(ShopService.meetsRankRequirement(inner, SectContributionService.STAGE_OUTER_DISCIPLE));
        assertTrue(ShopService.meetsRankRequirement(inner, SectContributionService.STAGE_INNER_DISCIPLE));
        assertFalse(ShopService.meetsRankRequirement(core, SectContributionService.STAGE_INNER_DISCIPLE));
        assertTrue(ShopService.meetsRankRequirement(core, SectContributionService.STAGE_PHASE10_COMPLETE));
        assertEquals("screen.seeking_immortals.sect.rank.inner", ShopService.rankDescriptionId(inner.rankMin()));
    }

    @Test
    void builtInMarketHerbalStallUsesItemCurrency() {
        ShopService.Shop shop = ShopService.getShop(ShopService.MARKET_HERBAL_STALL);

        assertEquals(ShopService.MARKET_HERBAL_STALL, shop.id());
        assertEquals(22, shop.entries().size());
        for (ShopService.Entry entry : shop.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:metal_spirit_stone", entry.currencyItemId());
        }
        assertEntry(shop, "spirit_grass_bundle", "seeking_immortals:spirit_grass");
        assertEntry(shop, "cloud_mushroom", "seeking_immortals:cloud_mushroom");
        assertEntry(shop, "spirit_mushroom", "seeking_immortals:cloud_mushroom");
        assertEntry(shop, "clear_spirit_powder", "seeking_immortals:clear_spirit_powder_low");
        assertEntry(shop, "spirit_gathering_pill", "seeking_immortals:spirit_gathering_pill");
        assertEntry(shop, "spirit_condense_pill", "seeking_immortals:spirit_gathering_pill");
        assertEntry(shop, "fasting_pill", "seeking_immortals:fasting_pill_low");
        assertEntry(shop, "bigu_pill", "seeking_immortals:fasting_pill_low");
        assertEntry(shop, "fire_burst_talisman", "seeking_immortals:fire_talisman");
        assertEntry(shop, "body_guard_talisman", "seeking_immortals:armor_talisman");
        assertEntry(shop, "demon_core_fragment", "seeking_immortals:beast_core");
        assertEntry(shop, "jiangchen_pill", "seeking_immortals:jiangchen_pill");
        assertEntry(shop, "recipe_jiangchen", "seeking_immortals:recipe_jiangchen");
        assertEntry(shop, "yellow_essence", "seeking_immortals:spirit_grass");
        assertEntry(shop, "ginseng_spirit", "seeking_immortals:immortal_ginseng");
        assertEntry(shop, "fire_sparrow_fruit", "seeking_immortals:phoenix_feather_flower");
        assertEntry(shop, "spirit_recovery_pill", "seeking_immortals:spirit_recovery_pill");
        assertEntry(shop, "cultivate_speed_pill", "seeking_immortals:cultivate_speed_pill");
        assertEntry(shop, "body_tempering_pill", "seeking_immortals:body_tempering_pill");
        assertEntry(shop, "huanglong_pill", "seeking_immortals:huanglong_pill");
        assertEntry(shop, "recipe_huanglong", "seeking_immortals:recipe_huanglong");
        assertEntry(shop, "heqi_pill", "seeking_immortals:heqi_pill");

        ShopService.Entry jiangchen = shop.find("jiangchen_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry jiangchen_pill"));
        assertEquals(120, jiangchen.cost());
        assertEquals(3, jiangchen.stock());

        ShopService.Entry spiritCondense = shop.find("spirit_condense_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry spirit_condense_pill"));
        assertEquals(45, spiritCondense.cost());
        assertEquals(5, spiritCondense.stock());

        ShopService.Entry bigu = shop.find("bigu_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry bigu_pill"));
        assertEquals(25, bigu.cost());
        assertEquals(8, bigu.stock());

        ShopService.Entry jiangchenFormula = shop.find("recipe_jiangchen")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_jiangchen"));
        assertEquals(25, jiangchenFormula.cost());
        assertEquals(4, jiangchenFormula.stock());

        ShopService.Entry huanglongFormula = shop.find("recipe_huanglong")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_huanglong"));
        assertEquals(45, huanglongFormula.cost());
        assertEquals(3, huanglongFormula.stock());

        ShopService.Entry heqi = shop.find("heqi_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry heqi_pill"));
        assertEquals(6, heqi.cost());
        assertEquals(8, heqi.stock());

        ShopService.Entry bodyTempering = shop.find("body_tempering_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry body_tempering_pill"));
        assertEquals(35, bodyTempering.cost());
        assertEquals(6, bodyTempering.stock());
    }

    @Test
    void builtInTextMaterialMarketShopsUseItemCurrency() {
        assertTrue(ShopService.isMarketShop(ShopService.MARKET_HERBAL_STALL));
        assertTrue(ShopService.isMarketShop(ShopService.TIANNAN_DEMONIC_DUAL_MARKET));
        assertTrue(ShopService.isMarketShop(ShopService.CHAOTIC_SEA_ISLAND_GENERAL));
        assertTrue(ShopService.isMarketShop(ShopService.INVERSE_STAR_BLACK_MARKET));
        assertTrue(ShopService.isMarketShop(ShopService.TIANNAN_REFINEMENT_FORGE));
        assertTrue(ShopService.isMarketShop(ShopService.STAR_PALACE_PATROL_SUPPLY));
        assertTrue(ShopService.isMarketShop(ShopService.QIXUAN_VILLAGE_STALL));
        assertTrue(ShopService.isMarketShop(ShopService.OUTER_SEA_PUBLIC_STALL));
        assertTrue(ShopService.isMarketShop(ShopService.TIANYUAN_MERIT_EXCHANGE));
        assertFalse(ShopService.isMarketShop(ShopService.QINGLAN_CONTRIBUTION_HALL));

        ShopService.Shop demonicDual = ShopService.getShop(ShopService.TIANNAN_DEMONIC_DUAL_MARKET);
        assertEquals(ShopService.TIANNAN_DEMONIC_DUAL_MARKET, demonicDual.id());
        assertEquals(1, demonicDual.entries().size());
        for (ShopService.Entry entry : demonicDual.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:metal_spirit_stone", entry.currencyItemId());
        }
        assertEntry(demonicDual, "heqi_pill", "seeking_immortals:heqi_pill");

        ShopService.Entry demonicHeqi = demonicDual.find("heqi_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry heqi_pill"));
        assertEquals(1, demonicHeqi.count());
        assertEquals(40, demonicHeqi.cost());
        assertEquals(6, demonicHeqi.stock());
        assertEquals(48000, demonicHeqi.refreshTicks());

        ShopService.Shop islandGeneral = ShopService.getShop(ShopService.CHAOTIC_SEA_ISLAND_GENERAL);
        assertEquals(ShopService.CHAOTIC_SEA_ISLAND_GENERAL, islandGeneral.id());
        assertEquals(10, islandGeneral.entries().size());
        for (ShopService.Entry entry : islandGeneral.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:metal_spirit_stone", entry.currencyItemId());
        }
        assertEntry(islandGeneral, "spirit_recovery_pill", "seeking_immortals:spirit_recovery_pill");
        assertEntry(islandGeneral, "soul_gathering_pill", "seeking_immortals:soul_gathering_pill");
        assertEntry(islandGeneral, "ningshen_pill", "seeking_immortals:calming_pill_low");
        assertEntry(islandGeneral, "yanghun_pill", "seeking_immortals:soul_gathering_pill");
        assertEntry(islandGeneral, "sea_calm_pill", "seeking_immortals:calming_pill_low");
        assertEntry(islandGeneral, "recipe_sea_calm", "seeking_immortals:alchemy_formula_calming_pill_jade");
        assertEntry(islandGeneral, "jiao_scale", "seeking_immortals:dragon_scale");
        assertEntry(islandGeneral, "deep_sea_cold_iron", "seeking_immortals:spirit_iron");
        assertEntry(islandGeneral, "anti_demon_talisman", "seeking_immortals:fire_talisman");
        assertEntry(islandGeneral, "star_palace_tax_receipt", "seeking_immortals:star_palace_tax_receipt");

        ShopService.Entry ningshen = islandGeneral.find("ningshen_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry ningshen_pill"));
        assertEquals(95, ningshen.cost());
        assertEquals(4, ningshen.stock());

        ShopService.Entry yanghun = islandGeneral.find("yanghun_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry yanghun_pill"));
        assertEquals(110, yanghun.cost());
        assertEquals(3, yanghun.stock());

        ShopService.Entry seaCalm = islandGeneral.find("sea_calm_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry sea_calm_pill"));
        assertEquals(35, seaCalm.cost());
        assertEquals(10, seaCalm.stock());

        ShopService.Entry seaCalmFormula = islandGeneral.find("recipe_sea_calm")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_sea_calm"));
        assertEquals(20, seaCalmFormula.cost());
        assertEquals(5, seaCalmFormula.stock());

        ShopService.Entry islandAntiDemon = islandGeneral.find("anti_demon_talisman")
                .orElseThrow(() -> new AssertionError("Missing shop entry anti_demon_talisman"));
        assertEquals(1, islandAntiDemon.count());
        assertEquals(65, islandAntiDemon.cost());
        assertEquals(10, islandAntiDemon.stock());
        assertEquals(48000, islandAntiDemon.refreshTicks());

        ShopService.Entry taxReceipt = islandGeneral.find("star_palace_tax_receipt")
                .orElseThrow(() -> new AssertionError("Missing shop entry star_palace_tax_receipt"));
        assertEquals(1, taxReceipt.count());
        assertEquals(5, taxReceipt.cost());
        assertEquals(20, taxReceipt.stock());
        assertEquals(24000, taxReceipt.refreshTicks());

        ShopService.Shop blackMarket = ShopService.getShop(ShopService.INVERSE_STAR_BLACK_MARKET);
        assertEquals(ShopService.INVERSE_STAR_BLACK_MARKET, blackMarket.id());
        assertEquals(3, blackMarket.entries().size());
        for (ShopService.Entry entry : blackMarket.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:metal_spirit_stone", entry.currencyItemId());
        }
        assertEntry(blackMarket, "demon_core_mid", "seeking_immortals:beast_core");
        assertEntry(blackMarket, "demon_heart_pill", "seeking_immortals:calming_pill_low");
        assertEntry(blackMarket, "recipe_demon_heart", "seeking_immortals:alchemy_formula_calming_pill_jade");

        ShopService.Entry demonHeartPill = blackMarket.find("demon_heart_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry demon_heart_pill"));
        assertEquals(180, demonHeartPill.cost());
        assertEquals(3, demonHeartPill.stock());
        assertEquals(48000, demonHeartPill.refreshTicks());

        ShopService.Entry demonHeartFormula = blackMarket.find("recipe_demon_heart")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_demon_heart"));
        assertEquals(350, demonHeartFormula.cost());
        assertEquals(4, demonHeartFormula.stock());
        assertEquals(48000, demonHeartFormula.refreshTicks());

        ShopService.Shop refinementForge = ShopService.getShop(ShopService.TIANNAN_REFINEMENT_FORGE);
        assertEquals(ShopService.TIANNAN_REFINEMENT_FORGE, refinementForge.id());
        assertEquals(2, refinementForge.entries().size());
        for (ShopService.Entry entry : refinementForge.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:metal_spirit_stone", entry.currencyItemId());
        }
        assertEntry(refinementForge, "low_spirit_iron", "seeking_immortals:spirit_iron");
        assertEntry(refinementForge, "spirit_iron_ingot_mid", "seeking_immortals:spirit_iron");

        ShopService.Entry midIngot = refinementForge.find("spirit_iron_ingot_mid")
                .orElseThrow(() -> new AssertionError("Missing shop entry spirit_iron_ingot_mid"));
        assertEquals(2, midIngot.count());
        assertEquals(45, midIngot.cost());

        ShopService.Shop patrolSupply = ShopService.getShop(ShopService.STAR_PALACE_PATROL_SUPPLY);
        assertEquals(ShopService.STAR_PALACE_PATROL_SUPPLY, patrolSupply.id());
        assertEquals(2, patrolSupply.entries().size());
        for (ShopService.Entry entry : patrolSupply.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:metal_spirit_stone", entry.currencyItemId());
        }
        assertEntry(patrolSupply, "body_guard_talisman", "seeking_immortals:armor_talisman");
        assertEntry(patrolSupply, "anti_demon_talisman", "seeking_immortals:fire_talisman");

        ShopService.Entry patrolTalisman = patrolSupply.find("body_guard_talisman")
                .orElseThrow(() -> new AssertionError("Missing shop entry body_guard_talisman"));
        assertEquals(1, patrolTalisman.count());
        assertEquals(25, patrolTalisman.cost());
        assertEquals(10, patrolTalisman.stock());
        assertEquals(48000, patrolTalisman.refreshTicks());

        ShopService.Entry antiDemonTalisman = patrolSupply.find("anti_demon_talisman")
                .orElseThrow(() -> new AssertionError("Missing shop entry anti_demon_talisman"));
        assertEquals(1, antiDemonTalisman.count());
        assertEquals(55, antiDemonTalisman.cost());
        assertEquals(5, antiDemonTalisman.stock());
        assertEquals(48000, antiDemonTalisman.refreshTicks());

        ShopService.Shop qixuanStall = ShopService.getShop(ShopService.QIXUAN_VILLAGE_STALL);
        assertEquals(ShopService.QIXUAN_VILLAGE_STALL, qixuanStall.id());
        assertEquals(3, qixuanStall.entries().size());
        for (ShopService.Entry entry : qixuanStall.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:metal_spirit_stone", entry.currencyItemId());
            assertEquals(24000, entry.refreshTicks());
        }
        assertEntry(qixuanStall, "herb_bundle_low", "seeking_immortals:spirit_grass");
        assertEntry(qixuanStall, "mortal_medicine", "seeking_immortals:healing_pill_low");
        assertEntry(qixuanStall, "qixuan_mortal_art_scroll", "seeking_immortals:technique_manual_mortal_martial");

        ShopService.Entry herbBundle = qixuanStall.find("herb_bundle_low")
                .orElseThrow(() -> new AssertionError("Missing shop entry herb_bundle_low"));
        assertEquals(4, herbBundle.count());
        assertEquals(8, herbBundle.cost());
        assertEquals(20, herbBundle.stock());

        ShopService.Entry mortalMedicine = qixuanStall.find("mortal_medicine")
                .orElseThrow(() -> new AssertionError("Missing shop entry mortal_medicine"));
        assertEquals(1, mortalMedicine.count());
        assertEquals(3, mortalMedicine.cost());
        assertEquals(50, mortalMedicine.stock());

        ShopService.Entry qixuanScroll = qixuanStall.find("qixuan_mortal_art_scroll")
                .orElseThrow(() -> new AssertionError("Missing shop entry qixuan_mortal_art_scroll"));
        assertEquals(1, qixuanScroll.count());
        assertEquals(100, qixuanScroll.cost());
        assertEquals(1, qixuanScroll.stock());

        ShopService.Shop outerSeaStall = ShopService.getShop(ShopService.OUTER_SEA_PUBLIC_STALL);
        assertEquals(ShopService.OUTER_SEA_PUBLIC_STALL, outerSeaStall.id());
        assertEquals(3, outerSeaStall.entries().size());
        for (ShopService.Entry entry : outerSeaStall.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:metal_spirit_stone", entry.currencyItemId());
            assertEquals(24000, entry.refreshTicks());
        }
        assertEntry(outerSeaStall, "spirit_stone_shard", "seeking_immortals:spirit_stone_shard");
        assertEntry(outerSeaStall, "herb_bundle_low", "seeking_immortals:spirit_grass");
        assertEntry(outerSeaStall, "pearl_raw", "seeking_immortals:pearl_raw");

        ShopService.Entry shard = outerSeaStall.find("spirit_stone_shard")
                .orElseThrow(() -> new AssertionError("Missing shop entry spirit_stone_shard"));
        assertEquals(10, shard.count());
        assertEquals(1, shard.cost());
        assertEquals(999, shard.stock());

        ShopService.Entry outerSeaHerbBundle = outerSeaStall.find("herb_bundle_low")
                .orElseThrow(() -> new AssertionError("Missing shop entry herb_bundle_low"));
        assertEquals(4, outerSeaHerbBundle.count());
        assertEquals(12, outerSeaHerbBundle.cost());
        assertEquals(30, outerSeaHerbBundle.stock());

        ShopService.Entry pearl = outerSeaStall.find("pearl_raw")
                .orElseThrow(() -> new AssertionError("Missing shop entry pearl_raw"));
        assertEquals(1, pearl.count());
        assertEquals(8, pearl.cost());
        assertEquals(50, pearl.stock());

        ShopService.Shop tianyuanMerit = ShopService.getShop(ShopService.TIANYUAN_MERIT_EXCHANGE);
        assertEquals(ShopService.TIANYUAN_MERIT_EXCHANGE, tianyuanMerit.id());
        assertEquals(4, tianyuanMerit.entries().size());
        for (ShopService.Entry entry : tianyuanMerit.entries()) {
            assertEquals(ShopService.CURRENCY_ITEM, entry.currency());
            assertEquals("seeking_immortals:alliance_merit_token", entry.currencyItemId());
        }
        assertEntry(tianyuanMerit, "spirit_stone_high", "seeking_immortals:metal_spirit_stone_high");
        assertEntry(tianyuanMerit, "pressure_resist_charm", "seeking_immortals:pressure_resist_charm");
        assertEntry(tianyuanMerit, "diyuan_permit", "seeking_immortals:diyuan_permit");
        assertEntry(tianyuanMerit, "wind_feather_raft_ticket", "seeking_immortals:wind_feather_raft_ticket");

        ShopService.Entry highStone = tianyuanMerit.find("spirit_stone_high")
                .orElseThrow(() -> new AssertionError("Missing shop entry spirit_stone_high"));
        assertEquals(1, highStone.count());
        assertEquals(50, highStone.cost());
        assertEquals(16, highStone.stock());
        assertEquals(24000, highStone.refreshTicks());

        ShopService.Entry pressureCharm = tianyuanMerit.find("pressure_resist_charm")
                .orElseThrow(() -> new AssertionError("Missing shop entry pressure_resist_charm"));
        assertEquals(1, pressureCharm.count());
        assertEquals(200, pressureCharm.cost());
        assertEquals(6, pressureCharm.stock());
        assertEquals(48000, pressureCharm.refreshTicks());

        ShopService.Entry diyuanPermit = tianyuanMerit.find("diyuan_permit")
                .orElseThrow(() -> new AssertionError("Missing shop entry diyuan_permit"));
        assertEquals(1, diyuanPermit.count());
        assertEquals(1200, diyuanPermit.cost());
        assertEquals(1, diyuanPermit.stock());
        assertEquals(96000, diyuanPermit.refreshTicks());

        ShopService.Entry raftTicket = tianyuanMerit.find("wind_feather_raft_ticket")
                .orElseThrow(() -> new AssertionError("Missing shop entry wind_feather_raft_ticket"));
        assertEquals(1, raftTicket.count());
        assertEquals(300, raftTicket.cost());
        assertEquals(4, raftTicket.stock());
        assertEquals(48000, raftTicket.refreshTicks());
    }

    private static void assertEntry(ShopService.Shop shop, String entryId, String itemId) {
        ShopService.Entry entry = shop.find(entryId).orElseThrow(() -> new AssertionError("Missing shop entry " + entryId));
        assertEquals(itemId, entry.itemId());
    }

    @Test
    void builtInQinglanContributionHallBackfillsTextMaterialEntries() {
        ShopService.Shop shop = ShopService.getShop(ShopService.QINGLAN_CONTRIBUTION_HALL);

        assertEquals(ShopService.QINGLAN_CONTRIBUTION_HALL, shop.id());
        assertEquals(17, shop.entries().size());
        for (ShopService.Entry entry : shop.entries()) {
            assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, entry.currency());
        }
        assertEntry(shop, "foundation_formula", "seeking_immortals:alchemy_formula_foundation_building_pill_sect");
        assertEntry(shop, "longevity_formula", "seeking_immortals:alchemy_formula_longevity_pill_sect");
        assertEntry(shop, "return_yang_true_water_formula", "seeking_immortals:alchemy_formula_return_yang_true_water_sect");
        assertEntry(shop, "foundation_pill", "seeking_immortals:foundation_building_pill_low");
        assertEntry(shop, "recipe_spirit_condense", "seeking_immortals:alchemy_formula_spirit_gathering_pill_paper");
        assertEntry(shop, "recipe_bigu", "seeking_immortals:alchemy_formula_fasting_pill_paper");
        assertEntry(shop, "recipe_calm_spirit", "seeking_immortals:alchemy_formula_clear_void_pill_paper");
        assertEntry(shop, "recipe_jiangchen", "seeking_immortals:recipe_jiangchen");
        assertEntry(shop, "recipe_huanglong", "seeking_immortals:recipe_huanglong");
        assertEntry(shop, "recipe_ningshen", "seeking_immortals:alchemy_formula_calming_pill_jade");
        assertEntry(shop, "alchemy_furnace_g2", "seeking_immortals:alchemy_furnace_tier_2");
        assertEntry(shop, "spirit_herb_bundle", "seeking_immortals:spirit_grass");
        assertEntry(shop, "sect_herb_garden_seed_pack", "seeking_immortals:spirit_grass");
        assertEntry(shop, "spirit_recovery_pill", "seeking_immortals:spirit_recovery_pill");
        assertEntry(shop, "body_guard_talisman", "seeking_immortals:armor_talisman");
        assertEntry(shop, "flying_sword_low", "seeking_immortals:flying_sword_low");
        assertEntry(shop, "artifact_repair_kit", "seeking_immortals:artifact_repair_kit");

        ShopService.Entry foundationPill = shop.find("foundation_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry foundation_pill"));
        assertEquals(1, foundationPill.count());
        assertEquals(1200, foundationPill.cost());
        assertEquals(ShopService.RANK_INNER_DISCIPLE, foundationPill.rankMin());

        ShopService.Entry herbBundle = shop.find("spirit_herb_bundle")
                .orElseThrow(() -> new AssertionError("Missing shop entry spirit_herb_bundle"));
        assertEquals(8, herbBundle.count());
        assertEquals(40, herbBundle.cost());

        ShopService.Entry seedPack = shop.find("sect_herb_garden_seed_pack")
                .orElseThrow(() -> new AssertionError("Missing shop entry sect_herb_garden_seed_pack"));
        assertEquals(8, seedPack.count());
        assertEquals(50, seedPack.cost());

        ShopService.Entry huanglongFormula = shop.find("recipe_huanglong")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_huanglong"));
        assertEquals(1, huanglongFormula.count());
        assertEquals(45, huanglongFormula.cost());

        ShopService.Entry jiangchenFormula = shop.find("recipe_jiangchen")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_jiangchen"));
        assertEquals(1, jiangchenFormula.count());
        assertEquals(25, jiangchenFormula.cost());

        ShopService.Entry ningshenFormula = shop.find("recipe_ningshen")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_ningshen"));
        assertEquals(1, ningshenFormula.count());
        assertEquals(90, ningshenFormula.cost());
        assertEquals(ShopService.RANK_INNER_DISCIPLE, ningshenFormula.rankMin());

        ShopService.Entry flyingSword = shop.find("flying_sword_low")
                .orElseThrow(() -> new AssertionError("Missing shop entry flying_sword_low"));
        assertEquals(1, flyingSword.count());
        assertEquals(180, flyingSword.cost());
        assertEquals(ShopService.RANK_OUTER_DISCIPLE, flyingSword.rankMin());

        ShopService.Entry repairKit = shop.find("artifact_repair_kit")
                .orElseThrow(() -> new AssertionError("Missing shop entry artifact_repair_kit"));
        assertEquals(1, repairKit.count());
        assertEquals(25, repairKit.cost());
        assertEquals("", repairKit.rankMin());
        assertEquals(5, repairKit.stock());
        assertEquals(96000, repairKit.refreshTicks());
    }

    @Test
    void builtInDanxiaContributionHallBackfillsAlchemyTextMaterialEntries() {
        ShopService.Shop shop = ShopService.getShop("danxia_valley_contribution_hall");

        assertEquals("danxia_valley_contribution_hall", shop.id());
        assertEquals(19, shop.entries().size());
        for (ShopService.Entry entry : shop.entries()) {
            assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, entry.currency());
        }
        assertEntry(shop, "foundation_formula", "seeking_immortals:alchemy_formula_foundation_building_pill_sect");
        assertEntry(shop, "foundation_pill", "seeking_immortals:foundation_building_pill_low");
        assertEntry(shop, "recipe_spirit_condense", "seeking_immortals:alchemy_formula_spirit_gathering_pill_paper");
        assertEntry(shop, "recipe_bigu", "seeking_immortals:alchemy_formula_fasting_pill_paper");
        assertEntry(shop, "recipe_calm_spirit", "seeking_immortals:alchemy_formula_clear_void_pill_paper");
        assertEntry(shop, "recipe_jiangchen", "seeking_immortals:recipe_jiangchen");
        assertEntry(shop, "recipe_huanglong", "seeking_immortals:recipe_huanglong");
        assertEntry(shop, "recipe_ningshen", "seeking_immortals:alchemy_formula_calming_pill_jade");
        assertEntry(shop, "recipe_pressure_resist", "seeking_immortals:alchemy_formula_pressure_resist_pill_sect");
        assertEntry(shop, "recipe_spirit_realm_condense", "seeking_immortals:alchemy_formula_spirit_realm_condense_pill_sect");
        assertEntry(shop, "alchemy_furnace_g2", "seeking_immortals:alchemy_furnace_tier_2");
        assertEntry(shop, "spirit_herb_bundle", "seeking_immortals:spirit_grass");
        assertEntry(shop, "sect_herb_garden_seed_pack", "seeking_immortals:spirit_grass");
        assertEntry(shop, "spirit_recovery_pill", "seeking_immortals:spirit_recovery_pill");
        assertEntry(shop, "body_guard_talisman", "seeking_immortals:armor_talisman");
        assertEntry(shop, "flying_sword_low", "seeking_immortals:flying_sword_low");
        assertEntry(shop, "artifact_repair_kit", "seeking_immortals:artifact_repair_kit");
        assertEntry(shop, "alchemy_lid_mid", "seeking_immortals:alchemy_lid_mid");
        assertEntry(shop, "dan_fire_mid", "seeking_immortals:dan_fire_mid");

        ShopService.Entry foundationPill = shop.find("foundation_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry foundation_pill"));
        assertEquals(1, foundationPill.count());
        assertEquals(1200, foundationPill.cost());
        assertEquals(ShopService.RANK_INNER_DISCIPLE, foundationPill.rankMin());

        ShopService.Entry herbBundle = shop.find("spirit_herb_bundle")
                .orElseThrow(() -> new AssertionError("Missing shop entry spirit_herb_bundle"));
        assertEquals(8, herbBundle.count());
        assertEquals(40, herbBundle.cost());

        ShopService.Entry seedPack = shop.find("sect_herb_garden_seed_pack")
                .orElseThrow(() -> new AssertionError("Missing shop entry sect_herb_garden_seed_pack"));
        assertEquals(8, seedPack.count());
        assertEquals(50, seedPack.cost());

        ShopService.Entry huanglongFormula = shop.find("recipe_huanglong")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_huanglong"));
        assertEquals(1, huanglongFormula.count());
        assertEquals(45, huanglongFormula.cost());

        ShopService.Entry jiangchenFormula = shop.find("recipe_jiangchen")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_jiangchen"));
        assertEquals(1, jiangchenFormula.count());
        assertEquals(25, jiangchenFormula.cost());

        ShopService.Entry ningshenFormula = shop.find("recipe_ningshen")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_ningshen"));
        assertEquals(1, ningshenFormula.count());
        assertEquals(90, ningshenFormula.cost());
        assertEquals(ShopService.RANK_INNER_DISCIPLE, ningshenFormula.rankMin());

        ShopService.Entry pressureResistFormula = shop.find("recipe_pressure_resist")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_pressure_resist"));
        assertEquals(1, pressureResistFormula.count());
        assertEquals(9000, pressureResistFormula.cost());
        assertEquals(1, pressureResistFormula.stock());
        assertEquals(96000, pressureResistFormula.refreshTicks());

        ShopService.Entry spiritRealmCondenseFormula = shop.find("recipe_spirit_realm_condense")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_spirit_realm_condense"));
        assertEquals(1, spiritRealmCondenseFormula.count());
        assertEquals(18000, spiritRealmCondenseFormula.cost());
        assertEquals(1, spiritRealmCondenseFormula.stock());
        assertEquals(96000, spiritRealmCondenseFormula.refreshTicks());

        ShopService.Entry flyingSword = shop.find("flying_sword_low")
                .orElseThrow(() -> new AssertionError("Missing shop entry flying_sword_low"));
        assertEquals(1, flyingSword.count());
        assertEquals(180, flyingSword.cost());
        assertEquals(ShopService.RANK_OUTER_DISCIPLE, flyingSword.rankMin());

        ShopService.Entry repairKit = shop.find("artifact_repair_kit")
                .orElseThrow(() -> new AssertionError("Missing shop entry artifact_repair_kit"));
        assertEquals(1, repairKit.count());
        assertEquals(25, repairKit.cost());
        assertEquals("", repairKit.rankMin());
        assertEquals(5, repairKit.stock());
        assertEquals(96000, repairKit.refreshTicks());
    }

    @Test
    void builtInCangmingContributionHallBackfillsCondensationEntries() {
        ShopService.Shop shop = ShopService.getShop("cangming_isle_contribution_hall");

        assertEquals("cangming_isle_contribution_hall", shop.id());
        assertEquals(9, shop.entries().size());
        for (ShopService.Entry entry : shop.entries()) {
            assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, entry.currency());
        }
        assertEntry(shop, "leyline_compass", "seeking_immortals:leyline_compass");
        assertEntry(shop, "water_spirit_stone_mid", "seeking_immortals:water_spirit_stone_mid");
        assertEntry(shop, "chaotic_star_sea_manual", "seeking_immortals:technique_manual_chaotic_star_sea");
        assertEntry(shop, "condensation_pill", "seeking_immortals:essence_condensing_pill");
        assertEntry(shop, "recipe_condensation", "seeking_immortals:alchemy_formula_essence_condensing_pill_jade");
        assertEntry(shop, "recipe_yanghun", "seeking_immortals:alchemy_formula_soul_gathering_pill_jade");
        assertEntry(shop, "body_guard_talisman", "seeking_immortals:armor_talisman");
        assertEntry(shop, "sea_calm_pill", "seeking_immortals:calming_pill_low");
        assertEntry(shop, "recipe_sea_calm", "seeking_immortals:alchemy_formula_calming_pill_jade");

        ShopService.Entry pill = shop.find("condensation_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry condensation_pill"));
        assertEquals(1, pill.count());
        assertEquals(8000, pill.cost());

        ShopService.Entry formula = shop.find("recipe_condensation")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_condensation"));
        assertEquals(1, formula.count());
        assertEquals(2500, formula.cost());

        ShopService.Entry yanghunFormula = shop.find("recipe_yanghun")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_yanghun"));
        assertEquals(1, yanghunFormula.count());
        assertEquals(600, yanghunFormula.cost());

        ShopService.Entry talisman = shop.find("body_guard_talisman")
                .orElseThrow(() -> new AssertionError("Missing shop entry body_guard_talisman"));
        assertEquals(1, talisman.count());
        assertEquals(25, talisman.cost());

        ShopService.Entry seaCalm = shop.find("sea_calm_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry sea_calm_pill"));
        assertEquals(1, seaCalm.count());
        assertEquals(80, seaCalm.cost());

        ShopService.Entry seaCalmFormula = shop.find("recipe_sea_calm")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_sea_calm"));
        assertEquals(1, seaCalmFormula.count());
        assertEquals(40, seaCalmFormula.cost());
    }

    @Test
    void builtInStarPalaceMeritHallBackfillsImplementedFormulaEntries() {
        ShopService.Shop shop = ShopService.getShop("star_palace_merit_hall");

        assertEquals("star_palace_merit_hall", shop.id());
        // Wave466 specialty deepen: patrol supplies + void crystal.
        assertEquals(7, shop.entries().size());
        for (ShopService.Entry entry : shop.entries()) {
            assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, entry.currency());
        }
        assertEntry(shop, "condensation_pill", "seeking_immortals:essence_condensing_pill");
        assertEntry(shop, "recipe_condensation", "seeking_immortals:alchemy_formula_essence_condensing_pill_jade");
        assertEntry(shop, "recipe_yanghun", "seeking_immortals:alchemy_formula_soul_gathering_pill_jade");
        assertEntry(shop, "sea_calm_pill", "seeking_immortals:calming_pill_low");
        assertEntry(shop, "recipe_sea_calm", "seeking_immortals:alchemy_formula_calming_pill_jade");
        assertEntry(shop, "water_spirit_stone", "seeking_immortals:water_spirit_stone");
        assertEntry(shop, "void_crystal", "seeking_immortals:void_crystal");

        ShopService.Entry formula = shop.find("recipe_condensation")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_condensation"));
        assertEquals(1200, formula.cost());
        assertEquals(2, formula.stock());

        ShopService.Entry yanghunFormula = shop.find("recipe_yanghun")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_yanghun"));
        assertEquals(600, yanghunFormula.cost());
        assertEquals(2, yanghunFormula.stock());

        ShopService.Entry seaCalm = shop.find("sea_calm_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry sea_calm_pill"));
        assertEquals(80, seaCalm.cost());
        assertEquals(15, seaCalm.stock());

        ShopService.Entry seaCalmFormula = shop.find("recipe_sea_calm")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_sea_calm"));
        assertEquals(40, seaCalmFormula.cost());
        assertEquals(6, seaCalmFormula.stock());
    }

    @Test
    void builtInYulingContributionHallBackfillsBeastSoulEssence() {
        ShopService.Shop shop = ShopService.getShop("yuling_pavilion_contribution_hall");

        assertEquals("yuling_pavilion_contribution_hall", shop.id());
        assertEquals(5, shop.entries().size());
        for (ShopService.Entry entry : shop.entries()) {
            assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, entry.currency());
        }
        assertEntry(shop, "spirit_taming_basic", "seeking_immortals:technique_manual_spirit_taming_basic");
        assertEntry(shop, "beast_core", "seeking_immortals:beast_core");
        assertEntry(shop, "spirit_beast_bone", "seeking_immortals:spirit_beast_bone");
        assertEntry(shop, "spirit_beast_feed", "seeking_immortals:spirit_grass");
        assertEntry(shop, "beast_soul_essence", "seeking_immortals:beast_core");

        ShopService.Entry feed = shop.find("spirit_beast_feed")
                .orElseThrow(() -> new AssertionError("Missing shop entry spirit_beast_feed"));
        assertEquals(4, feed.count());
        assertEquals(15, feed.cost());

        ShopService.Entry essence = shop.find("beast_soul_essence")
                .orElseThrow(() -> new AssertionError("Missing shop entry beast_soul_essence"));
        assertEquals(1, essence.count());
        assertEquals(200, essence.cost());
    }

    @Test
    void builtInLuoyunContributionHallBackfillsAlchemyEntries() {
        ShopService.Shop shop = ShopService.getShop("luoyun_contribution_hall");

        assertEquals("luoyun_contribution_hall", shop.id());
        assertEquals(5, shop.entries().size());
        for (ShopService.Entry entry : shop.entries()) {
            assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, entry.currency());
        }
        assertEntry(shop, "luoyun_spirit_pill", "seeking_immortals:spirit_gathering_pill");
        assertEntry(shop, "recipe_luoyun_spirit", "seeking_immortals:alchemy_formula_spirit_gathering_pill_paper");
        assertEntry(shop, "alchemy_furnace_g3", "seeking_immortals:alchemy_furnace_tier_3");
        assertEntry(shop, "sect_herb_garden_seed_pack", "seeking_immortals:spirit_grass");
        assertEntry(shop, "condensation_pill", "seeking_immortals:essence_condensing_pill");

        ShopService.Entry luoyunPill = shop.find("luoyun_spirit_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry luoyun_spirit_pill"));
        assertEquals(1, luoyunPill.count());
        assertEquals(280, luoyunPill.cost());

        ShopService.Entry luoyunFormula = shop.find("recipe_luoyun_spirit")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_luoyun_spirit"));
        assertEquals(1, luoyunFormula.count());
        assertEquals(400, luoyunFormula.cost());
        assertEquals(ShopService.RANK_INNER_DISCIPLE, luoyunFormula.rankMin());

        ShopService.Entry furnace = shop.find("alchemy_furnace_g3")
                .orElseThrow(() -> new AssertionError("Missing shop entry alchemy_furnace_g3"));
        assertEquals(1, furnace.count());
        assertEquals(800, furnace.cost());

        ShopService.Entry seedPack = shop.find("sect_herb_garden_seed_pack")
                .orElseThrow(() -> new AssertionError("Missing shop entry sect_herb_garden_seed_pack"));
        assertEquals(8, seedPack.count());
        assertEquals(60, seedPack.cost());

        ShopService.Entry pill = shop.find("condensation_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry condensation_pill"));
        assertEquals(1, pill.count());
        assertEquals(2500, pill.cost());
    }

    @Test
    void builtInYanyueContributionHallBackfillsImplementedPills() {
        ShopService.Shop shop = ShopService.getShop("yanyue_contribution_hall");

        assertEquals("yanyue_contribution_hall", shop.id());
        // Wave466 specialty deepen: clear-void / body-tempering formulas + soul packs.
        assertEquals(7, shop.entries().size());
        for (ShopService.Entry entry : shop.entries()) {
            assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, entry.currency());
        }
        assertEntry(shop, "foundation_pill", "seeking_immortals:foundation_building_pill_low");
        assertEntry(shop, "calm_spirit_pill", "seeking_immortals:clear_void_pill");
        assertEntry(shop, "recipe_heqi", "seeking_immortals:recipe_heqi");
        assertEntry(shop, "ningshen_pill", "seeking_immortals:calming_pill_low");
        assertEntry(shop, "recipe_clear_void", "seeking_immortals:alchemy_formula_clear_void_pill_paper");
        assertEntry(shop, "recipe_body_tempering", "seeking_immortals:alchemy_formula_body_tempering_pill_jade");
        assertEntry(shop, "soul_fragment_pack", "seeking_immortals:soul_fragment");

        ShopService.Entry foundationPill = shop.find("foundation_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry foundation_pill"));
        assertEquals(1, foundationPill.count());
        assertEquals(1400, foundationPill.cost());
        assertEquals(ShopService.RANK_INNER_DISCIPLE, foundationPill.rankMin());

        ShopService.Entry pill = shop.find("calm_spirit_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry calm_spirit_pill"));
        assertEquals(1, pill.count());
        assertEquals(60, pill.cost());

        ShopService.Entry ningshen = shop.find("ningshen_pill")
                .orElseThrow(() -> new AssertionError("Missing shop entry ningshen_pill"));
        assertEquals(1, ningshen.count());
        assertEquals(70, ningshen.cost());

        ShopService.Entry heqiFormula = shop.find("recipe_heqi")
                .orElseThrow(() -> new AssertionError("Missing shop entry recipe_heqi"));
        assertEquals(1, heqiFormula.count());
        assertEquals(40, heqiFormula.cost());
    }

    @Test
    void everySectContributionHallLoadsEntries() {
        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            ShopService.Shop shop = ShopService.getShop(definition.shopId());

            assertEquals(definition.shopId(), shop.id());
            org.junit.jupiter.api.Assertions.assertFalse(shop.entries().isEmpty(), definition.id() + " shop must not be empty");
            for (ShopService.Entry entry : shop.entries()) {
                assertEquals(ShopService.CURRENCY_SECT_CONTRIBUTION, entry.currency());
            }
        }
    }

    @Test
    void appliesWorldpackMarketCostModifiers() {
        ShopService.Entry entry = new ShopService.Entry("herb", "minecraft:stone", 1, 4,
                ShopService.CURRENCY_ITEM, "minecraft:emerald", 8, 24000);

        int discount = ShopService.adjustedCost("market_herbal_stall", entry,
                (shopId, shopEntry, baseCost) -> WorldpackGameplayService.adjustMarketCostForEffects(
                        baseCost, List.of(WorldpackGameplayService.EFFECT_HERB_SHOP_BONUS)));
        int risk = ShopService.adjustedCost("market_herbal_stall", entry,
                (shopId, shopEntry, baseCost) -> WorldpackGameplayService.adjustMarketCostForEffects(
                        baseCost, List.of(WorldpackGameplayService.EFFECT_TRADE_RISK_UP)));
        int both = ShopService.adjustedCost("market_herbal_stall", entry,
                (shopId, shopEntry, baseCost) -> WorldpackGameplayService.adjustMarketCostForEffects(
                        baseCost, List.of(WorldpackGameplayService.EFFECT_HERB_SHOP_BONUS,
                                WorldpackGameplayService.EFFECT_TRADE_RISK_UP)));

        assertEquals(3, discount);
        assertEquals(6, risk);
        assertEquals(5, both);
    }

    @Test
    void appliesStarPalaceIslandMarketTaxUntilReceiptIsUsed() {
        ShopService.Entry pill = new ShopService.Entry("sea_calm_pill", "minecraft:stone", 1, 35,
                ShopService.CURRENCY_ITEM, "minecraft:emerald", 10, 24000);
        ShopService.Entry receipt = new ShopService.Entry("star_palace_tax_receipt", "minecraft:paper", 1, 5,
                ShopService.CURRENCY_ITEM, "minecraft:emerald", 20, 24000);

        int unpaidCost = WorldpackGameplayService.adjustMarketCostForShop(
                ShopService.CHAOTIC_SEA_ISLAND_GENERAL, pill, pill.cost(), List.of(), false);
        int paidCost = WorldpackGameplayService.adjustMarketCostForShop(
                ShopService.CHAOTIC_SEA_ISLAND_GENERAL, pill, pill.cost(), List.of(), true);
        int receiptCost = WorldpackGameplayService.adjustMarketCostForShop(
                ShopService.CHAOTIC_SEA_ISLAND_GENERAL, receipt, receipt.cost(), List.of(), false);
        int outerSeaCost = WorldpackGameplayService.adjustMarketCostForShop(
                ShopService.OUTER_SEA_PUBLIC_STALL, pill, pill.cost(), List.of(), false);
        int nonTaxedShopCost = WorldpackGameplayService.adjustMarketCostForShop(
                ShopService.MARKET_HERBAL_STALL, pill, pill.cost(), List.of(), false);
        int riskPlusTax = WorldpackGameplayService.adjustMarketCostForShop(
                ShopService.CHAOTIC_SEA_ISLAND_GENERAL, pill, pill.cost(),
                List.of(WorldpackGameplayService.EFFECT_TRADE_RISK_UP), false);

        assertEquals(39, unpaidCost);
        assertEquals(35, paidCost);
        assertEquals(5, receiptCost);
        assertEquals(39, outerSeaCost);
        assertEquals(35, nonTaxedShopCost);
        assertEquals(58, riskPlusTax);
    }
}
