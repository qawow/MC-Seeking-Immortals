package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialNodeFeeRulesTest {
    @Test
    void chaoticSeaPortalRequiresStarPalaceReceipt() {
        SpatialNodeFeeRules.Fee fee = SpatialNodeFeeRules.portalDestinationFee("tiannan", "chaotic_sea");
        assertTrue(fee.present());
        assertEquals(SpatialNodeFeeRules.STAR_PALACE_TAX_RECEIPT, fee.itemId());
        assertEquals(1, fee.count());
    }

    @Test
    void mulanPortalRequiresWarToken() {
        SpatialNodeFeeRules.Fee fee = SpatialNodeFeeRules.portalDestinationFee("tiannan", "mulan");
        assertTrue(fee.present());
        assertEquals(SpatialNodeFeeRules.WAR_CONTRIBUTION_TOKEN, fee.itemId());
    }

    @Test
    void sameRegionHasNoFee() {
        assertFalse(SpatialNodeFeeRules.portalDestinationFee("tianyuan", "tianyuan").present());
    }

    @Test
    void tianyuanToFengyuanDefersToExistingAllianceFee() {
        assertFalse(SpatialNodeFeeRules.portalDestinationFee("tianyuan", "spirit_fengyuan").present());
    }
}
