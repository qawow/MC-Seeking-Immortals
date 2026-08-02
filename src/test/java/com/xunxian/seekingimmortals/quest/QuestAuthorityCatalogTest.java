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
        assertTrue(politics.stages().get(3) == null || politics.stages().get(3).requiresBranch().isBlank(),
                "chaotic_sea_politics must not hard-gate stages after the one-time branch lock");

        QuestAuthorityCatalog.ChainRule blood = QuestAuthorityCatalog.find("blood_forbidden_campaign").orElseThrow();
        assertEquals(4, blood.partySizeMax());

        QuestAuthorityCatalog.ChainRule war = QuestAuthorityCatalog.find("mulan_war_campaign").orElseThrow();
        assertTrue(war.stages().get(5) == null || war.stages().get(5).prerequisite().isBlank(),
                "optional step 5 must not hard-gate the campaign");
    }
}
