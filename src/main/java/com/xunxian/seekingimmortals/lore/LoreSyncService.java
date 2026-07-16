package com.xunxian.seekingimmortals.lore;

import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.catalog.ChronicleTradeSoftService;
import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService;
import com.xunxian.seekingimmortals.network.SyncLoreUnlockPacket;
import com.xunxian.seekingimmortals.quest.TimelineChronicleService;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** M16: assemble unlock snapshots and open encyclopedia screens (read-only). */
public final class LoreSyncService {
    private LoreSyncService() {}

    public static void sync(ServerPlayer player, boolean openNothing) {
        syncAndOpen(player, openNothing ? "" : "");
    }

    public static void syncOnly(ServerPlayer player) {
        if (player == null) {
            return;
        }
        SyncLoreUnlockPacket.send(player, bestiaryIds(player), chronicleIds(player), timelineIds(player), "");
    }

    public static void syncAndOpen(ServerPlayer player, String screen) {
        if (player == null) {
            return;
        }
        String open = screen == null ? "" : screen.trim().toLowerCase(Locale.ROOT);
        SyncLoreUnlockPacket.send(player, bestiaryIds(player), chronicleIds(player), timelineIds(player), open);
    }

    private static List<String> bestiaryIds(ServerPlayer player) {
        return BestiaryUnlockService.unlockedIds(player);
    }

    private static List<String> chronicleIds(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        for (FactionQuestCatalogService.Entry entry : FactionQuestCatalogService.builtin().chronicleEvents().values()) {
            if (ChronicleTradeSoftService.hasDiscovered(player, entry.id())) {
                out.add(entry.id().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    private static List<String> timelineIds(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        for (TimelineChronicleService.TimelinePhase phase : TimelineChronicleService.builtin().phases()) {
            if (TimelineChronicleService.hasPhase(player, phase.phase())) {
                out.add(phase.phase().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }
}
