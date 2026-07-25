package com.xunxian.seekingimmortals.shop;

import com.xunxian.seekingimmortals.cultivation.Realm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantShopPolicyCatalogTest {
    @Test
    void authorRealmReputationAccessAndRiskFieldsAreCompiled() {
        MerchantShopPolicyCatalog.ShopPolicy herb = MerchantShopPolicyCatalog.find("tiannan_herb_market").orElseThrow();
        assertEquals("QI_REFINING", herb.entry("jiangchen_pill").orElseThrow().realmMax());

        MerchantShopPolicyCatalog.ShopPolicy inverse = MerchantShopPolicyCatalog.find("inverse_star_black_market").orElseThrow();
        assertEquals(-15, inverse.reputationGates().get(0).threshold());

        MerchantShopPolicyCatalog.ShopPolicy pirate = MerchantShopPolicyCatalog.find("pirate_black_market_outer_sea").orElseThrow();
        assertTrue(pirate.accessAny().contains("inverse_star_rep_50"));
        assertTrue(pirate.entry("chaotic_sea_teleport_permit").orElseThrow().illegal());
        assertEquals("NASCENT_SOUL", pirate.entry("void_palace_map_fragment").orElseThrow().realmMin());
        assertTrue(pirate.riskEvents().contains("star_palace_raid_chance"));

        MerchantShopPolicyCatalog.ShopPolicy tianyuan = MerchantShopPolicyCatalog.find("tianyuan_merit_exchange").orElseThrow();
        assertEquals("DEITY_TRANSFORMATION", tianyuan.realmMin());
    }

    @Test
    void learnRequirementEntryGateIsPromotedToRuntimePolicy() {
        MerchantShopPolicyCatalog.ShopPolicy blackMarket = MerchantShopPolicyCatalog.find("chaotic_sea_black_market").orElseThrow();
        assertEquals("FOUNDATION", blackMarket.realmMin());
        assertTrue(blackMarket.accessAny().contains("inverse_star_friendly"));
        assertTrue(blackMarket.accessAny().contains("smuggler_contact"));
    }

    @Test
    void unknownNonBlankRealmRequirementsFailClosed() {
        assertTrue(MerchantShopPolicyCatalog.meetsMinimum(Realm.QI_REFINING, ""));
        assertTrue(MerchantShopPolicyCatalog.meetsMaximum(Realm.QI_REFINING, ""));
        assertTrue(MerchantShopPolicyCatalog.meetsMinimum(Realm.FOUNDATION_ESTABLISHMENT, "QI_REFINING"));
        assertTrue(MerchantShopPolicyCatalog.meetsMaximum(Realm.QI_REFINING, "FOUNDATION"));
        assertFalse(MerchantShopPolicyCatalog.meetsMinimum(Realm.TRUE_IMMORTAL, "UNKNOWN_REALM"));
        assertFalse(MerchantShopPolicyCatalog.meetsMaximum(Realm.QI_REFINING, "UNKNOWN_REALM"));
    }
}
