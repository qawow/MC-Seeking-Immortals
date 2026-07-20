package com.xunxian.seekingimmortals.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncLearnedMethodsPacketLayerTest {
    @Test
    void clampsLayersAgainstEachMethodDefinition() {
        assertEquals(13, new SyncLearnedMethodsPacket.Entry("changchun_gong", 99).layer());
        assertEquals(2, new SyncLearnedMethodsPacket.Entry("treasure_appraisal_art", 99).layer());
        assertEquals(1, new SyncLearnedMethodsPacket.Entry("huangfeng_alchemy_scripture", 99).layer());
        assertEquals(1, new SyncLearnedMethodsPacket.Entry("unknown_method", 99).layer());
        assertEquals(0, new SyncLearnedMethodsPacket.Entry("changchun_gong", -1).layer());
    }
}
