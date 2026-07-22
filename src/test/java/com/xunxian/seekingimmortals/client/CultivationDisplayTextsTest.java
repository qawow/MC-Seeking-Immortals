package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CultivationDisplayTextsTest {
    @Test
    void spiritSeveringUsesTheRealmSemanticsDefinedByRealm() {
        assertEquals("soul_transformation", CultivationDisplayTexts.canonicalRealmId("SPIRIT_SEVERING"));
        assertEquals(CultivationDisplayTexts.realmText("SOUL_TRANSFORMATION"),
                CultivationDisplayTexts.realmText("SPIRIT_SEVERING"));
    }

    @Test
    void mixedCatalogLabelsLoseImplementationTokensButKeepChineseText() {
        String cleaned = PlayerDisplayText.sanitizeCatalogText("hehuan_sect道途");
        assertEquals("道途", cleaned);
        assertFalse(cleaned.contains("hehuan_sect"));
        assertEquals("", PlayerDisplayText.sanitizeCatalogText("unknown_method_id"));
    }
}
