package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncQuestTrackerPacket;

import java.util.ArrayList;
import java.util.List;

public final class ClientQuestTrackerData {
    private static final List<String> LINES = new ArrayList<>();

    private ClientQuestTrackerData() {}

    public static void set(SyncQuestTrackerPacket packet) {
        LINES.clear();
        if (packet != null && packet.lines() != null) {
            LINES.addAll(packet.lines());
        }
    }

    public static void reset() {
        LINES.clear();
    }

    public static List<String> lines() {
        return List.copyOf(LINES);
    }
}
