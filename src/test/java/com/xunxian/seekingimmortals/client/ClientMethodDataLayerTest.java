package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientMethodDataLayerTest {
    @AfterEach
    void resetClientMirror() {
        ClientMethodData.reset();
    }

    @Test
    void clampsSyncedLayersPerMethod() {
        ClientMethodData.setLearnedMethods(Map.of(
                "changchun_gong", 99,
                "huangfeng_alchemy_scripture", 99));

        assertEquals(13, ClientMethodData.getLayer("changchun_gong"));
        assertEquals(1, ClientMethodData.getLayer("huangfeng_alchemy_scripture"));
    }
}
