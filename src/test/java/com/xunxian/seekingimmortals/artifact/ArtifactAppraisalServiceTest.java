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

    @Test
    void appraisalConsumesSpiritBeforeWritingTags() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "artifact", "ArtifactAppraisalService.java"));
        String compact = source.replaceAll("\\s+", "");
        int method = compact.indexOf("publicstaticbooleanappraise(");
        assertTrue(method >= 0);
        int bodyStart = compact.indexOf('{', method);
        int depth = 0;
        int bodyEnd = -1;
        for (int i = bodyStart; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                bodyEnd = i;
                break;
            }
        }
        String body = compact.substring(method, bodyEnd + 1);
        int cost = body.indexOf("consumeSpiritualPower(");
        int write = body.indexOf("tag.putBoolean(TAG_APPRAISED,true)");
        // skill-gate failures may grant practice earlier; success practice is the last grant.
        int practice = body.lastIndexOf("LifeSkillService.grantPractice(");
        assertTrue(cost >= 0 && write > cost,
                "spiritual power must be consumed before appraisal NBT is written");
        assertTrue(practice > write, "success practice/reputation must follow a successful cost commit");
        assertTrue(body.contains("appraisal.no_power") || body.contains("getSpiritualPower()<spiritCost"),
                "insufficient spiritual power must fail closed");
        assertTrue(body.indexOf("consumeSpiritualPower(") < body.indexOf("tag.putBoolean(TAG_APPRAISED,true)"));
    }
}
