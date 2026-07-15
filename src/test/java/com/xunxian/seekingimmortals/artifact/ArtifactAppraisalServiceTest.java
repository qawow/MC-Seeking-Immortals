package com.xunxian.seekingimmortals.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactAppraisalServiceTest {
    @Test
    void detectsAppraisalToolsById() {
        assertTrue(ArtifactAppraisalService.isAppraisalTool("ancient_treasure_appraisal_lens"));
        assertTrue(ArtifactAppraisalService.isAppraisalTool("artifact_identify_scroll"));
        assertFalse(ArtifactAppraisalService.isAppraisalTool("flying_sword_low"));
        assertFalse(ArtifactAppraisalService.isAppraisalTool(null));
    }

    @Test
    void estimateValueHasTierFallback() {
        int v = ArtifactAppraisalService.estimateValue(null, "not_a_real_lot_id", 3);
        assertTrue(v >= 10);
    }

    @Test
    void appraisalToolsRecognizeMixedCaseAndPaths() {
        assertTrue(ArtifactAppraisalService.isAppraisalTool("APPRAISAL_LENS"));
        assertTrue(ArtifactAppraisalService.isAppraisalTool("item.identify_scroll"));
    }
}
