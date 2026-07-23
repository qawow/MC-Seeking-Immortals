package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectWarFactionAliasTest {
    @Test
    void mulanAuthoredFactionMatchesThePlayableSectId() {
        assertTrue(SectWarService.factionMatches("mulan_fashi_council", "mulan_council"));
        assertTrue(SectWarService.factionMatches("mulan_fashi", "mulan_council"));
        assertFalse(SectWarService.factionMatches("tianlan_temple", "mulan_council"));

        SectWarService.WarData data = new SectWarService.WarData();
        data.factionA = "mulan_council";
        data.factionB = "tianlan_temple";
        assertTrue(SectWarService.sameArmies(
                data, "mulan_fashi_council", "tianlan_temple", ""));
    }

    @Test
    void tiannanAggregateMatchesOnlyTheSevenRuntimeSectIds() {
        List<String> sevenSects = List.of(
                "huangfeng_valley",
                "yanyue_sect",
                "spirit_beast_mountain",
                "qingxu_gate",
                "huadao_wu",
                "tianque_fort",
                "giant_sword_gate");
        for (String sectId : sevenSects) {
            assertTrue(SectWarService.factionMatches(sectId, "tiannan_seven_sects"), sectId);
            assertTrue(SectWarService.factionMatches(sectId, "tiannan_alliance"), sectId);
        }

        assertFalse(SectWarService.factionMatches("qixuan_men", "tiannan_seven_sects"));
        assertFalse(SectWarService.factionMatches("hehuan_sect", "tiannan_seven_sects"));
        assertFalse(SectWarService.factionMatches("mulan_fashi_council", "tiannan_seven_sects"));
    }
}
