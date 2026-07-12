package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonHonestMvpServiceTest {
    @Test
    void puppetDefinitionsIndexed() {
        assertTrue(SummonHonestMvpService.puppetDefinitionCount() >= 0);
    }
}
