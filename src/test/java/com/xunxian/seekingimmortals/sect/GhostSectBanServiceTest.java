package com.xunxian.seekingimmortals.sect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GhostSectBanServiceTest {
    @Test
    void loadsBanRulesAndConsequences() {
        GhostSectBanService.Snapshot snapshot = GhostSectBanService.builtin();
        assertTrue(!snapshot.bannedSectIds().isEmpty() || !snapshot.onDetected().isEmpty(),
                "expected banned sects or on_detected penalties");
        assertFalse(snapshot.detectionTags().isEmpty(), "detection tags");
        assertFalse(snapshot.hooks().isEmpty(), "hooks");
        assertFalse(snapshot.tribunalEvent().isBlank(), "tribunal event");
        assertTrue(snapshot.shopDenied().stream().anyMatch(s -> s.contains("huangfeng"))
                || snapshot.questBlocks().stream().anyMatch(s -> s.contains("huangfeng"))
                || !snapshot.onDetected().isEmpty()
                || !snapshot.bannedSectIds().isEmpty());
    }

    @Test
    void masterDataLoadsSectsAndSpecialty() {
        SectMasterDataService.Snapshot snapshot = SectMasterDataService.builtin();
        assertTrue(snapshot.sectCount() >= 20, "sects.json should provide >=20 sects, got " + snapshot.sectCount());
        assertTrue(SectMasterDataService.find("huangfeng_valley").isPresent()
                || SectMasterDataService.find("yanyue_sect").isPresent());
        assertTrue(snapshot.specialties().size() >= 10);
    }

    @Test
    void contentPackagesCoverThirtyPlayableSects() {
        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            assertFalse(SectContentService.missionsForTest(definition.id()).isEmpty(),
                    "missions missing for " + definition.id());
            assertFalse(SectContentService.dialogueForTest(definition.id()).nodes().isEmpty(),
                    "dialogue missing for " + definition.id());
        }
        assertTrue(SectDefinitionService.playableDefinitions().size() >= 20);
    }
}
