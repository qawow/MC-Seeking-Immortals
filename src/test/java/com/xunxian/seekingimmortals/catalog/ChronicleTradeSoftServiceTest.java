package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChronicleTradeSoftServiceTest {
    @Test
    void loadsChronicleAndTradeRouteIndexes() {
        assertTrue(ChronicleTradeSoftService.chronicleCount() >= 20);
        assertTrue(ChronicleTradeSoftService.tradeRouteCount() >= 5);
        assertFalse(ChronicleTradeSoftService.sampleChronicle(3).isEmpty());
        assertFalse(ChronicleTradeSoftService.sampleTradeRoutes(3).isEmpty());
    }

    @Test
    void allRoutesHaveFeesAndMappedChains() {
        for (String line : ChronicleTradeSoftService.sampleTradeRoutes(50)) {
            String id = line.split("\\|")[0].trim();
            List<ChronicleTradeSoftService.EmbarkFee> fees = ChronicleTradeSoftService.feeFor(id);
            assertFalse(fees.isEmpty(), "missing fee for " + id);
            assertTrue(fees.get(0).count() >= 1);
            assertTrue(ChronicleTradeSoftService.mappedChainId(id).isPresent(), "missing chain for " + id);
            assertTrue(TextQuestChainService.find(
                    ChronicleTradeSoftService.mappedChainId(id).orElseThrow()).isPresent());
        }
    }

    @Test
    void dualFeeForDajinToTianyuan() {
        List<ChronicleTradeSoftService.EmbarkFee> fees = ChronicleTradeSoftService.feeFor("dajin_to_tianyuan");
        assertTrue(fees.size() >= 2);
        assertTrue(fees.stream().anyMatch(f -> f.itemId().contains("wind_feather_raft_ticket")));
    }

    @Test
    void netherFerryUsesYinStone() {
        List<ChronicleTradeSoftService.EmbarkFee> fees = ChronicleTradeSoftService.feeFor("nether_river_ferry");
        assertTrue(fees.stream().anyMatch(f -> f.itemId().contains("yin_stone") && f.count() == 30));
    }

    @Test
    void wave479ChronicleIdsMapToRealChains() {
        // Explicit map + heuristics should cover core chronicle ids from the catalog index.
        assertTrue(ChronicleTradeSoftService.mappedChronicleChainId("cycle_void_palace").isPresent());
        assertTrue(ChronicleTradeSoftService.mappedChronicleChainId("d1_demon_invasion").isPresent());
        assertTrue(ChronicleTradeSoftService.mappedChronicleChainId("k3_mulan_tianlan_war_1").isPresent());
        assertTrue(ChronicleTradeSoftService.mappedChronicleChainId("l1_tianyuan_founded").isPresent());
        assertTrue(ChronicleTradeSoftService.mappedChronicleChainId("y1_yinsi_realm").isPresent());
        assertTrue(ChronicleTradeSoftService.mappedChronicleChainId("m1_five_realms").isPresent());
        assertTrue(ChronicleTradeSoftService.mappedChronicleChainId("e_ancient_demon_seal_weak").isPresent());
        assertTrue(TextQuestChainService.find(
                ChronicleTradeSoftService.mappedChronicleChainId("cycle_void_palace").orElseThrow()).isPresent());
        // Sample lines now include mapping column.
        assertTrue(ChronicleTradeSoftService.sampleChronicle(5).stream().anyMatch(line -> line.contains("->")));
    }

    @Test
    void discountedFeesNeverDropBelowOne() {
        List<ChronicleTradeSoftService.EmbarkFee> base = ChronicleTradeSoftService.feeFor("tiannan_internal");
        List<ChronicleTradeSoftService.EmbarkFee> discounted = ChronicleTradeSoftService.discountedFees(null, base);
        assertFalse(discounted.isEmpty());
        assertTrue(discounted.get(0).count() >= 1);
    }

    @Test
    void embarkAndStageCostsReserveTheSameItemTogether() {
        List<ChronicleTradeSoftService.EmbarkFee> fees = List.of(
                new ChronicleTradeSoftService.EmbarkFee(
                        "seeking_immortals:spirit_stone_shard", 4, "spirit_stone_shard"));
        TextQuestChainService.StageCost stageCost = new TextQuestChainService.StageCost(
                "seeking_immortals:spirit_stone_shard", 2, "spirit_stone_shard");
        assertEquals(6, ChronicleTradeSoftService.combinedRequiredCount(
                "seeking_immortals:spirit_stone_shard", fees, Optional.of(stageCost)));
    }
}
