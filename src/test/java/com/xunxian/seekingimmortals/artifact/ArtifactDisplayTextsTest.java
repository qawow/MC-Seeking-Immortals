package com.xunxian.seekingimmortals.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ArtifactDisplayTextsTest {
    @Test
    void spiritSeveringAliasUsesSoulTransformationRealm() {
        assertEquals(ArtifactDisplayTexts.realmText("SOUL_TRANSFORMATION"),
                ArtifactDisplayTexts.realmText("SPIRIT_SEVERING"));
        assertNotEquals(ArtifactDisplayTexts.realmText("UNITY"),
                ArtifactDisplayTexts.realmText("SPIRIT_SEVERING"));
    }
}
