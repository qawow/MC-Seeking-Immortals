package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestAuthorityCatalogTest {
    @Test
    void compilesChainAndStageAuthorityFields() {
        QuestAuthorityCatalog.ChainRule demonic = QuestAuthorityCatalog.find("demonic_six_expanded").orElseThrow();
        assertEquals("demonic_karma", demonic.karmaRequired());
        assertEquals("demonic_six_path", demonic.extendsChain());
        assertTrue(demonic.stages().get(2).branchAny().contains("hehuan_sect"));

        QuestAuthorityCatalog.ChainRule politics = QuestAuthorityCatalog.find("chaotic_sea_politics").orElseThrow();
        assertEquals("rebel", politics.stages().get(3).requiresBranch());

        QuestAuthorityCatalog.ChainRule blood = QuestAuthorityCatalog.find("blood_forbidden_campaign").orElseThrow();
        assertEquals(4, blood.partySizeMax());

        QuestAuthorityCatalog.ChainRule war = QuestAuthorityCatalog.find("mulan_war_campaign").orElseThrow();
        assertEquals("m4_holy_bird_mulan", war.stages().get(5).prerequisite());
    }
}
